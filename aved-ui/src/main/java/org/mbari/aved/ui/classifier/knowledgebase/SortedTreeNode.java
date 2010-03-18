/*
 * @(#)SortedTreeNode.java   10/03/17
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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * @author brian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SortedTreeNode extends DefaultMutableTreeNode {

    /**
     *
     */
    public SortedTreeNode() {
        super();
    }

    /**
     * @param userObject
     */
    public SortedTreeNode(Object userObject) {
        super(userObject);
    }

    /**
     * @param userObject
     * @param allowsChildren
     */
    public SortedTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param node
     */
    public void add(MutableTreeNode node) {
        if (node instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            Object                 obj      = treeNode.getUserObject();
            boolean                added    = false;

            if (obj instanceof Comparable) {
                Comparable thisComp = (Comparable) obj;

                for (int i = 0; i < getChildCount(); i++) {
                    TreeNode thatNode = getChildAt(i);

                    if (thatNode instanceof DefaultMutableTreeNode) {
                        Object thatObj = ((DefaultMutableTreeNode) thatNode).getUserObject();

                        if (thatObj instanceof Comparable) {
                            int c = thisComp.compareTo((Comparable) thatObj);

                            if (c == 0) {
                                super.add(node);
                                added = true;

                                break;
                            } else if (c < 0) {
                                super.insert(node, i);
                                added = true;

                                break;
                            }
                        }
                    }
                }
            }

            if (!added) {
                super.add(node);
            }
        }
    }
}
