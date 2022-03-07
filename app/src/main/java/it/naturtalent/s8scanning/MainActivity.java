package it.naturtalent.s8scanning;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.rtt.RangingRequest;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements DownloadCallback
{

    @SuppressLint("StaticFieldLeak")
    public static Context context;

    // Reference to the TextView showing fetched data, so we can clear it with a button
    // as necessary.
    private TextView mDataText;

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;

    // Boolean telling us whether a download is in progress, so we don't trigger overlapping
    // downloads with consecutive button clicks.
    private boolean mDownloading = false;

    private WifiManager wifiManager;


    private static final String TAG = "Info";

    private Camera.Camera2Service camera;
    SurfaceView surfaceView;
    private Surface surface;

    // LocalHotspot Variable
    WifiConfiguration currentConfig;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Toolbar erzeugen und ActionBar ersetzen (Titel aus AndroidManifest)
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // beweglichen Aktionsbutten adressieren und einen Listener zuordnen
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setTooltipText("maueller Schnappschuss");
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                /*
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    // Genehmigung holen
                    Intent startIntent = new Intent(context, CamerPermissionRequestActivity.class);
                    startActivity(startIntent);
                    return;
                }
*/

                //Camera.Camera2Service camera =  new Camera.Camera2Service(MainActivity.this, surface);
                //camera.readyCamera();
                //camera.checkCharacteristics();

                camera.checkCharacteristics();

                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Textview zur Anzeige der fetched-Daten
        mDataText = (TextView) findViewById(R.id.data_text);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "http://192.168.178.120");


        //Camera camera = new Camera();
        //camera.forceScanAllCameras(this);

        /*
        Camera.Camera2Service camera =  new Camera.Camera2Service(this);
        String checkCamera = camera.getCamera((CameraManager) this.getSystemService(Context.CAMERA_SERVICE));
        camera.readyCamera();

         */

                // existiert ein Netzwerkzugang
        //boolean netCheck = isNetworkAvailable();


        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback()
        {
            /**
             * Callback unmittelbar nach der Erzeugung des Surfaces
             * @param surfaceHolder
             */
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder)
            {
                surface = surfaceHolder.getSurface();

                Log.e(TAG, "TEST");
            }

            /**
             * Callback unmittelbar nach strukturellen Aenderungen des Surfaces
             * (geeigneter Zeitpunkt zu Starten der Kamera)
             *
             * @param surfaceHolder
             * @param i
             * @param i1
             * @param i2
             */
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2)
            {
                camera =  new Camera.Camera2Service(MainActivity.this, surface);
                camera.readyCamera();
            }

            /**
             *  Callback unmitelbar bevor das Surface zerstoerd wird.
             * @param surfaceHolder
             */
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder)
            {
                camera.cameraDevice.close();
            }
        });

        // context modulglobal machen
        context = getApplicationContext();

        // Local Hotspot Test (momentan nicht unterstützt)
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume()");
        super.onResume();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

    }

    /*
       Das Hauptmenue zur Toolbar hinzufuegen
       in 'res' menu werden die Toolbar - Aktion definiert
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /*
       Toolbar - Aktionen abgefangen und via 'key' und
       FragmentResultListener an andere Fragmente gemeldet.
    */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        // wird momentan nicht genutzt
        // mit dem Fragmentmanager und abhaengig won der Selektion werden 'keys' gesendet, die in
        // den Fragmenten mit dem FragmentResultListener abgefragt werden koennen
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        switch (item.getItemId())
        {
            case R.id.fetch_action:
                startDownload();
                break;

            case R.id.clear_action:
                finishDownloading();
                mDataText.setText("");
                return true;

            case R.id.action_connections:

                turnOnHotspot();

                /*
                if (mLocationPermissionApproved) {
                    logToUi(getString(R.string.retrieving_access_points));
                    wifiManager.startScan();

                } else {
                    logToUi("start Permission");
                    // On 23+ (M+) devices, fine location permission not granted. Request permission.
                    Intent startIntent = new Intent(this, LocationPermissionRequestActivity.class);
                    startActivity(startIntent);
                }

                 */


             //fragments.get(0).getChildFragmentManager().setFragmentResult("storeSocketKey", new Bundle());
             break;
        }



        return super.onOptionsItemSelected(item);
    }


    private void startDownload() {
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            mNetworkFragment.startDownload();
            mDownloading = true;
        }
    }

    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            mDataText.setText(result);
        } else {
            mDataText.setText(getString(R.string.connection_error));
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch(progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                break;
            case Progress.CONNECT_SUCCESS:
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                mDataText.setText("" + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }



    /*

                HotSpot Test


     */



    //
    // prueft Netzwerkzugang
    //
    public boolean isNetworkAvailable()
    {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        // if no network is available networkInfo will be null
        // otherwise check if we are connected
        if (networkInfo != null && networkInfo.isConnected())
        {
            return true;
        }
        return false;
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOnHotspot()
    {


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Genehmigung holen
            Intent startIntent = new Intent(this, LocationPermissionRequestActivity.class);
            startActivity(startIntent);


            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)


            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }



        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback()
        {

            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation)
            {
                super.onStarted(reservation);
                hotspotReservation = reservation;
                currentConfig = hotspotReservation.getWifiConfiguration();

                Log.v("DANG", "THE PASSWORD IS: "
                        + currentConfig.preSharedKey
                        + " \n SSID is : "
                        + currentConfig.SSID);

                //hotspotDetaisDialog();

            }

            @Override
            public void onStopped()
            {
                super.onStopped();
                Log.v("DANG", "Local Hotspot Stopped");
            }

            @Override
            public void onFailed(int reason)
            {
                super.onFailed(reason);
                Log.v("DANG", "Local Hotspot failed to start");
            }
        }, new Handler());
    }

    private void turnOffHotspot() {
       // active = false;
        if (hotspotReservation != null) {
            hotspotReservation.close();
            System.out.println("CLOSE HOTSPOT");
        }
    }

    private void hotspotDetaisDialog()
    {

        Log.v(TAG, context.getString(R.string.hotspot_details_message) + "\n" + context.getString(
                R.string.hotspot_ssid_label) + " " + currentConfig.SSID + "\n" + context.getString(
                R.string.hotspot_pass_label) + " " + currentConfig.preSharedKey);

    }

    protected SurfaceHolder.Callback surfaceViewHolderCallback = new SurfaceHolder.Callback()
    {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder)
        {
            Log.e(TAG, "TEST");
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2)
        {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder)
        {

        }
    };



}