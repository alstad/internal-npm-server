package com.danielflower.internalnpmserver.webserver;

import com.danielflower.internalnpmserver.App;
import com.danielflower.internalnpmserver.Config;
import com.danielflower.internalnpmserver.controllers.HomepageHandler;
import com.danielflower.internalnpmserver.controllers.NpmHandler;
import com.danielflower.internalnpmserver.controllers.StaticHandler;
import com.danielflower.internalnpmserver.controllers.StaticHandlerImpl;
import com.danielflower.internalnpmserver.rendering.HttpViewRenderer;
import com.danielflower.internalnpmserver.rendering.NonCachableHttpViewRenderer;
import com.danielflower.internalnpmserver.rendering.VelocityViewRenderer;
import com.danielflower.internalnpmserver.rendering.ViewRenderer;
import com.danielflower.internalnpmserver.services.*;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static ViewRenderer viewRenderer = new VelocityViewRenderer("/views/");
    private static HttpViewRenderer httpViewRenderer = new NonCachableHttpViewRenderer(viewRenderer);
    public static final File STATIC_ROOT;

    static {
        File root = new File("src/main/resources/webroot");
        if (!root.isDirectory()) {
            // for when run from start.sh... this is horrible... forgive me
            root = new File("server/webroot");
        }
        STATIC_ROOT = root;
    }

    private SocketConnection connection;
    private final Container webContainer;
    private final int port;
    private final String hostname;

    private WebServer(Container webContainer, int port, String hostname) {
        this.webContainer = webContainer;
        this.port = port;
        this.hostname = hostname;
    }

    public static WebServer createWebServer(Config config) {

        FileDownloader downloader =
                new PackageReWritingFileDownloader(
                new FileDownloaderImpl(config.getProxy()), config.getNpmRepositoryURL(), config.getNpmEndPoint().toString());

        StaticHandler npmCacheStaticHandler = new StaticHandlerImpl(config.getNpmCacheFolder(), config.getPort(), config.getWebServerHostName(), config.isOffline(), config.getNpmRepositoryURL());
        RemoteDownloadPolicy remoteDownloadPolicy = getRemoteDownloadPolicy(config, npmCacheStaticHandler);
        RequestHandler[] handlers = new RequestHandler[]{
                new HomepageHandler(httpViewRenderer, config),
                new NpmHandler(downloader, npmCacheStaticHandler, config.getNpmRepositoryURL(), config.getNpmCacheFolder(), remoteDownloadPolicy),
                new StaticHandlerImpl(STATIC_ROOT, config.getPort(), config.getWebServerHostName(), config.isOffline(), config.getNpmRepositoryURL())
        };
        RequestRouter router = new RequestRouter(handlers);
        ErrorHandlingWebContainer errorHandler = new ErrorHandlingWebContainer(router);
        return new WebServer(new LoggingWebContainer(errorHandler), config.getPort(), config.getWebServerHostName());
    }

    private static RemoteDownloadPolicy getRemoteDownloadPolicy(Config config, StaticHandler npmCacheStaticHandler) {
        if (config.isOffline()) {
            return new NeverRemoteDownloadPolicy();
        } else {
            return new ReDownloadOldJSONFilesPolicy(npmCacheStaticHandler);
        }
    }

    public void start() throws IOException {
        this.connection = new SocketConnection(new ContainerServer(webContainer));
        InetSocketAddress address = new InetSocketAddress(port);
        connection.connect(address);
        String localUrl = "http://" + hostname + ":" + address.getPort();
        log.info("Server started at " + localUrl);
        log.info("To use this as your NPM registry, run the following on your local PC:");
        log.info("npm config set registry " + localUrl + "/npm/");
    }

    public void stop() throws IOException {
        log.info("Stopping server...");
        connection.close();
        log.info("Server stopped.");
    }

}
