package com.danielflower.internalnpmserver.services;

public class NeverRemoteDownloadPolicy implements RemoteDownloadPolicy {

    public NeverRemoteDownloadPolicy() {
    }

	@Override
	public boolean shouldDownload(String localPath) {
		return false;
	}
}
