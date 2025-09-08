package org.sil.hearthis;
import android.content.Context;
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
        // extract and handle sync status that HT inserted into the notification.
        // The notification received from the HearThis PC is an HttpRequest (RFC 7230), like this:
        //    POST /notify?message=sync_success HTTP/1.1
        // Sync status is in the "message" portion. Extract it and send it along.
        //
        // NOTE: like several things in HearThisAndroid, HttpRequest is deprecated. It will be
        // replaced with something more appropriate, hopefully soon.

        String status = null;
        try {
            String s1 = request.getRequestLine().getUri();
            URI uri = new URI(s1);
            String query = uri.getQuery();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("message")) {
                    status = pair[1];
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (status == null) {
            // Something went wrong. Make sure the user sees a non-success message.
            status = "sync_interrupted";
        }

        for (NotificationListener listener: notificationListeners.toArray(new NotificationListener[notificationListeners.size()])) {
            listener.onNotification(status);
        }
        response.setEntity(new StringEntity(status));
    }
}
