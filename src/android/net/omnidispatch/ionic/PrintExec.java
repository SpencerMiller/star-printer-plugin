package net.omnidispatch.ionic;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class PrintExec implements Runnable {
    private final String address;
    private final JSONArray content;
    private final CallbackContext callbackContext;

    public PrintExec(String address, JSONArray content, CallbackContext callbackContext) {
        this.address = address;
        this.content = content;
        this.callbackContext = callbackContext;
    }

    @Override
    public void run() {
        OutputStream out = null;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            BluetoothDevice device = adapter.getRemoteDevice(address);
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();

            out = new BufferedOutputStream(socket.getOutputStream());
            for (int i = 0; i < content.length(); i++) {
                out.write(content.getInt(i));
            }

            out.flush();
            // none of out.flush(), out.close() nor socket.close() appear to wait for the buffer to flush so we wait here
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Log.e("LOG", Log.getStackTraceString(e));
            }

            callbackContext.success("Done");
        } catch (Exception e) {
            callbackContext.error(OmniPrinterPlugin.formatResultError(e));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.wtf(OmniPrinterPlugin.TAG, "Close OutputStream", e);
                }
            }
        }
    }
}
