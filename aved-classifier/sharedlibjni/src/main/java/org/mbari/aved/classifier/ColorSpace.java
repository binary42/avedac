/*
 * @(#)ColorSpace.java
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



package org.mbari.aved.classifier;

/**
 *
 * @author dcline
 */
public enum ColorSpace {

    /** Defines the color space to run the classifier in */
    GRAY(1), RGB(2), YCBCR(3);

    public final int color;

    /**
     * constructor
     *
     * @param color color for this color space
     */
    ColorSpace(int color) {
        this.color = color;
    }
}
