package com.greenaddress.abcore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;

/**
 * Dialog that opens to explain the changes the user, then open the Android Battery Optimization
 * general page. Note the user will have to make several steps to complete:
 *
 * 1 - switch the setting from "Show Unoptimized apps" to "Show all apps"
 * 2 - toggle the setting for ABCore
 */

import static android.content.Context.POWER_SERVICE;

public class BatteryOptimizationDialog extends DialogFragment {

    private static final String TAG = "BatteryOptimizationDial";

    Context mContext;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        mContext = getActivity();


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title - I'll move these to Strings values latter, once we decide what to say
        builder.setTitle("Please whitelist ABCore from Battery Optimization")
        .setMessage("From android Marshmallow onward, apps are no longer allowed to run in the background " +
                "without being whitelisted. ABCore needs to run in the background to sync the blockchain. " +
                "Click accept to be taken to the battery settings page, select All Apps at the top, then turn ABCORE OFF")
                .setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        launchSettings();
                    }
                });
        return builder.create();
    }

    public void launchSettings(){

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        //intent.setData(Uri.parse("package:" + packageName));
        Log.d(TAG, "onCreate: starting intent");
        startActivity(intent);


        /**
         * Handler which checks the settings every 200 milliseconds
         */

        final Handler handler2 = new Handler();
        Runnable checkOverlaySetting2 = new Runnable() {

            @Override
            //@TargetApi(23)
            public void run() {
                Log.d(TAG, "run: 1");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.d(TAG, "skipping, not needed");
                    return;
                }
                if (checkBatteryOptimizations()) {
                    //You have the permission, re-launch MainActivity
                    Intent i = new Intent(mContext, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(i);
                    return;
                }
                handler2.postDelayed(this, 200);
            }
        };
        handler2.postDelayed(checkOverlaySetting2, 1000);


    }
    /**
     * Check to see if the battery optimization has been turned off. Used in the handler above, so that the user will automatically
     * be taken back to app when the setting is turned off. Solves random back issues when stepping through Android Settings menus
     */


    public boolean checkBatteryOptimizations(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = mContext.getPackageName();
            PowerManager pm = (PowerManager)mContext.getSystemService(POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return true;
    }
}
