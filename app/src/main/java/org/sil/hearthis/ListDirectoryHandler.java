package org.sil.hearthis;

import android.content.Context;
import android.net.Uri;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Thomson on 12/28/2014.
 */
public class ListDirectoryHandler implements HttpRequestHandler
{
    Context parent;

    public ListDirectoryHandler(Context parent)
    {
        this.parent = parent;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext)
			throws HttpException, IOException
	{
        File baseDir = parent.getExternalFilesDir(null);
        Uri uri = Uri.parse(request.getRequestLine().getUri());
        String filePath = uri.getQueryParameter("path");
        String path = baseDir  + "/" + filePath;
        File file = new File(path);
        StringBuilder sb = new StringBuilder();
        if (file.isDirectory())
		{
            File[] files = file.listFiles();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            for (File f : files)
			{
                sb.append(f.getName());
                sb.append(";");
                sb.append(df.format(new Date(f.lastModified())));
                sb.append(";");
                sb.append(f.isDirectory() ? "d" : "f");
                sb.append("\n");
            }
            response.setEntity(new StringEntity(sb.toString()));
        }
        else
		{
            response.setEntity(new StringEntity(""));
        }
    }
}
