/*
 * @(#)StringValueModel.java
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

public class StringValueModel extends AbstractValueModel {
    Object             bean;
    PropertyDescriptor descriptor;

    public StringValueModel(Object bean, String propertyName) {
        super();
        this.bean = bean;

        try {
            descriptor = BeanUtils.getPropertyDescriptor(bean.getClass(), propertyName);

            if (!descriptor.getPropertyType().equals(String.class)) {
                throw new IllegalArgumentException("Property must be of type String");    // $NON-NLS-1$
            }
        } catch (IntrospectionException e) {

            // Do nothing, hope for the best
        }

        BeanUtils.addPropertyChangeListener(bean, propertyName, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
                String oldValue = null,
                       newValue = null;

                oldValue = (String) arg0.getOldValue();
                newValue = (String) arg0.getNewValue();
                fireValueChange(oldValue, newValue);
            }
        });
    }

    public Object getValue() {
        return BeanUtils.getValue(bean, descriptor);
    }

    public void setValue(Object newValue) {
        Object old = BeanUtils.getValue(bean, descriptor);

        try {
            BeanUtils.setValue(bean, descriptor, newValue);
        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }

        fireValueChange(old, newValue);
    }
}
