package com.greenaddress.abcore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class ABCoreService extends Service {

    private final static String TAG = ABCoreService.class.getName();
    private final static int NOTIFICATION_ID = 922430164;
    private Process mProcess;
    private static final String PARAM_OUT_MSG = "rpccore";

    private static void removeNotification(final Context c) {
        ((NotificationManager) c.getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "exception");
        broadcastIntent.putExtra("exception", "");
        c.sendBroadcast(broadcastIntent);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    private void setupNotification() {
        final Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pI;
        pI = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_ONE_SHOT);
        final NotificationManager nM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String version = prefs.getBoolean("useknots", false) ? Packages.BITCOIN_KNOTS_NDK : Packages.BITCOIN_NDK;

        final Notification.Builder b = new Notification.Builder(this)
                .setContentTitle("ABCore is running")
                .setContentIntent(pI)
                .setContentText(String.format("Version %s", version))
                .setSmallIcon(R.drawable.ic_info_black_24dp)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_LOW;

            final NotificationChannel mChannel = new NotificationChannel("channel_00", "ABCore", importance);
            mChannel.setDescription(String.format("Version %s", version));
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            nM.createNotificationChannel(mChannel);
            b.setChannelId("channel_00");
        }


        final Notification n = b.build();

        nM.notify(NOTIFICATION_ID, n);

        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "OK");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (mProcess != null || intent == null)
            return START_STICKY;
        Log.i(TAG, "Core service msg");
        Log.d(TAG, "onStartCommand: this is ABC core service, where is the best place to add my stuff");

        // start core
        try {

            Log.d(TAG, "onStartCommand: starting core");

            // allow to pass in a different datadir directory

            // HACK: if user sets a datadir in the bitcoin.conf file that should then be the one
            // used
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            final String path = getNoBackupFilesDir().getCanonicalPath();
            final ProcessBuilder pb = new ProcessBuilder(
                    String.format("%s/bitcoind", path),
                    "--server=1",
                    String.format("--datadir=%s", Utils.getDataDir(this)),
                    String.format("--conf=%s", Utils.getBitcoinConf(this)));

            pb.directory(new File(path));


            mProcess = pb.start();
            final ProcessLogger.OnError er = new ProcessLogger.OnError() {
                @Override
                public void onError(final String[] error) {
                    removeNotification(ABCoreService.this);
                    mProcess = null;
                    final StringBuilder bf = new StringBuilder();
                    for (final String e : error)
                        if (!TextUtils.isEmpty(e))
                            bf.append(String.format("%s%s", e, System.getProperty("line.separator")));

                    Log.i(TAG, bf.toString());
                }
            };
            final ProcessLogger errorGobbler = new ProcessLogger(mProcess.getErrorStream(), er);
            final ProcessLogger outputGobbler = new ProcessLogger(mProcess.getInputStream(), er);

            errorGobbler.start();
            outputGobbler.start();

            setupNotification();

        } catch (final IOException e) {
            Log.i(TAG, "Native exception!");
            Log.i(TAG, e.getMessage());

            Log.i(TAG, e.getLocalizedMessage());
            removeNotification(this);
            mProcess = null;
            e.printStackTrace();
        }
        Log.i(TAG, "background Task finished");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destroying core service");

        if (mProcess != null) {
            mProcess.destroy();
            mProcess = null;
        }
    }
}