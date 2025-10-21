package com.kshitiz.bletester;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ClientThread extends Thread {
    private static final String TAG = "ClientThread";
    private final BluetoothSocket socket;
    private final Callback callback;

    public ClientThread(BluetoothDevice device, Callback cb) {
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"));
        } catch (IOException ignored) {
        }
        socket = tmp;
        callback = cb;
    }

    public void run() {
        try {
            socket.connect();
            callback.onConnected(new ConnectedThread(socket));
        } catch (IOException e) {
            Log.e(TAG, "connect() failed", e);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    interface Callback {
        void onConnected(ConnectedThread t);
    }
}
