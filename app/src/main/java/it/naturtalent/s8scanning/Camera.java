package it.naturtalent.s8scanning;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Camera
{
    /*
        die vorhandenen Geraete checken
     */
    public void forceScanAllCameras(Activity activity)
    {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            String[] idList = manager.getCameraIdList();

            int maxCameraCnt = idList.length;

            for (int index = 0; index < maxCameraCnt; index++)
            {
                String cameraId = manager.getCameraIdList()[index];
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            }
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    public static class Camera2Service extends Service
    {
        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        protected static final int CAMERA_CALIBRATION_DELAY = 500;
        protected static final String TAG = "myLog";
        protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
        protected static long cameraCaptureStartTime;
        protected CameraDevice cameraDevice;
        public static CameraCaptureSession session;
        public static ImageReader imageReader;
        protected SurfaceHolder surfaceHolder;

        //protected Activity activity;
        protected CameraCharacteristics characteristics;

        private Surface viewSurface;




        public Camera2Service()
        {
            SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
            this.viewSurface = surfaceView.getHolder().getSurface();

        }

        /**
         * Konstruktion
         * @param activity
         * @param surface
         */
        public Camera2Service(Activity activity, Surface surface)
        {
           // this.activity = activity;
            //this.viewSurface = surface;

            //SurfaceView surfaceView = (SurfaceView) activity.findViewById(R.id.surfaceView);
            //Surface test = surfaceView.getHolder().getSurface();
        }

        /**
         * Callbackfunktionen, die Auskunft ueber den aktuellen Status der Cameraverbindung geben
         */
        protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback()
        {
            @Override
            public void onOpened(@NonNull CameraDevice camera)
            {
                Log.d(TAG, "CameraDevice.StateCallback onOpened");
                cameraDevice = camera;
                actOnReadyCameraDevice();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera)
            {
                Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error)
            {
                Log.e(TAG, "CameraDevice.StateCallback onError " + error);
            }
        };



        /**
         *  Eine CameraCaptureSession beschreibt alle Pipelines, die an ein CameraDevice gebunden sind und verwaltet einen Queue von
         *  CaptureRequests. Diese CaptureRequests sind die aktuelle Konfiguration.
         */
        protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback()
        {
            @Override
            public void onReady(CameraCaptureSession session)
            {
                Camera2Service.this.session = session;
                try
                {
                    // ein Repeating - CaptureRequest erzeugen und an die Camera senden
                    //session.setRepeatingRequest(createCaptureRequest(), null, null);

                    session.setRepeatingRequest(createViewerCaptureRequest(), null, null);

                    cameraCaptureStartTime = System.currentTimeMillis();
                    session.capture(createSnapShotCaptureRequest(), null, null);

                    Log.d(TAG, "CameraCaptureSession.StateCallback - onReaddy");

                } catch (CameraAccessException e)
                {
                    Log.e(TAG, "CameraCaptureSession.StateCallback Error: " + e.getMessage());
                }
            }


            @Override
            public void onConfigured(CameraCaptureSession session)
            {

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session)
            {
            }
        };

        /**
         *  Einen 'onImageAvailableListener' erstellen.
         *  Der Listener wird aufgerufen, wenn ein neues Image verfügbar ist.
         *  In der 'setCamera() - Funktion wird dieser Listener dem ImageReader zugeordnet
         */
        protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener()
        {
            @Override
            public void onImageAvailable(ImageReader reader)
            {
                // acquireLatestImage() - Das neueste Bild aus der Warteschlange des ImageReaders wird übernommen,
                // ältere Bilder werden verworfen.
                Log.d(TAG, "onImageAvailable");
                Image img = reader.acquireLatestImage();
                if (img != null)
                {
                    if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY)
                    {
                        processImage(img);
                    }
                    img.close();
                }
            }
        };

        /**
         *  Die Kamera klar machen.
         *  Camera Permission checken und ggf. erteilen
         *  Einen ImageReader erstellen und den oben definierten Listener 'onImageAvailableListener' zuordnen.
         *  Mit dem ImageReader kann auf die im Surface dargestellten Image-Daten zugegriffen werden (um sie z.B. in einer Datei zu speichern)
         *  Der Imagereader wird von dem CameraDevice mit Images versorgt und stellt diese in eine Warteschlange.
         */
        public void readyCamera()
        {
            //CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            CameraManager manager = (CameraManager) MainActivity.context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);

            try
            {
                // Camera Permission checken
                String pickedCamera = getCamera(manager);
                if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.activity, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                    return;
                }

                manager.openCamera(pickedCamera, cameraStateCallback, null);
                imageReader = ImageReader.newInstance(1920, 1088, ImageFormat.JPEG, 2 /* images buffered */);
                imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
                Log.d(TAG, "readyCamera - imageReader created");

            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }

        /**
         *  ID der ausgewaehlten Camera 'CAMERACHOICE' zurueckgeben.
         *  - ueberprueft wird die Characteristik 'LENS_FACING'
         *  - diejenige Camera, die mit CAMERACHOICE uebereinstimmt gilt aus ausgewaehlt
         *
         * @param manager
         * @return ID der ausgewaehlten Kamera
         */
        public String getCamera(CameraManager manager)
        {
            try
            {
                for (String cameraId : manager.getCameraIdList())
                {
                    characteristics = manager.getCameraCharacteristics(cameraId);
                    int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (cOrientation == CAMERACHOICE)
                    {
                        return cameraId;
                    }
                }
            } catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Erinnerung: Ueberschreibt onStartCommand() der Klasse Service
         * wird jedesmal aufgerufen, wenn startService() aufgerufen wird.
         *
         * @param intent
         * @param flags
         * @param startId
         * @return
         */
        @Override
        public int onStartCommand(Intent intent, int flags, int startId)
        {
            Log.d(TAG, "Service - onStartCommand flags " + flags + " startId " + startId);

            readyCamera();

            return super.onStartCommand(intent, flags, startId);
        }

        /**
         * Erinnerung: Ueberschreibt oncreate() der Klasse Service
         * Wird aufgerufen, wenn der Service zum erstenmal aufgerufen wird.
         */
        @Override
        public void onCreate()
        {
            Log.d(TAG, "Service - onCreate");
            super.onCreate();
        }

        /**
         * Aktion wenn DeviceCallback 'onOpened' meldet
         * - eine CaptureSession erzeugen
         * - verwendbaren Output Puffer (Surfaces) definieen und in einer Liste der Session hinzufuegen
         * - einen Sessioncallback 'sessionStateCallback' hinzufuegen
         */
        public void actOnReadyCameraDevice()
        {
            try
            {
                // hier werden die Puffer (ImageReaderSurface und ViewSurface) hinzugefuegt
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface(), viewSurface), sessionStateCallback, null);
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }

        /**
         * Erinnerung: Ueberschreibt onDestroy() der Klasse Service
         */
        @Override
        public void onDestroy()
        {
            try
            {
                session.abortCaptures();
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
            }
            session.close();
        }


        /**
         * Nachdem der ImageReader ein neues Image empfangen hat, wird dies hier weiter verarbeitet.
         *
         * @param image
         */
        private void processImage(Image image)
        {
            //Process image data
            ByteBuffer buffer;
            byte[] bytes;
            boolean success = false;
            //File file = new File(Environment.getExternalStorageDirectory() + "/Pictures/image.jpg");

            File file  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/image.jpg");

            FileOutputStream output = null;

            if (image.getFormat() == ImageFormat.JPEG)
            {
                buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                buffer.get(bytes); // copies image from buffer to byte array
                try
                {
                    output = new FileOutputStream(file);
                    output.write(bytes);    // write the byte array to file
                    //j++;
                    success = true;
                } catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                } finally
                {
                    image.close(); // close this to free up buffer for other images
                    if (null != output)
                    {
                        try
                        {
                            output.close();
                        } catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }

        /**
         *  Anforderungen an die Camera erzeugen damit diese Images an die verwendeten Puffer senden kann. Es koennen nur die Puffer verwendet werden die
         *  in der CaptureSession - Erzeugung definiert wurden.
         *
         * @return CaptureRequest zur Anforderung des Images
         */
        protected CaptureRequest createViewerCaptureRequest()
        {
            try
            {
                // eine verdefinierte Schablone beutzen
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                // ImageReader - Puffer hinzufuegen
                //builder.addTarget(imageReader.getSurface());

                // ViewSurface - Puffer hinzufuegen
                SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
                builder.addTarget(surfaceView.getHolder().getSurface());

                // ein CaptureRequest-Feld auf einen Wert setzen (gueltige Definitionen @see CaptureRequest)
                //builder.setPhysicalCameraKey(Key<T>, T value, cameraID)

                return builder.build();
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }


        /**
         *  Anforderungen an die Camera erzeugen damit diese Images an die verwendeten Puffer senden kann. Es koennen nur die Puffer verwendet werden die
         *  in der CaptureSession - Erzeugung definiert wurden.
         *
         * @return CaptureRequest zur Anforderung des Images
         */
        public CaptureRequest createSnapShotCaptureRequest()
        {
            try
            {
                // eine verdefinierte Schablone beutzen
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);

                // ImageReader - Puffer hinzufuegen
                builder.addTarget(imageReader.getSurface());

                // ViewSurface - Puffer hinzufuegen
                //Log.e(TAG, "ViewSurfcae check: "+viewSurface);
                //builder.addTarget(viewSurface);

                // ein CaptureRequest-Feld auf einen Wert setzen (gueltige Definitionen @see CaptureRequest)
                //builder.setPhysicalCameraKey(Key<T>, T value, cameraID)

                return builder.build();
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        /**
         *  Anforderungen an die Camera erzeugen damit diese Images an die verwendeten Puffer senden kann. Es koennen nur die Puffer verwendet werden die
         *  in der CaptureSession - Erzeugung definiert wurden.
         *
         * @return CaptureRequest zur Anforderung des Images
         */
        protected CaptureRequest createCaptureRequest()
        {
            try
            {
                // eine verdefinierte Schablone beutzen
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                // ImageReader - Puffer hinzufuegen
                builder.addTarget(imageReader.getSurface());

                // ViewSurface - Puffer hinzufuegen
                builder.addTarget(viewSurface);

                // ein CaptureRequest-Feld auf einen Wert setzen (gueltige Definitionen @see CaptureRequest)
                //builder.setPhysicalCameraKey(Key<T>, T value, cameraID)

                return builder.build();
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }


        @Override
        public IBinder onBind(Intent intent)
        {
            return null;
        }

        /**
         *   TEST
         */
        public void checkCharacteristics()
        {
            int[] cameraCapabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

            int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cOrientation == CameraCharacteristics.LENS_FACING_BACK)
                Log.e(TAG, "Hintergrundkamera");


            //boolean cameraCompatible = cameraCapabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) ? true : false;



            //List<CaptureResult.Key<?>> characteristicsKeys = characteristics.getAvailableCaptureResultKeys();
            //CaptureResult.Key key = characteristicsKeys.get(0);
        }
    }


}
