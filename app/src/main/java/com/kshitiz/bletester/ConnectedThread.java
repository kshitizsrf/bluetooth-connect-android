package com.kshitiz.bletester;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private static final String TAG = "ConnectedThread";
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private OnMessageReceived listener;

    public ConnectedThread(BluetoothSocket socket) {
        this.socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException ignored) {
        }
        in = tmpIn;
        out = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                bytes = in.read(buffer);
                String msg = new String(buffer, 0, bytes);
                Log.d(TAG, "Received: " + msg);
                if (listener != null) listener.onReceived(msg);
            } catch (IOException e) {
                break;
            }
        }
    }

    public void write(String msg) {
        try {
            out.write(msg.getBytes());
            Log.d(TAG, "Sent: " + msg);
        } catch (IOException e) {
            Log.e(TAG, "write failed", e);
        }
    }

    public void setOnMessageReceived(OnMessageReceived l) {
        listener = l;
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public interface OnMessageReceived {
        void onReceived(String msg);
    }
}
