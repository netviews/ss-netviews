package org.onosproject.nifwd_combined.impl;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import gov.nist.csd.pm.exceptions.PMException;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

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
//            PrintWriter serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);
//
//            serverPrintOut.println("Hello World! Enter Peace to exit.");

            //Have the server take input from the client and echo it back
            //This should be placed in a loop that listens for a terminator text e.g. bye
//            boolean done = false;

            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
//                serverPrintOut.println("Echo from <Your Name Here> Server: " + line);
//
//                if(line.toLowerCase().trim().equals("peace")) {
//                    done = true;
//                }
                if (line.equals("exit")) {
                	break;
                }
                
                //trigger a recreation of the policy
                try {
					PolicyEngine.getInstance().createPolicyGraph("/PATH-FROM-HOME/input-files/demo-topo-ref/topo-ref-policy.json");
				} catch (PMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				};
//                policyEngine.createPolicyGraph("/PATH-FROM-HOME/input-files/demo-topo-ref/topo-ref-policy.json");
            }
            
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
