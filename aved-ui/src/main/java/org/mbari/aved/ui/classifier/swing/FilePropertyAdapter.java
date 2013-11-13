/*
 * @(#)FilePropertyAdapter.java
 * 
 * Copyright 2013 MBARI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */



package org.mbari.aved.ui.classifier.swing;

//~--- non-JDK imports --------------------------------------------------------

import com.jgoodies.binding.beans.BeanUtils;
import com.jgoodies.binding.value.AbstractValueModel;

//~--- JDK imports ------------------------------------------------------------

import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;

import java.io.File;

/**
 * Delegates all calls to a private instance of PropertyAdapter. Used to avoid
 * having to create a ValueHolder and FileConverter for adapting Files to
 * textfields
 *
 * @author achase
 */
public class FilePropertyAdapter extends AbstractValueModel {
    Object             bean;
    PropertyDescriptor descriptor;

    public FilePropertyAdapter(Object bean, String propertyName) {
        super();
        this.bean = bean;

        try {
            descriptor = BeanUtils.getPropertyDescriptor(bean.getClass(), propertyName);

            if (!descriptor.getPropertyType().equals(File.class)) {
                throw new IllegalArgumentException("Property must be of type File");    // $NON-NLS-1$
            }
        } catch (IntrospectionException e) {

            // Do nothing, hope for the best
        }

        BeanUtils.addPropertyChangeListener(bean, propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
                String oldValue = null,
                       newValue = null;

                if (arg0.getOldValue() != null) {
                    oldValue = ((File) arg0.getOldValue()).getAbsolutePath();
                }

                if (arg0.getNewValue() != null) {
                    newValue = ((File) arg0.getNewValue()).getAbsolutePath();
                }

                fireValueChange(oldValue, newValue);
            }
        });
    }

    public Object getValue() {
        if (bean != null) {
            Object o = BeanUtils.getValue(bean, descriptor);

            if (o != null) {
                return ((File) o).getAbsolutePath();
            }
        }

        return null;
    }

    public void setValue(Object newValue) {
        if (bean != null) {
            Object old = BeanUtils.getValue(bean, descriptor);

            if (newValue instanceof String) {
                newValue = new File((String) newValue);
            }

            try {
                BeanUtils.setValue(bean, descriptor, newValue);
            } catch (PropertyVetoException ex) {
                ex.printStackTrace();
            }

            if (newValue == null) {
                fireValueChange(old, null);
            } else {
                fireValueChange(old, ((File) newValue).getAbsolutePath());
            }
        }
    }
}
