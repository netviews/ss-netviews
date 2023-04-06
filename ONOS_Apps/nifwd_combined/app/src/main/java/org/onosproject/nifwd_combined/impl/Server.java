package org.onosproject.nifwd_combined.impl;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import gov.nist.csd.pm.exceptions.PMException;

public class Server {
//    public static void main(String[] args) {
//        connectToServer();
//    }
	

    public static void connectToServer() {
        //Try connect to the server on an unused port eg 9191. A successful connection will return a socket
    	
        try(ServerSocket serverSocket = new ServerSocket(9191)) {
            Socket connectionSocket = serverSocket.accept();

            //Create Input&Outputstreams for the connection
            InputStream inputToServer = connectionSocket.getInputStream();
            OutputStream outputFromServer = connectionSocket.getOutputStream();

            Scanner scanner = new Scanner(inputToServer, "UTF-8");
            PrintWriter serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);

            
            serverPrintOut.println("Server has been created on port 9191");

            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                serverPrintOut.println("You entered something");

                if (line.equals("exit")) {
                	break;
                }
                
                //trigger a recreation of the policy
                try {
					PolicyEngine.getInstance().createPolicyGraph("/PATH-FROM-HOME/input-files/demo-topo-ref/topo-ref-policy.json");
				} catch (PMException e) {
					 serverPrintOut.println(e.getLocalizedMessage());
				};
				 serverPrintOut.println("Might have changed the policy engine");
            }
            
            serverSocket.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
