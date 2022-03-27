package it.naturtalent.s8scanning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class WifiTools
{

    /**
     *
     *
     *
     */

    private WifiManager wifiManager;

    private static final String DEBUG_TAG = "WIFIStatusExample";

    final WifiNetworkSuggestion suggestion1 =
                new WifiNetworkSuggestion.Builder()
                        .setSsid("Popolski1")
                        .setWpa3Passphrase("64893587696996089284")
                        .setIsAppInteractionRequired(true) // Optional (Needs location permission)
                        .build();

        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<WifiNetworkSuggestion>();

    /**
     *   Konstruktion
     */
    public WifiTools()
    {
        wifiManager = (WifiManager) MainActivity.activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        suggestionsList.add(suggestion1);
    }

    public void connect()
    {
        final int status = wifiManager.addNetworkSuggestions(suggestionsList);
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS)
        {
            Log.d(DEBUG_TAG, "suggestion failed");
        }

         final IntentFilter intentFilter =
                    new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

         BroadcastReceiver wifiScanReceiver = new BroadcastReceiver()
         {
             @Override
             public void onReceive(Context context, Intent intent)
             {
                if (!intent.getAction().equals(
                            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION))
                    {
                        return;
                    }

                    Log.d(DEBUG_TAG, "super Connected");

              }
          };

           MainActivity.context.registerReceiver(wifiScanReceiver, intentFilter);
    }
}

