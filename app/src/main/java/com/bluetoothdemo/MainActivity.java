package com.bluetoothdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bluetooth.btserialport.BluetoothUtils;

public class MainActivity extends AppCompatActivity {

    Button btn_show;
    BluetoothUtils bluetoothUtils;
    EditText et_re;
    EditText et_send;
    Button btn_send;
    Button btn_open;
    Button btn_close;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothUtils = new BluetoothUtils(MainActivity.this);


        initBt();

        btn_show = findViewById(R.id.btn_show);
        et_re = findViewById(R.id.et_re);
        et_send = findViewById(R.id.et_send);
        btn_send = findViewById(R.id.btn_send);
        btn_open = findViewById(R.id.btn_open);
        btn_close = findViewById(R.id.btn_close);

        btn_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothUtils.showPairedDevices();
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if(et_send.getText().toString().isEmpty())return;
                bluetoothUtils.send(et_send.getText().toString());
            }
        });
        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothUtils.openBluetooth();
            }
        });
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothUtils.closeBluetooth();
            }
        });
    }

    private void initBt()
    {
        bluetoothUtils.init();
        bluetoothUtils.connectChange(new BluetoothUtils.ConnectStateChange() {
            @Override
            public void onConnectSuccess(BluetoothDevice device) {
                    handler.sendEmptyMessage(2);
            }

            @Override
            public void onConnectFailed(String msg) {
                Message message = new Message();
                message.what = 3;
                message.obj = msg;
                handler.sendMessage(message);
            }

            @Override
            public void newConnect(BluetoothDevice device) {

            }

            @Override
            public void onDisConnect() {
                Message message = new Message();
                message.what = 4;
                handler.sendMessage(message);
            }
        });

        bluetoothUtils.onReceiveBytes(new BluetoothUtils.ReceiveBytes() {
            @Override
            public void onReceiveBytes(byte[] bytes) {
                Message message = new Message();
                message.what = 1;
                message.obj = bytes;
                handler.sendMessage(message);

            }
        });
    }



    @Override
    protected void onDestroy() {
        bluetoothUtils.onDestory();
        super.onDestroy();
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what)
            {
                case 1:
                    byte[] arr = (byte[]) msg.obj;
                    for (int i = 0; i <arr.length ; i++) {
                        et_re.append(String.format("%02x ",arr[i]));
                    }

                    break;
                case 2:
                    Toast.makeText(MainActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(MainActivity.this,msg.obj.toString(),Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    Toast.makeText(MainActivity.this,"设备已断开连接",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}