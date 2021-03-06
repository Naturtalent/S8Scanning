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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
        public static CameraDevice cameraDevice;
        public static CameraCaptureSession session;
        public static ImageReader imageReader;
        protected SurfaceHolder surfaceHolder;

        //protected Activity activity;
        public static CameraCharacteristics characteristics;

        public static AutoFitSurfaceView surfaceView;
        private static Surface viewSurface;

        public static Zoom zoom;
        //public static float zoomFaktor = 1;  // (1-4)
        public static float zoomFactor = Zoom.DEFAULT_ZOOM_FACTOR;

        public static Camera2Service camera2Service;
        private int mTotalRotation;

        private File mImageFolder;
        private String mCameraId;
        private String mImageFileName;

        private Size mPreviewSize;
        public static double mExposureTimesISO;

        //public static CaptureRequest.Builder preViewBuilder;

        //public static int curExposureProgress;


        private static SparseIntArray ORIENTATIONS = new SparseIntArray();
        static {
            ORIENTATIONS.append(Surface.ROTATION_0, 0);
            ORIENTATIONS.append(Surface.ROTATION_90, 90);
            ORIENTATIONS.append(Surface.ROTATION_180, 180);
            ORIENTATIONS.append(Surface.ROTATION_270, 270);
        }

        private static class CompareSizeByArea implements Comparator<Size>
        {
            @Override
            public int compare(Size lhs, Size rhs) {
                return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                        (long)(rhs.getWidth() * rhs.getHeight()));
            }
        }


/*
        private TextureView mTextureView;
        private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setupCamera(width, height);
                //connectCamera();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };
 */

        /**
         *  Konstruktion des Service
         *
         *  (wird aufgerufen durch
         *   'getApplicationContext().startService(new Intent(MainActivity.this, Camera.Camera2Service.class));'
         *   im 'onCreate()-Callback von MainActivity)
         */
        public Camera2Service()
        {
                camera2Service = this;

                surfaceView = (AutoFitSurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
                viewSurface = surfaceView.getHolder().getSurface();

                //AutoFitSurfaceView autoFitSurfaceView = (AutoFitSurfaceView)surfaceView;
                // surfaceView.setAspectRatio(4,3);
                // surfaceView.setAspectRatio(1,1);
                surfaceView.setAspectRatio(16,9);


            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback()
            {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder)
                {

                    //Log.e(TAG, "SurfaceHolder created - openCamera");
                    //setupCamera(surfaceView.width, surfaceView.height);
                    //readyCamera();

                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
                {
                    Log.e(TAG, "SurfaceHolder changed");

                    //setupCamera(surfaceView.width, surfaceView.height);
                    setupCamera(width, height);
                    //readyCamera();
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder)
                {
                    Log.e(TAG, "SurfaceHolder destroyed");
                    //cameraDevice.close();
                }
            });


            /*
            surfaceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
            {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
                {
                    AutoFitSurfaceView autoFitSurfaceView = (AutoFitSurfaceView)surfaceView;
                    setupCamera(autoFitSurfaceView.width, autoFitSurfaceView.height);
                    //readyCamera();
                    Log.d(TAG, "LayoutChange");
                }
            });

             */



                //AutoFitSurfaceView autoFitSurfaceView = (AutoFitSurfaceView)surfaceView;
                //autoFitSurfaceView.setAspectRatio(4,3);

                readyCamera();

                /*
                surfaceView.getHolder().addCallback(new SurfaceHolder.Callback()
                {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder)
                {
                    Log.d(TAG, "Surface created-111");
                    //readyCamera();

                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height)
                {

                    Log.d(TAG, "Surface  changed");
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder)
                {
                    Log.d(TAG, "Surface destroyed-125");
                    if(cameraDevice != null)
                    {
                        Log.d(TAG, "Surface destroyed- camera closes- 129");
                        //cameraDevice.close();
                        //session.close();
                    }



                }
                });


                 */



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



        // eine Camera StateCallback Instanz erzeugen
        // diese Variable wird der Funktion Camera Manager#openCamera (readyCamera()) uebergeben und gibt mittels ihrer
        // Callbackfunktionen Auskunft ueber den aktuellen Status der Cameraverbindung

        protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback()
        {

            @Override
            public void onOpened(@NonNull CameraDevice camera)
            {
                Log.e(TAG, "CameraDevice.StateCallback onOpened");
                cameraDevice = camera;
                actOnReadyCameraDevice();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera)
            {
                cameraDevice.close();
                Log.e(TAG, "CameraDevice.StateCallback disconnected");
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error)
            {
                cameraDevice.close();
                cameraDevice = null;
                Log.e(TAG, "CameraDevice.StateCallback onError " + error);
            }


        };





        /**
         *  Eine CameraCaptureSession beschreibt alle Pipelines, die an ein CameraDevice gebunden sind und verwaltet einen Queue von
         *  CaptureRequests. Diese CaptureRequests sind die aktuelle Konfiguration.
         */
        protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback()
        {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onReady(CameraCaptureSession session)
            {
                Camera2Service.this.session = session;
                try
                {
                    // ein Repeating - CaptureRequest erzeugen und an die Camera senden
                    //session.setRepeatingRequest(createCaptureRequest(), null, null);

                    // session.abortCaptures();
                    // session.setRepeatingRequest(createViewerCaptureRequest(), null, null);
                    //session.setRepeatingRequest(createViewerCaptureRequest(), sessionCaptureCallback, null);
                    session.setRepeatingRequest(getPreViewBuilder().build(), sessionCaptureCallback, null);


                    cameraCaptureStartTime = System.currentTimeMillis();
                    //session.capture(createImageCaptureRequest(), null, null);

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

        protected CameraCaptureSession.CaptureCallback sessionCaptureCallback = new CameraCaptureSession.CaptureCallback()
        {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result)
            {
                super.onCaptureCompleted(session, request, result);
                mExposureTimesISO = (double) (result.get(TotalCaptureResult.SENSOR_SENSITIVITY) * result.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME));
            }
        };

        /**
         *  Einen 'onImageAvailableListener' erstellen.
         *  Der Listener wird aufgerufen, wenn ein neues Image verf??gbar ist.
         *  In der 'readyCamera() - Funktion wird dieser Listener dem ImageReader zugeordnet
         */
        protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener()
        {
            @Override
            public void onImageAvailable(ImageReader reader)
            {
                // acquireLatestImage() - Das neueste Bild aus der Warteschlange des ImageReaders wird ??bernommen,
                // ??ltere Bilder werden verworfen.
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
                if (ActivityCompat.checkSelfPermission(MainActivity.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.activity, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                    return;
                }

                // Id der gewaehlte Kamera (front/back) ermitteln
                String pickedCamera = getCamera(manager);

                // eine Kamera oeffnen
                manager.openCamera(pickedCamera, cameraStateCallback, null);

                ///////tests
                StreamConfigurationMap configsMap = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

               // SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
               // AutoFitSurfaceView autoFitSurfaceView = (AutoFitSurfaceView)surfaceView;
               // Log.e(TAG, "width: "+autoFitSurfaceView.width+"        height:"+autoFitSurfaceView.height);


/*
                WindowManager windowManager = (WindowManager)MainActivity.context.getSystemService(Context.WINDOW_SERVICE);
                int deviceOrientation = 0;
                if(windowManager != null)
                    deviceOrientation = windowManager.getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

 */



               // int[] formats = configsMap.getOutputFormats();
               // Size[]sizes = configsMap.getOutputSizes(formats[0]);

                //Size[]sizesSurfaceView = configsMap.getOutputSizes(SurfaceView.class);

                /*
                SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) surfaceView.getLayoutParams();
                //Object lo = surfaceView.getLayoutParams();
                lp.width = 648;
                surfaceView.setLayoutParams(lp);


                sizes = configsMap.getOutputSizes(formats[1]);
                sizes = configsMap.getOutputSizes(formats[2]);
                int format = formats[2];
                Size size = configsMap.getOutputSizes(format)[6];

                 */
                ///////


                // imageReader - ein 'SpeicherSurface' zur aufnahme der aufgenommenen Images
                imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2 /* images buffered */);

                // Listener wird aufgerufen, wenn eine Aufnahme (Image) zur Verfuegung steht
                imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
                Log.d(TAG, "readyCamera - imageReader created");

                // eine Zoomklasse generieren, ueber die das Zoomen gesteuert wird
                zoom = new Zoom(characteristics);

            } catch (CameraAccessException e)
            {
                Log.e(TAG, "Line 276 " + e.getMessage());
            }
        }

        private void setupCamera(int width, int height) {

            CameraManager manager = (CameraManager) MainActivity.context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
            mCameraId = getCamera(manager);

            /*
            SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
            int height = surfaceView.getHeight();
            int width = surfaceView.getWidth();



            AutoFitSurfaceView autoFitSurfaceView = (AutoFitSurfaceView)surfaceView;
            Log.e(TAG, "width: "+autoFitSurfaceView.width+"        height:"+autoFitSurfaceView.height);

             */

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int deviceOrientation = MainActivity.activity.getWindowManager().getDefaultDisplay().getRotation();
            mTotalRotation = sensorToDeviceRotation(characteristics, deviceOrientation);
            boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
            int rotatedWidth = width;
            int rotatedHeight = height;
            if(swapRotation) {
                rotatedWidth = height;
                rotatedHeight = width;
            }

            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);

            Log.e(TAG, "rotation:"+swapRotation+"  mPrevW/mPrevH: "+mPreviewSize.getWidth()+"/"+mPreviewSize.getHeight() + "width/height:"+width+"/"+height);
        }


        private static Size chooseOptimalSize(Size[] choices, int width, int height) {
            List<Size> bigEnough = new ArrayList<Size>();
            for(Size option : choices) {
                if(option.getHeight() == option.getWidth() * height / width &&
                        option.getWidth() >= width && option.getHeight() >= height) {
                    bigEnough.add(option);
                }
            }
            if(bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizeByArea());
            } else {
                return choices[0];
            }
        }

        private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
            int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            deviceOrientation = ORIENTATIONS.get(deviceOrientation);
            return (sensorOrienatation + deviceOrientation + 360) % 360;
        }

        private void openCamera()
        {
            readyCamera();
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

            //readyCamera();

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
                Log.e(TAG, "Line 352 "+e.getMessage());
            }
        }

        /**
         * Erinnerung: Ueberschreibt onDestroy() der Klasse Service
         */
        @Override
        public void onDestroy()
        {


            Log.e(TAG, "Camera Destroy");
            cameraDevice.close();

            /*
            try
            {
                Log.e(TAG, "Service beenden");
                session.abortCaptures();
                viewSurface.release();
                imageReader.close();
                cameraDevice.close();
                Log.e(TAG, "Service beenden 2");
            } catch (CameraAccessException e)
            {
                Log.e(TAG, "Line 367 "+e.getMessage());
            }
            //session.close();

             */
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


            File file = createImageFileName();
            Log.e(TAG, "Create ImageFolder: "+mImageFolder);

            //String fileName = "/image"+Integer.valueOf(MainActivity.snapshotCounter).toString()+".jpg";
            //File file  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + fileName);
            //File file  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/image.jpg");


            Log.e(TAG, "Save Image: "+file.toString());

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


        private File createImageFileName() {
            //String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            //String prepend = "IMAGE_" + timestamp + "_";

            String prepend = "IMAGE_" + Integer.valueOf(MainActivity.snapshotCounter).toString()+".jpg";

            File imageFile = new File(createImageFolder(), prepend);
            mImageFileName = imageFile.getAbsolutePath();
            return imageFile;
        }

        private File createImageFolder() {
            File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            mImageFolder = new File(imageFile, "S8VideoImage");
            if(!mImageFolder.exists()) {
                mImageFolder.mkdirs();
            }
            return mImageFolder;
        }

        /**
         * einen Schappschuss ausloesen
         */
        public void cameraSnapShot()
        {

            CaptureRequest.Builder imageBuilder = getImageCaptureBuilder();
            setImageRequest(imageBuilder);


            /*
            try
            {
                CaptureRequest request = createImageCaptureRequest();
                Log.e(TAG, "SnapShot1");

                session.capture(request, null, null);

                //session.setRepeatingRequest(createSnapShotCaptureRequest(), null, null);

                //session.stopRepeating();

            } catch (CameraAccessException e)
            {
                Log.e(TAG, "SnapShotError");
                e.printStackTrace();
            }

             */



        }


        /**
         *
         */
        /*
        //@RequiresApi(api = Build.VERSION_CODES.Q)
        public void cameraSnapShot()
        {
            try
            {
                //session.stopRepeating();

                CaptureRequest request = createImageCaptureRequest();
                Log.e(TAG, "SnapShot1");

                session.capture(request, null, null);

                //session.setRepeatingRequest(createSnapShotCaptureRequest(), null, null);

                //session.stopRepeating();

            } catch (CameraAccessException e)
            {
                Log.e(TAG, "SnapShotError");
                e.printStackTrace();
            }
        }

         */

        /*
        public void cameraSnapShot()
        {
            try
            {
                //session.stopRepeating();

                session.setRepeatingRequest(createViewerCaptureRequest(), null, null);

                CaptureRequest request = createImageCaptureRequest();
                Log.e(TAG, "SnapShot1");

                session.setRepeatingRequest(request, null, null);

                //session.setRepeatingRequest(createSnapShotCaptureRequest(), null, null);

                session.stopRepeating();

            } catch (CameraAccessException e)
            {
                Log.e(TAG, "SnapShotError");
                e.printStackTrace();
            }
        }

         */


        /**
         *  Anforderungen an die Camera erzeugen damit diese Images an die verwendeten Puffer senden kann. Es koennen nur die Puffer verwendet werden die
         *  in der CaptureSession - Erzeugung definiert wurden.
         *
         * @return CaptureRequest zur Anforderung des Images
         */
        @RequiresApi(api = Build.VERSION_CODES.P)
        public static CaptureRequest createViewerCaptureRequest()
        {
            try
            {
                // eine vordefinierte Schablone beutzen
                CaptureRequest.Builder preViewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // ViewSurface - Puffer hinzufuegen
                SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);

                preViewBuilder.addTarget(surfaceView.getHolder().getSurface());

                //preViewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                //preViewBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true);



                return preViewBuilder.build();
            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
                return null;
            }

        }

        /**
         * Einen Builder fuer den VorschauView generieren
         *
         * @return
         */
        public static CaptureRequest.Builder getPreViewBuilder()
        {
            try
            {
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // ViewSurface - Puffer hinzufuegen
                SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);

                builder.addTarget(surfaceView.getHolder().getSurface());

                // Parameter des Buildes setzen
                setBuilderSettings(builder);

                builder.set(CaptureRequest.CONTROL_AE_LOCK, true);

                return builder;

            } catch (CameraAccessException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        public static void setPreViewRepeatingRequest(CaptureRequest.Builder previewBuilder)
        {
            CaptureRequest  captureRequest = previewBuilder.build();
            try
            {
                session.stopRepeating();
                session.setRepeatingRequest(captureRequest, null, null);
            } catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        /**
         *  setzt die Parameter der beiden Builder (preview und image)
         */
        private static void setBuilderSettings(CaptureRequest.Builder builder)
        {
            zoom.setZoom(builder, zoomFactor); // see createSnapShotCaptureRequest()
        }

        /**
         * Einen Builder fuer den ImageRequest generieren
         *
         * @return
         */
        public static CaptureRequest.Builder getImageCaptureBuilder()
        {
            try
            {
                // eine verdefinierte Schablone beutzen
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);

                // ImageReader - Puffer hinzufuegen
                builder.addTarget(imageReader.getSurface());

                // Parameter des Buildes setzen
                setBuilderSettings(builder);

                builder.set(CaptureRequest.CONTROL_AE_LOCK, true);

                return builder;

            } catch (CameraAccessException e)
            {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        public static void setImageRequest(CaptureRequest.Builder imageBuilder)
        {
            CaptureRequest  captureRequest = imageBuilder.build();
            try
            {
                //session.stopRepeating();
                session.capture(captureRequest, null, null);
            } catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        public static CaptureRequest createViewerCaptureRequestORG()
        {
                try
                {
                    // eine vordefinierte Schablone beutzen
                    CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                    // ImageReader - Puffer hinzufuegen
                    //builder.addTarget(imageReader.getSurface());

                    // ViewSurface - Puffer hinzufuegen
                    SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);



                    /*
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) surfaceView.getLayoutParams();
                    lp.width = mPreviewSize.getWidth();
                    lp.height = mPreviewSize.getHeight();
                    surfaceView.setLayoutParams(lp);

                     */

                    //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
                    //builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                    //builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                    //builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, .2f);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1000000L);

                    //builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);


                    builder.addTarget(surfaceView.getHolder().getSurface());





                    // bestes ergebnis bisher

                    builder.set(CaptureRequest.CONTROL_AE_LOCK, true);

                    //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 50000000L);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 60000000L);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 90000000L);

                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 400000000L);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 6000000000L);
                    //builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1000000000L);


                    // 0,3 - 10.0
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
                    float yourMinFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                    float yourMaxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
                    builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);
                    //builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.8f);
                    //builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.8f);
                    //builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 1.0f);

                    //builder.set(CaptureRequest.CONTROL_AE_LOCK, true);
                    ////builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);




                    // ein CaptureRequest-Feld auf einen Wert setzen (gueltige Definitionen @see CaptureRequest)
                    // builder.setPhysicalCameraKey(Key<T>, T value, cameraID)
                    // Key see CaptureRequest (Fields) - Value see CameraCharacteristics

                    //builder.setPhysicalCameraKey(CaptureRequest.CONTROL_MODE, CameraCharacteristics.CONTROL_MODE_OFF, cameraDevice.getId());

                    // Autobelichtung AE (auto-exposure) ausschalten
                    //builder.setPhysicalCameraKey(CaptureRequest.CONTROL_AE_MODE, CameraCharacteristics.CONTROL_AE_MODE_OFF, cameraDevice.getId());

                    // Autofokus AF ausschalten
                    //builder.setPhysicalCameraKey(CaptureRequest.CONTROL_AF_MODE, CameraCharacteristics.CONTROL_AF_MODE_OFF, cameraDevice.getId());

                    //zoom.setZoom(builder, zoomFaktor); // see createSnapShotCaptureRequest()



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
        private CaptureRequest createImageCaptureRequest()
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

                //zoom.setZoom(builder,zoomFaktor);

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

        /*
        public static void setExposureProgress(int progress)
        {
            curExposureProgress = progress;
        }

        public static int getExposureProgress()
        {
            return curExposureProgress;
        }

         */



        public static boolean isExposureCompensationSupportedCamera2()
        {
            Range<Integer> expRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                return expRange.getLower() == expRange.getUpper() ? false : true;
        }

        private static int getMinExposureCompensation()
        {
            Range<Integer> range1 = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            return range1.getLower();
        }

        private static int getMaxExposureCompensation()
        {
            Range<Integer> range1 = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            return range1.getUpper();
        }



    }


}
