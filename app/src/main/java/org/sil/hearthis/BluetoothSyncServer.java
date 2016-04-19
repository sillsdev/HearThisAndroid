package org.sil.hearthis;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * SyncServer manages the 'web server' for the synchronization service that supports data
 * exchange with HearThis desktop
 */
public class BluetoothSyncServer extends Thread {
    static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final BluetoothServerSocket serverSocket;
    private InputStream inStream;
    private OutputStream outStream;
    final File rootDirectory;
    boolean cancelled = false;

    // These two must match HT windows
    private final String bluetoothServiceName = "HearThis Sync";
    UUID bluetoothServiceId = UUID.fromString("bd17ec40-1475-4f66-a661-4c4a0a65e92d");

    // Returns true if we can do BlueTooth
    public static boolean initBlueTooth() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        // Enhance: could offer to enable
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public BluetoothSyncServer(File directory) {
        rootDirectory = directory;
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(bluetoothServiceName, bluetoothServiceId);
        } catch (IOException e) { }
        serverSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (!cancelled) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Get the input and output streams, using temp objects because
                // member streams are final
                try {
                    inStream = socket.getInputStream();
                    outStream = socket.getOutputStream();
                    handleRequests();
                    serverSocket.close();
                } catch (IOException e) { }

                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            serverSocket.close();
        } catch (IOException e) { }
        cancelled = true;
    }

    void handleRequests() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        // or we get a notification (which indicates sync complete)
        // or the user pauses the sync activity.
        while (!cancelled) {
            try {
                // Read from the InputStream
                bytes = inStream.read(buffer);
                String input = new String(buffer,0,bytes, "UTF-8");
                String[] parts = input.split(";", 500);
                if (parts[0] == "getfile") {
                    getFile(parts[1]);
                }
                else if (parts[0] == "putfile") {
                    putFile(parts[1], parts[2], buffer, bytes);
                }
                else if (parts[0] ==  "listfiles") {
                    listFiles(parts[1]);
                }
                else if (parts[0] == "notify") {
                    // currently only one notification, we are done.
                    cancel();
                    return;
                }
                // not yet implemented
//                else if (parts[0] ==  "deleteFile") {
//                    deleteFile(parts[1]);
//                }

            } catch (IOException e) {
                break;
            }
        }
    }

    void getFile(String filePath) throws IOException {
        String path = rootDirectory  + "/" + filePath;
        File file = new File(path);
        byte[] content = new byte[0];
        if (file.exists()) {
            content = new byte[(int)file.length()];
            try {
                FileInputStream fileIn = new FileInputStream(file);
                fileIn.read(content);
            } catch (FileNotFoundException e) {
                e.printStackTrace(); // can't relly happen, we checked it exists
            }
        }
        putBlock(content);
    }

    void putFile(String filePath, String lengthString, byte[] initialData, int bytesRead) throws IOException{
        int length = Integer.parseInt(lengthString);
        byte sep = (byte)';';
        int start = 0;
        for (int i = 0; i < 2; i++) {// find 2 semicolons
            while (initialData[start] != sep) start++;
            start++;
        }
        String path = rootDirectory  + "/" + filePath;
        File file = new File(path);
        File dir = file.getParentFile();
        if (!dir.exists())
            dir.mkdirs();
        FileOutputStream fs = new FileOutputStream(file);
        fs.write(initialData, start, bytesRead - start);
        int remaining = length - (bytesRead - start);
        byte[] buffer = new byte[1024];
        while (remaining > 0) {
            int bytes = inStream.read(buffer);
            remaining -= bytes;
            fs.write(buffer, 0, bytes);
        }
        fs.close();
    }

    void listFiles(String path) throws IOException {
        String list = ListDirectoryHandler.getFileList(path, rootDirectory);
        try {
            putBlock(list.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(); // super unlikely
        }
    }

    // not yet implemented
//    void deleteFile(String path) {
//
//    }

    void putBlock(byte[] data) throws IOException{
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN); // appropriate for x86 destination
        bb.putInt(data.length);
        outStream.write(bb.array());
        outStream.write(data);
    }
}
