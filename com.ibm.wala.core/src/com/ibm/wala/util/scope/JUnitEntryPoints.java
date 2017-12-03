/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.scope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

/**
 * This class represents entry points ({@link Entrypoint})s of JUnit test methods. JUnit test methods are those invoked by the JUnit
 * framework reflectively The entry points can be used to specify entry points of a call graph.
 * 
 * This implementation only handles JUnit 3.
 */
public class JUnitEntryPoints {

  private static final boolean DEBUG = false;

  /**
   * Construct JUnit entrypoints for all the JUnit test methods in the given scope.
   * 
   * @throws IllegalArgumentException if cha is null
   */
  public static Iterable<Entrypoint> make(IClassHierarchy cha) {

    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    final HashSet<Entrypoint> result = HashSetFactory.make();
    for (IClass klass : cha) {
      if (klass.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        // if the class is a subclass of the Junit TestCase
        if (isJUnitTestCase(klass)) {

          System.out.println("application class: " + klass);

          // return all the tests methods
          Collection<IMethod> methods = klass.getAllMethods();
          Iterator<IMethod> methodsIt = methods.iterator();

          while (methodsIt.hasNext()) {
            IMethod m = methodsIt.next();
            if (isJUnitMethod(m)) {
              result.add(new DefaultEntrypoint(m, cha));
              System.out.println("- adding test method as entry point: " + m.getName().toString());
            }
          }
        }
      }
    }
    return result::iterator;
  }

  /**
   * Construct JUnit entrypoints for the specified test method in a scope.
   * 
   * @throws IllegalArgumentException if cha is null
   */
  public static Iterable<Entrypoint> makeOne(IClassHierarchy cha, String targetPackageName, String targetSimpleClassName,
      String targetMethodName) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    // assume test methods don't have parameters
    final Atom targetPackageAtom = Atom.findOrCreateAsciiAtom(targetPackageName);
    final Atom targetSimpleClassAtom = Atom.findOrCreateAsciiAtom(targetSimpleClassName);
    final TypeName targetType = TypeName.findOrCreateClass(targetPackageAtom, targetSimpleClassAtom);
    final Atom targetMethodAtom = Atom.findOrCreateAsciiAtom(targetMethodName);

    if (DEBUG) {
      System.err.println(("finding entrypoint " + targetMethodAtom + " in " + targetType));
    }

    final Set<Entrypoint> entryPts = HashSetFactory.make();

    for (IClass klass : cha) {
      TypeName klassType = klass.getName();
      if (klassType.equals(targetType) && isJUnitTestCase(klass)) {
        if (DEBUG) {
          System.err.println("found test class");
        }
        // add entry point corresponding to the target method
        for (IMethod method : klass.getDeclaredMethods()) {
          Atom methodAtom = method.getName();
          if (methodAtom.equals(targetMethodAtom)) {
            entryPts.add(new DefaultEntrypoint(method, cha));
            System.out.println("- adding entry point of the call graph: " + methodAtom.toString());
          }
        }

        // add entry points of setUp/tearDown methods
        Set<IMethod> setUpTearDowns = getSetUpTearDownMethods(klass);
        for (IMethod m : setUpTearDowns) {
          entryPts.add(new DefaultEntrypoint(m, cha));
        }
      }
    }
    return entryPts::iterator;
  }

  /**
   * Check if the given class is a JUnit test class. A JUnit test class is a subclass of junit.framework.TestCase or
   * junit.framework.TestSuite.
   * 
   * @throws IllegalArgumentException if klass is null
   */
  public static boolean isJUnitTestCase(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    final Atom junitPackage = Atom.findOrCreateAsciiAtom("junit/framework");
    final Atom junitClass = Atom.findOrCreateAsciiAtom("TestCase");
    final Atom junitSuite = Atom.findOrCreateAsciiAtom("TestSuite");
    final TypeName junitTestCaseType = TypeName.findOrCreateClass(junitPackage, junitClass);
    final TypeName junitTestSuiteType = TypeName.findOrCreateClass(junitPackage, junitSuite);

    IClass ancestor = klass.getSuperclass();
    while (ancestor != null) {
      TypeName t = ancestor.getName();
      if (t.equals(junitTestCaseType) || t.equals(junitTestSuiteType)) {
        return true;
      }
      ancestor = ancestor.getSuperclass();
    }
    return false;
  }

  /**
   * Check if the given method is a JUnit test method, assuming that it is declared in a JUnit test class. A method is a JUnit test
   * method if the name has the prefix "test", or its name is "setUp" or "tearDown".
   * 
   * @throws IllegalArgumentException if m is null
   */
  public static boolean isJUnitMethod(IMethod m) {
    if (m == null) {
      throw new IllegalArgumentException("m is null");
    }
    if (!isJUnitTestCase(m.getDeclaringClass())) {
      return false;
    }
    Atom method = m.getName();
    String methodName = method.toString();
    return methodName.startsWith("test") || methodName.equals("setUp") || methodName.equals("tearDown");
  }

  /**
   * Get the "setUp" and "tearDown" methods in the given class
   */
  public static Set<IMethod> getSetUpTearDownMethods(IClass testClass) {
    final Atom junitPackage = Atom.findOrCreateAsciiAtom("junit/framework");
    final Atom junitClass = Atom.findOrCreateAsciiAtom("TestCase");
    final Atom junitSuite = Atom.findOrCreateAsciiAtom("TestSuite");
    final TypeName junitTestCaseType = TypeName.findOrCreateClass(junitPackage, junitClass);
    final TypeName junitTestSuiteType = TypeName.findOrCreateClass(junitPackage, junitSuite);

    final Atom setUpMethodAtom = Atom.findOrCreateAsciiAtom("setUp");
    final Atom tearDownMethodAtom = Atom.findOrCreateAsciiAtom("tearDown");

    Set<IMethod> result = HashSetFactory.make();

    IClass currClass = testClass;
    while (currClass != null && !currClass.getName().equals(junitTestCaseType) && !currClass.getName().equals(junitTestSuiteType)) {

      for (IMethod method : currClass.getDeclaredMethods()) {

        final Atom methodAtom = method.getName();
        if (methodAtom.equals(setUpMethodAtom) || methodAtom.equals(tearDownMethodAtom) || method.isClinit() || method.isInit()) {
          result.add(method);
        }
      }
      currClass = currClass.getSuperclass();
    }
    return result;
  }
}
