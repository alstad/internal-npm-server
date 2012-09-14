package com.danielflower.webstarter;

import org.apache.commons.io.IOUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticHandler implements RequestHandler {
    public static final File webroot = new File("src/main/resources/webroot");

    private final ContentTypeGuesser contentTypeGuesser = new ContentTypeGuesser();

    @Override
    public boolean canHandle(String path) {
        if (path.contains("..") || path.contains("~")) {
            return false;
        }
        File localFile = new File(webroot, path);
        return localFile.exists() && !localFile.isDirectory();
    }

    @Override
    public void handle(Request request, Response resp) throws Exception {

        String path = request.getPath().getPath();

        final File localFile = new File(webroot, path);

            String mimeType = contentTypeGuesser.fromName(localFile.getName());
            long time = System.currentTimeMillis();

            resp.set("Content-Type", mimeType);
            resp.set("Server", "UpVote/1.0");
            resp.setDate("Date", time);
            resp.setDate("Last-Modified", localFile.lastModified());

            OutputStream out = resp.getOutputStream();

            InputStream in = new FileInputStream(localFile);
            IOUtils.copy(in, out);
            in.close();

            out.flush();
            out.close();

    }
}
