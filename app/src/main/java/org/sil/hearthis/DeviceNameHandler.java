package org.sil.hearthis;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Handler responds to HTTP request be returning a string, the name of this device.
 */
public class DeviceNameHandler implements HttpRequestHandler {
    SyncService _parent;
    public DeviceNameHandler(SyncService parent) {
        _parent = parent;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        String contentType = "text";
        HttpEntity entity = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
                String resp = "John's Android";

                writer.write(resp);
                writer.flush();
            }
        });

        ((EntityTemplate)entity).setContentType(contentType);

        response.setEntity(entity);
    }
}
