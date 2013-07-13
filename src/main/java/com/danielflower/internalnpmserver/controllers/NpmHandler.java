package com.danielflower.internalnpmserver.controllers;

import com.danielflower.internalnpmserver.services.FileDownloader;
import com.danielflower.internalnpmserver.webserver.RequestHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import java.io.File;
import java.net.URL;

public class NpmHandler implements RequestHandler {


    public static final String PREFIX = "/npm";
    private final FileDownloader proxyService;
    private final StaticHandler staticHandler;
    private final String npmRepositoryURL;
    private final File cacheFolder;

    public NpmHandler(FileDownloader proxyService, StaticHandler staticHandler, String npmRepositoryURL, File cacheFolder) {
        this.proxyService = proxyService;
        this.staticHandler = staticHandler;
        this.npmRepositoryURL = StringUtils.stripEnd(npmRepositoryURL, "/");

        this.cacheFolder = cacheFolder;
    }

    @Override
    public boolean canHandle(String path) {
        return path.startsWith(PREFIX);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        String remotePath = request.getTarget().substring(PREFIX.length());

        String localPath = getLocalPathTreatingExtensionlessFilesAsJSONFiles(remotePath);

        if (!staticHandler.canHandle(localPath)) {
            proxyService.fetch(new URL(npmRepositoryURL + remotePath), new File(cacheFolder, localPath));
        }

        if (staticHandler.canHandle(localPath)) {
            staticHandler.streamFileToResponse(localPath, response);
        }

//        URL url = new URL(npmRepositoryURL + remotePath);
//        proxyService.fetch(url, response);
    }

    private String getLocalPathTreatingExtensionlessFilesAsJSONFiles(String path) {
        boolean hasFileExtension = FilenameUtils.getExtension(path).length() > 1;
        if (!hasFileExtension) {
            path += ".json";
        }
        return path;
    }
}
