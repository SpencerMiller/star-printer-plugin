package net.omnidispatch.ionic;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.cordova.CallbackContext;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class PrintExec implements Runnable {
    private final String portName;
    private final String content;
    private final CallbackContext callbackContext;

    public PrintExec(String portName, String content, CallbackContext callbackContext) {
        this.portName = portName;
        this.content = content;
        this.callbackContext = callbackContext;
    }

    @Override
    public void run() {
        OutputStream out = null;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            BluetoothDevice device = adapter.getRemoteDevice("00:15:0E:E5:71:C6");
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();

            out = new BufferedOutputStream(socket.getOutputStream());
            out.write(new byte[]{0x1b, 0x40}); // ESC @ - Initialize printer
            out.write(content.getBytes());

            // x lines for stamp + 5 to feed past cutter
            out.write(new byte[]{0x1b, 0x64, 5}); // ESC d n - Prints the data in the print buffer and feeds n lines
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
