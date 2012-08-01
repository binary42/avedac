/*
 * Copyright 2009 MBARI
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
package org.mbari.aved.ui.utils;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Node;

/**
 *
 * @author dcline
 */
public class ImageMetadata extends IIOMetadata {

    String comment;

    public ImageMetadata(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public IIOMetadataNode getAsTree(String s) {
        IIOMetadataNode child = new IIOMetadataNode("Comment"); 
        child.setAttribute("Comment", comment); 
        return child;
    }

    @Override
    public void mergeTree(String string, Node node) throws IIOInvalidTreeException {
    }

    @Override
    public void reset() {
    }
}