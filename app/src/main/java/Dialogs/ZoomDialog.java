package Dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import it.naturtalent.s8scanning.Camera;
import it.naturtalent.s8scanning.R;
import it.naturtalent.s8scanning.Zoom;

public class ZoomDialog  extends Dialog
{

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

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.progressbar);
        tvProgressLabel = findViewById(R.id.textView);
        zoomFactor = Camera.Camera2Service.zoomFactor;
        tvProgressLabel.setText(zoomText + zoomFactor);

        // kein Dialoghintergrunddimming
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = 0.0f; // Dim level. 0.0 - no dim, 1.0 - completely opaque
        getWindow().setAttributes(lp);

        // ZoomProgressBar einbinden
        SeekBar zoomBar = (SeekBar) findViewById(R.id.zoomBar);
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {

                //Log.e("ZoomDialog", "progress: "+progress);
                zoomFactor = 10+progress;
                zoomFactor = (float) (zoomFactor/10.0);
                tvProgressLabel.setText(zoomText + zoomFactor);

                Camera.Camera2Service.zoomFactor = zoomFactor;
                try
                {
                    CaptureRequest.Builder previewBuilder = Camera.Camera2Service.getPreViewBuilder();
                    Camera.Camera2Service.setPreViewRepeatingRequest(previewBuilder);
                    //Camera.Camera2Service.session.setRepeatingRequest(Camera.Camera2Service.createViewerCaptureRequest(), null, null);
                } catch (Exception e)
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
    }
}
