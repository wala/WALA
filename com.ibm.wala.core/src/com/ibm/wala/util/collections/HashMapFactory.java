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
import java.util.Map;

/**
 * 
 * A debugging aid.  When HashSetFactory.DEBUG is set, this class creates ParanoidHashMaps.  Otherwise,
 * it returns java.util.HashMaps
 * 
 * @author sfink
 */
public class HashMapFactory {

  /**
   * @param size
   * @return A ParanoidHashMap if DEBUG = true, a java.util.HashMap otherwise
   */
  public static <K,V> HashMap<K,V> make(int size) {
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<K,V>(size);
    } else {
      return new HashMap<K,V>(size);
    }
  }

  /**
   * @return A ParanoidHashMap if DEBUG = true, a java.util.HashMap otherwise
   */
  public static <K,V> HashMap<K,V> make() {
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<K,V>();
    } else {
      return new HashMap<K,V>();
    }
  }


  /**
   * @param t
   * @return A ParanoidHashMap if DEBUG = true, a java.util.HashMap otherwise
   */
  public static <K,V> HashMap<K,V> make(Map<K,V> t) {
    if (HashSetFactory.DEBUG) {
      return new ParanoidHashMap<K,V>(t);
    } else {
      return new HashMap<K,V>(t);
    }
  }
}