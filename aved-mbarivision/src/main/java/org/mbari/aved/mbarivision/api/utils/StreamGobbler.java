package org.mbari.aved.mbarivision.api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;

/**
 * StreamGobbler
 */
public class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    OutputStream printstream;
    boolean isRunning = false;
    Vector linevector = null;    
    
    /**
     * The StreamGobbler constructor
     * @param is the input stream
     * @param ps the stream to print the stream text to, typically System.out, but 
     * can be a custom implementation
     */
    public StreamGobbler(InputStream is, OutputStream ps)
    {
        this.is = is;
        this.type = "";
        this.printstream = ps;
    }
    
    /**
     * The StreamGobbler constructor
     * @param is the input stream
     * @param ps the stream to print the stream text to, typically System.out, but 
     * can be a custom implementation
     * @param type the type of input stream
     */
    public StreamGobbler(InputStream is, OutputStream ps, String type)
    {
        this.is = is;
        this.type = type;
        this.printstream = ps;
    }
    /**
     * Sets a line vector to which the input stream should be 
     * written to. This is optional and is generally used to
     * capture a short text output from stdout/stderr
     * @param line
     */
    public void setLineVector(Vector line) {
        this.linevector = line;
    }
    /**
     * Runs the StreamGobbler
     */
    public void run()
    {
        try {
            isRunning = true;
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                printstream.write(line.getBytes());
                printstream.write('\n');                
                if (line.length() > 0 && linevector != null) {
                    linevector.add(line);
                }
                line = null;
                if(isRunning != true)
                    break;
            }
            isRunning = false;

        } catch (IOException ioe) {
            return;
        } catch ( Exception e) {
        	e.printStackTrace();
        } catch (ThreadDeath e) {
        	e.printStackTrace();
        }
    }
    /**
     * Gracefully stops the Stream Gobbler
     * 
     */
    public void kill()
    {
    	isRunning = false;
    }  
}