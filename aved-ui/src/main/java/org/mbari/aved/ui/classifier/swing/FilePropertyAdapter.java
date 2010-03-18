/*
 * @(#)FilePropertyAdapter.java   10/03/17
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
