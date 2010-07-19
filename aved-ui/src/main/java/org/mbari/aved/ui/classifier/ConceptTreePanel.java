/*
 * @(#)ConceptTreePanel.java
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
package org.mbari.aved.ui.classifier;

//~--- non-JDK imports --------------------------------------------------------
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.mbari.aved.ui.classifier.knowledgebase.ConceptTreeReadOnly;
import org.mbari.aved.ui.classifier.knowledgebase.SearchableConceptTreePanel;
import org.mbari.vars.dao.DAOException;
import org.mbari.vars.knowledgebase.model.Concept;
import org.mbari.vars.knowledgebase.model.dao.KnowledgeBaseCache;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jdesktop.swingworker.SwingWorker;
import org.mbari.aved.ui.classifier.knowledgebase.KnowledgeBaseUtil;

/**
 *
 * @author dcline
 */
public class ConceptTreePanel extends JPanel {

    private boolean isInitialized = false;
    private ConceptTreeReadOnly conceptTree;
    private MouseListener listener;
    private SearchableConceptTreePanel treePanel;

    /**
     *  @param listener an optional listener to register
     * to handle clicking the concept tree
     */
    public ConceptTreePanel(MouseListener listener) {
        this.listener = listener;
    }

    public String getSelectedConceptName() {
        return conceptTree.getSelectedName();
    }

    /**
     * This starts a thread to create the tree panel
     */
    public void buildPanel() {
        if (!isInitialized) {
            JPanel panel = new JPanel();

            CellConstraints cc = new CellConstraints();
            JPanel temporaryPanel = new JPanel(new FormLayout("fill:m:grow", "fill:d:grow"));
            final JTextArea patience = new JTextArea();

            patience.setEditable(false);
            patience.setWrapStyleWord(true);
            patience.setLineWrap(true);

            String s = System.getProperty("os.name").toLowerCase();

            // First test for KB existence - skip this for Linux because this
            // breaks the classifier TODO: investigate pthreads exceptions
            if (KnowledgeBaseUtil.isKnowledgebaseAvailable()) {

                patience.setText("Loading Knowledge Base, your patience is appreciated...");
                temporaryPanel.add(patience, cc.xy(1, 1));

                final JScrollPane scrollPane = new JScrollPane(temporaryPanel);
                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, scrollPane);

                splitPane.setDividerLocation(.8);
                splitPane.setDividerSize(1);
                splitPane.setResizeWeight(0);
                this.setLayout(new BorderLayout());
                this.add(splitPane);
                this.setFocusable(true);
                this.repaint();


                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {

                        SwingWorker kbThread = new SwingWorker() {

                            @Override
                            protected Object doInBackground() throws Exception {
                                System.out.println("Loading concept tree");
                                treePanel = new SearchableConceptTreePanel();

                                Concept rootConcept = null;

                                try {
                                    rootConcept = KnowledgeBaseCache.getInstance().findRootConcept();
                                } catch (DAOException e1) {

                                    // e1.printStackTrace();
                                    SwingUtilities.invokeLater(new Runnable() {

                                        public void run() {
                                            patience.setText("Error loading knowledge base.");
                                            patience.repaint();
                                        }
                                    });
                                    isInitialized = false;
                                    return 0;
                                }

                                conceptTree = new ConceptTreeReadOnly(rootConcept);

                                if (listener != null) {
                                    conceptTree.addMouseListener(listener);
                                }

                                treePanel.setJTree(conceptTree);
                                System.out.println("replacing wait panel");
                                SwingUtilities.invokeLater(new Runnable() {

                                    public void run() {
                                        scrollPane.getViewport().removeAll();
                                        scrollPane.getViewport().add(treePanel);
                                        scrollPane.repaint();
                                        System.out.println("finished");
                                        isInitialized = true;
                                    }
                                });
                                return 0;
                            }
                        };

                        kbThread.execute();
                    }
                });
            } else {
                patience.setText("For more information about VARS see: http://www.mbari.org/vars/");
                temporaryPanel.add(patience, cc.xy(1, 1));

                final JScrollPane scrollPane = new JScrollPane(temporaryPanel);
                JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel, scrollPane);

                splitPane.setDividerLocation(.8);
                splitPane.setDividerSize(1);
                splitPane.setResizeWeight(0);
                this.setLayout(new BorderLayout());
                this.add(splitPane);
                this.setFocusable(true);
                this.repaint();
            }
        }
    }

    public void removeListener(MouseListener mouseListener) {
        if (conceptTree != null) {
            conceptTree.removeMouseListener(listener);
        }
    }

    public void replaceListener(MouseListener listener) {
        if ((conceptTree != null) && (listener != null)) {
            this.listener = listener;
            conceptTree.addMouseListener(listener);
        }
    }
}
