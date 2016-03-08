package org.sil.hearthis;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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


public class SyncActivity extends ActionBarActivity implements AcceptNotificationHandler.NotificationListener,
        AcceptFileHandler.IFileReceivedNotification,
    RequestFileHandler.IFileSentNotification {

    Button scanBtn;
    Button continueButton;
    TextView ipView;
    int desktopPort = 11007; // port on which the desktop is listening for our IP address.
    TextView progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        getActionBar().setTitle(R.string.sync_title);
        startSyncServer();
        progressView = (TextView)findViewById(R.id.progress);
        continueButton = (Button)findViewById(R.id.continue_button);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sync, menu);
        ipView = (TextView)findViewById(R.id.ip_address);
        scanBtn = (Button)findViewById(R.id.scan_button);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(SyncActivity.this);
                integrator.addExtra("SCAN_MODE", "QR_CODE_MODE");
                //customize the prompt message before scanning
                integrator.addExtra("PROMPT_MESSAGE", "Scan the HearThis Desktop barcode");
                // scan result comes to our onActivityResult method.
                integrator.initiateScan();
            }
        });
        String ourIpAddress = getOurIpAddress();
        TextView ourIpView = (TextView)findViewById(R.id.our_ip_address);
        ourIpView.setText(ourIpAddress);
        AcceptNotificationHandler.addNotificationListener(this);
        return true;
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

    // Receiver the result of a successful QR scan, assumed to be the one from the desktop.
    // Enhance: do something (add a magic number?) so we can tell if they somehow scanned
    // the wrong QR code.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            String contents = result.getContents();
            if (contents != null) {
                ipView.setText(contents);
                SendMessage sendMessageTask = new SendMessage();
                sendMessageTask.ourIpAddress = getOurIpAddress();
                sendMessageTask.execute();
            }
        }
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
