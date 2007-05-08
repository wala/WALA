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
package com.ibm.wala.emf.wrappers;

import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.ECollection;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.ENotContainer;
import com.ibm.wala.ecore.common.EObjectWithContainerId;
import com.ibm.wala.util.intset.MutableMapping;

/**
 * 
 * A dictionary of EObjects, which canonicalizes its contents by-value according
 * to java.lang.Object.equals()
 * 
 * Note that this differs from the "unique" attribute on an EList when
 * containment is accounted for.
 * 
 * @author sfink
 */
public class EObjectDictionary {

  /**
   * Implementation of a canonical dictionary of EObjects
   */
  private MutableMapping<EObject> map = new MutableMapping<EObject>();

  /**
   * @param key
   *          an EObject value
   * @return the canonical representative for this value in this dictionary
   */
  public EObject findOrAdd(EObject key) {
    int index = map.getMappedIndex(key);
    if (index == -1) {
      map.add(key);
      return key;
    } else {
      return (EObject) map.getMappedObject(index);
    }
  }

  /**
   * Populate this dictionary with the contents of an EContainer
   * @param c the EContainer
   * @throws IllegalArgumentException  if c is null
   */
  public void load(EContainer c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    for (Iterator it = c.getContents().iterator(); it.hasNext();) {
      EObject o = (EObject) it.next();
      findOrAdd(o);
    }
  }

  /**
   * Export this dictionary to an ECollection
   * @param container should the result be an EContainer?
   * @return an EContainer or an ECollection, as appropriate
   */
  public ECollection export(boolean container) {
    return export(container, false);
  }

  /**
   * Export this dictionary to an ECollection
   * @param container should the result be an EContainer?
   * @param createUniqueIds should we set unique ids on each EObjectWithContainerId in the dictionary
   * as a side effect?
   * @return an EContainer or an ECollection, as appropriate
   */
  @SuppressWarnings("unchecked")
  public ECollection export(boolean container, boolean createUniqueIds) {
    if (container) {
      EContainer c = CommonFactory.eINSTANCE.createEContainer();
      // note: be careful to maintain iteration order here.
      for (int i = 0; i < map.getMappingSize(); i++) {
        EObject o = (EObject) map.getMappedObject(i);
        if (createUniqueIds && o instanceof EObjectWithContainerId) {
          EObjectWithContainerId x = (EObjectWithContainerId) o;
          x.setId(i);
        }
        c.getContents().add(o);
      }
      return c;
    } else {
      ENotContainer c = CommonFactory.eINSTANCE.createENotContainer();
      // note: be careful to maintain iteration order here.
      for (int i = 0; i < map.getMappingSize(); i++) {
        c.getContents().add(map.getMappedObject(i));
      }
      return c;
    }
  }

  /**
   * @return the number of a given object, or -1 if the object is not currently
   *         in the range.
   */
  public int indexOf(EObject o) {
    return map.getMappedIndex(o);
  }

  /**
   * @param index an index into this collection
   * @return the corresponding element of this collection
   */
  public Object get(int index) {
    return map.getMappedObject(index);
  }

  /**
   * @return the number of objects tracked by this dictionary
   */
  public int size() {
    return map.getMappingSize();
  }
}