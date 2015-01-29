package com.bnj.bnj.accessory.agent;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getName();
    private BroadcastReceiver usbAccessoryDetachReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BNJ Accessory is detached");
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(usbAccessoryDetachReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED));
        if (getIntent() != null && getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY) != null) {
            Log.i(TAG, "BNJ Accessory is plugged in");
            Intent intent = new Intent(this, AgentService.class);
            intent.putExtra(UsbManager.EXTRA_ACCESSORY, getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY));
            startService(intent);
        }
        Intent intent = new Intent(this, AgentService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (((AgentService.LocalBinder) service).getService().getAccessory() != null) {
                    findViewById(R.id.imageView).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
                }
                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbAccessoryDetachReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
}
