package com.danielflower.internalnpmserver.controllers;

import com.danielflower.internalnpmserver.webserver.ContentTypeGuesser;
import com.danielflower.internalnpmserver.webserver.RequestHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import java.io.*;
import java.util.Date;

public class StaticHandlerImpl implements StaticHandler, RequestHandler {

	private final File webroot;
	private final String webServerEndPoint;
	private final boolean offline;
	private final String npmRepositoryURL;
	private final ContentTypeGuesser contentTypeGuesser = new ContentTypeGuesser();

	public StaticHandlerImpl(File folderToServeFrom, int port, String webServerHostName, boolean offline, String npmRepositoryURL) {
		this.webroot = folderToServeFrom;
		this.webServerEndPoint = "http://" +webServerHostName +":"  +port;
		this.offline = offline;
		this.npmRepositoryURL = npmRepositoryURL;
	}

	@Override
	public boolean canHandle(String path) {
		if (path.contains("..") || path.contains("~")) {
			return false;
		}

		int queryIndex = path.indexOf('?');
		if (queryIndex > -1) {
			path = path.substring(0, queryIndex);
		}

		File localFile = new File(webroot, path);
		return localFile.isFile();
	}

	@Override
	public void handle(Request request, Response resp) throws Exception {
		String path = request.getPath().getPath();
		String etag = request.getValue("If-None-Match");
		streamFileToResponse(path, etag, resp);
	}


	@Override
	public void streamFileToResponse(String path, String etagFromClient, Response resp) throws IOException {
		final File localFile = new File(webroot, path);
		String etag = String.valueOf(localFile.lastModified());
		OutputStream out = resp.getOutputStream();
		if (etag.equals(etagFromClient)) {
			resp.setCode(304);
			resp.setDescription("304 Not Modified");
		} else {

			String mimeType = contentTypeGuesser.fromName(localFile.getName());
			long time = System.currentTimeMillis();

			resp.setValue("Content-Type", mimeType);
			resp.setDate("Date", time);
			resp.setDate("Last-Modified", localFile.lastModified());
			resp.setValue("ETag", String.valueOf(localFile.lastModified()));

			if ("/robots.txt".equals(path) || "/favicon.ico".equals(path)) {
				resp.setValue("Cache-Control", "max-age=604800, public");
			} else {
				resp.setValue("Cache-Control", "max-age=29030400, public");
			}

			if (path.endsWith(".json") && isOffline()) {
				String contents = FileUtils.readFileToString(localFile, "UTF-8").replace(npmRepositoryURL, webServerEndPoint);
				IOUtils.write(contents, out, "UTF-8");
			} else {
				InputStream in = new FileInputStream(localFile);
				IOUtils.copy(in, out);
				IOUtils.closeQuietly(in);
			}
		}
		IOUtils.closeQuietly(out);
	}

	private boolean isOffline() {
		return offline && !npmRepositoryURL.equalsIgnoreCase(webServerEndPoint);
	}

	@Override
	public Date dateCreated(String path) {
		final File localFile = new File(webroot, path);
		return new Date(localFile.lastModified());
	}
}
