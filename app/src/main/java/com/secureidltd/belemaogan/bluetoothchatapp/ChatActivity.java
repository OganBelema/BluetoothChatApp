package com.secureidltd.belemaogan.bluetoothchatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private Button mListenBtn;
    private Button mListDevicesBtn;
    private TextView mStatusTv;
    private ListView mDeviceListView;
    private TextView mMessageTv;
    private EditText mMessageEt;
    private Button mSendBtn;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice[] mBluetoothDevices;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECIEVED = 5;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "Bluetooth Chat App";
    private static final UUID MY_UUID = java.util.UUID.fromString("d1d2ea89-538e-4a2c-bed4-77566b339e37");

    private SendReceive mSendReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setUpViews();

        if (!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        implementListeners();
    }

    private void implementListeners() {
        mListDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bluetoothDevices = mBluetoothAdapter.getBondedDevices();
                ArrayList<String> deviceNames = new ArrayList<>();
                mBluetoothDevices = new BluetoothDevice[bluetoothDevices.size()];
                int index = 0;


                for (BluetoothDevice bluetoothDevice : bluetoothDevices){
                    mBluetoothDevices[index] = bluetoothDevice;
                    deviceNames.add(bluetoothDevice.getName());
                    index++;
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ChatActivity.this,
                        android.R.layout.simple_list_item_1, deviceNames);
                mDeviceListView.setAdapter(arrayAdapter);
            }
        });

        mListenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientClass = new ClientClass(mBluetoothDevices[position]);
                clientClass.start();

                mStatusTv.setText("Connecting");
            }
        });

        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mMessageEt.getText().toString();
                mSendReceive.write(message.getBytes());
            }
        });
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what){
                case STATE_LISTENING:
                    mStatusTv.setText("Listening");
                    break;

                case STATE_CONNECTING:
                    mStatusTv.setText("Connecting");
                    break;

                case STATE_CONNECTED:
                    mStatusTv.setText("Connected");
                    break;

                case STATE_CONNECTION_FAILED:
                    mStatusTv.setText("Connection Failed");
                    break;

                case STATE_MESSAGE_RECIEVED:

                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer,0, msg.arg1);
                    mMessageTv.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void setUpViews() {
        mListenBtn = findViewById(R.id.listen_btn);
        mListDevicesBtn = findViewById(R.id.list_devices_btn);
        mStatusTv = findViewById(R.id.textView_status);
        mDeviceListView = findViewById(R.id.recyclerView);
        mMessageTv = findViewById(R.id.message_tv);
        mMessageEt = findViewById(R.id.editText);
        mSendBtn = findViewById(R.id.button_send);
    }

    public class ServerClass extends Thread {
        private BluetoothServerSocket mBluetoothServerSocket;

        public ServerClass(){
            try {
                mBluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket bluetoothSocket = null;

            while (bluetoothSocket == null){
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    mHandler.sendMessage(message);
                    bluetoothSocket = mBluetoothServerSocket.accept();
                } catch (IOException e){
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    mHandler.sendMessage(message);
                }

                if (bluetoothSocket != null){
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    mHandler.sendMessage(message);

                    //TODO
                    mSendReceive = new SendReceive(bluetoothSocket);
                    mSendReceive.start();
                    break;
                }
            }
        }
    }

    public class ClientClass extends Thread {

        private BluetoothDevice mBluetoothDevice;
        private BluetoothSocket mBluetoothSocket;

        public ClientClass(BluetoothDevice bluetoothDevice){
            mBluetoothDevice = bluetoothDevice;
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try {
                mBluetoothSocket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                mHandler.sendMessage(message);

                mSendReceive = new SendReceive(mBluetoothSocket);
                mSendReceive.start();
            } catch (IOException e){
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                mHandler.sendMessage(message);
            }
        }
    }

    public class SendReceive extends Thread {
        private final BluetoothSocket mBluetoothSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public SendReceive(BluetoothSocket bluetoothSocket){

            mBluetoothSocket = bluetoothSocket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = mBluetoothSocket.getInputStream();
                tempOut = mBluetoothSocket.getOutputStream();
            } catch (IOException e){
                e.printStackTrace();
            }

            mInputStream = tempIn;
            mOutputStream = tempOut;

        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try {
                    bytes = mInputStream.read(buffer);
                    mHandler.obtainMessage(STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                mOutputStream.write(bytes);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /*protected void checkLocationPermission() {
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
                   // mBluetoothAdapter.startDiscovery(); // --->
                } else {
                    //TODO re-request
                }
                break;
            }
        }
    }*/

}
