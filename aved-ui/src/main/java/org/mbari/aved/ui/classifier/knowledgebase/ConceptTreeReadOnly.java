/*
 * @(#)ConceptTreeReadOnly.java
 * 
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbari.aved.ui.classifier.knowledgebase;

//~--- non-JDK imports --------------------------------------------------------

import org.mbari.vars.knowledgebase.model.Concept;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * <p>This class is very similar to the <code>ConceptTree</code> from the knowledgebase
 * maintenance application.  It is a new class because it also implements drag and drop
 * functionality.  It is a tree representation of the information in the knowledgebase
 * and is a read only format so the user's cannot add or delete nodes from the tree.
 * The drag and drop functionality was developed for use with the
 * <code>ConceptButtonDropPanel</code> class so user's can drag concepts from the tree
 * to the concept button panel and then click on those buttons to add annotations.
 *
 * Although this class can be used with any tree of concepts, the main usage would be
 * by getting an instance of a <code>KnowledgeBaseMngr</code>
 * <pre>
 *      KnowledgeBaseMngr knowledgeBaseMngr = null;
 *      try {
 *              knowledgeBaseMngr = new KnowledgeBaseMngr();
 *      }
 *      catch ( IOException ioe ) {
 *              System.err.println("IOException caught while trying to create the knowledge base manager: " + ioe.getMessage());
 *              ioe.printStackTrace();
 *      }
 *      catch ( Exception e ) {
 *              System.err.println("Unknown Exception caught while trying to create the knowledge base manager: " + e.getMessage());
 *              e.printStackTrace();
 *      }
 *      ConceptTreeReadOnly conceptTreeReadOnly = new ConceptTreeReadOnly(knowledgeBaseMngr.getRootConcept());
 * </pre>
 * This can then be used to instantiate something like a <code>KnowledgeBasePanel</code> and placed in a GUI.
 * </p><hr>
 *
 * @author  : $Author: dcline $
 * @version  : $Revision: 1.1 $
 * @see  org.mbari.vars.knowledgebase.ui.ConceptTree
 * @see  ConceptButtonDropPanel
 * @see  ConceptButtonTransferable
 * @see  KnowledgeBasePanel
 *
 * <hr><p><font size="-1" color="#336699"><a href="http://www.mbari.org">
 * The Monterey Bay Aquarium Research Institute (MBARI)</a> provides this
 * documentation and code &quot;as is&quot;, with no warranty, express or
 * implied, of its quality or consistency. It is provided without support and
 * without obligation on the part of MBARI to assist in its use, correction,
 * modification, or enhancement. This information should not be published or
 * distributed to third parties without specific written permission from
 * MBARI.</font></p><br>
 *
 * <font size="-1" color="#336699">Copyright 2002 MBARI.<br>
 * MBARI Proprietary Information. All rights reserved.</font><br><hr><br>
 */
public class ConceptTreeReadOnly extends ConceptTree {
    private static Logger log = Logger.getLogger("vars.annotation");

    // End getSelectedConcept

    /**
     * The constructor for the tree
     *
     * @param  rootConcept The <code>Concept</code> to be used as the root of the tree
     */
    public ConceptTreeReadOnly(Concept rootConcept) {
        super(rootConcept);

        // Now load the model into the tree
        loadModel(rootConcept);

        // Initialize the tree properties
        initTreeProperties();

        // Setup any tree listeners
        setupListeners();
    }

    /**
     * This method returns the currently selected concept in the concept tree
     *
     * @return  the <code>Concept</code> that is currently selected in the tree
     */
    public Concept getSelectedConcept() {
        Concept                concept = null;
        DefaultMutableTreeNode node    = getSelectedNode();

        if (node != null) {
            concept = (Concept) node.getUserObject();
        }

        return concept;
    }

    // End constructor

    /**
     * Intentially does nothing
     */
    public void removeConcept() {

        // Overridden to avoid write access
    }

    /**
     * Intentially does nothing
     *
     * @param  concept
     */
    public void removeConcept(String concept) {

        // Overridden to avoid write access
    }

    /**
     * Intentially does nothing
     *
     * @param  name Description of the Parameter
     */
    public void removeConceptName(String name) {

        // Overridden to avoid write access
    }
}
