/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.core.util.scope;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class represents entry points ({@link Entrypoint})s of JUnit test methods. JUnit test
 * methods are those invoked by the JUnit framework reflectively The entry points can be used to
 * specify entry points of a call graph.
 */
public class JUnitEntryPoints {

  private static final Logger logger = Logger.getLogger(JUnitEntryPoints.class.getName());

  /** Names of annotations that denote JUnit4/5 test methods. */
  private static final Set<String> TEST_ENTRY_POINT_ANNOTATION_NAMES =
      new HashSet<>(
          Arrays.asList(
              "org.junit.After",
              "org.junit.AfterClass",
              "org.junit.Before",
              "org.junit.BeforeClass",
              "org.junit.ClassRule",
              "org.junit.Rule",
              "org.junit.Test",
              "org.junit.runners.Parameterized.Parameters",
              "org.junit.jupiter.api.AfterAll",
              "org.junit.jupiter.api.AfterEach",
              "org.junit.jupiter.api.BeforeAll",
              "org.junit.jupiter.api.BeforeEach",
              "org.junit.jupiter.api.RepeatedTest",
              "org.junit.jupiter.api.Test"));

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
      IClassLoader classLoader = klass.getClassLoader();
      ClassLoaderReference reference = classLoader.getReference();
      if (reference.equals(ClassLoaderReference.Application)) {
        // if the class is a subclass of the Junit TestCase
        if (isJUnitTestCase(klass)) {
          logger.fine(() -> "application class: " + klass);

          // return all the tests methods
          Collection<? extends IMethod> methods = klass.getAllMethods();

          for (IMethod m : methods) {
            if (isJUnitMethod(m)) {
              result.add(new DefaultEntrypoint(m, cha));
              logger.fine(() -> "- adding test method as entry point: " + m.getName().toString());
            }
          }

          // add entry points of setUp/tearDown methods
          Set<IMethod> setUpTearDowns;
          try {
            setUpTearDowns = getSetUpTearDownMethods(klass);
          } catch (Exception e) {
            throw new IllegalArgumentException(
                "Can't find test method entry points using class hierarchy: " + cha, e);
          }
          for (IMethod m : setUpTearDowns) {
            result.add(new DefaultEntrypoint(m, cha));
          }
        } else { // JUnit4?
          boolean isTestClass = false;

          // Since JUnit4 test classes are POJOs, look through each method.
          for (com.ibm.wala.classLoader.IMethod method : klass.getDeclaredMethods()) {
            // if method has an annotation
            if (!(method instanceof ShrikeCTMethod)) continue;
            for (Annotation annotation : ((ShrikeCTMethod) method).getAnnotations())
              if (isTestEntryPoint(annotation.getType().getName())) {
                result.add(new DefaultEntrypoint(method, cha));
                isTestClass = true;
              }
          }

          // if the class has a test method, we'll also need to add it's ctor.
          if (isTestClass) {
            IMethod classInitializer = klass.getClassInitializer();

            if (classInitializer != null) result.add(new DefaultEntrypoint(classInitializer, cha));

            IMethod ctor = klass.getMethod(MethodReference.initSelector);

            if (ctor != null) result.add(new DefaultEntrypoint(ctor, cha));
          }
        }
      }
    }
    return result::iterator;
  }

  private static boolean isTestEntryPoint(TypeName typeName) {
    // WALA uses $ to refers to inner classes. We have to replace "$" by "."
    // to make it a valid class name in Java source code.
    String javaName =
        StringStuff.jvmToReadableType(typeName.getPackage() + "." + typeName.getClassName());

    return TEST_ENTRY_POINT_ANNOTATION_NAMES.contains(javaName);
  }

  /**
   * Construct JUnit entrypoints for the specified test method in a scope.
   *
   * @throws IllegalArgumentException if cha is null
   * @apiNote Only handles JUnit3.
   */
  public static Iterable<Entrypoint> makeOne(
      IClassHierarchy cha,
      String targetPackageName,
      String targetSimpleClassName,
      String targetMethodName) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    // assume test methods don't have parameters
    final Atom targetPackageAtom = Atom.findOrCreateAsciiAtom(targetPackageName);
    final Atom targetSimpleClassAtom = Atom.findOrCreateAsciiAtom(targetSimpleClassName);
    final TypeName targetType =
        TypeName.findOrCreateClass(targetPackageAtom, targetSimpleClassAtom);
    final Atom targetMethodAtom = Atom.findOrCreateAsciiAtom(targetMethodName);

    logger.finer("finding entrypoint " + targetMethodAtom + " in " + targetType);

    final Set<Entrypoint> entryPts = HashSetFactory.make();

    for (IClass klass : cha) {
      TypeName klassType = klass.getName();
      if (klassType.equals(targetType) && isJUnitTestCase(klass)) {
        logger.finer("found test class");
        // add entry point corresponding to the target method
        for (IMethod method : klass.getDeclaredMethods()) {
          Atom methodAtom = method.getName();
          if (methodAtom.equals(targetMethodAtom)) {
            entryPts.add(new DefaultEntrypoint(method, cha));
            logger.fine(() -> "- adding entry point of the call graph: " + methodAtom);
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
   * Check if the given class is a JUnit test class. A JUnit test class is a subclass of
   * junit.framework.TestCase or junit.framework.TestSuite.
   *
   * @throws IllegalArgumentException if klass is null
   * @apiNote Applicable only to JUnit3.
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
   * Check if the given method is a JUnit test method, assuming that it is declared in a JUnit test
   * class. A method is a JUnit test method if the name has the prefix "test", or its name is
   * "setUp" or "tearDown".
   *
   * @throws IllegalArgumentException if m is null
   * @apiNote Only handles JUnit3.
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
    return methodName.startsWith("test")
        || methodName.equals("setUp")
        || methodName.equals("tearDown");
  }

  /**
   * Get the "setUp" and "tearDown" methods in the given class
   *
   * @apiNote Only handles JUnit3.
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
    while (currClass != null
        && !currClass.getName().equals(junitTestCaseType)
        && !currClass.getName().equals(junitTestSuiteType)) {

      for (IMethod method : currClass.getDeclaredMethods()) {

        final Atom methodAtom = method.getName();
        if (methodAtom.equals(setUpMethodAtom)
            || methodAtom.equals(tearDownMethodAtom)
            || method.isClinit()
            || method.isInit()) {
          result.add(method);
        }
      }
      currClass = currClass.getSuperclass();
    }
    return result;
  }
}
