/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.np;

import java.io.IOException;
import java.io.FileNotFoundException;
import gov.nist.csd.pm.exceptions.PMException;

/**
 * Netviews Service Interface.
 */
public interface NetviewsService {

    /**
     * Returns a boolean decision.
     *
     * @return boolean decision to install.
     */
    //boolean getPermission(String subject, String object, String action) throws IOException, PMException;
    boolean getPermission() throws IOException, PMException;
    public void createPolicyGraph(String filePath) throws FileNotFoundException,IOException, PMException;
}
