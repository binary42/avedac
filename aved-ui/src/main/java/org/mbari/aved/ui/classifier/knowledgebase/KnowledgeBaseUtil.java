opyright 2009 MBARI
 *
 *  * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1 
 *   * (the "License"); you may not use this file except in compliance 
 *    * with the License. You may obtain a copy of the License at
 *     *
 *      * http://www.gnu.org/copyleft/lesser.html
 *       *
 *        * Unless required by applicable law or agreed to in writing, software
 *         * distributed under the License is distributed on an "AS IS" BASIS,
 *          * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *           * See the License for the specific language governing permissions and
 *            * limitations under the License.
 *             */


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

