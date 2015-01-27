package com.bnj.bnj.accessory.agent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;

public class AgentService extends Service {

    private static final String TAG = AgentService.class.getName();
    private Runnable readTask = new Runnable() {

        @Override
        public void run() {
            FileInputStream fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            byte[] buffer = new byte[16384];
            int numOfBytes = 0;
            Log.i(TAG, "start reading from accessory");
            while (!Thread.interrupted() && numOfBytes != -1) {
                try {
                    numOfBytes = fileInputStream.read(buffer);
                    Log.i(TAG, new String(buffer, 0, numOfBytes));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "exit from read task");
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private ParcelFileDescriptor fileDescriptor;
    private Thread workerThread;
    private BroadcastReceiver usbAccessoryDetachReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BNJ Accessory is detached");
            if (workerThread != null) {
                workerThread.interrupt();
            }
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            stopSelf();
        }
    };

    public AgentService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(usbAccessoryDetachReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbAccessoryDetachReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            Log.i(TAG, "BNJ Accessory is attached");
            if(workerThread!=null){
                workerThread.interrupt();
            }
            if(fileDescriptor!=null){
                try {
                    fileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
            fileDescriptor = usbManager.openAccessory(accessory);
            if (fileDescriptor != null) {
                workerThread = new Thread(readTask);
                workerThread.start();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
