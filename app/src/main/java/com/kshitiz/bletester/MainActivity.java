// MainActivity.java

package com.kshitiz.bletester;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PERMS = 2;
    BluetoothAdapter btAdapter;
    ArrayAdapter<String> listAdapter;
    List<BluetoothDevice> devices = new ArrayList<>();
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onReceive(Context c, Intent i) {
            if (BluetoothDevice.ACTION_FOUND.equals(i.getAction())) {
                BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && d.getName() != null) {
                    devices.add(d);
                    listAdapter.add(d.getName() + "\n" + d.getAddress());
                }
            }
        }
    };
    private ActivityResultLauncher<Intent> btEnableLauncher;

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        MaterialButton refreshBtn = findViewById(R.id.refreshBtn);
        refreshBtn.setOnClickListener(v -> startScan());

        ListView listView = findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((a, v, pos, id) -> {
            BluetoothDevice d = devices.get(pos);
            Intent i = new Intent(this, DeviceActivity.class);
            i.putExtra("device", d.getAddress());
            startActivity(i);
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        btEnableLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        startScan();
                    }
                }
        );

        checkPerms();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkPerms() {
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (!perms.isEmpty())
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), REQ_PERMS);
        else initBT();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    private void initBT() {
        btEnableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));

        if (btAdapter == null) {
            Toast.makeText(this, "No Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            btEnableLauncher.launch(enableBtIntent);
        }

        // ask to enable location (Google dialog)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        startScan();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        listAdapter.clear();
        devices.clear();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        btAdapter.startDiscovery();
    }

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Override
    public void onRequestPermissionsResult(int r, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQ_PERMS) initBT();
    }
}
