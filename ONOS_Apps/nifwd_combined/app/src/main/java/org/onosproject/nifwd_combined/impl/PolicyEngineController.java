///**
// * 
// */
//package org.onosproject.nifwd_combined.impl;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//
//import gov.nist.csd.pm.exceptions.PMException;
//
//import javax.ejb.Stateless;
//import javax.jws.WebService;
//
///**
// * A class that uses the singleton instance of the PolicyEngine.
// * Defines SOAP API endpoints and functions as a server.
// * 
// */
//
//@Stateless
//@WebService(endpointInterface = "netviews.policyengine")
//public class PolicyEngineController {
//	private PolicyEngine policyEngine = PolicyEngine.getInstance();
//	
//	/**
//	 * 
//	 * @param filePath The path to the policy json file that we will make
//	 * 		  policy graph from
//	 * @throws FileNotFoundException If the given file is not found
//	 * @throws IOException
//	 * @throws PMException
//	 */
//	 public void createPolicyGraph(String filePath) throws FileNotFoundException,IOException, PMException{
//		 policyEngine.createPolicyGraph(filePath);
//	 }
//}