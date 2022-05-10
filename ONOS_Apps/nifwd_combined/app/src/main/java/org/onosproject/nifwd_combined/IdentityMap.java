/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.nifwd.app;
package org.onosproject.nifwd_combined.impl;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.gson.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

public class IdentityMap {
    private final Logger log = getLogger(getClass());
    HashMap<String, Identity> map = new HashMap<>();

    public void createMapping(String filePath) throws FileNotFoundException, IOException{
        log.info("\n&&& Identity mapping will be created &&&\n");
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            // Convert JSON File to Java Object
            JsonElement jelement = gson.fromJson(reader, JsonElement.class);
            JsonArray jarray = jelement.getAsJsonArray();
            Iterator<?> itr = jarray.iterator();
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            HashMap<String, String> node = new HashMap<>();

            // Iterate over the JSON mapping each IP to ID & Name
            log.info("\n&&& START &&&\n");
            while(itr.hasNext()) {
                String json = itr.next().toString();
                // Convert the JSON in String to HashMap
                node = gson.fromJson(json, type);
                // Temporary HashMap to store ID and Name mapping
                Identity temp = new Identity(node.get("ID"), node.get("Name"));
                map.put(node.get("IP"), temp);
                //log.info(node.get("IP")+" "+ map.get(node.get("IP")).getID());
            }
            log.info("\n&&& END &&&\n");

        } catch (IOException e) {
            log.info(e.toString());
        }
    }

    public Identity getHostIdentity(String ip){
        return map.get(ip);
	/*if (ip.equals("10.0.0.10")){ 
                        return("h10");
	} else if(ip.equals("10.0.0.13")){
                        return("h13");
        } else {
		return("");
	}*/
    }

    public String getSudoIdentiy(String ip) {
        if (ip.compareTo("10.0.0.1")==0){
            return "5";
        }
        if (ip.compareTo("10.0.0.2")==0){
            return "6";
        }
        if (ip.compareTo("10.0.0.3")==0){
            return "7";
        }
        if (ip.compareTo("10.0.0.4")==0){
            return "8";
        }
        else
            return null;
    }
}
