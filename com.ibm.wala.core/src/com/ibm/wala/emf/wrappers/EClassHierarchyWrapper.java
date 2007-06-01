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
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.java.EClassHierarchy;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * This is a convenience class: it provides a view of an EClassHierarchy that
 * should be more convenient for most client-side use.
 * 
 * Note that an EMF class hierarchy only includes classes (single inheritance)
 * use in conjunction with InterfaceHierarchy to look at the entire
 * TypeHierarchy
 * 
 * @author sfink
 *  
 */
public class EClassHierarchyWrapper extends EObjectTree {
  /**
   * @return an EClassHierarchy representing the contents of this object
   * @see com.ibm.wala.emf.wrappers.EObjectGraphImpl#export()
   */
  @Override
  public EObject export() {
    EClassHierarchy cha = JavaFactory.eINSTANCE.createEClassHierarchy();
    makeNodes(cha);
    makeEdges(cha);
    return cha;
  }


  /**
   * Create edges in this structure corresponding to the class hierarchy
   * @param cha a single-inheritance class hierarchy from EMF
   */
  @SuppressWarnings("unchecked")
  private void makeEdges(EClassHierarchy cha) {
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
    cha.setEdges(r);
  }


  /**
   * Create a set of nodes for this graph
   * @param cha an EMF class hierarchy object
   */
  private void makeNodes(EClassHierarchy cha) {
    EObjectDictionary d = new EObjectDictionary();
    for (Iterator it = iterator(); it.hasNext();) {
      EObject o = (EObject) it.next();
      d.findOrAdd(o);
    }
    EContainer c = (EContainer) d.export(true);
    cha.setNodes(c);
  }


  /**
   * @return a IClassHierarchy populated according to the contents of o
   * @throws IllegalArgumentException  if cha is null
   */
  @SuppressWarnings("unchecked")
  public static EClassHierarchyWrapper load(EClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    EClassHierarchyWrapper result = new EClassHierarchyWrapper();
    
    for (Iterator<EObject> it = cha.getNodes().getContents().iterator(); it.hasNext(); ) {
      result.addNode(it.next());
    }
    for (Iterator it = cha.getEdges().getContents().iterator(); it.hasNext(); ) {
      EPair p = (EPair)it.next();
      result.addEdge(p.getX(),p.getY());
    }
    return result;
  }
  

  

  /**
   * Add a class to this hierarchy
   * @param javaKlass
   */
  public void addClass(EJavaClass javaKlass) {
    addNode(javaKlass);
  }

  /**
   * Record that a child of a parent 
   * @param parentClass
   * @param childClass
   */
  public void addSubClass(EJavaClass parentClass, EJavaClass childClass) {
    addEdge(parentClass,childClass);
  }


  public Collection<EJavaClass> getAllSuperclasses(EJavaClass klass) {
    HashSet<EJavaClass> result = new HashSet<EJavaClass>();
    EJavaClass superclass = getSuperclass(klass);
    while (superclass != null) {
      result.add(superclass);
      superclass = getSuperclass(superclass);
    }
    return result;
  }
  
  /**
   * @param klass
   * @return the superclass, or null if none
   */
  public EJavaClass getSuperclass(EJavaClass klass) {
    if (getPredNodeCount(klass) == 0) {
      return null;
    } else {
      if (Assertions.verifyAssertions) {
        Assertions._assert(getPredNodeCount(klass) == 1);
      }
      return (EJavaClass)getPredNodes(klass).next();
    }
  }
}