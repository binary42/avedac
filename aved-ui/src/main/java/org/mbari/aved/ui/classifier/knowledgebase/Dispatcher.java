/*
 * @(#)Dispatcher.java   10/03/17
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a dispatcher to notify listeners when an object has been changed. Use as:
 * <pre>
 * Dispatcher d = Dispatcher.getDispather("someKey");
 * d.addPropertyChangeListener(new PropertyChangeListener() {
 *     public void propertyChange(PropertyChangeEvent evt) {
 *           // Get the old value of the property
 *          Object oldValue = evt.getOldValue();
 *
 *          // Get the new value of the property
 *          Object newValue = evt.getNewValue();
 *          System.out.println("Old value = " + old + "; New value = " + new");
 *      }
 * });
 * d.setValueObject(object);
 * d.setValueObject(anotherObject);
 * Object refToAnotherObject = d.getValueObject();
 * </pre>
 *
 * Note: When Java 5.0 is more prevailent we can change this to use templates to
 * constrain the type of object that can be set.
 *
 * @author brian
 * @version $Id: Dispatcher.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 */
public class Dispatcher {
    private static final String         PROP_VALUE_OBJECT = "valueObject";
    protected static final Map          map               = new HashMap();
    private final PropertyChangeSupport propSupport       = new PropertyChangeSupport(this);
    private final Object                key;
    private Object                      valueObject;

    /**
     *
     *
     * @param key
     */
    protected Dispatcher(Object key) {
        super();
        this.key = key;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propSupport.addPropertyChangeListener(PROP_VALUE_OBJECT, listener);
    }

    /**
     * Returns an instance of a dispatcher for a particular class
     *
     * @param key
     * @return
     */
    public static Dispatcher getDispatcher(Object key) {
        Dispatcher dispatcher = null;

        synchronized (map) {
            if (map.containsKey(key)) {
                dispatcher = (Dispatcher) map.get(key);
            } else {
                dispatcher = new Dispatcher(key);
                map.put(key, dispatcher);
            }
        }

        return dispatcher;
    }

    /**
     * @return Returns the observedClass.
     */
    public Object getKey() {
        return key;
    }

    /**
     *
     * @return All the keys registered for the different dispatchers.
     */
    public static Collection getKeys() {
        Collection keys;

        synchronized (map) {
            keys = map.keySet();
        }

        return keys;
    }

    /**
     * @return Returns the valueObject.
     */
    public Object getValueObject() {
        return valueObject;
    }

    /**
     * <p><!-- Method description --></p>
     *
     *
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propSupport.removePropertyChangeListener(PROP_VALUE_OBJECT, listener);
    }

    /**
     *
     * @param observedObject
     */
    public synchronized void setValueObject(Object observedObject) {
        Object oldObject = this.valueObject;

        this.valueObject = observedObject;
        propSupport.firePropertyChange(PROP_VALUE_OBJECT, oldObject, observedObject);
    }
}
