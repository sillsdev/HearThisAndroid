package org.sil.hearthis;

import android.os.Build;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Handler responds to HTTP request by returning a string, the name of this device.
 */
public class DeviceNameHandler {
    final SyncService _parent;
    public DeviceNameHandler(SyncService parent) {
        _parent = parent;
    }

    public Response handle(NanoHTTPD.IHTTPSession session) {
        // Use the device model name instead of a hardcoded string.
        String deviceName = Build.MODEL;
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Android Device";
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain", deviceName);
    }
}
