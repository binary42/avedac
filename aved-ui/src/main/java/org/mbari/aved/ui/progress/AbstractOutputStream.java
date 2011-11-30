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
package org.mbari.aved.ui.progress;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The root of the Aved output stream class hierarchy. 
 * 
 *  @author dcline
 * <p>
 */
public abstract class AbstractOutputStream extends OutputStream {
    
    
    public void write(String l) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override  
    public void write(int b) throws IOException {   
        throw new IOException("Not supported yet.");
    }  
  
    @Override  
    public void write(byte[] b, int off, int len) throws IOException {   
        throw new IOException("Not supported yet."); 
    }  
  
    @Override  
    public void write(byte[] b) throws IOException {  
       throw new IOException("Not supported yet.");  
    }  
}
