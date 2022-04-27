package Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.naturtalent.s8scanning.Camera;
import it.naturtalent.s8scanning.R;
import it.naturtalent.s8scanning.Zoom;

public class ZoomDialog  extends Dialog
{

    private SeekBar zoomBar;
    private TextView tvProgressLabel;
    private float zoomFactor = Zoom.DEFAULT_ZOOM_FACTOR;
    private static final String zoomText = "Zoom: ";


    public interface ZoomDialogListener {
        public void ready(float zoomValue);
        public void cancelled();
    }

    private ZoomDialogListener mReadyListener;

    public ZoomDialog(@NonNull Context context, ZoomDialogListener zoomDialogListener)
    {
        super(context);
        mReadyListener = zoomDialogListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progressbar);
        tvProgressLabel = findViewById(R.id.textView);


        zoomBar = (SeekBar) findViewById (R.id.zoomBar);
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                //Log.e("ZoomDialog", "progress: "+progress);
                zoomFactor = 10+progress;
                zoomFactor = (float) (zoomFactor/10.0);
                tvProgressLabel.setText(zoomText + zoomFactor);

                Camera.Camera2Service.zoomFaktor = zoomFactor;
                Camera.Camera2Service.createViewerCaptureRequest();
                try
                {
                    Camera.Camera2Service.session.setRepeatingRequest(Camera.Camera2Service.createViewerCaptureRequest(), null, null);
                } catch (CameraAccessException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                mReadyListener.ready(zoomFactor);
            }
        });

        int progress = zoomBar.getProgress();
        tvProgressLabel = findViewById(R.id.textView);
        tvProgressLabel.setText(zoomText + zoomFactor);
    }
}
