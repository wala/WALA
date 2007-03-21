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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource.XMLMap;

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
   * Load a class hierarchy from a file
   * @param loader loader that knows how to find the file
   */
  public static EClassHierarchyWrapper load(String fileName, ClassLoader loader) {
    EClassHierarchy cha = loadFromFile(fileName,loader);
    return load(cha);
  }

  /**
   * @param o an EClassHierarchy
   * @return a ClassHierarchy populated according to the contents of o
   */
  @SuppressWarnings("unchecked")
  public static EClassHierarchyWrapper load(EObject o) {
    EClassHierarchy cha = (EClassHierarchy)o;
    Assertions.productionAssertion(cha != null);
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
   * TODO: this should go away?
   */
  @SuppressWarnings("unchecked")
  private static EClassHierarchy loadFromFile(String fileName, ClassLoader loader) {
    // InputStream s = loader.getResourceAsStream(fileName);
    File f = new File(fileName);
    InputStream s = null;
    try {
      s = new FileInputStream(f);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
      Assertions.productionAssertion(f != null,  " could not find " + fileName);
    }
    
    ResourceSet resSet = new ResourceSetImpl();
    Resource r = resSet.createResource(URI.createURI("junk"));
    Map<String, XMLMap> options = new HashMap<String, XMLMap>();
    try {
      r.load(s, options);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    EList contents = r.getContents();
    for (Iterator<EObject> it = contents.iterator(); it.hasNext();) {
      Object o = it.next();
      if (o instanceof EClassHierarchy) {
        return (EClassHierarchy)o;
      }
    }
    return null;
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