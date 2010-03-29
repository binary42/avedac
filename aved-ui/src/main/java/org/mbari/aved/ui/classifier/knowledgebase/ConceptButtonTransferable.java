/*
 * @(#)ConceptButtonTransferable.java
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.util.Arrays;
import java.util.List;

/**
 * <p>This class represents the &quot;transferable&quot; object that
 * is used in a drag and drop operation in the VARS annotation application.
 * It currently only contains a string as the data being transferred, but
 * it has the structure to do more than that in the future.</p><hr>
 *
 * @author  : $Author: dcline $
 * @version  : $Revision: 1.1 $
 * @see  ConceptDropPanel
 * @see  ConceptTreeReadOnly
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
public class ConceptButtonTransferable implements Transferable {

    /**
     *  Description of the Field
     */
    public final static DataFlavor LOCAL_STRING_FLAVOR = DataFlavor.stringFlavor;

    /**
     *  Description of the Field
     */
    public final static DataFlavor[] flavors = { ConceptButtonTransferable.LOCAL_STRING_FLAVOR };

    // Although these declarations are not really necessary, they serve more as a placeholder
    // for our own flavors that we can implement later to do more interesting
    // data transfers with.
    private final static List flavorList = Arrays.asList(flavors);

    /**
     * This is the string that contains the actual data that will be transferred
     */
    private String string;

    /**
     * The default constructor that simply initializes the instance
     *
     * @param  string is the String that will be transferred as data in the operation
     */
    public ConceptButtonTransferable(String string) {
        this.string = string;
    }

    /**
     * This is the method that is used to get the transferred data object.  Once
     * the object is retrieved, it should be cast into it appropriate class and
     * then it can be used
     *
     * @param  flavor This is the flavor of data that is being requested from the
     * operation.  Usually the caller will call <code>isDataFlavorSupported</code>
     * first to see if the data can be retrieved in that flavor.  If it is supported,
     * the user can then call this method with that DataFlavor, get the returned object
     * and cast it into what they were expecting
     * @return  The <code>Object</code> that contains the data that is transferred
     * @exception  UnsupportedFlavorException Description of the Exception
     */
    public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (ConceptButtonTransferable.LOCAL_STRING_FLAVOR.equals(flavor)) {
            return this.string;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    /**
     * A method to return the data flavors that are being transferred with this
     * transferable class
     *
     * @return  An array of DataFlavor classes that tell what kind of data is transferred with this class
     */
    public synchronized DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    /**
     * A method to use to determine if this class supports a certain data flavor for
     * transfer.
     *
     * @param  flavor Is the DataFlavor that is checked for transferrability
     * @return  a boolean to indicate if the DataFlavor can be transferred (true) or not (false)
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return (flavorList.contains(flavor));
    }

    /**
     * Simply overriding the toString() method
     *
     * @return  Description of the Return Value
     */
    public String toString() {
        return "ConceptButtonTransferable with data: " + this.string;
    }
}
