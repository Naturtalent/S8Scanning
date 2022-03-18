package it.naturtalent.s8scanning;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

public class ConnectionUseCase
{

    // Interface des Connection Listeners
    // Instancen @see MainActivity
    public interface ConnectionListener
    {
        void onConnectionEstablished(String data);

        void onConnectionFailed(String message);
    }


    private final DataFetcher mDataFetcher;
    private final BackgroundThreadPoster mBackgroundThreadPoster;
    private final UiThreadPoster mUiThreadPoster;

    // Mao zur Aufnahme aller Connectionlistener
    private final Set<ConnectionListener> mListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<ConnectionListener, Boolean>());



    /**
     * Konstruktion
     * @param dataFetcher
     * @param backgroundThreadPoster
     * @param uiThreadPoster
     */
    public ConnectionUseCase(DataFetcher dataFetcher,
                             BackgroundThreadPoster backgroundThreadPoster,
                             UiThreadPoster uiThreadPoster)
    {
        mDataFetcher = dataFetcher;
        mBackgroundThreadPoster = backgroundThreadPoster;
        mUiThreadPoster = uiThreadPoster;
    }

    public void registerListener(ConnectionListener listener)
    {
        mListeners.add(listener);
    }

    public void unregisterListener(ConnectionListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Http Verbindung aufbauen (im Hintergrundthread)
     */
    public void connectHttp()
    {
        // die eigentliche Arbeit an den Hintergrundthread auslagern
        mBackgroundThreadPoster.post(new Runnable()
        {
            @Override
            public void run()
            {
                connectSync();
            }
        });
    }

    @WorkerThread
    public void connectSync()
    {
        try
        {
            final String result =  mDataFetcher.connectHttp();
            mUiThreadPoster.post(new Runnable()
            {
                // Listener im UI thread informieren
                @Override
                public void run()
                {
                    notifySuccess(result);
                }
            });
        } catch (DataFetcher.ConnectException e)
        {
            mUiThreadPoster.post(new Runnable()
            {
                // notify listeners on UI thread
                @Override
                public void run()
                {
                    //android.util.Log.d("ConnectionUserCase", "ConnectionException");
                    notifyFailure(e.message);
                }
            });
        }
    }

    // Listener ueber den erfolgreichen Verbindungsaufbau informieren
    // @see MainActivity
    @UiThread
    private void notifySuccess(String data)
    {
        for (ConnectionListener listener : mListeners)
        {
            listener.onConnectionEstablished(data);
        }
    }

    // Listener ueber den gescheiterten Verbindungsaufbau informieren
    // @see MainActivity
    @UiThread
    public void notifyFailure(String message)
    {
        for (ConnectionListener listener : mListeners)
        {
            listener.onConnectionFailed(message);
        }
    }

}

