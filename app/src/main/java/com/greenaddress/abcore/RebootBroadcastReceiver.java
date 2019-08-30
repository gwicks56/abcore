package com.greenaddress.abcore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class RebootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "RebootBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d(TAG, "onPreferenceChange: "+ prefs.getBoolean("restartOnBoot", false));
        boolean restart = prefs.getBoolean("restartOnBoot", false);

        if(restart){
            Intent myIntent = new Intent(context, MainActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
    }
}
