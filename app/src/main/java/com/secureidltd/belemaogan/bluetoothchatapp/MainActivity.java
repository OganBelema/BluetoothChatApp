package com.secureidltd.belemaogan.bluetoothchatapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT= 1;
    private static final int REQUEST_COARSE_LOCATION = 3;
    private Button buttonOn;
    private Button buttonOff;
    private BluetoothAdapter mBluetoothAdapter;
    private Button mShowButton;
    private Spinner mSpinner;
    private Button mStartDiscoverBtn;
    private ArrayList<String> mDeviceNames;
    private ArrayAdapter<String> mArrayAdapter;
    private Button mMakeDeviceDiscoverableBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonOn = findViewById(R.id.button_on);
        buttonOff = findViewById(R.id.button_off);
        mShowButton = findViewById(R.id.show_btn);
        mSpinner = findViewById(R.id.spinner_devices);
        mStartDiscoverBtn = findViewById(R.id.start_discovery_btn);
        mMakeDeviceDiscoverableBtn = findViewById(R.id.device_discoverable_btn);

        mDeviceNames = new ArrayList<>();
        mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                mDeviceNames);
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mArrayAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();
            }
        });

        buttonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();
            }
        });

        mShowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                populateSpinnerWithPairedDevices();
            }
        });

        mStartDiscoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationPermission();
            }
        });

        mMakeDeviceDiscoverableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDeviceDiscoverable();
            }
        });

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);

    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceNames.add(device.getName());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void populateSpinnerWithPairedDevices() {
        Set<BluetoothDevice> bluetoothDevices = mBluetoothAdapter.getBondedDevices();
        if (bluetoothDevices.size() > 0){
            for (BluetoothDevice bluetoothDevice : bluetoothDevices){
                mDeviceNames.add(bluetoothDevice.getName());
            }
        }

        mArrayAdapter.notifyDataSetChanged();

    }

    private void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }

    private void bluetoothOn() {
        if (mBluetoothAdapter == null){
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Permission canceled", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
        } else {
            mBluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mBluetoothAdapter.startDiscovery(); // --->
                } else {
                    //TODO re-request
                }
                break;
            }
        }
    }

    private void makeDeviceDiscoverable(){
        Intent makeDeviceDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        makeDeviceDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(makeDeviceDiscoverableIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
