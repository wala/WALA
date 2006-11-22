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
package com.ibm.wala.core.tests.cha;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.ecore.java.impl.JavaPackageImpl;
import com.ibm.wala.emf.wrappers.EMFBridge;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.ETypeHierarchyWrapper;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 */

public class CHATest extends WalaTestCase {

  static {
    JavaPackageImpl.init();
  }

  private static final ClassLoader MY_CLASSLOADER = CHATest.class.getClassLoader();

  public static void main(String[] args) {
    justThisTest(CHATest.class);
  }

  public CHATest(String arg0) {
    super(arg0);
  }

  /**
   * regression test with class hierarchy of primordial loader
   * @throws ClassHierarchyException 
   */
  public void testPrimordial() throws ClassHierarchyException {
    System.err.println("build ...");
    // build a class hierarchy for the primordial loader
    AnalysisScope scope = new EMFScopeWrapper(TestConstants.WALA_TESTDATA, "J2SEClassHierarchyExclusions.xml", MY_CLASSLOADER);
    WarningSet warnings = new WarningSet();
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    com.ibm.wala.emf.wrappers.ETypeHierarchyWrapper t1 = EMFBridge.makeTypeHierarchy(cha);
    System.err.println("save ...");
    // save it to disk
    ETypeHierarchy et = (ETypeHierarchy) t1.toEMF();
    Properties p = null;;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String outputDir = p.getProperty(WalaProperties.OUTPUT_DIR);
    String fileName = outputDir + File.separator + "primordialTH.xml";
    Collection<EObject> persist = new LinkedList<EObject>();
    persist.add(et);
    persist.add(et.getClasses().getNodes());
    persist.add(et.getInterfaces().getNodes());
    try {
      EUtil.saveToFile(persist, fileName);
    } catch (WalaException e1) {
      e1.printStackTrace();
      Assertions.UNREACHABLE("failed to save to file " + fileName + "\n. outputDir was " + outputDir);
    }

    System.err.println("read ...");
    // load it back into memory
    ETypeHierarchyWrapper et2 = ETypeHierarchyWrapper.loadFromFile(fileName);
    System.err.println("check ...");
    // check that they are isomorphic
    // TODO: add general utilities for isomorphism
    com.ibm.wala.ipa.callgraph.impl.Util.checkGraphSubset(t1.getClasses(), et2.getClasses());
    com.ibm.wala.ipa.callgraph.impl.Util.checkGraphSubset(et2.getClasses(), t1.getClasses());
    com.ibm.wala.ipa.callgraph.impl.Util.checkGraphSubset(t1.getInterfaces(), et2.getInterfaces());
    com.ibm.wala.ipa.callgraph.impl.Util.checkGraphSubset(et2.getInterfaces(), t1.getInterfaces());
    checkInterfaces(cha, et2);
  }

  /**
   * check that the set of interfaces for each class is consistent between cha
   * and T.
   * 
   * @param cha
   * @param T
   */
  private void checkInterfaces(ClassHierarchy cha, ETypeHierarchyWrapper T) {
    try {
      for (Iterator<IClass> it = cha.iterateAllClasses(); it.hasNext();) {
        IClass klass = (IClass) it.next();
        if (!klass.isInterface()) {
          EJavaClass eKlass = EMFBridge.makeJavaClass(klass.getReference());
          HashSet<EJavaClass> impls = new HashSet<EJavaClass>(5);
          for (Iterator<IClass> it2 = klass.getDirectInterfaces().iterator(); it2.hasNext();) {
            IClass iface = (IClass) it2.next();
            impls.add(EMFBridge.makeJavaClass(iface.getReference()));
          }
          Collection<EJavaClass> c = T.getImplements(eKlass);
          checkSetsEqual(impls, c);
        }
      }
    } catch (ClassHierarchyException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  private void checkSetsEqual(Collection<EJavaClass> c1, Collection<EJavaClass> c2) {
    if (!c1.containsAll(c2)) {
      c2.removeAll(c1);
      Trace.printCollection("DIFF: c2 contains but not c1 ", c2);
      assertTrue("c1 not superset of c2, see trace file for details", false);
      return;
    }
    if (!c2.containsAll(c1)) {
      c1.removeAll(c2);
      Trace.printCollection("DIFF: c1 contains but not c2 ", c1);
      assertTrue("c2 not superset of c1, see trace file for details", false);
      return;
    }
  }
}
