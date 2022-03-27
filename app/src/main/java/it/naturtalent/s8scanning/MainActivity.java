package it.naturtalent.s8scanning;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.techyourchance.threadposter.UiThreadPoster;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 *
 */

public class MainActivity extends AppCompatActivity  implements DownloadCallback
{

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static Activity activity;

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

    private Camera.Camera2Service cameraService;
    //SurfaceView surfaceView;
    private Surface surface;

    // Camera-Ausloeser Klick
    private MediaPlayer _shootMP;




    private static final String httpMsgCounterPrefix = "Counter: ";
    private boolean fetchDataEnable = false;
    public static int snapshotCounter = 0;


    // LocalHotspot Variable
    WifiConfiguration currentConfig;
    WifiManager.LocalOnlyHotspotReservation hotspotReservation;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onCreate");

        // Energisparmodus abschalten
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Toolbar erzeugen und ActionBar ersetzen (Titel aus AndroidManifest)
        setContentView(R.layout.activity_main);
        //setContentView(R.layout.fragment_first);

        // ViewModel - Variante testen
        final CameraViewModel viewModel = new ViewModelProvider(this).get(CameraViewModel.class);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);




        //
        // beweglichen Aktionsbutten adressieren und einen Listener zuordnen
        //
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setTooltipText("maueller Schnappschuss");
        fab.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View view)
            {
                //snapShot();
                cameraSnapShot();

                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Instanziiert und startet den CameraService



        //Intent intent = new Intent(MainActivity.this, Camera.Camera2Service.class);
        //if(Camera.Camera2Service.camera2Service.viewSurfaceValid)
          //  getApplicationContext().stopService(intent);

         getApplicationContext().startService(new Intent(MainActivity.this, Camera.Camera2Service.class));




        // Textview zur Anzeige der fetched-Daten
        mDataText = (TextView) findViewById(R.id.data_text);
        //mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "http://192.168.178.120");


        //Camera camera = new Camera();
        //camera.forceScanAllCameras(this);

        /*
        Camera.Camera2Service camera =  new Camera.Camera2Service(this);
        String checkCamera = camera.getCamera((CameraManager) this.getSystemService(Context.CAMERA_SERVICE));
        camera.readyCamera();

         */

        // existiert ein Netzwerkzugang
        //boolean netCheck = isNetworkAvailable();




        // context modulglobal machen
        context = getApplicationContext();
        activity = MainActivity.this;

        // Local Hotspot Test (momentan nicht unterstützt)
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        // Instanziiert und startet den CameraService
        // Start der Kamera durch Callback onStartCommand() in Service
        getApplicationContext().startService(new Intent(MainActivity.this, Camera.Camera2Service.class));


        //WifiTools wifiTools = new WifiTools();
        //wifiTools.connect();


    }






    /**
     * Beim Starten der App wird eine Http Verbindung zum S8 Server aufgebaut
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        //doHttpConnection();
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume()");
        super.onResume();

    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause()");
        super.onPause();

    }

    @Override
    protected void onDestroy()
    {
        Log.e(TAG, "onDestroy()");
        super.onDestroy();

    }

    /**
     * Wird nur aufgerufen, wenn im AndroidManifest 'configChangees' definiert ist.
     *
     *  <activity android:name=".MyActivity"
     *             ....
     *           android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
     *           ...
     *  Mit dieser Definition wird verhindert, das bei Konfigurationsaenderungen die
     *  Actifity erneut gestartet wird.
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        Log.e(TAG, "onConfigurationChanged");

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }

        super.onConfigurationChanged(newConfig);
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

    @RequiresApi(api = Build.VERSION_CODES.Q)
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
            case R.id.start_action:
                fetchDataEnable = true;
                doHttpConnection();
                break;

            case R.id.stop_action:
                fetchDataEnable = false;
                finishDownloading();
                mDataText.setText("");
                return true;

            case R.id.start_accesspoint:
                turnOnHotspot();
                break;

            case R.id.stop_accesspoint:
                turnOffHotspot();
                break;

            case R.id.wifi_connection:
                new WifiTools().connect();
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void cameraSnapShot()
    {
        // Ausloeser Sound
        shootSound();

        //Camera.Camera2Service.cameraSnapShot();
        Camera.Camera2Service.camera2Service.cameraSnapShot();
    }


    /**
     *  Die eigenliche Kameraausloeserfunktion
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void snapShot()
    {
        // Ausloeser Sound
        shootSound();

        try
        {
            // eine neue Aufnahmeanforderung erzeugen und an die Kamera senden
            CameraCaptureSession cameraCaptureSession = Camera.Camera2Service.session;
            //cameraCaptureSession.capture(createSnapShotCaptureRequest(cameraCaptureSession), null, null);

            // Aufnahmeanforderung erstellen
            CaptureRequest captureRequest = createSnapShotCaptureRequest(cameraCaptureSession);

           // Integer sceneMode = captureRequest.get(CaptureRequest.CONTROL_SCENE_MODE);
           // Integer distortion = captureRequest.get(CaptureRequest.DISTORTION_CORRECTION_MODE);

            // Aufnahmeanforderung senden

            //cameraCaptureSession.capture(captureRequest, null, null);
            cameraCaptureSession.setRepeatingRequest(captureRequest, null, null);

        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**
     *
     *      Beginn der BackgroundThread (WorkerThread) Variante
     *      (Http Countermessages des Super 8 (S8) WiFi-Moduls (D1 Wemos) kontinuierlich abfragen
     *
     *
     */

    /**
     * Einen Verbindung zum S8 Server aufbauen.
     * Die Aktion wird in einem separaten Thread (WorkerThread) realisiert.
     * Ein Listener informiert ueber das Ergebnis.
     */
    private void doHttpConnection()
    {
        android.util.Log.d("MainActivity", "Fetch Data");

        ConnectionUseCase mConnectionUseCase =  new ThreadPool().getConnectionUseCase();

        // registriert einen Listener (s. unten) der das WorkerThread Ergebnis kolportiert
        mConnectionUseCase.registerListener(connectionListener);

        // Watchdog begrenzt die Dauer bei Verbindungsaufbau (Timeout error Tinkerforge)
        //WatchdogUseCase mWatchdogCase =  new ThreadPool().getWatchdogCase();
        //mWatchdogCase.registerListener(watchdogListener);
        //mWatchdogCase.startTimer(4000);

        // Verbindungsaufbau im separaten Thread
        mConnectionUseCase.connectHttp();

        // Dialog fuer die Dauer des Verbindungsversuchs
        //showConnectDialog();

    }

    /**
     * Listener informiert ueber das Ergebnis des Verbindungsaufbau
     * @see ConnectionUseCase
     */
    private ConnectionUseCase.ConnectionListener connectionListener = new ConnectionUseCase.ConnectionListener()
    {
        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public void onConnectionEstablished(String remoteData)
        {
            if(fetchDataEnable)
            {
                // existiert ein Teilstring 'Counter: '
                int idx = remoteData.indexOf(httpMsgCounterPrefix);
                if(idx >= 0)
                {
                    // String 'Counter: ' uebersprongen
                    String stgCnt = remoteData.substring(httpMsgCounterPrefix.length());
                    int counterVal = new Integer(stgCnt);

                    if(counterVal > snapshotCounter)
                    {
                        snapshotCounter = counterVal;

                        //snapShot();
                        cameraSnapShot();

                        Toast.makeText(MainActivity.this,
                                "Snapshot: "+remoteData, Toast.LENGTH_LONG).show();
                    }
                }

                // erneuten Verbinungsaufbau und Abfrage
                doHttpConnection();
            }
        }

        @Override
        public void onConnectionFailed(String message)
        {
           // MainActivity.this.remoteData = null;

            // Ueber den misslungen Verbindungversuch informieren
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(R.drawable.delete_icon_gray)
                    .setTitle(message)
                    .setCancelable(true)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int whichButton)
                                {
                                }
                            })
                    .create().show();
        }
    };


    /**
     *
     *  Ende BackgroundThread Section
     *
     */

    /**
     *
     *      Beginn der AsyncTask Variante
     *      (Http Countermessages des Super 8 (S8) WiFi-Moduls (D1 Wemos) kontinuierlich abfragen
     *      Deprecated und in dieser Variante fuer die kontinuieliche Abfrage nicht nutzbar
     *
     */

    /**
     *  Startet die Abrage des SnapShot Servers
     */
    private void startDownload() {
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.

            mNetworkFragment.startDownload();
            mDownloading = true;
        }
    }

    /**
     *
     * Info: DownloadCallback ist Parent
     *
     * @param result String als heruntergeladener Wert
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            mDataText.setText(result);

            // Ausloeser Sound
            shootSound();

            // eine neue Aufnahmeanforderung an die Kamera senden
            CameraCaptureSession cameraCaptureSession = Camera.Camera2Service.session;
            try
            {
                cameraCaptureSession.capture(createSnapShotCaptureRequest(cameraCaptureSession), null, null);
            } catch (CameraAccessException e)
            {
                e.printStackTrace();
            }

            Log.e(TAG, "download Check: "+result);


        } else {
            mDataText.setText(getString(R.string.connection_error));
        }
    }

    /**
     * Info: DownloadCallback ist Parent
     *
     * @return
     */
    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    /**
     * Stoppt die Abrage des SnapShot Servers
     * Info: DownloadCallback ist Parent
     */
    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
    }

    /**
     * Info: DownloadCallback ist Parent
     * @param progressCode must be one of the constants defined in DownloadCallback.Progress.
     * @param percentComplete must be 0-100.
     */
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

    /**
     *  Kamera Aufnahme-Ausloesergeraeusch
     */
    public void shootSound()
    {
        AudioManager meng = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION);

        if (volume != 0)
        {
            if (_shootMP == null)
                _shootMP = MediaPlayer.create(context, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (_shootMP != null)
                _shootMP.start();
        }
    }

    /**
     * eine neue Aufnahmeanforderung an die Camera senden
     *
     * @return CaptureRequest zur Anforderung des Images
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public CaptureRequest createSnapShotCaptureRequest(CameraCaptureSession cameraCaptureSession)
    {
        try
        {
            // eine verdefinierte Schablone beutzen
            CaptureRequest.Builder builder = cameraCaptureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);

            // ImageReader - Puffer hinzufuegen
            builder.addTarget(cameraService.imageReader.getSurface());
            //builder.addTarget(cameraCaptureSession.getInputSurface());

            // ein CaptureRequest-Feld auf einen Wert setzen (gueltige Definitionen @see CaptureRequest)
            //builder.setPhysicalCameraKey(Key<T>, T value, cameraID)


            //Rect cropRectangle = new Rect(0, 0, 640, 480);
            //captureRequestBuilder.set(SCALER_CROP_REGION, cropRectangle);

            // Autofocus
            //builder.set( CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);

            // CONTROL_ZOOM_RATIO

            //builder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_FAST);
            //Rect cropRectangle = new Rect(0, 0, 640, 480);
            //Rect cropRectangle = new Rect(500, 375, 1500, 1125);
            //builder.set(CaptureRequest.SCALER_CROP_REGION, cropRectangle);

            Camera.Camera2Service.zoom.setZoom(builder,Camera.Camera2Service.zoomFaktor);

            return builder.build();
        } catch (CameraAccessException e)
        {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }




    /**
     *
     *
     *
     *       HotSpot Test
     *
     *
     *
     */

    /**
     * prueft Netzwerkzugang
     *
     * @return
     */
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

    /*
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
*/



}