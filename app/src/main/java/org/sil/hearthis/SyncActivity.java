package org.sil.hearthis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;


public class SyncActivity extends AppCompatActivity implements AcceptNotificationHandler.NotificationListener,
        AcceptFileHandler.IFileReceivedNotification,
        RequestFileHandler.IFileSentNotification {

    Button scanBtn;
    Button continueButton;
    TextView ipView;
    SurfaceView preview;
    int desktopPort = 11007; // port on which the desktop is listening for our IP address.
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    boolean scanning = false;
    TextView progressView;

    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        getSupportActionBar().setTitle(R.string.sync_title);
        startSyncServer();
        progressView = (TextView) findViewById(R.id.progress);
        continueButton = (Button) findViewById(R.id.continue_button);
        preview = (SurfaceView) findViewById(R.id.surface_view);
        preview.setVisibility(View.INVISIBLE);
        continueButton.setEnabled(false);
        final SyncActivity thisActivity = this;
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thisActivity.finish();
            }
        });
    }

    private void startSyncServer() {
        Intent serviceIntent = new Intent(this, SyncService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AcceptFileHandler.requestFileReceivedNotification(this);
        RequestFileHandler.requestFileSentNotification((this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sync, menu);
        ipView = (TextView) findViewById(R.id.ip_address);
        scanBtn = (Button) findViewById(R.id.scan_button);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This approach is deprecated, but the new approach (using ML_Kit)
                // requires us to increase MinSdk from 18 to 19 (4.4) and barcode scanning is
                // not important enough for us to do that. This works fine on an app that targets
                // SDK 33, at least while running on Android 12.
                barcodeDetector = new BarcodeDetector.Builder(SyncActivity.this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();
                if (cameraSource != null)
                {
                    //cameraSource.stop();
                    cameraSource.release();
                    cameraSource = null;
                }

                cameraSource = new CameraSource.Builder(SyncActivity.this, barcodeDetector)
                        .setRequestedPreviewSize(1920, 1080)
                        .setAutoFocusEnabled(true)
                        .build();

                barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                    @Override
                    public void release() {
                        // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void receiveDetections(Detector.Detections<Barcode> detections) {
                        final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                        if (scanning && barcodes.size() != 0) {
                            String contents = barcodes.valueAt(0).displayValue;
                            if (contents != null) {
                                scanning = false; // don't want to repeat this if it finds the image again
                                runOnUiThread(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      // Enhance: do something (add a magic number or label?) so we can tell if they somehow scanned
                                                      // some other QR code. We've reduced the chances by telling the BarCodeDetector to
                                                      // only look for QR codes, but conceivably the user could find something else.
                                                      // It's only used for one thing: we will try to use it as an IP address and send
                                                      // a simple DataGram to it containing our own IP address. So if it's no good,
                                                      // there'll probably be an exception, and it will be ignored, and nothing will happen
                                                      // except that whatever text the QR code represents shows on the screen, which might
                                                      // provide some users a clue that all is not well.
                                                      ipView.setText(contents);
                                                      preview.setVisibility(View.INVISIBLE);
                                                      SendMessage sendMessageTask = new SendMessage();
                                                      sendMessageTask.ourIpAddress = getOurIpAddress();
                                                      sendMessageTask.execute();
                                                      cameraSource.stop();
                                                      cameraSource.release();
                                                      cameraSource = null;
                                                  }
                                              });

                            }
                        }
                    }
                });

                if (ActivityCompat.checkSelfPermission(SyncActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        scanning = true;
                        preview.setVisibility(View.VISIBLE);
                        cameraSource.start(preview.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    ActivityCompat.requestPermissions(SyncActivity.this, new
                            String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
            }
        });
        String ourIpAddress = getOurIpAddress();
        TextView ourIpView = (TextView) findViewById(R.id.our_ip_address);
        ourIpView.setText(ourIpAddress);
        AcceptNotificationHandler.addNotificationListener(this);
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        try {
                            scanning = true;
                            preview.setVisibility(View.VISIBLE);
                            cameraSource.start(preview.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }
    }

    // Get the IP address of this device (on the WiFi network) to transmit to the desktop.
    private String getOurIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
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

    @Override
    public void onNotification(String message) {
        AcceptNotificationHandler.removeNotificationListener(this);
        setProgress(getString(R.string.sync_success));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                continueButton.setEnabled(true);
            }
        });
    }

    void setProgress(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                progressView.setText(text);
            }
        });
    }

    Date lastProgress = new Date();
    boolean stopUpdatingProgress = false;

    @Override
    public void receivingFile(final String name) {
        // To prevent excess flicker and wasting compute time on progress reports,
        // only change once per second.
        if (new Date().getTime() - lastProgress.getTime() < 1000)
            return;
        lastProgress = new Date();
        setProgress("receiving " + name);
    }

    @Override
    public void sendingFile(final String name) {
        if (new Date().getTime() - lastProgress.getTime() < 1000)
            return;
        lastProgress = new Date();
        setProgress("sending " + name);
    }

    // This class is responsible to send one message packet to the IP address we
    // obtained from the desktop, containing the Android's own IP address.
    private class SendMessage extends AsyncTask<Void, Void, Void> {

        public String ourIpAddress;
        @Override
        protected Void doInBackground(Void... params) {
            try {
                String ipAddress = ipView.getText().toString();
                InetAddress receiverAddress = InetAddress.getByName(ipAddress);
                DatagramSocket socket = new DatagramSocket();
                byte[] buffer = ourIpAddress.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, receiverAddress, desktopPort);
                socket.send(packet);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
