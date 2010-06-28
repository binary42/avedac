
package org.mbari.aved.ui.classifier.knowledgebase;

import org.mbari.vars.knowledgebase.model.Concept;
import org.mbari.vars.knowledgebase.model.dao.ConceptDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  * Utility to check if MBARI knowledge base is available
 *   * @author dcline
 *    */
public class KnowledgeBaseUtil {
    
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseUtil.class);
    
    /**
 *      * Checks to see if the knowldegebase is available
 *           * 
 *                * @return true if the knowledgebase is available, false otherwise
 *                     */
    public static boolean isKnowledgebaseAvailable() {
        boolean available = false;
        try {
            ConceptDAO dao = ConceptDAO.getInstance();
            Concept root = dao.findRoot();
            available = (root != null);
        }
        catch (Exception e) {
            log.info("Failed to connect to knowledebase", e);
        }
        return available;
    }

}

