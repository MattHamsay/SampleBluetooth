package com.matthias.samplebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{

	// bluetooth
	private List<String> bluetooth_log;
	private	BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket mBluetoothSocket;

	// list view
	private ListView              listView_bt_devices;
	private mySimpleListAdapter   mySimpleListAdapter;
	private List<BluetoothDevice> listView_devices;          // List < String dev_name, String mac_addr >

	// buttons
	private TextView textView_startBluetooth;
	private TextView textView_viewPaired;
	private TextView textView_startSearching;
	private TextView textView_stopSearching;
	private TextView textView_sendMessage;
	private EditText editText_userMessage;
	private TextView textView_viewLog;

	// request names
	private final static int REQUEST_ENABLE_BT = 1000;      // random number - request to enable bluetooth

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bluetooth_log = new ArrayList<>();

		initiateViews();

		Toast.makeText(MainActivity.this, "App Started", Toast.LENGTH_SHORT).show();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_ENABLE_BT)
		{
			// user said yes
			if (resultCode == RESULT_OK)
			{
				performActionOnBtEnabled();
			}
			// user said no
			else if (resultCode == RESULT_CANCELED)
			{
				Toast.makeText(MainActivity.this, "Bluetooth is supported but not enabled", Toast.LENGTH_SHORT).show();
			}
		}
	}

	// ================================================================================================
	// Methods for Constructor
	// ================================================================================================
	private void initiateViews()
	{
		listView_devices = new ArrayList<>();
		mySimpleListAdapter = new mySimpleListAdapter(this, listView_devices);
		listView_bt_devices = (ListView) findViewById(R.id.MainActivity_ListView_PairedDevices);
		listView_bt_devices.setAdapter(mySimpleListAdapter);
		mySimpleListAdapter.notifyDataSetChanged();

		// buttons - start bluetooth
		textView_startBluetooth = (TextView) findViewById(R.id.MainActivity_TextView_StartBluetooth);
		textView_startBluetooth.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				setupBluetooth();
			}
		});

		// buttons - display paired devices
		textView_viewPaired = (TextView) findViewById(R.id.MainActivity_TextView_ViewPairedDevice);
		textView_viewPaired.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				displayPairedDevices();
			}
		});

		// buttons - start to search nearby bluetooth devices
		textView_startSearching = (TextView) findViewById(R.id.MainActivity_TextView_StartSearch);
		textView_startSearching.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startSearchNearbyBtDevices();
			}
		});

		// buttons - stop searching nearby bluetooth devices
		textView_stopSearching = (TextView) findViewById(R.id.MainActivity_TextView_StopSearch);
		textView_stopSearching.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				stopSearchNearbyBtDevices();
			}
		});

		// buttons - send message defined in Edit Text
		editText_userMessage = (EditText) findViewById(R.id.MainActivity_EditText_userMessage);     // used by onClickSendMessage();
		textView_sendMessage = (TextView) findViewById(R.id.MainActivity_TextView_sendMessage);
		textView_sendMessage.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onClickSendMessage();
			}
		});

		textView_viewLog = (TextView) findViewById(R.id.MainActivity_TextView_viewLog);
		textView_viewLog.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				BluetoothLogActivity.startActivity(textView_viewLog.getContext(), bluetooth_log);
			}
		});
	}

	// ================================================================================================
	// Methods for ListView
	// ================================================================================================
	private void addItemToListView(BluetoothDevice device)
	{
		listView_devices.add(device);
		mySimpleListAdapter.notifyDataSetChanged();
	}


	// ================================================================================================
	// Methods for Bluetooth
	// ================================================================================================
	private void setupBluetooth()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			// Device does not support Bluetooth
			Toast.makeText(MainActivity.this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
		}
		else
		{
			// if not enabled, request to turn on Bt to user by dialog
			if (!mBluetoothAdapter.isEnabled())
			{
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                      // will proceed upon successful return
			}
			// if already enabled, proceed
			else
			{
				// should be, in real version. for now, let's put each in each list
				performActionOnBtEnabled();
			}
		}
	}

	// this is called when Bluetooth is enabled
	// 1) it was enabled from the beginning, or
	// 2) startActivityForResult returns
	private void performActionOnBtEnabled()
	{
		Toast.makeText(MainActivity.this, "Bluetooth is now enabled!", Toast.LENGTH_SHORT).show();

		// reset the list before starts
		resetBtDeviceList();

		// checking paired first is worth
//		displayPairedDevices();
	}

	private void resetBtDeviceList()
	{
		listView_devices.clear();
		mySimpleListAdapter.notifyDataSetChanged();
	}

	private boolean isBluetoothEnabled()
	{
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
		{
			Toast.makeText(MainActivity.this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();

			return false;
		}
		return true;
	}

	private void displayPairedDevices()
	{
		// reset the list before starts
		resetBtDeviceList();

		if (isBluetoothEnabled())
		{
			Toast.makeText(MainActivity.this, "Displaying Paired Bluetooth Devices", Toast.LENGTH_SHORT).show();

			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			// If there are paired devices
			if (pairedDevices.size() > 0)
			{
				// Loop through paired devices
				for (BluetoothDevice device : pairedDevices)
				{
					// Add the name and address to an array adapter to show in a ListView
					addItemToListView(device);
				}
			}
		}
	}

	private void startSearchNearbyBtDevices()
	{
		if (isBluetoothEnabled())
		{
			Toast.makeText(MainActivity.this, "Searching nearby Bluetooth Devices", Toast.LENGTH_SHORT).show();

			resetBtDeviceList();

			// Register the BroadcastReceiver
			IntentFilter bt_discover_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			registerReceiver(mReceiver, bt_discover_filter); // Don't forget to unregister during onDestroy

			mBluetoothAdapter.startDiscovery();
		}
	}

	// used by ConnectThread
	private void stopSearchNearbyBtDevices()
	{
		if (isBluetoothEnabled())
		{
			Toast.makeText(MainActivity.this, "Stopped Searching nearby Bluetooth Devices", Toast.LENGTH_SHORT).show();

			if (mBluetoothAdapter.isDiscovering())
			{
				mBluetoothAdapter.cancelDiscovery();
				unregisterReceiver(mReceiver);
			}
		}
	}

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				// Add the name and address to an array adapter to show in a ListView
				addItemToListView(device);
			}
		}
	};

	// connected to Arduino!
	private void manageConnectedSocket(BluetoothSocket socket)
	{
		Toast.makeText(MainActivity.this, "Connected to Bluetooth Device!", Toast.LENGTH_SHORT).show();
		mBluetoothSocket = socket;
	}

	// ================================================================================================
	// Methods for Bluetooth
	// ================================================================================================
	ConnectedThread connectedThread = null;

	private void onClickSendMessage()
	{
		if (mBluetoothSocket == null)
		{
			Toast.makeText(MainActivity.this, "No Socket Connected!", Toast.LENGTH_SHORT).show();
			return;
		}

		String userMessage = editText_userMessage.getText().toString();

		if (connectedThread == null)
		{
			connectedThread = new ConnectedThread(mBluetoothSocket);
			connectedThread.start();
		}

//		connectedThread.run();
		connectedThread.write(userMessage.getBytes());
		editText_userMessage.setText("");

		Toast.makeText(MainActivity.this, "Sent message to device!", Toast.LENGTH_SHORT).show();
	}

	// ================================================================================================
	// Inner Classes for Bluetooth
	// ================================================================================================

	/**
	 * Pretty much copied from:
	 * https://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingAsAClient
	 */
	private class ConnectThread extends Thread
	{
		// UUID for Arduino - http://stackoverflow.com/questions/10327506/android-arduino-bluetooth-data-transfer
		private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID

		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

//		private final MainActivity context;

		public ConnectThread(BluetoothDevice device)
		{
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) { }
			mmSocket = tmp;
		}

		public void run()
		{
			// Cancel discovery because it will slow down the connection
			stopSearchNearbyBtDevices();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) { }
				return;
			}

			// Do work to manage the connection (in a separate thread)
			manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel()
		{
			try
			{
				mmSocket.close();
			} catch (IOException e) { }
		}
	}

	// ================================================================================================
	// Inner Classes for Bluetooth
	// ================================================================================================

	private class mySimpleListAdapter extends BaseAdapter
	{
		private Context               mContext;
		private List<BluetoothDevice> devices;
		private LayoutInflater inflater = null;

		public mySimpleListAdapter(Context context, List<BluetoothDevice> devices)
		{
			this.mContext = context;
			this.devices = devices;
			this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount()
		{
			return devices.size();
		}

		@Override
		public Object getItem(int position)
		{
			return devices.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
				view = inflater.inflate(R.layout.layout_listitem_bluetooth_info, null, false);

			// get views
			TextView device_name = (TextView) view.findViewById(R.id.listitem_textview_device_name);
			TextView device_mac  = (TextView) view.findViewById(R.id.listitem_textview_device_mac_address);

			// set texts
			device_name.setText(devices.get(position).getName());
			device_mac.setText(devices.get(position).getAddress());

			view.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Toast.makeText(MainActivity.this, String.format("listitem [%s] is clicked!", devices.get(position)), Toast.LENGTH_SHORT).show();
					new ConnectThread(devices.get(position)).run();
				}
			});

			return view;
		}
	}


	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream     mmInStream;
		private final OutputStream    mmOutStream;

		public ConnectedThread(BluetoothSocket socket)
		{
			mmSocket = socket;
			InputStream  tmpIn  = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try
			{
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			}
			catch (IOException e)
			{
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run()
		{
			byte[] buffer = new byte[1024];  // buffer store for the stream
			int    bytes; // bytes returned from read()

			// each line will be sent, without '\n' symbol at the end
			// final msg from arduino will always end with '\n'
			// each bluetooth arduino message ends with '\n'
			String currLine = "";

			// Keep listening to the InputStream until an exception occurs
			while (true)
			{
				try
				{
					// Read from the InputStream
					bytes = mmInStream.read(buffer);

					// do something
					String buffer_str = new String(buffer, StandardCharsets.US_ASCII);

//					bluetooth_log.add("buffer start");

					for (int i = 0; i < bytes; i++)
					{
						char ch = buffer_str.charAt(i);

						// do not keep '\n'
						if (ch == '\n')
						{
							bluetooth_log.add(currLine);
							currLine = "";
						}
						else
						{
							currLine += ch;
						}
					}

//					bluetooth_log.add("last line: " + currLine);

//					bluetooth_log.add("buffer end");

					// Send the obtained bytes to the UI activity
//					mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//					        .sendToTarget();
				}
				catch (IOException e)
				{
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes)
		{
			try
			{
				mmOutStream.write(bytes);
			}
			catch (IOException e)
			{

			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel()
		{
			try
			{
				mmSocket.close();
			}
			catch (IOException e)
			{

			}
		}
	}
}

