/*
 * @(#)ConceptTree.java   10/03/17
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mbari.vars.dao.DAOException;
import org.mbari.vars.knowledgebase.model.Concept;
import org.mbari.vars.knowledgebase.model.dao.KnowledgeBaseCache;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A JTree implementation for displaying <code>KnowledgeBase</code>
 * <code>Concept</code> objects in the <code>MaintGui</code>.
 *
 *
 * @author  brian
 * @created  November 11, 2004
 * @version  $Id: ConceptTree.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class ConceptTree extends JTree {

    // RxTBD wcpr - Waiting for refactor to ConceptTree
    final static String        ADD_CONCEPT        = "Add Concept";
    final static String        COLLAPSE_ALL       = "Collapse All";
    final static String        COLLAPSE_CHILDREN  = "Collapse Children";
    final static String        EXPAND_ALL         = "Expand All";
    final static String        EXPAND_DESCENDENTS = "Expand Descendents";
    final static String        REMOVE_CONCEPT     = "Remove Concept";
    public final static Cursor WAIT_CURSOR        = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    public final static Cursor DEFAULT_CURSOR     = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static Log         log                = LogFactory.getLog(ConceptTree.class);

    /**
     * Required for subclasses used by annotation ui
     *
     * @param  rootConcept
     */
    public ConceptTree(Concept rootConcept) {
        loadModel(rootConcept);
        initTreeProperties();
        setupListeners();
    }

    /**
     * Adds a node to the specifed tree node for each of the children of the
     * specified <code>Concept.
     *
     * @param  node   The node to which to add children nodes.
     * @param  concept   The <code>Concept</code> whose children are to be added.
     */
    private void addChildrenNodes(DefaultMutableTreeNode node, Concept concept) {
        Iterator iterator = concept.getChildConceptColl().iterator();

        while (iterator.hasNext()) {
            Concept                childConcept = (Concept) iterator.next();
            DefaultMutableTreeNode childNode    = new SortedTreeNode(new TreeConcept(childConcept));

            node.add(childNode);
            addChildrenNodes(childNode, childConcept);
        }
    }

    /**
     * Collapses all tree nodes.
     */
    public void collapseAll() {
        setCursor(WAIT_CURSOR);

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();

        makeChildrenInvisible(rootNode);
        setCursor(DEFAULT_CURSOR);
    }

    /**
     * Collapses all children nodes under the currently selected node.
     */
    public void collapseChildren() {
        setCursor(WAIT_CURSOR);

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getSelectionPath().getLastPathComponent();

        makeChildrenInvisible(selectedNode);
        setCursor(DEFAULT_CURSOR);
    }

    /**
     * Expands all tree nodes.
     */
    public void expandAll() {
        setCursor(WAIT_CURSOR);

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) getModel().getRoot();

        makeChildrenVisible(rootNode);
        setCursor(DEFAULT_CURSOR);
    }

    /**
     * Expands all descendent nodes under the currently selected node.
     */
    public void expandDescendents() {
        setCursor(WAIT_CURSOR);

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) getSelectionPath().getLastPathComponent();

        makeChildrenVisible(selectedNode);
        setCursor(DEFAULT_CURSOR);
    }

    /**
     * Expands all tree nodes from the root down to the specified node name. Does
     * not expand the final node as that node may not have any children.
     *
     * @param  name   The name of the final node.
     * @return    The final node, which itself has not been expanded.
     */
    DefaultMutableTreeNode expandDownToNode(String name) {

        /*
         * Get a list of the family tree for the parameter concept. This list is
         * used to travel down the tree to the desired concept node.
         */
        List list = null;

        try {
            list = KnowledgeBaseCache.getInstance().findConceptFamilyTree(name);
        } catch (DAOException e) {
            if (log.isErrorEnabled()) {
                log.error("Call to knowledgebase cache failed");
            }

            AppFrameDispatcher.showErrorDialog("There was a problem talking" + " to the database. Unable to open '"
                                               + name + "' in the tree.");
        }

        Iterator familyTree = list.iterator();

        // Pop the root Concept off the stack since it is the degenerative case.
        familyTree.next();

        // Then walk down the family tree, starting at the root node.
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) getModel().getRoot();

        while (familyTree.hasNext()) {
            String nextConceptName = ((Concept) familyTree.next()).getPrimaryConceptNameAsString();

            // Need to ensure the tree node for the current family name is expanded.
            TreeConcept treeConcept = (TreeConcept) treeNode.getUserObject();

            treeConcept.lazyExpand(treeNode);

            // Find the child node for the next family member.
            boolean     found         = false;
            Enumeration childrenNodes = treeNode.children();

            while (!found && childrenNodes.hasMoreElements()) {
                treeNode    = (DefaultMutableTreeNode) childrenNodes.nextElement();
                treeConcept = (TreeConcept) treeNode.getUserObject();

                if (nextConceptName.equals(treeConcept.getName())) {
                    found = true;
                }
            }
        }

        /*
         * RxNOTE The final value of treeNode drops out of the above while loop
         * without a call to lazyExpand. This is purposeful as the final node may
         * or may not have children. We are only expanding down to the node, not
         * expanding the node itself.
         */
        return treeNode;
    }

    /**
     * Gets the name of the <code>Concept</code> represented by the currently
     * selected node.
     *
     * @return    The name of the <code>Concept</code> represented by the currently
     *  selected node.
     */
    public String getSelectedName() {
        String                 name = null;
        DefaultMutableTreeNode node = getSelectedNode();

        if (node != null) {
            name = ((TreeConcept) node.getUserObject()).getName();
        }

        return name;
    }

    /**
     * Gets the currently selected tree node.
     *
     * @return    The currently selected tree node.
     */
    protected DefaultMutableTreeNode getSelectedNode() {
        DefaultMutableTreeNode node = null;
        TreePath               path = getSelectionPath();

        if (path != null) {
            node = (DefaultMutableTreeNode) path.getLastPathComponent();
        }

        return node;
    }

    /**
     * Initialize the properties of this <code>ConceptTree</code>.
     */
    protected void initTreeProperties() {
        putClientProperty("JTree.lineStyle", "Angled");
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // don't allow edits to the Concept names directly in this tree
        setEditable(false);
        setCellRenderer(new ConceptTreeCellRenderer());
        setRootVisible(true);
    }

    /**
     * Loads the JTree model using the specified <code>Concept</code> as the root
     * node.
     *
     * @param  rootConcept Description of the Parameter
     */
    public void loadModel(Concept rootConcept) {
        if (rootConcept == null) {
            setModel(null);
        } else {
            TreeConcept            treeConcept = new TreeConcept(rootConcept);
            DefaultMutableTreeNode rootNode    = new SortedTreeNode(treeConcept);

//          addChildrenNodes( rootNode, rootConcept );
            // adding a boolean value as a child to this concept indicates that
            // the node has children that will be loaded dynamically, start off by
            // assuming this node has children, the rest will be taken care of in
            // the ConceptTreeLazyLoader
            rootNode.add(new DefaultMutableTreeNode(Boolean.TRUE));
            treeConcept.lazyExpand(rootNode);

            DefaultTreeModel model = new DefaultTreeModel(rootNode);

            setModel(model);
            addTreeExpansionListener(new ConceptTreeLazyLoader(model));
        }
    }

    /**
     * Makes the children nodes under the specified node invisible.
     *
     * @param  node   The node on which to act.
     */
    private void makeChildrenInvisible(DefaultMutableTreeNode node) {
        Enumeration children = node.children();

        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();

            if (!childNode.isLeaf()) {
                makeChildrenInvisible(childNode);

                TreeNode[] nodesFromRoot = childNode.getPath();
                TreePath   pathFromRoot  = new TreePath(nodesFromRoot);

                collapsePath(pathFromRoot);
            }
        }
    }

    /**
     * Makes the children nodes under the specified node visible.
     *
     * @param  node   The node on which to act.
     */
    private void makeChildrenVisible(DefaultMutableTreeNode node) {

        // RxTBD wcpr The Java API interaction of using TreeNodes and TreePaths
        // doesn't seem to make sense. There should be a cleaner way to implement
        // this method.
        if (node.isLeaf()) {
            return;
        }

        // Expand the node
        TreeConcept treeConcept = (TreeConcept) node.getUserObject();

        treeConcept.lazyExpand(node);

        boolean     allChildrenAreLeaves = true;
        Enumeration children             = node.children();

        while (children.hasMoreElements()) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();

            if (!childNode.isLeaf()) {
                makeChildrenVisible(childNode);
                allChildrenAreLeaves = false;
            }
        }

        if (allChildrenAreLeaves) {
            DefaultMutableTreeNode lastNode      = (DefaultMutableTreeNode) node.getLastChild();
            TreeNode[]             nodesFromRoot = node.getPath();
            TreePath               pathFromRoot  = new TreePath(nodesFromRoot).pathByAddingChild(lastNode);

            makeVisible(pathFromRoot);
        }
    }

    /**
     * Sets the selected tree node to the node representing the specified
     * <code>Concept</code> name.
     *
     * @param  name The new selectedConcept value
     */
    public void setSelectedConcept(String name) {
        if (name == null) {
            return;
        }

        // RxNOTE Strategy: The tree node for the Concept being selected may not
        // yet be expanded, so expand down to the desired node.
        DefaultMutableTreeNode treeNode = expandDownToNode(name);

        // Now select the node and scroll to it.
        TreePath path = new TreePath(treeNode.getPath());

        setSelectionPath(path);
        scrollPathToVisible(path);
    }

    /**
     * Sets up the various listeners needed for GUI interaction with this
     * <code>ConceptTree</code>.
     */
    protected void setupListeners() {

        // Add context popup menu and right mouse button selection
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {

                // Selected item before showing popup menu
                if (event.getModifiers() == MouseEvent.BUTTON3_MASK) {
                    int row = getRowForLocation(event.getX(), event.getY());

                    setSelectionRow(row);
                }
            }
        });

        // Toggle expand/collapse on ENTER
        addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    int row = getSelectionRows()[0];

                    if (isCollapsed(row)) {
                        expandRow(row);
                    } else {
                        collapseRow(row);
                    }
                }
            }
        });
    }

    /**
     *  Description of the Method
     */
    void updateTreeNode() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();

        selectedNode.setUserObject(new TreeConcept((Concept) Dispatcher.getDispatcher(Concept.class).getValueObject()));

        /*
         * Announcing the node structure change triggers the necessary repaint to
         * display the effects of changes.
         */
        DefaultTreeModel model = (DefaultTreeModel) getModel();

        model.nodeStructureChanged(selectedNode);
    }

    /**
     * Sets the parent node of the currently selected node to be the node
     * representing the <code>Concept</code> of the specified name.
     *
     * @param  newParentName   The name of the <code>Concept</code> for which the currently selected
     *  node is to become a child.
     */
    public void updateTreeNodeParent(String newParentName) {

        // Get the node being moved
        DefaultMutableTreeNode conceptNode     = (DefaultMutableTreeNode) getSelectionPath().getLastPathComponent();
        String                 conceptNodeName = ((TreeConcept) conceptNode.getUserObject()).getName();
        DefaultTreeModel       treeModel       = (DefaultTreeModel) getModel();

        // Remove node from current parent node and update structure
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) conceptNode.getParent();

        parentNode.remove(conceptNode);
        treeModel.nodeStructureChanged(parentNode);

        // Get the new parent node
        DefaultMutableTreeNode newParentNode         = expandDownToNode(newParentName);
        TreeConcept            treeConcept           = (TreeConcept) newParentNode.getUserObject();
        boolean                parentNeededExpanding = treeConcept.lazyExpand(newParentNode);

        // Branch on whether parent needed expanding:
        // - The parent node needed to be expanded. The call to lazyExpand()
        // updates the parent node's children so we don't need to explicitly add
        // the new child node. Find and select the new child node.
        // - The parent node is already expanded, so insert the new child node in
        // the appropriate slot and select the new child node.
        if (parentNeededExpanding) {
            Enumeration children = newParentNode.children();

            while (children.hasMoreElements()) {
                DefaultMutableTreeNode node     = (DefaultMutableTreeNode) children.nextElement();
                String                 nodeName = ((TreeConcept) node.getUserObject()).getName();

                if (nodeName.equals(conceptNodeName)) {
                    setSelectionPath(new TreePath(node.getPath()));

                    break;
                }
            }
        } else {

            // Insert the node at the appropriate point in the new parent node.
            int         insertPosition = 0;
            Enumeration children       = newParentNode.children();

            while (children.hasMoreElements()) {
                DefaultMutableTreeNode node     = (DefaultMutableTreeNode) children.nextElement();
                String                 nodeName = ((TreeConcept) node.getUserObject()).getName();

                if (0 < nodeName.compareTo(conceptNodeName)) {
                    break;
                } else {
                    insertPosition++;
                }
            }

            treeModel.insertNodeInto(conceptNode, newParentNode, insertPosition);
            setSelectionPath(new TreePath(conceptNode.getPath()));
        }
    }
}
