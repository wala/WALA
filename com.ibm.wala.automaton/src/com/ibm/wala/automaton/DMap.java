/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DMap<K,V> implements Map<K,V> {
    public interface Factory<K,V> {
        public V create(K key);
    }
    
    private HashMap<K,V> map;
    private Factory<K,V> factory;
    
    public DMap(Factory<K,V> factory) {
        this.map = new HashMap<K,V>();
        this.factory = factory;
    }
    
    public DMap(){
        this(null);
    }
    
    protected V create(K key) {
        return factory.create(key);
    }
    
    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Collection<V> values() {
        return map.values();
    }

    public void putAll(Map<? extends K,? extends V> t) {
        map.putAll(t);
    }

    public Set<Entry<K,V>> entrySet() {
        return map.entrySet();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public V get(Object key) {
        if (containsKey(key)) {
            return map.get(key);
        }
        else {
            V v = create((K)key);
            map.put((K)key,v);
            return v;
        }
    }

    public V remove(Object key) {
        return map.remove(key);
    }

    public V put(K key, V value) {
        return map.put(key, value);
    }

    public String toString() {
        return map.toString();
    }
}
