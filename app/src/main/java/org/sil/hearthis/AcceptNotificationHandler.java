package org.sil.hearthis;
import android.content.Context;
import android.util.Log;  // WM, TEMPORARY!

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by Thomson on 1/18/2016.
 */
public class AcceptNotificationHandler implements HttpRequestHandler {

    private static String minHtaVersion = null;

    public interface NotificationListener {
        void onNotification(String message);
    }
    static ArrayList<NotificationListener> notificationListeners= new ArrayList<NotificationListener>();

    public static void addNotificationListener(NotificationListener listener) {
        notificationListeners.add(listener);
    }

    public static void removeNotificationListener(NotificationListener listener) {
        notificationListeners.remove(listener);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {

        // Enhance: allow the notification to contain a message, and pass it on.
        // The copy is made because the onNotification calls may well remove listeners, leading to concurrent modification exceptions.

        // HT-508: to prevent HTA from getting stuck in a bad state when sync is interrupted,
        // extract and handle sync status that HT inserted into the notification. HT also sets up
        // that notification by first sending a notification containing the minimum HTA version
        // needed for this exchange.
        // The notifications received from the HearThis PC are HttpRequest (RFC 7230), like this:
        //    POST /notify?minHtaVersion=1.0 HTTP/1.1       -- HT sends this first
        //    POST /notify?status=sync_success HTTP/1.1     -- HT sends this second
        // Payload is in the portion after the 'notify'. Extract it and send it along.
        // If something goes wrong and that is not possible, send along an error indication.
        // NOTIFICATION ORDER IS IMPORTANT. HT must send the HTA version info first, and then the
        // sync final status. This is enforced by an early return when the HTA version info is seen.
        //
        // NOTE: like several things in HearThisAndroid, HttpRequest is deprecated. It will be
        // replaced with something more appropriate, hopefully soon.

        String status = null;
        Log.d("Sync", "handle, begin, minHtaVersion = " + minHtaVersion); // WM, TEMPORARY
        try {
            String s1 = request.getRequestLine().getUri();
            URI uri = new URI(s1);
            String query = uri.getQuery();
            Log.d("Sync", "handle, query = " + query); // WM, TEMPORARY
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);  // limit=2 in case value contains '='
                    if (pair.length == 2) {
                        if (pair[0].equals("status")) {
                            status = pair[1];
                            Log.d("Sync", "handle, status = " + status); // WM, TEMPORARY
                        } else if (pair[0].equals("minHtaVersion")) {
                            minHtaVersion = pair[1];
                            Log.d("Sync", "handle, minHtaVersion = " + minHtaVersion + ", returning"); // WM, TEMPORARY
                            return;
                        }
                    }
                }
            }
            Log.d("Sync", "handle, results: status = " + status + ", minHtaVersion = " + minHtaVersion); // WM, TEMPORARY
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (status == null) {
            // Something went wrong. Make sure the user sees a non-success message.
            //if (minHtaVersion != null) {
            //    status = "sync_unknown";  // error also: we got something but it wasn't "status"
            //} else {
                status = "sync_error";
            //}
        }

        Log.d("Sync", "handle, final from HT, status = " + status); // WM, TEMPORARY
        for (NotificationListener listener: notificationListeners.toArray(new NotificationListener[notificationListeners.size()])) {
            Log.d("Sync", "handle, calling listener.onNotification(" + status + ")"); // WM, TEMPORARY
            listener.onNotification(status);
        }
        Log.d("Sync", "handle, putting status in response (I think)"); // WM, TEMPORARY
        response.setEntity(new StringEntity(status));
    }
}
