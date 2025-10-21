// DeviceActivity.java

package com.kshitiz.bletester;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";
    private static final UUID SPP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // classic SPP UUID
    private TextView statusText, chatView;
    private ProgressBar progressBar;
    private BluetoothAdapter btAdapter;
    private ConnectedThread connectedThread;
    private EditText input;
    private Button btnConnect, btnSend, btnServer, btnClient;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && d.equals(device)) {
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    if (state == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "Bonding complete, now connecting...");
                        connectToDevice(); // retry connect once paired
                    }
                }
            }

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && d.equals(device)) {
                    Log.d(TAG, "ACL connected to " + d.getName());
                    runOnUiThread(() -> {
                        statusText.setText("Connected to " + d.getName());
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && d.equals(device)) {
                    Log.d(TAG, "ACL disconnected from " + d.getName());
                    runOnUiThread(() -> statusText.setText("Disconnected"));
                }
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        TextView deviceInfo = findViewById(R.id.deviceInfo);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);
        btnConnect = findViewById(R.id.btnConnect);

        chatView = findViewById(R.id.chatView);
        input = findViewById(R.id.inputText);
        btnSend = findViewById(R.id.btnSend);
        btnServer = findViewById(R.id.btnServer);
        btnClient = findViewById(R.id.btnClient);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        String addr = getIntent().getStringExtra("device");
        if (addr != null) {
            device = btAdapter.getRemoteDevice(addr);
            Log.d(TAG, "Got device address: " + addr);
            deviceInfo.setText("Device: " + device.getName() + "\n" + device.getAddress());
        } else {
            Log.e(TAG, "No device address passed in intent!");
        }

        btnConnect.setOnClickListener(v -> connectToDevice());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(btReceiver, filter);

        btnServer.setOnClickListener(v -> new ServerThread(btAdapter, this::startChat).start());
        btnClient.setOnClickListener(v -> new ClientThread(device, this::startChat).start());
        btnSend.setOnClickListener(v -> {
            if (connectedThread != null) {
                String msg = input.getText().toString();
                connectedThread.write(msg);
                chatView.append("\nMe: " + msg);
                input.setText("");
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @SuppressLint("SetTextI18n")
    private void connectToDevice() {
        if (device == null) {
            statusText.setText("No device found");
            Log.e(TAG, "connectToDevice: device is null");
            return;
        }

        // 🔹 Ensure the device is paired first
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "Device not bonded. Starting bonding...");
            runOnUiThread(() -> statusText.setText("Pairing with device..."));
            device.createBond();
            // Return here; bonding takes time and will trigger a broadcast when done
            return;
        }

        statusText.setText("Pairing / Connecting...");
        progressBar.setVisibility(View.VISIBLE);
        btnConnect.setEnabled(false);

        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission");
                    runOnUiThread(() -> statusText.setText("Missing BLUETOOTH_CONNECT permission"));
                    return;
                }

                Log.d(TAG, "Creating RFCOMM socket...");
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);

                Log.d(TAG, "Cancelling discovery...");
                btAdapter.cancelDiscovery();

                Log.d(TAG, "Connecting to socket...");
                socket.connect();
                Log.d(TAG, "Socket connected successfully");

                runOnUiThread(() -> {
                    statusText.setText("Connected to " + device.getName());
                    progressBar.setVisibility(View.GONE);
                });
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                runOnUiThread(() -> {
                    statusText.setText("Connection failed");
                    progressBar.setVisibility(View.GONE);
                    btnConnect.setEnabled(true);
                });
                try {
                    if (socket != null) {
                        socket.close();
                        Log.d(TAG, "Socket closed after failure");
                    }
                } catch (IOException closeEx) {
                    Log.e(TAG, "Error closing socket", closeEx);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            try {
                socket.close();
                Log.d(TAG, "Socket closed in onDestroy()");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket in onDestroy()", e);
            }
        }
        unregisterReceiver(btReceiver);
    }

    private void startChat(ConnectedThread thread) {
        connectedThread = thread;
        connectedThread.setOnMessageReceived(msg ->
                runOnUiThread(() -> chatView.append("\nPeer: " + msg)));
        connectedThread.start();
        runOnUiThread(() -> Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show());
    }
}