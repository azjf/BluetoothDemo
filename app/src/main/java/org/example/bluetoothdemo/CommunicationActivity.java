package org.example.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class CommunicationActivity extends AppCompatActivity {

    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "98:D3:C1:FD:37:6B";
    public static final String TAG = "bluetoothdemo";


    TextView tvStatus, tvReceived;
    Button btnConnect, btnDisconnect;

    String ReceiveData = "";
    MyHandler handler;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private InputStream inStream = null;
    private ReceiveThread rThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        Init();
        InitBluetooth();

        handler = new MyHandler();

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                }
                mBluetoothAdapter.startDiscovery();

                new ConnectTask().execute(address);
            }
        });


        btnDisconnect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (btSocket != null) {
                    try {
                        btSocket.close();
                        btSocket = null;
                        if (rThread != null) {
                            rThread.join();
                        }
                        tvStatus.setText("Disconnected");
                        tvReceived.setText("");
                        ReceiveData = "";
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void Init() {
        tvStatus = (TextView) findViewById(R.id.tv_status);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        tvReceived = (TextView) findViewById(R.id.tv_received);
    }

    public void InitBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (rThread != null) {
                btSocket.close();
                btSocket = null;
                rThread.join();
            }
            this.finish();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }







    class ConnectTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(params[0]);  //params[0] = BT device addr

            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();

                Log.d(TAG, "ON RESUME: BT connection established, data transfer link opened.");
            } catch (IOException e) {
                try {
                    btSocket.close();
                    return "Failed to create socket";
                } catch (IOException e2) {
                    Log.e(TAG, "ON RESUME: Unable to close socket during connection failure", e2);
                    return "Failed to close socket";
                }
            }

            mBluetoothAdapter.cancelDiscovery();

            return "Connected";
        }

        @Override
        protected void onPostExecute(String result) {
            rThread = new ReceiveThread();
            rThread.start();
            tvStatus.setText(result);

            super.onPostExecute(result);
        }
    }



    class ReceiveThread extends Thread {

        String buffer = "";

        @Override
        public void run() {
            while (btSocket != null) {
                byte[] buff = new byte[1024];
                try {
                    inStream = btSocket.getInputStream();
                    Log.d(TAG, "Waiting for instream");
                    inStream.read(buff);
                    Log.d(TAG, "Buffer size: " + buff.length);

                    processBuffer(buff, 1024);
                    Log.d(TAG, "Received data: " + ReceiveData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void processBuffer(byte[] buff, int size) {
            int length = 0;
            for (int i = 0; i < size; i++) {
                if (buff[i] > '\0') {
                    length++;
                } else {
                    break;
                }
            }

            Log.d(TAG, "Fragment size: " + length);
            byte[] newbuff = new byte[length];

            for (int j = 0; j < length; j++) {
                newbuff[j] = buff[j];
            }
            Log.d(TAG, "Current fragment: " + new String(newbuff));
            ReceiveData = ReceiveData + new String(newbuff);
            Message msg = Message.obtain();
            msg.what = 1;
            handler.sendMessage(msg);
        }
    }




    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    tvReceived.setText(ReceiveData);
                    break;
            }
        }
    }
}