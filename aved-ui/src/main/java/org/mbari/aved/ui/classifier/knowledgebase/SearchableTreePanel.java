/*
 * @(#)SearchableTreePanel.java   10/03/17
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

import java.util.Enumeration;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * <p>Provides case-insensitive search capabilites on a JTree. This component can be easily
 * embedded in applications.</p>
 *
 * @author <a href="http://www.mbari.org">MBARI</a>
 * @version $Id: SearchableTreePanel.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class SearchableTreePanel extends JPanel {
    private static boolean            resetEnum = false;
    private static Enumeration        enm;
    private javax.swing.JButton       btnSearch         = null;
    private boolean                   globSearch        = true;
    private javax.swing.JToggleButton globToggleButton  = null;
    private javax.swing.JScrollPane   jScrollPane       = null;
    private javax.swing.JTree         jTree             = null;
    private javax.swing.JPanel        searchButtonPanel = null;
    private javax.swing.JPanel        searchPanel       = null;
    private javax.swing.JTextField    searchTextField   = null;
    private javax.swing.JPanel        searchTypePanel   = null;
    private char[]                    wordSeparators    = { ' ', '\t', '-', '_' };
    private javax.swing.JToggleButton wordToggleButton  = null;

    /**
     * This is the default constructor
     */
    public SearchableTreePanel() {
        super();
        initialize();
    }

    /**
     * This method initializes btnSearch
     *
     *
     * @return    javax.swing.JButton
     */
    protected javax.swing.JButton getBtnSearch() {
        if (btnSearch == null) {
            btnSearch = new javax.swing.JButton();
            btnSearch.setFocusPainted(false);
            btnSearch.setContentAreaFilled(false);
            btnSearch.setIcon(new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/find_text.png")));
            btnSearch.setPressedIcon(
                new ImageIcon(getClass().getResource("/org/mbari/aved/ui/images/find_text_r.png")));
            btnSearch.setName("btnSearch");
            btnSearch.setPreferredSize(new java.awt.Dimension(65, 26));
            btnSearch.setMargin(new java.awt.Insets(2, 6, 2, 6));
            btnSearch.setToolTipText("Search for words in the tree, use this button or press enter in search field");
            btnSearch.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (!goToMatchingNode(getSearchTextField().getText().toLowerCase())) {
                        SwingUtils.flashJComponent(getSearchTextField(), 1);
                    }
                }
            });
        }

        return btnSearch;
    }

    /**
     * This method initializes globToggleButton
     *
     *
     * @return    javax.swing.JToggleButton
     */
    private javax.swing.JToggleButton getGlobToggleButton() {
        if (globToggleButton == null) {
            globToggleButton = new javax.swing.JToggleButton();
            globToggleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
            globToggleButton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 10));
            globToggleButton.setText("GLOB");
            globToggleButton.setToolTipText("Search matches anywhere within a word");
            globToggleButton.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    setGlobSearch(globToggleButton.isSelected());
                }
            });
        }

        return globToggleButton;
    }

    /**
     * This method initializes jScrollPane
     *
     *
     * @return    javax.swing.JScrollPane
     */
    private javax.swing.JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new javax.swing.JScrollPane();
            jScrollPane.setViewportView(getJTree());
        }

        return jScrollPane;
    }

    /**
     * This method returns the JTree being used. By default it calls
     * <code>new JTree()</code> if no JTree has been set.
     *
     * @return    javax.swing.JTree
     */
    public javax.swing.JTree getJTree() {
        if (jTree == null) {
            jTree = new javax.swing.JTree();
        }

        return jTree;
    }

    /**
     * This class is provided to be overridden by subclasses. The <tt>String</tt> returned
     * should be the <tt>String</tt> to use in checking if the user's search string matches
     * the a specific node.<br/>
     * <em>NOTE</em>: The default implementation of this method returns <tt>node.toString</tt>
     * it is strongly recommended that this method be overrided when used. Perhaps with an
     * anonymous subclass like so:<br/>
     * <pre>
     * SearchableTreePanel searchTree = new SearchableTreePanel(){
     *  public String getNodeTextToSearch(TreeNode node){
     *          MyWonderfullClass mwc = (MyWonderfulClass)node.getUserObject();
     *          return mwc.getDisplayName();
     *  }
     * };
     * </pre>
     *
     *
     * @param  node  The node to return a <tt>String</tt> for
     * @return       The <tt>String</tt> representation of this node as will be searched against
     * the user's search string.
     */
    public String getNodeTextToSearch(DefaultMutableTreeNode node) {
        return node.toString();
    }

    /**
     * This method initializes searchButtonPanel
     *
     *
     * @return    javax.swing.JPanel
     */
    private javax.swing.JPanel getSearchButtonPanel() {
        if (searchButtonPanel == null) {
            searchButtonPanel = new javax.swing.JPanel();
            searchButtonPanel.setLayout(new java.awt.BorderLayout());
            searchButtonPanel.add(getBtnSearch(), java.awt.BorderLayout.CENTER);
            searchButtonPanel.add(getSearchTypePanel(), java.awt.BorderLayout.WEST);
        }

        return searchButtonPanel;
    }

    /**
     * This method initializes searchPanel
     *
     *
     * @return    javax.swing.JPanel
     */
    private javax.swing.JPanel getSearchPanel() {
        if (searchPanel == null) {
            searchPanel = new javax.swing.JPanel();
            searchPanel.setLayout(new java.awt.BorderLayout());
            searchPanel.add(getSearchButtonPanel(), java.awt.BorderLayout.EAST);
            searchPanel.add(getSearchTextField(), java.awt.BorderLayout.CENTER);
            searchPanel.setName("search");
            searchPanel.setToolTipText("Search for next matching item in tree");
        }

        return searchPanel;
    }

    /**
     * This method initializes searchTextField
     *
     *
     * @return    javax.swing.JTextField
     */
    protected javax.swing.JTextField getSearchTextField() {
        if (searchTextField == null) {
            searchTextField = new javax.swing.JTextField();
            searchTextField.setColumns(10);
            searchTextField.setText("");
            searchTextField.setToolTipText("Search for words in tree, press enter to search, or press search button");
            searchTextField.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (!goToMatchingNode(getSearchTextField().getText().toLowerCase())) {
                        SwingUtils.flashJComponent(getSearchTextField(), 1);
                    }
                }
            });
        }

        return searchTextField;
    }

    /**
     * This method initializes searchTypePanel
     *
     *
     * @return    javax.swing.JPanel
     */
    private javax.swing.JPanel getSearchTypePanel() {
        if (searchTypePanel == null) {
            searchTypePanel = new javax.swing.JPanel();

            java.awt.GridLayout layGridLayout9 = new java.awt.GridLayout();

            layGridLayout9.setRows(2);
            searchTypePanel.setLayout(layGridLayout9);

            ButtonGroup group = new ButtonGroup();

            group.add(getWordToggleButton());
            group.add(getGlobToggleButton());
            group.setSelected(getWordToggleButton().getModel(), true);
            searchTypePanel.add(getWordToggleButton(), null);
            searchTypePanel.add(getGlobToggleButton(), null);
        }

        return searchTypePanel;
    }

    /**
     * Get the <tt>char</tt> array used to separate a string into words
     *
     * @return    the array being used to separate string's into words.
     */
    public char[] getWordSeparators() {
        return wordSeparators;
    }

    /**
     * This method initializes wordToggleButton
     *
     *
     * @return    javax.swing.JToggleButton
     */
    private javax.swing.JToggleButton getWordToggleButton() {
        if (wordToggleButton == null) {
            wordToggleButton = new javax.swing.JToggleButton();
            wordToggleButton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 10));
            wordToggleButton.setText("WORD");
            wordToggleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
            wordToggleButton.setToolTipText("Search matches from the beginning of the word");
            wordToggleButton.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    setGlobSearch(!wordToggleButton.isSelected());
                }
            });
        }

        return wordToggleButton;
    }

    /**
     * Convience method for internal class use.
     *
     * @param  text
     * @return
     */
    boolean goToMatchingNode(String text) {
        return goToMatchingNode(text, isGlobSearch());
    }

    /**
     * Goes to the next node in the tree which matches the text in the
     * <tt>searchTextField</tt>.
     *
     * @param  text           The String to search for.
     * @param  useGlobSearch  true=glob search false= prefix mtaching
     * @return                Description of the Return Value
     */
    public boolean goToMatchingNode(String text, boolean useGlobSearch) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getJTree().getModel().getRoot();

        if (enm == null) {
            enm       = root.preorderEnumeration();
            resetEnum = true;
        } else {
            resetEnum = false;
        }

        DefaultMutableTreeNode nodeFound = null;

OUTER:
        while (enm.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enm.nextElement();

            // Searches are case insensitive
            final String nodeName = getNodeTextToSearch(node).toLowerCase();

            if (useGlobSearch) {
                if (nodeName.indexOf(text) != -1) {
                    nodeFound = node;

                    break;
                }
            } else {

                // This is obviously a non-optimized solution :) -- andrew
                // TODO 20040203 achase: look into optimizations for this pattern match
                // Not using String.split because it is java 1.4 and up
                // TODO 20040416 brian: We are already locked into JRE 1.4 and up
                // feel free to use String.split
                char[] charArray  = nodeName.toCharArray();
                int    boundary1  = 0;
                int    boundary2  = 0;
                int    wordlength = 0;

                // first check the beginnning of the string
                if (nodeName.startsWith(text)) {
                    nodeFound = node;

                    break;
                }

                for (int i = 0; i < charArray.length; i++) {
                    for (int j = 0; j < wordSeparators.length; j++) {
                        if (wordSeparators[j] == charArray[i]) {

                            // set the left boundary to the current right boundary
                            boundary1 = boundary2;

                            // set the right boundary to just past the whitespace
                            boundary2 = i + 1;

                            // subtract two from the wordlength to compensate for
                            // the addition above and the whitespace char.
                            wordlength = boundary2 - boundary1 - 1;

                            if (new String(charArray, boundary1, wordlength).startsWith(text)) {
                                nodeFound = node;

                                break OUTER;
                            }
                        }
                    }

                    // now check for the last word
                    if (i == charArray.length - 1) {
                        if (new String(charArray, boundary2, charArray.length - boundary2).startsWith(text)) {
                            nodeFound = node;

                            break OUTER;
                        }
                    }
                }
            }
        }

        // if no node was found, but the entire enum wasn't searched, go again
        // otherwise return false
        if (nodeFound == null) {
            if (!resetEnum) {
                enm = null;

                return goToMatchingNode(text, useGlobSearch);
            } else {
                return false;
            }
        }

        TreePath path = new TreePath(nodeFound.getPath());

        getJTree().setSelectionPath(path);
        getJTree().scrollPathToVisible(path);

        return true;
    }

    /**
     * This method initializes this
     *
     *
     */
    private void initialize() {
        this.setLayout(new java.awt.BorderLayout());
        this.add(getSearchPanel(), java.awt.BorderLayout.NORTH);
        this.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
        this.setSize(261, 329);
    }

    /**
     * Is the search mechanism a glob search or a leading search. A glob search
     * will match characters anywhere in the word, a leading search will only match
     * from the beginning of the word onwards.
     *
     *
     * @return    Whether or not this is a glob search
     */
    public boolean isGlobSearch() {
        return globSearch;
    }

    /**
     *  A demonstration method.
     *
     * @param  args  The command line arguments will be ignored.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test Searchable Tree");

        frame.getContentPane().add(new SearchableTreePanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(300, 300, 300, 300);
        frame.setVisible(true);
    }

    /**
     * Set the search mechanism to a glob search or a leading search. A glob search
     * will match characters anywhere in the word, a leading search will only match
     * from the beginning of the word onwards.
     *
     * @param  b
     */
    public void setGlobSearch(boolean b) {
        globSearch = b;
    }

    /**
     * This method sets the <tt>JTree</tt> which is to be displayed and searched on. The
     * nodes in this tree must be of type <tt>DefaultMutableTreeNode</tt> because the
     * <tt>DefaultMutableTreeNode.preorderEnumeration</tt> method is needed for searching the tree.
     *
     * @param  tree
     */
    public void setJTree(javax.swing.JTree tree) {
        this.remove(getJTree());
        jTree = tree;
        getJScrollPane().setViewportView(jTree);
    }

    /**
     * Set the <tt>char</tt> array of wordseparators used to check for whitespace which defines
     * word boundaries
     *
     * @param  cs  The array of whitespace separators
     */
    public void setWordSeparators(char[] cs) {
        wordSeparators = cs;
    }
}
