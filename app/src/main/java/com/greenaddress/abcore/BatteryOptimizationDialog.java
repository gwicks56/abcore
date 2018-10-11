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
        // Set the dialog title
        builder.setTitle("Please whitelist GreenAddress from Battery Optimization")
        .setMessage("From android Oreo onward, apps are no longer allowed to run in the background without being whitelisted. GreenAddress needs to run in the background to sync the blockchain. Click accept to be taken to the battery settings page, select All Apps at the top, then turn ABCORE OFF")

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



        final Handler handler2 = new Handler();

        Runnable checkOverlaySetting2 = new Runnable() {

            @Override
            //@TargetApi(23)
            public void run() {
                Log.d(TAG, "run: 1");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.d(TAG, "run: 2");
                    //return;
                }

                // 18th Jan 2018, below works, trying to stop using the intent ( ie try back button below).
                if (checkBatteryOptimizations()) {
                    Log.d(TAG, "run: 41");
                    Log.d(TAG, "run: Notificiation Enabled moterfucker");
                    //You have the permission, re-launch MainActivity
                    Intent i = new Intent(mContext, MainActivity.class);
                    Log.d(TAG, "run: 42");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(i);
                    //Log.d(TAG, "the activity is: " + getActivity());
                    return;
                }

                // 2018-10-10 21:10:28.927 18918f height=299594 version=0x00000002 log2_work=78.451656 tx=38300079 date='2014-05-07T19:32:00Z' progress=0.110530 cache=35.6MiB(188505txo)

                handler2.postDelayed(this, 200);
            }
        };

        handler2.postDelayed(checkOverlaySetting2, 1000);


    }

    public boolean checkBatteryOptimizations(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String packageName = mContext.getPackageName();
            PowerManager pm = (PowerManager)mContext.getSystemService(POWER_SERVICE);

            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return true;
    }
}
