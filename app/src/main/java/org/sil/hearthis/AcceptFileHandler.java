package org.sil.hearthis;

import android.content.Context;
import android.net.Uri;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Handles requests to write a file containing the text transmitted
 */
public class AcceptFileHandler implements HttpRequestHandler {
    Context _parent;
    public AcceptFileHandler(Context parent)
    {
        _parent = parent;
    }
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        File baseDir = _parent.getExternalFilesDir(null);
        Uri uri = Uri.parse(request.getRequestLine().getUri());
        String filePath = uri.getQueryParameter("path");
        String path = baseDir  + "/" + filePath;
        HttpEntity entity = null;
        String result = "failure";
        if (request instanceof HttpEntityEnclosingRequest)
            entity = ((HttpEntityEnclosingRequest)request).getEntity();
        if (entity != null) {
            try {
                byte[] data = EntityUtils.toByteArray(entity);
                File file = new File(path);
                File dir = file.getParentFile();
                if (!dir.exists())
                    dir.mkdirs();
                FileOutputStream fs = new FileOutputStream(file);
                fs.write(data);
                fs.close();
                result = "success";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        response.setEntity(new StringEntity(result));
    }}
