/*
 * @(#)KnowledgeBaseUtil.java
 * 
 * Copyright 2013 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */



package org.mbari.aved.ui.classifier.knowledgebase;

//~--- non-JDK imports --------------------------------------------------------

import com.google.inject.Injector;

import org.mbari.aved.ui.utils.PropertiesUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vars.ToolBelt;

import vars.knowledgebase.Concept;
import vars.knowledgebase.ConceptDAO;
import vars.knowledgebase.ui.Lookup;

//~--- JDK imports ------------------------------------------------------------

import java.util.Properties;

/**
 *  * Utility to check if MBARI knowledge base is available
 *   * @author dcline
 */
public class KnowledgeBaseUtil {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseUtil.class);

    /**
     * This is a properties object to house the properties related to the DSS
     * deployment
     */
    private static final Properties avedacProperties = PropertiesUtils.getAVEDacProperties();

    /**
     * Checks to see if the MBARI knowledge base is available.  This currently
     * only works within the MBARI firewall.
     *
     * @return true if the VARS knowledgebase is available, false otherwise
     */
    public static boolean isKnowledgebaseAvailable() {
        boolean available = false;
        
        boolean enabled   = Boolean.valueOf(avedacProperties.getProperty("vars.enable"));

        if (enabled) {
            try {
                Injector   injector = (Injector) Lookup.getGuiceInjectorDispatcher().getValueObject();
                ToolBelt   toolBelt = injector.getInstance(ToolBelt.class);
                ConceptDAO dao      = toolBelt.getKnowledgebaseDAOFactory().newConceptDAO();
                Concept    root     = dao.findRoot();

                available = (root != null);
            } catch (Exception e) {
                log.info("Failed to connect to knowledebase", e);
            }
        }

        return available;
    }
}
