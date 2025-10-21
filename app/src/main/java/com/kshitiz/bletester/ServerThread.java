package com.kshitiz.bletester;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.util.UUID;

public class ServerThread extends Thread {
    private static final String TAG = "ServerThread";
    private final BluetoothServerSocket serverSocket;
    private final Callback callback;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public ServerThread(BluetoothAdapter adapter, Callback cb) {
        BluetoothServerSocket tmp = null;
        try {
            tmp = adapter.listenUsingRfcommWithServiceRecord(
                    "MyAppChat",
                    UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"));
        } catch (IOException e) {
            Log.e(TAG, "listen() failed", e);
        }
        serverSocket = tmp;
        callback = cb;
    }

    public void run() {
        try {
            BluetoothSocket socket = serverSocket.accept();
            if (socket != null) {
                callback.onConnected(new ConnectedThread(socket));
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "accept() failed", e);
        }
    }

    interface Callback {
        void onConnected(ConnectedThread t);
    }
}
