package it.naturtalent.s8scanning;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

public class CameraViewModel  extends ViewModel
{

    public void startCameraService(Context applicationContexct)
    {
        applicationContexct.startService(new Intent(applicationContexct, Camera.Camera2Service.class));
    }


    @Override
    protected void onCleared()
    {
        super.onCleared();
    }
}
