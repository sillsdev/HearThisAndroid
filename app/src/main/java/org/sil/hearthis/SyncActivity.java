package org.sil.hearthis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SyncActivity extends AppCompatActivity implements AcceptNotificationHandler.NotificationListener,
        AcceptFileHandler.IFileReceivedNotification,
        RequestFileHandler.IFileSentNotification {

    private static final String TAG = "SyncActivity";
    Button scanBtn;
    Button continueButton;
    TextView ipView;
    PreviewView previewView;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 202;
    boolean scanning = false;
    TextView progressView;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private final Handler registrationHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EdgeToEdge.enable(this);
            // Explicitly set light icons for the black status bar when edge-to-edge is enabled
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(true);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        View syncLayout = findViewById(R.id.sync_layout);
        if (syncLayout != null) {
            // Get original padding from XML to preserve it
            int paddingLeft = syncLayout.getPaddingLeft();
            int paddingTop = syncLayout.getPaddingTop();
            int paddingRight = syncLayout.getPaddingRight();
            int paddingBottom = syncLayout.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(syncLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                    paddingLeft + systemBars.left,
                    paddingTop + systemBars.top,
                    paddingRight + systemBars.right,
                    paddingBottom + systemBars.bottom
                );
                return insets;
            });
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.sync_title);
        }
        requestNotificationPermissionAndStartSync();
        progressView = findViewById(R.id.progress);
        continueButton = findViewById(R.id.continue_button);
        previewView = findViewById(R.id.preview_view);
        previewView.setVisibility(View.INVISIBLE);
        continueButton.setEnabled(false);
        final SyncActivity thisActivity = this;
        continueButton.setOnClickListener(view -> thisActivity.finish());

        cameraExecutor = Executors.newSingleThreadExecutor();
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }

    private void requestNotificationPermissionAndStartSync() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.need_permissions)
                            .setMessage(R.string.notification_for_sync)
                            .setPositiveButton(R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(SyncActivity.this,
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_NOTIFICATION_PERMISSION))
                            .setNegativeButton(R.string.cancel, (dialog, which) -> startSyncServer())
                            .create().show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
                }
                return;
            }
        }
        startSyncServer();
    }

    private void startSyncServer() {
        Intent serviceIntent = new Intent(this, SyncService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        startRegistrationRetry();
    }

    private void stopSyncServer() {
        Intent serviceIntent = new Intent(this, SyncService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRegistrationRetry();
    }

    @Override
    protected void onPause() {
        registrationHandler.removeCallbacks(registrationRunnable);
        unregisterListeners();
        super.onPause();
    }

    private final Runnable registrationRunnable = new Runnable() {
        @Override
        public void run() {
            if (registerListeners()) {
                Log.d(TAG, "Successfully registered sync listeners");
            } else {
                Log.d(TAG, "SyncService not ready yet, retrying registration...");
                registrationHandler.postDelayed(this, 500);
            }
        }
    };

    private void startRegistrationRetry() {
        registrationHandler.removeCallbacks(registrationRunnable);
        registrationHandler.post(registrationRunnable);
    }

    private boolean registerListeners() {
        SyncService service = SyncService.getInstance();
        if (service != null && service.getServer() != null) {
            SyncServer server = service.getServer();
            server.getAcceptFileHandler().setListener(this);
            server.getRequestFileHandler().setListener(this);
            server.getAcceptNotificationHandler().addNotificationListener(this);
            return true;
        }
        return false;
    }

    private void unregisterListeners() {
        SyncService service = SyncService.getInstance();
        if (service != null && service.getServer() != null) {
            SyncServer server = service.getServer();
            server.getAcceptFileHandler().setListener(null);
            server.getRequestFileHandler().setListener(null);
            server.getAcceptNotificationHandler().removeNotificationListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stopSyncServer();
        }
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sync, menu);
        ipView = findViewById(R.id.ip_address);
        scanBtn = findViewById(R.id.scan_button);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            // The manifest declares the camera optional, so we may be on a device without one.
            scanBtn.setVisibility(View.GONE);
        }
        scanBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(SyncActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(SyncActivity.this, new
                        String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        });
        String ourIpAddress = getOurIpAddress();
        TextView ourIpView = findViewById(R.id.our_ip_address);
        ourIpView.setText(ourIpAddress);
        return true;
    }

    private void startCamera() {
        scanning = true;
        previewView.setVisibility(View.VISIBLE);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        if (!scanning) {
                            imageProxy.close();
                            return;
                        }

                        @SuppressLint("UnsafeOptInUsageError")
                        android.media.Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            barcodeScanner.process(image)
                                    .addOnSuccessListener(barcodes -> {
                                        if (scanning && !barcodes.isEmpty()) {
                                            handleBarcode(barcodes.get(0));
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed", e))
                                    .addOnCompleteListener(task -> imageProxy.close());
                        } else {
                            imageProxy.close();
                        }
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException | IllegalArgumentException | IllegalStateException e) {
                // IllegalArgumentException: no camera matches the selector (e.g. no back camera).
                // IllegalStateException: the camera can't be bound (e.g. lifecycle already destroyed).
                Log.e(TAG, "Use case binding failed", e);
                cameraUnavailable();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Backs out of scanning when the camera can't be used and points the user at manual IP entry.
     * Must be called on the main thread.
     */
    private void cameraUnavailable() {
        scanning = false;
        previewView.setVisibility(View.INVISIBLE);
        Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show();
    }

    /**
     * Returns true if the string is a dotted-quad IPv4 address: exactly four decimal
     * octets, each 0-255, with no whitespace, signs, or leading zeros. Does no DNS lookup.
     */
    static boolean isValidIpv4(String s) {
        if (s == null) {
            return false;
        }
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            int len = part.length();
            if (len < 1 || len > 3) {
                return false;
            }
            if (len > 1 && part.charAt(0) == '0') {
                return false;
            }
            int value = 0;
            for (int i = 0; i < len; i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                value = value * 10 + (c - '0');
            }
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private void handleBarcode(Barcode barcode) {
        String contents = barcode.getDisplayValue();
        if (contents == null) {
            Log.e(TAG, "Barcode content is null");
            return;
        }
        if (!isValidIpv4(contents)) {
            // Return before setting scanning=false so a bad QR code won't stop the scan.
            // No reason not to continue scanning for a valid address.
            Log.e(TAG, "Barcode content is not a valid IPv4 address: " + contents);
            return;
        }

        scanning = false;

        if (ipView == null) {
            Log.e(TAG, "ipView is null");
            return;
        }
        runOnUiThread(() -> {
            ipView.setText(contents);
            previewView.setVisibility(View.INVISIBLE);

            sendRegistrationMessage(contents);

            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Failed to unbind camera", e);
                }
            }, ContextCompat.getMainExecutor(this));
        });
    }

    private void sendRegistrationMessage(final String desktopIpAddress) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress desktop = InetAddress.getByName(desktopIpAddress);
                // Advertise the address on the interface that routes to the desktop, so we don't
                // send a cellular or VPN address the desktop can't reach.
                String ourIpAddress = getRoutedAddressTo(socket, desktop, 11007);
                if (ourIpAddress == null) {
                    Log.e(TAG, "No usable IPv4 address to register with the desktop");
                    return;
                }
                // Registration must be sent to port 11007 as a UTF-8 encoded string containing
                // only the Android device's IPv4 address (no prefix or whitespace), as expected
                // by the desktop application.
                byte[] data = ourIpAddress.getBytes(StandardCharsets.UTF_8);
                // The socket is already connected to the desktop (see getRoutedAddressTo).
                socket.send(new DatagramPacket(data, data.length));
            } catch (IOException e) {
                Log.e(TAG, "Error sending registration packet", e);
            }
        }).start();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                }
                break;
            case REQUEST_NOTIFICATION_PERMISSION:
                startSyncServer();
                break;
        }
    }

    @Override
    public void onNotification(String message) {
        Log.d(TAG, "Notification received: " + message);
        // TODO: HearThis sends empty message instead of success/fail. Make it send a real result.
        // TODO: HearThisAndroid ignores it and assumes success. Make it process 'message' appropriately.
        runOnUiThread(() -> {
            progressView.setText(R.string.sync_success);
            continueButton.setEnabled(true);
        });
    }

    @Override
    public void receivingFile(String path) {
        Log.d(TAG, "File received: " + path);
        runOnUiThread(() -> progressView.setText(getString(R.string.receiving_file, path)));
    }

    @Override
    public void receiveFailed(String path) {
        Log.e(TAG, "File receive failed: " + path);
        runOnUiThread(() -> progressView.setText(getString(R.string.receive_failed, path)));
    }

    @Override
    public void sendingFile(String path) {
        Log.d(TAG, "File sent: " + path);
        runOnUiThread(() -> progressView.setText(getString(R.string.sending_file, path)));
    }

    /** An IP address found on a local network interface, with what we need to rank it. */
    static final class AddressCandidate {
        final InetAddress address;
        final boolean pointToPoint;

        AddressCandidate(InetAddress address, boolean pointToPoint) {
            this.address = address;
            this.pointToPoint = pointToPoint;
        }
    }

    /**
     * Picks the address most likely to be our LAN address, or null if there is none.
     * Only non-loopback, non-link-local IPv4 addresses qualify. Preference order: a site-local
     * (RFC1918) address on an ordinary interface, then any address on an ordinary interface, then
     * anything on a point-to-point interface. Cellular and VPN tunnels are usually point-to-point,
     * so this keeps them from winning over a Wi-Fi/Ethernet address regardless of the order in which
     * the interfaces are enumerated. Within a rank, the first candidate wins.
     */
    static String pickBestAddress(List<AddressCandidate> candidates) {
        String best = null;
        int bestRank = Integer.MAX_VALUE;
        for (AddressCandidate c : candidates) {
            InetAddress a = c.address;
            if (!(a instanceof Inet4Address) || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()) {
                continue;
            }
            int rank = c.pointToPoint ? 2 : (a.isSiteLocalAddress() ? 0 : 1);
            if (rank < bestRank) {
                best = a.getHostAddress();
                bestRank = rank;
            }
        }
        return best;
    }

    /**
     * Best guess at our LAN address, for display. When the desktop's address is known, use
     * {@link #getRoutedAddressTo} instead, since it asks the OS which interface actually reaches it.
     */
    private String getOurIpAddress() {
        List<AddressCandidate> candidates = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en != null && en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (!intf.isUp() || intf.isVirtual()) {
                    continue;
                }
                boolean p2p = intf.isPointToPoint();
                for (Enumeration<InetAddress> addrs = intf.getInetAddresses(); addrs.hasMoreElements(); ) {
                    candidates.add(new AddressCandidate(addrs.nextElement(), p2p));
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return pickBestAddress(candidates);
    }

    /**
     * Returns the local IPv4 address the OS would use to reach the given host, which is the address
     * the host can reach us on. Connecting a UDP socket sends nothing; it just selects the route.
     * Falls back to {@link #getOurIpAddress()} if the route can't be determined.
     */
    private String getRoutedAddressTo(DatagramSocket socket, InetAddress host, int port) {
        try {
            socket.connect(host, port);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isAnyLocalAddress() && !local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not determine route to " + host, e);
        }
        return getOurIpAddress();
    }
}
