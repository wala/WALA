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

import java.io.UTFDataFormatException;
import java.util.Iterator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ecore.java.ECallSite;
import com.ibm.wala.ecore.java.EClassLoaderName;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.JavaFactory;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Utilities that bridge between EMF types and WALA native types
 * 
 * @author sfink
 */
public class EMFBridge {

  /**
   * @param cg
   *          a WALA call graph
   * @return a EMF object representing a WALA callgraph
   */
  public static com.ibm.wala.emf.wrappers.ECallGraphWrapper makeCallGraph(CallGraph cg) throws IllegalArgumentException {
    if (cg == null) {
      throw new IllegalArgumentException("cg must not be null");
    }
    com.ibm.wala.emf.wrappers.ECallGraphWrapper result = new com.ibm.wala.emf.wrappers.ECallGraphWrapper();
    for (Iterator it = cg.iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      EJavaMethod method = makeJavaMethod(n.getMethod().getReference());
      result.addNode(method);
      for (Iterator it2 = n.iterateSites(); it2.hasNext();) {
        CallSiteReference site = (CallSiteReference) it2.next();
        ECallSite eSite = makeCallSite(method, site);
        result.addNode(eSite);
        result.addEdge(method, eSite);
      }
    }
    for (Iterator it = cg.iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      EJavaMethod method = makeJavaMethod(n.getMethod().getReference());
      for (Iterator it2 = n.iterateSites(); it2.hasNext();) {
        CallSiteReference site = (CallSiteReference) it2.next();
        ECallSite eSite = makeCallSite(method, site);
        for (Iterator it3 = n.getPossibleTargets(site).iterator(); it3.hasNext();) {
          CGNode target = (CGNode) it3.next();
          result.addEdge(eSite, makeJavaMethod(target.getMethod().getReference()));
        }
      }
    }
    return result;
  }

  /**
   * @param method
   *          an EMF method
   * @param site
   *          a WALA name for a call site
   * @return an EMF call site
   * @throws IllegalArgumentException  if site is null
   */
  public static ECallSite makeCallSite(EJavaMethod method, CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    ECallSite result = JavaFactory.eINSTANCE.createECallSite();
    result.setBytecodeIndex(site.getProgramCounter());
    result.setJavaMethod(method);
    result.setDeclaredTarget(makeJavaMethod(site.getDeclaredTarget()));
    return result;
  }

  /**
   * @return EMF method representing the WALA fake root method
   */
  public static EJavaMethod makeFakeRootMethod() {
    return makeJavaMethod(FakeRootMethod.getRootMethod());
  }

  /**
   * @param m
   *          method reference
   * @return corresponding EMF method
   * @throws IllegalArgumentException  if m is null
   */
  public static EJavaMethod makeJavaMethod(MethodReference m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    try {
      EJavaMethod result = JavaFactory.eINSTANCE.createEJavaMethod();
      EJavaClass klass = makeJavaClass(m.getDeclaringClass());
      result.setJavaClass(klass);
      result.setDescriptor(m.getDescriptor().toUnicodeString());
      result.setMethodName(m.getName().toUnicodeString());
      return result;
    } catch (UTFDataFormatException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public static EJavaClass makeJavaClass(TypeReference t) {
    if (t == null) {
      throw new IllegalArgumentException("t is null");
    }
    EJavaClass klass = JavaFactory.eINSTANCE.createEJavaClass();
    String className = t.getName().toUnicodeString();
    // strip a leading "L"
    if (className.charAt(0) == TypeReference.ClassTypeCode) {
      className = className.substring(1);
    }
    klass.setClassName(className.replace('/', '.'));
    klass.setLoader(getEClassLoaderName(t));
    return klass;
  }

  private static EClassLoaderName getEClassLoaderName(TypeReference t) {
    ClassLoaderReference loader = t.getClassLoader();
    if (loader.equals(ClassLoaderReference.Primordial)) {
      return EClassLoaderName.PRIMORDIAL_LITERAL;
    } else if (loader.equals(ClassLoaderReference.Application)) {
      return EClassLoaderName.APPLICATION_LITERAL;
    } else if (loader.equals(ClassLoaderReference.Extension)) {
      return EClassLoaderName.EXTENSION_LITERAL;
    } else {
      Assertions.UNREACHABLE("unexpected class loader reference: " + loader);
      return null;
    }

  }

  /**
   * @param cha
   *          a WALA class hierarchy
   * @return a EMF class hierarchy
   */
  public static com.ibm.wala.emf.wrappers.EClassHierarchyWrapper makeClassHierarchy(ClassHierarchy cha)
      throws IllegalArgumentException {
    if (cha == null) {
      throw new IllegalArgumentException("cha must not be null");
    }

    com.ibm.wala.emf.wrappers.EClassHierarchyWrapper result = new com.ibm.wala.emf.wrappers.EClassHierarchyWrapper();
    // create nodes
    for (IClass klass : cha) {
      if (!klass.isInterface()) {
        EJavaClass javaKlass = makeJavaClass(klass.getReference());
        result.addClass(javaKlass);
      }
    }
    // create edges
    for (IClass parent : cha) {
      EJavaClass parentClass = makeJavaClass(parent.getReference());
      if (!parent.isInterface()) {
        for (IClass child : cha.getImmediateSubclasses(parent)) {
          if (!child.isInterface()) {
            EJavaClass childClass = makeJavaClass(child.getReference());
            result.addSubClass(parentClass, childClass);
          }
        }
      }
    }
    return result;
  }

  /**
   * @param cha
   *          a WALA class hierarchy
   * @return a EMF interface hierarchy
   * @throws IllegalArgumentException  if cha is null
   */
  public static EInterfaceHierarchyWrapper makeInterfaceHierarchy(ClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    EInterfaceHierarchyWrapper result = new EInterfaceHierarchyWrapper();
    // create nodes
    for (IClass klass : cha) {
      if (klass.isInterface()) {
        EJavaClass javaKlass = makeJavaClass(klass.getReference());
        result.addInterface(javaKlass);
      }
    }
    // create edges
    for (IClass parent : cha) {
      EJavaClass parentClass = makeJavaClass(parent.getReference());
      if (parent.isInterface()) {
        for (IClass child : cha.getImmediateSubclasses(parent)) {
          if (child.isInterface()) {
            EJavaClass childClass = makeJavaClass(child.getReference());
            result.addSubClass(parentClass, childClass);
          }
        }
      }
    }
    return result;
  }

  /**
   * @param cha
   *          a WALA class hierarchy
   * @return a EMF type hierarchy
   * @throws IllegalArgumentException  if cha is null
   */
  public static ETypeHierarchyWrapper makeTypeHierarchy(ClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    com.ibm.wala.emf.wrappers.EClassHierarchyWrapper c = makeClassHierarchy(cha);
    com.ibm.wala.emf.wrappers.EInterfaceHierarchyWrapper i = makeInterfaceHierarchy(cha);
    ETypeHierarchyWrapper result = new ETypeHierarchyWrapper(c, i);
    for (IClass klass : cha) {
      EJavaClass eklass = makeJavaClass(klass.getReference());
      if (!klass.isInterface()) {
        try {
          for (Iterator<IClass> it2 = klass.getDirectInterfaces().iterator(); it2.hasNext();) {
            IClass iface = it2.next();
            EJavaClass eIface = makeJavaClass(iface.getReference());
            result.recordImplements(eklass, eIface);
          }
        } catch (ClassHierarchyException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE();
        }
      }
    }
    return result;
  }
}