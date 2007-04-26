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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.java.EInterfaceHierarchy;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * 
 * This is a convenience class: it provides a view of an EMF EClassHierarchy
 * that should be more convenient for common client-side use.
 * 
 * the structure of interfaces in a Java hierarchy (a DAG)
 * 
 * @author sfink
 */
public class EInterfaceHierarchyWrapper extends EObjectGraphImpl {
  public EObject export() {
    EInterfaceHierarchy h = JavaFactory.eINSTANCE.createEInterfaceHierarchy();
    makeNodes(h);
    makeEdges(h);
    return h;
  }

  @SuppressWarnings("unchecked")
  private void makeEdges(EInterfaceHierarchy h) {
    ERelation r = CommonFactory.eINSTANCE.createERelation();
    for (Iterator it = iterator(); it.hasNext();) {
      EObject src = (EObject) it.next();
      for (Iterator it2 = getSuccNodes(src); it2.hasNext();) {
        EObject dst = (EObject) it2.next();
        EPair p = CommonFactory.eINSTANCE.createEPair();
        p.setX(src);
        p.setY(dst);
        r.getContents().add(p);
      }
    }
    h.setEdges(r);
  }

  private void makeNodes(EInterfaceHierarchy h) {
    EObjectDictionary d = new EObjectDictionary();
    for (Iterator it = iterator(); it.hasNext();) {
      EObject o = (EObject) it.next();
      d.findOrAdd(o);
    }
    EContainer c = (EContainer) d.export(true);
    h.setNodes(c);
  }

  /**
   * TODO: refactor
   */
  @SuppressWarnings("unchecked")
  public static EInterfaceHierarchyWrapper load(EInterfaceHierarchy h) throws IllegalArgumentException {
    if (h == null) {
      throw new IllegalArgumentException("h cannot be null");
    }
    EInterfaceHierarchyWrapper result = new EInterfaceHierarchyWrapper();

    for (Iterator<EObject> it = h.getNodes().getContents().iterator(); it.hasNext();) {
      result.addNode(it.next());
    }
    for (Iterator it = h.getEdges().getContents().iterator(); it.hasNext();) {
      EPair p = (EPair) it.next();
      result.addEdge(p.getX(), p.getY());
    }
    return result;
  }

  /**
   * Add a class to this hierarchy
   * 
   * @param javaKlass
   */
  public void addInterface(EJavaClass javaKlass) {
    addNode(javaKlass);
  }

  /**
   * Record that a child of a parent
   * 
   * @param parentClass
   * @param childClass
   */
  public void addSubClass(EJavaClass parentClass, EJavaClass childClass) {
    addEdge(parentClass, childClass);
  }

  @SuppressWarnings("unchecked")
  public Collection<EJavaClass> getAllSuperclasses(EJavaClass klass) {
    EObject kludge = klass;
    Collection result = DFS.getReachableNodes(GraphInverter.invert(this), Collections.singleton(kludge));
    result.remove(klass);
    return result;
  }
}