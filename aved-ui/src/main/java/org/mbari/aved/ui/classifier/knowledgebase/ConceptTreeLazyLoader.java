/*
 * @(#)ConceptTreeLazyLoader.java
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

//~--- JDK imports ------------------------------------------------------------

import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author achase
 *
 *
 */
public class ConceptTreeLazyLoader implements TreeExpansionListener {
    private final DefaultTreeModel treeModel;

    /**
     * Constructs ...
     *
     *
     * @param model
     */
    public ConceptTreeLazyLoader(DefaultTreeModel model) {
        treeModel = model;
    }

    /*
     *  (non-Javadoc)
     * @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent)
     */

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param event
     */
    public void treeCollapsed(TreeExpansionEvent event) {

        // TODO Auto-generated method stub
    }

    /*
     *  (non-Javadoc)
     * @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent)
     */

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param event
     */
    public void treeExpanded(final TreeExpansionEvent event) {
        final DefaultMutableTreeNode node        = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
        final TreeConcept            treeConcept = (TreeConcept) node.getUserObject();
        Thread                       lazyLoader  = new Thread() {
            public void run() {
                if ((treeConcept != null) && treeConcept.lazyExpand(node)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            treeModel.reload(node);
                        }
                    });
                }
            }
        };

        lazyLoader.start();
    }
}
