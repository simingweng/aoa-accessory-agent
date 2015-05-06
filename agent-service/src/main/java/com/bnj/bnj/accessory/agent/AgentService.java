package com.bnj.bnj.accessory.agent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentService extends Service {

    private static final String TAG = AgentService.class.getName();
    private ParcelFileDescriptor fileDescriptor;
    private FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private Runnable readTask = new Runnable() {

        @Override
        public void run() {
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
        }
    };
    private Thread workerThread;
    private ScheduledExecutorService dummyTxService = Executors.newSingleThreadScheduledExecutor();
    private BroadcastReceiver usbAccessoryDetachReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "BNJ Accessory is detached");
            dummyTxService.shutdownNow();
            if (workerThread != null) {
                workerThread.interrupt();
                workerThread = null;
            }
            if (fileDescriptor != null) {
                try {
                    fileInputStream.close();
                    fileOutputStream.close();
                    fileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileDescriptor = null;
            }
            stopSelf();
        }
    };
    private IBinder localBinder = new LocalBinder();
    private UsbAccessory accessory;

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
        accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) {
            Log.i(TAG, "BNJ Accessory is attached");
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
            UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
            fileDescriptor = usbManager.openAccessory(accessory);
            if (fileDescriptor != null) {
                fileOutputStream = new FileOutputStream(fileDescriptor.getFileDescriptor());
                fileInputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
                workerThread = new Thread(readTask);
                workerThread.start();
                dummyTxService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i(TAG, "send heart beat to accessory");
                            fileOutputStream.write("heart beat from device".getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, 3, TimeUnit.SECONDS);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public UsbAccessory getAccessory() {
        return accessory;
    }

    public class LocalBinder extends Binder {
        public AgentService getService() {
            return AgentService.this;
        }
    }
}
