package com.greenaddress.abcore;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;


public class RPCIntentService extends IntentService {

    public static final String PARAM_OUT_MSG = "rpccore";
    private static final String TAG = RPCIntentService.class.getName();

    public RPCIntentService() {


        super(RPCIntentService.class.getName());

        Log.d(TAG, "RPCIntentService: in constructor 1");
    }

    private Properties getBitcoinConf() throws IOException {
        final Properties p = new Properties();
        final InputStream i = new BufferedInputStream(new FileInputStream(Utils.getBitcoinConf(this)));
        try {
            p.load(i);
        } finally {
            IOUtils.closeQuietly(i);
        }
        return p;
    }

    private String getRpcUrl() throws IOException {
        final Properties p = getBitcoinConf();
        String user = p.getProperty("rpcuser");
        String password = p.getProperty("rpcpassword");
        final String testnet = p.getProperty("testnet");
        final String nonMainnet = testnet == null || !testnet.equals("1") ? p.getProperty("regtest") : testnet;
        if (user == null || password == null) {
            final String cookie = String.format("%s/%s", p.getProperty("datadir"), ".cookie");
            final String cookieTestnet = String.format("%s/%s", p.getProperty("datadir"), "testnet3/.cookie");

            final String fCookie = nonMainnet == null || !nonMainnet.equals("1") ? cookie : cookieTestnet;
            final File file = new File(fCookie);

            final StringBuilder text = new StringBuilder();

            try {
                final BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
            }
            catch (final IOException ignored) {
            }
            final String cookie_content = text.toString();
            user = "__cookie__";
            if (cookie_content.length() > user.length() + 2)
                password = cookie_content.substring(user.length() + 1);
        }
        final String host = p.getProperty("rpcconnect", "127.0.0.1");
        final String port = p.getProperty("rpcport");
        final String url = "http://" + user + ':' + password + "@" + host + ":" + (port == null ? "8332" : port) + "/";
        final String testUrl = "http://" + user + ':' + password + "@" + host + ":" + (port == null ? "18332" : port) + "/";

        return nonMainnet == null || !nonMainnet.equals("1") ? url : testUrl;
    }

    private BitcoindRpcClient getRpc() throws IOException {
        return new BitcoinJSONRPCClient(getRpcUrl());
    }

    private void broadcastPeerlist() throws IOException {
        final BitcoindRpcClient bitcoin = getRpc();

        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "peerlist");

        final List<BitcoindRpcClient.PeerInfoResult> pir = bitcoin.getPeerInfo();
        final ArrayList<String> peers = new ArrayList<>();
        // find the most common blockchain height that is higher than hardcoded constant
        for (final BitcoindRpcClient.PeerInfoResult r : pir)
            peers.add(String.format("%s - %s - %s", r.getAddr(), r.getSubVer(), r.getStartingHeight()));
        broadcastIntent.putStringArrayListExtra("peerlist", peers);

        sendBroadcast(broadcastIntent);

    }

    private void broadcastProgress() throws IOException {
        final BitcoindRpcClient bitcoin = getRpc();

        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "progress");

        final BitcoindRpcClient.BlockChainInfo info = bitcoin.getBlockChainInfo();
        broadcastIntent.putExtra("sync", info.verificationProgress() * 100);
        broadcastIntent.putExtra("blocks", info.blocks());
        sendBroadcast(broadcastIntent);

    }

    private void broadcastError(final Exception e) {
        Log.e(TAG, e.getClass().getName());
        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "exception");
        broadcastIntent.putExtra("exception", e.getMessage());
        sendBroadcast(broadcastIntent);
    }


    @Override
    protected void onHandleIntent(final Intent intent) {

        if (intent.getStringExtra("stop") != null) {
            while (true) {
                try {
                    getRpc().stop();
                    break;
                } catch (final BitcoinRPCException | IOException e) {
                    try {
                        Thread.sleep(200);
                    } catch (final InterruptedException e1) {
                        break;
                    }
                }
            }
            return;
        }


        final String console_request = intent.getStringExtra("CONSOLE_REQUEST");

        if (console_request != null) {
            final Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra(PARAM_OUT_MSG, "CONSOLE_REQUEST");
            try {
                final BitcoinJSONRPCClient bitcoin = new BitcoinJSONRPCClient(getRpcUrl());

                Log.v(TAG, console_request);

                try {

                    final String[] array = console_request.split(" ");
                    if (array.length > 1)
                        broadcastIntent.putExtra("res", bitcoin.query(array[0],
                                (Object[]) Arrays.copyOfRange(array, 1, array.length)).toString());

                    else
                        broadcastIntent.putExtra("res", bitcoin.query(console_request).toString());

                } catch (final BitcoinRPCException e) {
                    broadcastIntent.putExtra("res", "Failed, Verifying blocks?");

                }

                sendBroadcast(broadcastIntent);
            } catch (final IOException e) {
                broadcastIntent.putExtra("res", "Failed");
            } catch (final NullPointerException e) {
                broadcastIntent.putExtra("res", "No value");
            }

            return;
        }

        final String request = intent.getStringExtra("REQUEST");

        try {

            if (request != null)
                if (request.equals("peerlist")) {
                    broadcastPeerlist();
                    return;
                } else if (request.equals("progress")) {
                    broadcastProgress();
                    return;
                }

            final BitcoindRpcClient bitcoin = getRpc();

            Log.i(TAG, "" + bitcoin.getEstimateFee(1));

            final Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra(PARAM_OUT_MSG, "OK");

            broadcastIntent.putExtra("FEE", "" + bitcoin.getEstimateFee(1));
            broadcastIntent.putExtra("INFO", "" + bitcoin.getInfo());


            final List<BitcoindRpcClient.PeerInfoResult> pir = bitcoin.getPeerInfo();
            // find the most common blockchain height that is higher than hardcoded constant
            for (final BitcoindRpcClient.PeerInfoResult r : pir) {
                Log.i(TAG, "Blocks synched " + r.getSyncedBlocks());
                Log.i(TAG, "Blocks starting " + r.getStartingHeight()); /*winner is*/
                Log.i(TAG, "Blocks headers " + r.getSyncedHeaders());
            }

            Log.i(TAG, "Blocks " + bitcoin.getInfo().blocks());

            sendBroadcast(broadcastIntent);

        } catch (final BitcoinRPCException | IOException i) {
            Log.i(TAG, "EXE", i);

            if (i instanceof BitcoinRPCException && (((BitcoinRPCException) i).getResponseCode() == 500)) {
                    final Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction(MainActivity.RPCResponseReceiver.ACTION_RESP);
                    broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    broadcastIntent.putExtra(PARAM_OUT_MSG, "OK");
                    sendBroadcast(broadcastIntent);
                    return;
            }

            broadcastError(i);
        }
    }
}