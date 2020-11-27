package com.bluetooth.btserialport;


import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * 微信公众号：智慧小巷
 * 日期：2020 8月24日
 */
public class BluetoothUtils {



	private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();;

	private static Context context;
	//连接状态标志位
	public static boolean isConnected = false;

	private static ConnectStateChange connectStateChange;
	private static ReceiveBytes receiveBytes;
	private  static SearchDevice searchDevice;


	private static ConnectTask connectTask;
	private  static Dialog dialog;
	public BluetoothUtils(Context context)
	{
		this.context = context;
	}
	public void init()
	{

		IntentFilter intentFilter = new IntentFilter();
		// 监视蓝牙关闭和打开的状态
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		// 监视蓝牙设备与APP连接的状态
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		// 注册广播
		context.registerReceiver(stateChangeReceiver, intentFilter);


		// 找到设备的广播
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		// 注册广播
		context.registerReceiver(receiver, filter);
		// 搜索完成的广播
		IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		// 注册广播
		context.registerReceiver(receiver, filter1);
	}


	/**
	 * 释放资源
	 */
	public void onDestory() {
		try {
			connectTask.isRunning = false;
			connectTask.cancel(true);
			context.unregisterReceiver(stateChangeReceiver);
			context.unregisterReceiver(receiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * 获取蓝牙状态
	 * @return
	 */
	public  boolean getBluetoothStatus()
	{
		if (!bluetoothAdapter.isEnabled()) {
			return false;
		}
		return true;
	}

	/**
	 * 打开蓝牙
	 */
	public  void openBluetooth()
	{
		// 获取蓝牙适配器
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(context, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show();
		}

		//请求开启蓝牙
		if (!bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.enable();
		}
	}

	/**
	 * 关闭蓝牙
	 */
	public  void closeBluetooth()
	{
		if (bluetoothAdapter.isEnabled()) {
			bluetoothAdapter.disable();
			if(isConnected) {
				isConnected = false;
				if(connectStateChange != null)
				connectStateChange.onDisConnect();
			}
		}
	}

	/**
	 * 获取已配对的列表
	 * @return
	 */
	public List<BluetoothDevice> getPairedDevices()
	{
		List<BluetoothDevice>  deviceList = new ArrayList<>();
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				deviceList.add(device);
			}
		}
		return deviceList;
	}

	/**
	 * 获取连接状态
	 * @return
	 */
	public static boolean getConnectStatus()
	{
		return isConnected;
	}

	/**
	 * 接收蓝牙状态变化广播
	 */
	private BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
				Log.d("TAG","蓝牙已连接");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(connectStateChange != null)
				connectStateChange.newConnect(device);
				isConnected = true;
			} else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String name = device.getName();
				Log.d("TAG","蓝牙已断开");
				if(connectStateChange != null)
				connectStateChange.onDisConnect();
				isConnected = false;

				//释放连接资源
				try {
					connectTask.isRunning = false;
					connectTask.cancel(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
				connectTask = null;
			}
		}
	};

	/**
	 * 广播接收器
	 */
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// 收到的广播类型
			String action = intent.getAction();
			// 发现设备的广播
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// 从intent中获取设备
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(searchDevice != null)
					searchDevice.onFoundDevice(device);
				Log.d("TAG","发现新设备");
				// 搜索完成
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if(searchDevice != null)
				searchDevice.onFinishFoundDevice();
			}
		}
	};


	/**
     * 初始化搜索设备
	 * @param searchDevice
	 */
	public  void initSearchDevice(SearchDevice searchDevice)
	{
		this.searchDevice = searchDevice;
	}

	/**
	 * 开始搜索
	 */
	public void startSearchDevice() {

		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}
		bluetoothAdapter.startDiscovery();
	}

	/**
	 * 停止搜索
	 */
	public void stopSearchDevice()
	{
		bluetoothAdapter.cancelDiscovery();
	}

	/**
	 * 连接任务
	 */
	private static class ConnectTask extends AsyncTask<String, Byte[], Void> {
		private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothSocket bluetoothSocket;
		BluetoothDevice remoteDevice;

		boolean isRunning = false;
		String stopString = "\r\n";

		@Override
		protected Void doInBackground(String... bluetoothDevicesMac) {
			// 记录标记位，开始运行
			isRunning = true;

			//如果正在搜索设备则停止
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}
			// 尝试获取 bluetoothStock
			try {
				UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
				remoteDevice = bluetoothAdapter.getRemoteDevice(bluetoothDevicesMac[0]);
				bluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID);
			} catch (Exception e) {
				isRunning = false;
				e.printStackTrace();
				return null;
			}

			// 检查有没有获取到
			if (bluetoothSocket == null) {
				if(connectStateChange != null)
					connectStateChange.onConnectFailed("连接失败：获取Stock失败!");
				isRunning = false;
				return null;
			}

			// 尝试连接
			try {
				// 等待连接，会阻塞线程
				bluetoothSocket.connect();
				isConnected = true;
				if(connectStateChange != null)
					connectStateChange.onConnectSuccess(remoteDevice);
				//关闭弹框
				closDialog();
				DevicesAdapter.progressDialog.cancel();
			} catch (Exception connectException) {
				connectException.printStackTrace();
				if(connectStateChange != null)
					connectStateChange.onConnectFailed("连接失败：" + connectException.getMessage());
				DevicesAdapter.progressDialog.cancel();
				return null;
			}

			// 开始监听数据接收
			try {
				InputStream inputStream = bluetoothSocket.getInputStream();
				int bytes;
 				while (isRunning) {
					//接收数据
					byte[] buffer = new byte[1024];
					try {
						bytes = inputStream.read(buffer);
						byte[] bytes1 = new byte[bytes];
						System.arraycopy(buffer, 0, bytes1, 0, bytes);
						if(receiveBytes != null)
						receiveBytes.onReceiveBytes(bytes1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}


		@Override
		protected void onCancelled() {
			try {
				isRunning = false;
				bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 发送
		 *
		 * @param msg 内容
		 */
		void send(byte[] msg) {

			try {
				bluetoothSocket.getOutputStream().write(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 连接到的设备，获取数据
	 *
	 * @param device 设备
	 */
	public void connect(BluetoothDevice device) {
		connect(device.getAddress());
	}

	/**
	 * 使用Mac地址连接
	 *
	 * @param deviceMac Mac地址
	 */
	 public void connect(String deviceMac) {
		 connectTask = new ConnectTask();
		if (connectTask.getStatus() == AsyncTask.Status.RUNNING && connectTask.isRunning) {
			if (connectStateChange != null) {
				connectStateChange.onConnectFailed("有正在连接的任务");
			}
			return;
		}
		try {
			connectTask.execute(deviceMac);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 断开连接
	 */
	public void disConnect()
	{
		if(isConnected)
		{
			connectTask.isRunning = false;
			connectTask.cancel(true);

			connectTask = null;
		}
	}

	/**
	 * 发送 byte 数组到串口
	 *
	 * @param bytes 要发送的数据
	 */
	public void send(byte[] bytes) {
		if (connectTask != null) {
			connectTask.send(bytes);
		}
	}

	/**
	 * 发送 byte 数组到串口
	 *
	 * @param str 要发送的字符串
	 */
	public void send(String str) {
		if (connectTask != null) {
			connectTask.send(stringToByte(str));
		}
	}

	/**
	 * 连接状态改变
	 * @param change
	 */
	public void connectChange(ConnectStateChange change)
	{
		connectStateChange = change;
	}

	/**
	 * 接收到数据
	 * @param receiveBytes
	 */
	public void onReceiveBytes(ReceiveBytes receiveBytes)
	{
		this.receiveBytes = receiveBytes;
	}

	/**
     * 显示已配对的设备
	 */
	public void showPairedDevices()
	{
		List<BluetoothDevice> objects = getPairedDevices();
		ListView listView;
		LinearLayout l_close;
		DevicesAdapter devicesAdapter = new DevicesAdapter(context, R.layout.item_bt, objects);
		devicesAdapter.setBluetoothUtils(this);
		LayoutInflater inflater = LayoutInflater.from(context);
		View myview = inflater.inflate(R.layout.item_bt_dialog, null);//引用自定义布局
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setView(myview);
		dialog = builder.create();//创建对话框
		dialog.show();//显示对话框
		dialog.setCanceledOnTouchOutside(false);
		listView = myview.findViewById(R.id.lv_bt);
		l_close = myview.findViewById(R.id.l_close);
		l_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.cancel();
			}
		});
		listView.setAdapter(devicesAdapter);
	}

	private static void closDialog()
	{
		if(dialog != null )
		dialog.cancel();
	}

	/**
	 * 字节转换成16进制字符串
	 *
	 * @param b 字节
	 * @return 字符串
	 */
	public static String byte2Hex(byte b) {
		StringBuilder hex = new StringBuilder(Integer.toHexString(b));
		if (hex.length() > 2) {
			hex = new StringBuilder(hex.substring(hex.length() - 2));
		}
		while (hex.length() < 2) {
			hex.insert(0, "0");
		}
		return hex.toString();
	}

	/**
	 * 字节数组转换成16进制字符串
	 *
	 * @param bytes 字节数组
	 * @return 字符串
	 */
	public static String byte2Hex(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String hash = formatter.toString();
		formatter.close();
		return hash;
	}

	public static byte[] stringToByte(String s)
	{
		char[] chars = s.toCharArray();
		byte[] bytes = new byte[chars.length];
		for (int i=0; i < chars.length; i++) {
			bytes[i] = (byte) chars[i];
		}
		return bytes;
	}

	/**
	 * 接收到数据接口
	 */
	public interface ReceiveBytes
	{
		/**
		 * 接收到 bytes 数组
		 *
		 * @param bytes 内容
		 */
		void onReceiveBytes(byte[] bytes);
	}

	/**
	 * 蓝牙连接状态发生改变
	 */
	public interface ConnectStateChange
	{
		/**
		 * 连接成功
		 *
		 * @param device 设备
		 */
		 void onConnectSuccess(BluetoothDevice device);

		/**
		 * 连接失败
		 *
		 * @param msg 信息
		 */
		void onConnectFailed(String msg);

		/**
		 * 发现新连接
		 * @param device
		 */
		void newConnect(BluetoothDevice device);

		/**
		 * 连接断开
		 */
		void onDisConnect();
	}

	/**
	 * 搜索设备接口
	 */
	public interface SearchDevice{

		/**
		 * 发现新设备
		 *
		 * @param device 设备
		 */
		void onFoundDevice(BluetoothDevice device);

		/**
		 * 结束搜索设备
		 */
		void onFinishFoundDevice();

	}

}
