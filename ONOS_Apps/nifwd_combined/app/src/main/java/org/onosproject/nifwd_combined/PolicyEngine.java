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
import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService
@Component(immediate = true)
public class PolicyEngine {

    Random rand = new Random();
    Graph graph = new MemGraph();
    private final Logger log = getLogger(getClass());
    Decider decider;

    public void createPolicyGraph(String filePath) throws FileNotFoundException,IOException, PMException{
        log.info("\n&&& createPolicyGraph &&&\n");
        try
        {
            //File file = new File("/home/ianjum/GitNetViews/netviews-code/netviews-policy-machine/src/policyInput/policySample01.json");
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String json = new String(data, "UTF-8");


            GraphSerializer.fromJson(graph, json);
            
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

    /**
     * Method to add a new policy to the policy graph. This method takes
     * a String where each line of the String is in 1 of 3 formats: 
     * ' node <name> <type> <properties>' to add a node
     * ' assign <child> <parent>' to add an assignment
     * ' assoc <ua> <target>' to add an association
     * 
     * @param newPolicyInfo is a String in the above format
     */
    public void addGraphElement(String newPolicyInfo) throws FileNotFoundException {
        //String format: node <name> <type> <properties>

        GraphSerializer.deserialize(graph, newPolicyInfo);

        decider = new PReviewDecider(graph, null);
    }

    /**
     * Method for deleting a node, association, and assignment.
     * One of each type may be provided.
     * 
     * @param node the name of the node to be deleted (or null)
     * @param assoc the association to be deleted (or null)
     * @param assign the assignment to be deleted (or null)
     */
    public void deleteGraphElement(String node, String assoc, String assign) throws Exception {
        //String format: node <name> <type> <properties>
    	//               assoc <child?> <parent?>
    	//               assign <> <>
    	// TODO: parse assoc and assign strings for child and parent in order to delete.
    	// Using space as delimeter I suppose.
    	String child; // need different ones for association and assignment???
    	String parent;
    	
        // try {
            if (node != null) {
                graph.deleteNode(node);
            } 
            if (assoc != null) {
                graph.dissociate(child, parent);
            }
    
            if (assign != null) {
                graph.deassign(child, parent);
            }

            decider = new PReviewDecider(graph, null);

        // } catch (FileNotFoundException e) {

        // } 
        
    }

}
