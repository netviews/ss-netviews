package org.onosproject.nifwd_combined.impl;

//Creates a thread to host a Socket Server
public class ServerThread implements Runnable {
	public void run() {
		Server.connectToServer();
	}
}
