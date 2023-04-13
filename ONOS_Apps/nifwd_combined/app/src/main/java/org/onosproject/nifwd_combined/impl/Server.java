
package org.onosproject.nifwd_combined.impl;

import java.io.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Intent;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.cli.AbstractShellCommand;
//import org.onosproject.cli.net.IntentRemoveCommand;


import gov.nist.csd.pm.exceptions.PMException;


public class Server {
//    public static void main(String[] args) {
//        connectToServer();
//    }

	//@Reference(cardinality = ReferenceCardinality.MANDATORY)
	//protected IntentService intentService;
	

    public static void connectToServer() {
        //Try connect to the server on an unused port eg 9191. A successful connection will return a socket
    	
        try(ServerSocket serverSocket = new ServerSocket(9191)) {
            while (true) {
            
            
            Socket connectionSocket = serverSocket.accept();

            //Create Input&Outputstreams for the connection
            InputStream inputToServer = connectionSocket.getInputStream();
            OutputStream outputFromServer = connectionSocket.getOutputStream();

            Scanner scanner = new Scanner(inputToServer, "UTF-8");
            PrintWriter serverPrintOut = new PrintWriter(new OutputStreamWriter(outputFromServer, "UTF-8"), true);

            
            serverPrintOut.println("Server has been created on port 9191");

            //while(scanner.hasNextLine()) {
                //String line = scanner.nextLine();
                serverPrintOut.println("You entered something");

                //if (line.equals("exit")) {
                	//break;
                //}
                
                //trigger a recreation of the policy
                try {
                
			PolicyEngine.getInstance().createPolicyGraph("/home/noah/netviews/ss-netviews/input-files/med-topo-ref/med-topo-policy.json");
			
		} catch (PMException e) {
			
			serverPrintOut.println(e.getLocalizedMessage());
		};
		serverPrintOut.println("About to run through Intents");
		
		//IntentSynchronizer.removeIntentsByAppId(org.onosproject.nifwd_combined);
		
		IntentService intentService = AbstractShellCommand.get(IntentService.class);
		
    		if (intentService != null) {
			serverPrintOut.println("Intent count: " + intentService.getIntentCount());
			Iterable<Intent> intents = intentService.getIntents();
			//IntentRemoveCommand.execute();
			//IntentRemoveCommand command = new IntentRemoveCommand();
			
			
               	serverPrintOut.println("Intent Service: " + intentService);
                
                	for (Intent intent : intents) {
                    		serverPrintOut.println("Intent ID: " + intent.id());
                    		intentService.withdraw(intent);
                    		intentService.purge(intent);
                	}
                	serverPrintOut.println("Intent count: " + intentService.getIntentCount());
                	
		}
		else {
			serverPrintOut.println("Intents are still null");
		}
		
		
		serverPrintOut.println("Might have changed the policy engine");
            //}
            }
            
            //serverSocket.close();
            //scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
            //serverSocket.close();
        }
    }
    
}

