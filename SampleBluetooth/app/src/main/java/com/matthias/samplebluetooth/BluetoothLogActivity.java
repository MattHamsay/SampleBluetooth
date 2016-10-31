package com.matthias.samplebluetooth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class BluetoothLogActivity extends AppCompatActivity
{
	private final static String DATA_MESSAGE = "Message to Show";

	TextView tv_log;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_log);

		tv_log = (TextView) findViewById(R.id.BluetoothLog_TextView_log);

		// if opened by startActivity(List<> log)
		Intent intent = this.getIntent();
		if (intent != null)
		{
			String[] log_array = intent.getStringArrayExtra(BluetoothLogActivity.DATA_MESSAGE);

			String text = "";

			for (String line : log_array)
				text += line + "\n";

			tv_log.setText(text);
		}
	}

	public static void startActivity(Context context, List<String> log)
	{
		Intent intent = new Intent(context, BluetoothLogActivity.class);
		intent.putExtra(BluetoothLogActivity.DATA_MESSAGE, log.toArray(new String[0]));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
