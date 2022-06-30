package Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.naturtalent.s8scanning.Camera;
import it.naturtalent.s8scanning.R;
import it.naturtalent.s8scanning.Zoom;

public class FocusDialog   extends Dialog
{

    private TextView seekbarLabel;
    private static final String seekbarText = "Focus: ";
    private static float distanceFactor = 0.0f;



    public FocusDialog(@NonNull Context context)
    {
        super(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progressbar);
        seekbarLabel = findViewById(R.id.textView);
        seekbarLabel.setText(seekbarText + distanceFactor);

        // ProgressBar adressieren
        SeekBar seekBar = (SeekBar) findViewById(R.id.zoomBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {

                try
                {

                    Camera.Camera2Service.session.stopRepeating();
                    Camera.Camera2Service.session.setRepeatingRequest(createSetFocusRequest(progress), null, null);

                    //distanceFactor = progress/3.0f;

                    //zoomFactor = 10+progress;
                    //zoomFactor = (float) (zoomFactor/10.0);

                    seekbarLabel.setText(seekbarText + distanceFactor);

                    //CaptureRequest.Builder builder = Camera.Camera2Service.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    //builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, distanceFactor);
                    //builder.build();

                    /*
                    double exposureAtISO100= Camera.Camera2Service.mExposureTimesISO/100.0;
                    CaptureRequest.Builder build = Camera.Camera2Service.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    build.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
                    build.set(CaptureRequest.SENSOR_SENSITIVITY,100);
                    build.set(CaptureRequest.SENSOR_EXPOSURE_TIME,(long)exposureAtISO100);

                    seekbarLabel.setText(seekbarText + zoomFactor);

                     */

                    //Log.e("Explosure Trecking", "ExposureTimesISO: "+exposureAtISO100);


                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                /*
                float minimumLens = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                float num = (((float)progress) * minimumLens / 100);
                mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num);

                 */
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
               // mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });


    }

    public static CaptureRequest createSetFocusRequest(int progress) throws CameraAccessException
    {
        distanceFactor = progress/3.0f;

        CaptureRequest.Builder builder = Camera.Camera2Service.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
       // builder.addTarget(surfaceView.getHolder().getSurface());
        builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, distanceFactor);
        return builder.build();
    }
}