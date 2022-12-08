package org.sil.hearthis;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * SyncServer manages the 'web server' for the synchronization service that supports data
 * exchange with HearThis desktop.
 * This is using classes like BasicHttpProcessor from org.apache.http with is considered obsolete.
 * However, there is no obvious replacement I can find for it in connection with making a
 * web server. For now, I've been able to keep it working at least as far as Android 12
 * by adding "useLibrary 'org.apache.http.legacy'" to build.gradle and (later) a uses-library
 * declaration to AndroidManifext.xml. The editor seems to think this file will not compile, even so,
 * but somehow it actually does.
 */
public class SyncServer extends Thread {
    SyncService _parent;
    Integer _serverPort = 8087;
    private BasicHttpProcessor httpproc = null;
    private BasicHttpContext httpContext = null;
    private HttpService httpService = null;
    private HttpRequestHandlerRegistry registry = null;
    boolean _running;

    public SyncServer(SyncService parent)
    {
        super("HearThisAndroidServer");
        _parent = parent;

        httpproc = new BasicHttpProcessor();
        httpContext = new BasicHttpContext();

        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        httpService = new HttpService(httpproc,
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory());


        registry = new HttpRequestHandlerRegistry();

        registry.register("*", new DeviceNameHandler(_parent));
        registry.register("/getfile*", new RequestFileHandler(_parent));
        registry.register("/putfile*", new AcceptFileHandler(_parent));
        registry.register("/list*", new ListDirectoryHandler(_parent));
        registry.register("/notify*", new AcceptNotificationHandler());
        httpService.setHandlerResolver(registry);
    }
    public synchronized void startThread() {
        if (_running)
            return; // already started, must not do twice.
        _running = true;

        super.start();
    }

    // Clear flag so main loop will terminate after next request.
    public synchronized void stopThread(){
        _running = false;
    }

    // Method executed in thread when super.start() is called.
    @Override
    public void run() {
        super.run();

        try {
            ServerSocket serverSocket = new ServerSocket(_serverPort);

            serverSocket.setReuseAddress(true);

            while(_running){
                try {
                    final Socket socket = serverSocket.accept();

                    DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();

                    serverConnection.bind(socket, new BasicHttpParams());

                    httpService.handleRequest(serverConnection, httpContext);

                    serverConnection.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (HttpException e) {
                    e.printStackTrace();
                }
            }

            serverSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
