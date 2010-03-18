/*
 * (C) Copyright 2009 MBARI. All rights reserved.
 * For information on the AVED project see http://www.mbari.org/aved."
 */
package org.mbari.aved.mbarivision.api.utils;

import java.util.Vector;

/**
 *
 * @author dcline
 */
public class ExtendedVector extends Vector {

    /**
     * Helper class to convert a vector to a stringbuffer
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(); // Like the use of the string buffer btw...
        for (int i = 0; i < this.size(); i++) {
            sb.append(this.get(i).toString());
        }
        return sb.toString();
    }
}
