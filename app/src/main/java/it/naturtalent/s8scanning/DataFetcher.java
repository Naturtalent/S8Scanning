package it.naturtalent.s8scanning;

import android.util.Log;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class DataFetcher
{

    public static class ConnectException extends Exception {
        public String message;
        public ConnectException(String message)
        {
            this.message = message;
        }
    }

    private boolean mIsError = true;

    @WorkerThread
    public String connectHttp() throws ConnectException
    {
        String result = null;

        // simulate 2 seconds worth of work
        try
        {
            // dummy Verzoegerung
            Thread.sleep(1000);

            // die reale Connectfunktion muss im Fehlerfall eine 'InterruptedException e' werfen
            result = doConnect();

        } catch (InterruptedException | IOException e)
        {
            throw new ConnectException(e.getMessage());
        }

        // Fehlerantwort fuer jedes zweite Mal (unklar)
        mIsError = !mIsError;
        if (mIsError)
        {
            throw new ConnectException("HTTP Connection Error");
        }

        return result;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public String doConnect()  throws IOException
    {
        URL url = new URL("http://192.168.178.120");

        InputStream stream = null;
        HttpURLConnection connection = null;
        String result = null;
        try
        {
            //connection = (HttpsURLConnection) url.openConnection();
            connection = (HttpURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
             // Open communications link (network traffic occurs here).
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();

             if (stream != null) {
            // Converts Stream to String with max length of 500.
                result = readStream(stream, 500);
            }
            } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    private String readStream(InputStream stream, int maxLength) throws IOException
    {
        String result = null;
        if (stream != null)
        {
            // Read InputStream using the UTF-8 charset.
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
            // Create temporary buffer to hold Stream data with specified max length.
            char[] buffer = new char[maxLength];
            // Populate temporary buffer with Stream data.
            int numChars = 0;
            int readSize = 0;
            while (numChars < maxLength && readSize != -1)
            {
                numChars += readSize;
                int pct = (100 * numChars) / maxLength;
                readSize = reader.read(buffer, numChars, buffer.length - numChars);
            }

            if (numChars != -1)
            {
                // The stream was not empty.
                // Create String that is actual length of response body if actual length was less than
                // max length.
                numChars = Math.min(numChars, maxLength);
                result = new String(buffer, 0, numChars);
            }

        }

        return result;
    }

}
