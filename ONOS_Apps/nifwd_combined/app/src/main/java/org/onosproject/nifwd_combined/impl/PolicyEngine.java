package org.onosproject.nifwd_combined.impl;

import com.google.gson.Gson;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.decider.Decider;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.graph.model.relationships.Assignment;
import gov.nist.csd.pm.pip.graph.model.relationships.Association;
import org.onosproject.nifwd_combined.impl.IntentReactiveForwarding;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.NotActiveException;
import java.rmi.NoSuchObjectException;
import java.util.NoSuchElementException;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class PolicyEngine {

    Random rand = new Random();
    Graph graph = new MemGraph();
    private final Logger log = getLogger(getClass());
    Decider decider;

    //Singleton instance of the PolicyEngine
    private static PolicyEngine policyEngine = null;
    
    private PolicyEngine() {
    	// None
    }
    
    //Retrieve the instance of the PolicyEngine
    public static synchronized PolicyEngine getInstance() {
    	if (policyEngine == null) {
    		policyEngine = new PolicyEngine();
    	}
    	return policyEngine;
    }
    
    public void createPolicyGraph(String filePath) throws FileNotFoundException,IOException, PMException{
        // This method is now used both when the app is first activated and when changing the
    	// policy after that, so we changed it to use a tempGraph so that there will still be
    	// policy in place until the new graph is ready
    	log.info("\n&&& createPolicyGraph &&&\n");
        Graph tempGraph = new MemGraph();
        // TODO: why would you do this next line? It defeats the purpose of having the tempGraph
        graph = new MemGraph();
        try
        {
            //File file = new File("/home/ianjum/GitNetViews/netviews-code/netviews-policy-machine/src/policyInput/policySample01.json");
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String json = new String(data, "UTF-8");
            
            //Displays the policy in the ONOS server display whenever the policy gets created
            log.info("\n\n\n************Policy************\n");
            log.info(json);
            log.info("\n\n\n************End Policy**************\n");


            GraphSerializer.fromJson(tempGraph, json);
            
            graph = tempGraph;
            
	    decider = new PReviewDecider(graph, null);
	    //boolean decision = getPermission("h1", "h10", "tcp/9100");

        } catch (FileNotFoundException f){
            log.info(f.getLocalizedMessage());
        }catch (IOException e) {
            log.info(e.getLocalizedMessage());
        }
        catch (PMException p){
            log.info("PM"+p.getMessage());
        }

    }
    
    public boolean getPermission(String subject, String object, String action) throws IOException, PMException{        
        Set<String> permissions = decider.list(subject, "0" , object);
        return permissions.contains(action);
	/*if (subject.equals("h10") && object.equals("h13") && action.equals("tcp/80")){
		return true;
	} else {
		return false;
	}*/
    }
    
}
