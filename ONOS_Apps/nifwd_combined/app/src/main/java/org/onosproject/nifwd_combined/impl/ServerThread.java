package org.onosproject.nifwd_combined.impl;


public class ServerThread implements Runnable {
	public void run() {
		Server.connectToServer();
	}
}
