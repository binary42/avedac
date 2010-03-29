/*
 * @(#)SearchableConceptTreePanel.java
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

import foxtrot.Job;
import foxtrot.Worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mbari.vars.dao.DAOException;
import org.mbari.vars.knowledgebase.model.Concept;
import org.mbari.vars.knowledgebase.model.dao.CacheClearedEvent;
import org.mbari.vars.knowledgebase.model.dao.CacheClearedListener;
import org.mbari.vars.knowledgebase.model.dao.KnowledgeBaseCache;
import org.mbari.vars.knowledgebase.model.dao.LWConceptNameDAO;

import vars.knowledgebase.IConceptName;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * <p>A Panel that contains the JTree that represents the knowledgebase. Has
 * great search methods.</p>
 *
 * <h2><u>License</u></h2>
 * <p><font size="-1" color="#336699"><a href="http://www.mbari.org">
 * The Monterey Bay Aquarium Research Institute (MBARI)</a> provides this
 * documentation and code &quot;as is&quot;, with no warranty, express or
 * implied, of its quality or consistency. It is provided without support and
 * without obligation on the part of MBARI to assist in its use, correction,
 * modification, or enhancement. This information should not be published or
 * distributed to third parties without specific written permission from
 * MBARI.</font></p>
 *
 * <p><font size="-1" color="#336699">Copyright 2004 MBARI.
 * MBARI Proprietary Information. All rights reserved.</font></p>
 *
 * @author <a href="http://www.mbari.org">MBARI</a>
 * @version $Id: SearchableConceptTreePanel.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class SearchableConceptTreePanel extends SearchableTreePanel {

    /**
     * Hard-coded MBARI specific informaiton. The video labe requested that
     * the Knowledgebase tree be open to the marin-organism node on startup,
     * since that will be the most often used node.
     *
     * TODO 20050909 brian: THis needs to be pulled out into a configuration
     * file.
     *
     */
    private static final String MARINE_ORGANISM = "marine-organism";
    private static final Log    log             = LogFactory.getLog(SearchableConceptTreePanel.class);

    /** <!-- Field description --> */
    public static final Object DISPATCHER_KEY = SearchableConceptTreePanel.class;

    /**
     * Store previous searches so that we don't try to do database lookup
     * on them again.
     */
    private final Collection cachedGlobSearches;

    /**
     * Store previous searches so that we don't try to do database lookup
     * on them again.
     */
    private final Collection cachedWordSearches;

    /**
     * Constructor
     */
    public SearchableConceptTreePanel() {
        super();
        KnowledgeBaseCache.getInstance().addCacheClearedListener(new CacheListener());
        cachedWordSearches = new HashSet();
        cachedGlobSearches = new HashSet();
        Dispatcher.getDispatcher(DISPATCHER_KEY).setValueObject(this);
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param node
     *
     * @return
     */
    public String getNodeTextToSearch(final DefaultMutableTreeNode node) {
        final Object       userObject  = node.getUserObject();
        final StringBuffer conceptName = new StringBuffer();

        // Objects whos children have not been loaded yet will return a Boolean
        // as a user object. We should ignore these as much as we can.
        if (userObject instanceof TreeConcept) {
            final TreeConcept concept = (TreeConcept) node.getUserObject();
            final Concept     c       = concept.getConcept();

            /*
             * The text is actually a composite of all names,
             * including primary, secondary, and common
             */
            final String[] names = c.getConceptNamesAsStrings();

            for (int i = 0; i < names.length; i++) {
                conceptName.append(names[i]);
                conceptName.append(" ");
            }
        }

        return conceptName.toString();
    }

    /**
     * This overridden method does a database lookup for searches. This is a
     * woorkaournd needed for lazy loading. This method will load the branches
     * of all matches from the database.
     *
     * @param text
     * @param useGlobSearch
     *
     * @return
     */
    public boolean goToMatchingNode(final String text, final boolean useGlobSearch) {

        /*
         * Disable so that folks can't start multiple searches.
         */
        getBtnSearch().setEnabled(false);
        getSearchTextField().setEnabled(false);
        loadNodes(text, useGlobSearch);

        boolean ok = super.goToMatchingNode(text, useGlobSearch);

        getBtnSearch().setEnabled(true);
        getSearchTextField().setEnabled(true);
        getSearchTextField().requestFocus();

        return ok;
    }

    /**
     * Perfroms the database lookup of all matching Concepts.
     * @param text
     * @param useGlobSearch
     */
    private void loadNodes(final String text, final boolean useGlobSearch) {
        Collection matches = null;

        try {
            if (useGlobSearch) {
                if (!cachedGlobSearches.contains(text)) {
                    matches = LWConceptNameDAO.getInstance().findNamesBySubString(text);
                    cachedGlobSearches.add(text);
                    cachedWordSearches.add(text);
                }
            } else {
                if (!cachedWordSearches.contains(text)) {
                    matches = LWConceptNameDAO.getInstance().findNamesStartingWith(text);
                    cachedWordSearches.add(text);
                }
            }
        } catch (DAOException e) {
            if (log.isErrorEnabled()) {
                log.error("Database lookup of " + text + " failed", e);
            }
        }

        /*
         * If we loaded the matched names from the database then we need
         * to open the Concept such that it gets cached under the root
         * concept.
         */
        if (matches != null) {
            final ProgressMonitor progressMonitor = new ProgressMonitor(AppFrameDispatcher.getFrame(),
                                                        "Loading search results for '" + text + "'", "", 0,
                                                        matches.size());
            int n = 0;

            for (Iterator i = matches.iterator(); i.hasNext(); ) {
                n++;

                final IConceptName cn = (IConceptName) i.next();

                progressMonitor.setProgress(n);
                progressMonitor.setNote("Loading '" + cn.getName() + "'");

                /*
                 * Have to open the node in a seperate thread for the
                 * progress monitor to update. Here we're using foxtrot.
                 */
                Worker.post(new Job() {
                    public Object run() {
                        openNode((Concept) cn.getConcept());

                        return null;
                    }
                });
            }

            progressMonitor.close();
        }
    }

    /**
     * Loads the branch of a particular concept. This method does the following
     * <ol>
     *      <li>Walks from the concept up the tree to the root concept, storing
     *      the concepts in a list. (This is very fast)</li>
     *  <li>Starts walking from the root down (using lazyExpand), searching each
     *      childnode for a matching primary name (which was stored in the first
     *      step</li>
     *  <li>If a matching primary name is found this stops otherwise
     *              it opens the next level and searches for the next mathc in the list.</li>
     *  <li></li>
     * </ol>
     * @param concept
     */
    private void openNode(final Concept concept) {
        if (log.isDebugEnabled()) {
            log.debug("Opening node containing " + concept);
        }

        if (concept == null) {
            return;
        }

        // Get the list of concepts up to root
        final LinkedList conceptList = new LinkedList();
        Concept          c           = concept;

        while (c != null) {
            conceptList.add(c);
            c = (Concept) c.getParentConcept();
        }

        // Walk the tree from root on down opening nodes as we go
        final ListIterator i = conceptList.listIterator(conceptList.size());

        // Skip the root
        i.previous();

        final JTree                  tree      = getJTree();
        final DefaultTreeModel       treeModel = (DefaultTreeModel) tree.getModel();
        final DefaultMutableTreeNode rootNode  = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath                     path      = new TreePath(rootNode.getPath());

        tree.setSelectionPath(path);

        DefaultMutableTreeNode parentNode = rootNode;

        while (i.hasPrevious()) {
            c = (Concept) i.previous();

            final TreeConcept parentTreeConcept = (TreeConcept) parentNode.getUserObject();

            parentTreeConcept.lazyExpand(parentNode);

            // treeModel.reload(parentNode);
            final Enumeration enm = parentNode.children();

            while (enm.hasMoreElements()) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) enm.nextElement();
                final TreeConcept            tc   = (TreeConcept) node.getUserObject();

                if (tc.getName().equals(c.getPrimaryConceptNameAsString())) {
                    parentNode = node;

                    break;
                }
            }
        }

        final TreeNode _parentNode = parentNode;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                treeModel.reload(_parentNode);
                tree.scrollPathToVisible(new TreePath(_parentNode));
            }
        });
    }

    /**
     * Override JTree to only accept instances of ConceptTree
     *
     * @param tree
     */
    public void setJTree(JTree tree) {
        if (!(tree instanceof ConceptTree)) {
            throw new IllegalArgumentException("JTree must be an instanceof ConceptTree");
        }

        super.setJTree(tree);
    }

    /**
     * Resets the KNowledgebaseTree when the cache is cleared
     * @author brian
     *
     */
    private final class CacheListener implements CacheClearedListener {

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param evt
         */
        public void afterClear(CacheClearedEvent evt) {
            cachedGlobSearches.clear();
            cachedWordSearches.clear();

            ConceptTree tree = (ConceptTree) getJTree();

            try {
                Concept rootConcept = evt.getCache().findRootConcept();

                tree.loadModel(rootConcept);
            } catch (DAOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to create a new concept tree", e);
                }

                JOptionPane.showMessageDialog(SearchableConceptTreePanel.this,
                                              "The knowledgebase tree and the database are no longer \n"
                                              + "in sync. Please restart the application.", "VARS - Error",
                                                  JOptionPane.ERROR_MESSAGE, null);
            }

            SearchableConceptTreePanel.this.validate();

            /*
             * Video lab requested that the physical object node be open
             * so that users on a ship can see where the marine organisms are.
             *
             * TODO 20040518 brian: This is a hard-coded feature specific
             * to the video la. If we ever export this out of MBARI we'll
             * need to remove this.
             */
            goToMatchingNode(MARINE_ORGANISM, false);
        }

        /**
         * <p><!-- Method description --></p>
         *
         *
         * @param evt
         */
        public void beforeClear(CacheClearedEvent evt) {

            // Do nothing
        }
    }
}
