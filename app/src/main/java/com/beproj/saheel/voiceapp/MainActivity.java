package com.beproj.saheel.voiceapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends Activity {

    public static final int RESULT_SPEECH = 1;

    public ImageButton btnSpeak;
    public TextView txtText,textStatus;
    BluetoothAdapter bluetoothAdapter;

    ThreadConnectBTdevice myThreadConnectBT;
    ThreadConnected myThreadConnected;

    public final static UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        txtText = (TextView) findViewById(R.id.txtText);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    MainActivity.this.startActivityForResult(intent, RESULT_SPEECH);
                    txtText.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(MainActivity.this.getApplicationContext(),
                            "Ops! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

//        String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
//        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this,
//                    "Bluetooth is not supported on this hardware platform",
//                    Toast.LENGTH_LONG).show();
//            finish();
 //       }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (myThreadConnectBT != null) {
            myThreadConnectBT.cancel();
        }
    }

    private class ThreadConnectBTdevice extends Thread {
        private final BluetoothServerSocket bluetoothSocket;
        public ThreadConnectBTdevice(){
            BluetoothServerSocket tmp= null;
            try{
                tmp= bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(null,myUUID);
            }
            catch (IOException e){}
            bluetoothSocket = tmp;

       /*private BluetoothSocket bluetoothSocket = null;
        private ThreadConnectBTdevice(BluetoothDevice device) {

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }*/
        }

        @Override
        public void run() {
            try {
                bluetoothSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textStatus.setText("something wrong bluetoothSocket.connect(): \n" + eMessage);
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }


        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {}

        }

    }

    private class ThreadConnected extends Thread {
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) try {
                bytes = connectedInputStream.read(buffer);
                String strReceived = new String(buffer, 0, bytes);
                final String msgReceived = String.valueOf(bytes) +
                        " bytes received:\n"
                        + strReceived;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textStatus.setText(msgReceived);
                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                final String msgConnectionLost = "Connection lost:\n"
                        + e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textStatus.setText(msgConnectionLost);
                    }
                });
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }


    @Override
        public boolean onCreateOptionsMenu (Menu menu){
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            //noinspection SimplifiableIfStatement
            if (id == R.id.action_settings) {
                return true;
            }

            return super.onOptionsItemSelected(item);
        }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtText.setText(text.get(0));
                }

                if (myThreadConnected != null) {
                    byte[] bytesToSend = txtText.getText().toString().getBytes();
                    myThreadConnected.write(bytesToSend);
                }
                break;
            }

        }}

    public void SearchBlue(View view) {

        Intent intBlue = new Intent();
        intBlue.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intBlue);

    }

}