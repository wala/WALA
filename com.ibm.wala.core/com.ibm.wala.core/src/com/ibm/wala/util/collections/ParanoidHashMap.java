/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * a debugging aid.  This implementation complains if you stick an object in here which
 * appears to use System.identityHashCode()
 * 
 * @author sfink
 */
public class ParanoidHashMap<K,V> extends HashMap<K,V> {
  public static final long serialVersionUID = 909018793791787198L;

  /**
   * @param t
   */
  public ParanoidHashMap(Map<K,V> t) {
    super(t.size());
    putAll(t);
  }


  /**
   * @param size
   */
  public ParanoidHashMap(int size) {
    super(size);
  }

  /**
   * 
   */
  public ParanoidHashMap() {
  }


  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#put(java.lang.Object, java.lang.Object)
   */
  public V put(K arg0, V arg1) {
    if (arg0.hashCode() == System.identityHashCode(arg0)) {
      Assertions._assert(false, arg0.getClass().toString());
    }
    return super.put(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Map#putAll(java.util.Map)
   */
  public void putAll(Map<? extends K,? extends V> arg0) {
    for (Iterator<?> it = arg0.entrySet().iterator(); it.hasNext();) {
      Map.Entry<? extends K,? extends V> E = (Entry<? extends K, ? extends V>) it.next();
      put(E.getKey(), E.getValue());
    }
  }

}