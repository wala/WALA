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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource.XMLMap;

import com.ibm.wala.ecore.common.CommonFactory;
import com.ibm.wala.ecore.common.EPair;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.java.EClassHierarchy;
import com.ibm.wala.ecore.java.EInterfaceHierarchy;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.ecore.java.JavaPackage;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;

/**
 * This is a convenience class: it provides a view of an EClassHierarchy that
 * should be more convenient for common client-side use.
 * 
 * a container of a class hierarchy and an interface hierarchy
 * 
 * @author sfink
 */
public class ETypeHierarchyWrapper {
  private final EClassHierarchyWrapper cha;

  private final EInterfaceHierarchyWrapper iface;

  private final BasicNaturalRelation implementR = new BasicNaturalRelation();

  /**
   * @param t
   * @throws IllegalArgumentException  if t is null
   */
  public ETypeHierarchyWrapper(ETypeHierarchy t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    this.cha = EClassHierarchyWrapper.load(t.getClasses());
    this.iface = EInterfaceHierarchyWrapper.load(t.getInterfaces());
    for (Iterator it = t.getImplements().getContents().iterator(); it.hasNext();) {
      EPair p = (EPair) it.next();
      EJavaClass x = (EJavaClass) p.getX();
      EJavaClass y = (EJavaClass) p.getY();
      recordImplements(x, y);
    }
  }

  /**
   * Warning: this constructor does <em>NOT</em> set up the "implements"
   * relation; the caller must do this separately.
   * 
   * 
   * @param cha
   * @param iface
   */
  public ETypeHierarchyWrapper(EClassHierarchyWrapper cha, EInterfaceHierarchyWrapper iface) {
    this.cha = cha;
    this.iface = iface;
  }

  @SuppressWarnings("unchecked")
  public EObject toEMF() {
    ETypeHierarchy t = JavaFactory.eINSTANCE.createETypeHierarchy();
    t.setClasses((EClassHierarchy) cha.export());
    t.setInterfaces((EInterfaceHierarchy) iface.export());

    ERelation impl = CommonFactory.eINSTANCE.createERelation();
    for (Iterator it = implementR.iterator(); it.hasNext();) {
      IntPair p = (IntPair) it.next();
      EJavaClass klass = (EJavaClass) cha.getNode(p.getX());
      EJavaClass interf = (EJavaClass) iface.getNode(p.getY());
      EPair pp = CommonFactory.eINSTANCE.createEPair();
      pp.setX(klass);
      pp.setY(interf);
      impl.getContents().add(pp);
    }
    t.setImplements(impl);

    return t;
  }

  public static ETypeHierarchyWrapper loadFromFile(String fileName) throws FileNotFoundException {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    System.err.println("eload..");
    ETypeHierarchy t = eloadFromFile(fileName);
    Assertions.productionAssertion(t != null);
    System.err.println("ctor..");
    ETypeHierarchyWrapper result = new ETypeHierarchyWrapper(t);

    return result;
  }

  // TODO: refactor!!!
  @SuppressWarnings("unchecked")
  private static ETypeHierarchy eloadFromFile(String fileName) throws FileNotFoundException {
    File f = new File(fileName);
    InputStream s = new FileInputStream(f);

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
      if (o instanceof ETypeHierarchy) {
        return (ETypeHierarchy) o;
      }
    }
    return null;
  }

  public EClassHierarchyWrapper getClasses() {
    return cha;
  }

  public EInterfaceHierarchyWrapper getInterfaces() {
    return iface;
  }

  public void recordImplements(EJavaClass klass, EJavaClass i) {
    int index1 = cha.getNumber(klass);
    int index2 = iface.getNumber(i);
    if (Assertions.verifyAssertions) {
      Assertions._assert(index1 > -1);
      if (index2 == -1) {
        Assertions._assert(index2 > -1, "interface not found " + i);
      }
    }
    implementR.add(index1, index2);
  }

  /**
   * @param klass
   * @return set of EJavaClass, the interfaces klass directly implements
   */
  public Collection<EJavaClass> getImplements(EJavaClass klass) {
    Assertions.precondition(cha.getNumber(klass) > -1, "invalid klass " + klass);
    IntSet s = implementR.getRelated(cha.getNumber(klass));
    if (s == null) {
      return Collections.emptySet();
    }
    HashSet<EJavaClass> result = new HashSet<EJavaClass>(3);
    for (IntIterator it = s.intIterator(); it.hasNext();) {
      int x = it.next();
      EJavaClass c = (EJavaClass) iface.getNode(x);
      result.add(c);
    }
    return result;
  }

  /**
   * @param klass
   * @return all superclasses of klass.
   */
  public Collection<EJavaClass> getAllSuperclasses(EJavaClass klass) {
    if (cha.containsNode(klass)) {
      return cha.getAllSuperclasses(klass);
    } else {
      return iface.getAllSuperclasses(klass);
    }
  }

  /*
   * (non-Javadoc)
   */
  public EClass getTargetType() {
    return JavaPackage.eINSTANCE.getETypeHierarchy();
  }
}