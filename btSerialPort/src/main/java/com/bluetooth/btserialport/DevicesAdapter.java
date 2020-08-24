package com.bluetooth.btserialport;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

public class DevicesAdapter extends ArrayAdapter<BluetoothDevice>{
    private static final String TAG="skx";
    private  int resource;
    private Context context;
    public static  ProgressDialog progressDialog;
    private BluetoothUtils bluetoothUtils;
    public DevicesAdapter(Context context, int resource, List<BluetoothDevice> objects){
        super(context,resource,objects);
        this.resource=resource;
        this.context=context;
    }

    public void setBluetoothUtils(BluetoothUtils bluetoothUtils) {
        this.bluetoothUtils = bluetoothUtils;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        final BluetoothDevice bluetoothDevice=getItem(position);
        DevicesAdapter.ViewHolder viewHolder;
        View view;
        if(convertView==null)
        {
            view= LayoutInflater.from(getContext()).inflate(resource,parent,false);
            viewHolder=new DevicesAdapter.ViewHolder();

            viewHolder.name=view.findViewById(R.id.tv_bt_name);
            viewHolder.mac=view.findViewById(R.id.tv_bt_mac);
            viewHolder.l_bt = view.findViewById(R.id.l_bt);
            view.setTag(viewHolder);
        }
        else{
            view=convertView;
            viewHolder=(DevicesAdapter.ViewHolder)view.getTag();
        }
        viewHolder.name.setText(bluetoothDevice.getName());
        viewHolder.mac.setText(bluetoothDevice.getAddress());
        viewHolder.l_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle("提示");
                dialog.setMessage("确定要连接"+bluetoothDevice.getName()+"吗？");
                dialog.setCancelable(false);
                dialog.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Toast.makeText(context, "连接中", Toast.LENGTH_SHORT).show();
                        progressDialog  =new ProgressDialog(context);
                        progressDialog.setTitle("连接中");
                        progressDialog.setMessage("Loading...");
                        progressDialog.setCancelable(true);
                        progressDialog.show();
                        bluetoothUtils.connect(bluetoothDevice);

                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dialog.show();
            }
        });
        return view;
    }

    class ViewHolder {

        TextView name;

        TextView mac;

        LinearLayout l_bt;
    }

}
