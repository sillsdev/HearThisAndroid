package org.sil.hearthis;

import android.content.Context;
import android.net.Uri;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.IOException;

/**
 * Created by Thomson on 12/28/2014.
 */
public class RequestFileHandler implements HttpRequestHandler {
    Context _parent;
    public RequestFileHandler(Context parent)
    {
        _parent = parent;
    }
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        File baseDir = _parent.getExternalFilesDir(null);
        Uri uri = Uri.parse(request.getRequestLine().getUri());
        String filePath = uri.getQueryParameter("path");
        String path = baseDir  + "/" + filePath;
        File file = new File(path);
        if (!file.exists()) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            response.setEntity(new StringEntity(""));
            return;
        }
        FileEntity body = new FileEntity(file, "audio/mpeg");
        response.setHeader("Content-Type", "application/force-download");
        //response.setHeader("Content-Disposition","attachment; filename=" + );
        response.setEntity(body);
    }
}
