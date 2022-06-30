package Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.security.Policy;
import java.util.List;

import it.naturtalent.s8scanning.Camera;
import it.naturtalent.s8scanning.MainActivity;
import it.naturtalent.s8scanning.R;

public class ExposureDialog extends Dialog
{

    private TextView seekbarLabel;
    private static final String seekbarText = "Belichtung: ";

    private int curProgress = 0;


    public ExposureDialog(@NonNull Context context)
    {
        super(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.exposure_seekbar);
        seekbarLabel = findViewById(R.id.textView);
        seekbarLabel.setText(seekbarText);
        // kein Dialoghintergrunddimming
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        getWindow().setAttributes(lp);




        // ProgressBar adressieren
        SeekBar seekBar = (SeekBar) findViewById(R.id.exposureSeekBar);
        //seekBar.setProgress(Camera.Camera2Service.getExposureProgress());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {

                try
                {
                    if(progress < curProgress)
                    {
                        curProgress = progress;
                        expoMinus();
                    }
                    else
                        if(progress > curProgress)
                        {
                            curProgress = progress;
                            expoPlus();
                        }




                    double exposureAtISO100=Camera.Camera2Service.mExposureTimesISO/100.0;

                    Range<Integer> range1 = Camera.Camera2Service.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                    int minExposure = range1.getLower();
                    int maxExposure = range1.getUpper();



                    /*

                    //CaptureRequest.Builder build = Camera.Camera2Service.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    CaptureRequest.Builder build = Camera.Camera2Service.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    SurfaceView surfaceView = (SurfaceView) MainActivity.activity.findViewById(R.id.surfaceView);
                    build.addTarget(surfaceView.getHolder().getSurface());

                    build.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    build.set(CaptureRequest.CONTROL_AE_LOCK, true);

                    build.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,progress);
                    CaptureRequest  captureRequest = build.build();
                    Camera.Camera2Service.session.setRepeatingRequest(captureRequest, null, null);

                     */




                    /*
                    build.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
                    build.set(CaptureRequest.SENSOR_SENSITIVITY,100);
                    build.set(CaptureRequest.SENSOR_EXPOSURE_TIME,(long)exposureAtISO100);



                    Log.e("Explosure Trecking", "ExposureTimesISO: "+exposureAtISO100);

                     */


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

            private void expoMinus()
            {

                int minValue = getMinExposureCompensation();

                int step = 1;

                int iEv = curProgress - step;
                if (iEv < 0)
                    iEv = 0;

                CaptureRequest.Builder previewBuilder = Camera.Camera2Service.getPreViewBuilder();
                previewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (iEv + minValue));
                Camera.Camera2Service.setPreViewRepeatingRequest(previewBuilder);

                //CameraController.setCameraExposureCompensation(iEv + minValue);
                //evBar.setProgress(iEv);

                Log.e("Exposure", "expoMinus: "+ curProgress + "   iEv: "+iEv);
            }

            public void expoPlus()
            {
                int minValue = getMinExposureCompensation();
                int maxValue = getMaxExposureCompensation();

                int step = 1;

                int iEv = curProgress + step;
                if (iEv > maxValue - minValue)
                    iEv = maxValue - minValue;

                CaptureRequest.Builder previewBuilder = Camera.Camera2Service.getPreViewBuilder();
                previewBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (iEv + minValue));
                Camera.Camera2Service.setPreViewRepeatingRequest(previewBuilder);

                Log.e("Exposure", "expoPlus: "+ curProgress + "   iEv: "+iEv);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
               // mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            }


            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                Log.e("Explosure", "Ende Belichtungsdialog");
            }
        });


    }

    private static int getMinExposureCompensation()
    {
        Range<Integer> range1 = Camera.Camera2Service.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        return range1.getLower();
    }

    private static int getMaxExposureCompensation()
    {
        Range<Integer> range1 = Camera.Camera2Service.characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        return range1.getUpper();
    }

}