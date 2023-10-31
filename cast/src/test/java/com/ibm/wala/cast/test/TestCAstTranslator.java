/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TestCAstTranslator {

  public static class TranslatorAssertions {
    private final Set<String> classes = new HashSet<>();

    private final Map<String, String> supers = new HashMap<>();

    private final Set<Pair<String, String>> instanceFields = new HashSet<>();

    private final Set<Pair<String, String>> staticFields = new HashSet<>();

    private final Map<Pair<String, Object>, Object> instanceMethods = HashMapFactory.make();

    private final Map<Pair<String, Object>, Object> staticMethods = HashMapFactory.make();

    private Set<String> getClasses() {
      return classes;
    }

    private Map<String, String> getSuperClasses() {
      return supers;
    }

    private Set<Pair<String, String>> getInstanceFields() {
      return instanceFields;
    }

    private Set<Pair<String, String>> getStaticFields() {
      return staticFields;
    }

    private Map<Pair<String, Object>, Object> getInstanceMethods() {
      return instanceMethods;
    }

    private Map<Pair<String, Object>, Object> getStaticMethods() {
      return staticMethods;
    }

    public TranslatorAssertions(Object[][] data) {
      for (Object[] entry : data) {
        String clsName = (String) entry[0];
        this.classes.add(clsName);

        String superName = (String) entry[1];
        this.supers.put(clsName, superName);

        String[] instanceFields = (String[]) entry[2];
        if (instanceFields != null) {
          for (String instanceField : instanceFields) {
            this.instanceFields.add(Pair.make(clsName, instanceField));
          }
        }

        String[] staticFields = (String[]) entry[3];
        if (staticFields != null) {
          for (String staticField : staticFields) {
            this.staticFields.add(Pair.make(clsName, staticField));
          }
        }

        Pair<?, ?>[] instanceMethods = (Pair<?, ?>[]) entry[4];
        if (instanceMethods != null) {
          for (Pair<?, ?> instanceMethod : instanceMethods) {
            this.instanceMethods.put(
                Pair.make(clsName, (Object) instanceMethod.fst), instanceMethod.snd);
          }
        }

        Pair<?, ?>[] staticMethods = (Pair<?, ?>[]) entry[5];
        if (staticMethods != null) {
          for (Pair<?, ?> staticMethod : staticMethods) {
            this.staticMethods.put(Pair.make(clsName, (Object) staticMethod.fst), staticMethod.snd);
          }
        }
      }
    }
  }

  protected abstract Language getLanguage();

  protected abstract String getTestPath();

  protected abstract SingleClassLoaderFactory getClassLoaderFactory();

  protected final IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();

  protected final SSAOptions options = new SSAOptions();

  public ClassHierarchy runTranslator(SourceFileModule[] fileNames) throws Exception {
    SingleClassLoaderFactory loaders = getClassLoaderFactory();

    AnalysisScope scope = CAstCallGraphUtil.makeScope(fileNames, loaders, getLanguage());

    ClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, getLanguage());

    return cha;
  }

  protected void dump(ClassHierarchy cha) {
    for (Object name : cha) {
      IClass cls = (IClass) name;
      System.err.println(("class " + cls));
      for (Object name2 : cls.getDeclaredInstanceFields()) {
        IField fld = (IField) name2;
        System.err.println(("instance field " + fld));
      }
      for (Object name2 : cls.getDeclaredStaticFields()) {
        IField fld = (IField) name2;
        System.err.println(("static field " + fld));
      }
      for (Object name2 : cls.getDeclaredMethods()) {
        IMethod mth = (IMethod) name2;
        if (mth.isStatic()) System.err.print("static ");
        System.err.println(
            ("method " + mth + " with " + mth.getNumberOfParameters() + " parameters"));
        for (int i = 0; i < mth.getNumberOfParameters(); i++) {
          System.err.println(("param " + i + ": " + mth.getParameterType(i)));
        }
        System.err.println(factory.makeIR(mth, Everywhere.EVERYWHERE, options));
      }
    }
  }

  public void checkAssertions(ClassHierarchy cha, TranslatorAssertions assertions) {
    Set<String> classes = assertions.getClasses();
    Map<String, String> supers = assertions.getSuperClasses();
    Set<Pair<String, String>> instanceFields = assertions.getInstanceFields();
    Set<Pair<String, String>> staticFields = assertions.getStaticFields();
    Map<Pair<String, Object>, Object> instanceMethods = assertions.getInstanceMethods();
    Map<Pair<String, Object>, Object> staticMethods = assertions.getStaticMethods();

    int clsCount = 0;
    for (Object name : cha) {
      IClass cls = (IClass) name;
      clsCount++;
      assertTrue(
          classes.contains(cls.getName().toString()), "found class " + cls.getName().toString());

      if (cls.getSuperclass() == null) {
        assertNull(supers.get(cls.getName().toString()), cls.getName() + " has no superclass");
      } else {
        assertEquals(
            "super of " + cls.getName() + " is " + cls.getSuperclass().getName(),
            supers.get(cls.getName().toString()),
            () -> cls.getSuperclass().getName().toString());
      }

      for (Object name2 : cls.getDeclaredInstanceFields()) {
        IField fld = (IField) name2;
        assertTrue(
            instanceFields.contains(Pair.make(cls.getName().toString(), fld.getName().toString())),
            cls.getName() + " has field " + fld.getName());
      }

      for (Object name2 : cls.getDeclaredStaticFields()) {
        IField fld = (IField) name2;
        assertTrue(
            staticFields.contains(Pair.make(cls.getName().toString(), fld.getName().toString())),
            cls.getName() + " has static field " + fld.getName());
      }

      for (Object name2 : cls.getDeclaredMethods()) {
        IMethod mth = (IMethod) name2;
        Integer np = mth.getNumberOfParameters();
        Pair<String, String> key = Pair.make(cls.getName().toString(), mth.getName().toString());

        if (mth.isStatic()) {
          assertTrue(
              staticMethods.containsKey(key),
              cls.getName() + " has static method " + mth.getName());
          assertEquals(
              staticMethods.get(key),
              np,
              cls.getName() + "::" + mth.getName() + " has " + np + " parameters");
        } else {
          assertTrue(
              instanceMethods.containsKey(key), cls.getName() + " has method " + mth.getName());
          assertEquals(
              instanceMethods.get(key),
              np,
              cls.getName() + "::" + mth.getName() + " has " + np + " parameters");
        }
      }
    }

    assertEquals(Float.parseFloat("want " + classes.size() + " classes"), clsCount, classes.size());
  }

  protected void testInternal(String[] args, TranslatorAssertions assertions) throws Exception {
    String testPath = getTestPath();
    SourceFileModule[] fileNames = new SourceFileModule[args.length];
    for (int i = 0; i < args.length; i++) {
      if (new File(args[i]).exists()) {
        fileNames[i] =
            CAstCallGraphUtil.makeSourceModule(new File(args[i]).toURI().toURL(), args[i]);
      } else if (new File(testPath + args[i]).exists()) {
        fileNames[i] =
            CAstCallGraphUtil.makeSourceModule(
                new File(testPath + args[i]).toURI().toURL(), args[i]);
      } else {
        URL url = getClass().getClassLoader().getResource(args[i]);
        fileNames[i] = CAstCallGraphUtil.makeSourceModule(url, args[i]);
      }
      assertNotNull(fileNames[i], args[i]);
    }

    ClassHierarchy cha = runTranslator(fileNames);
    dump(cha);
    if (assertions != null) {
      checkAssertions(cha, assertions);
    } else {
      System.err.println(("WARNING: no assertions for " + getClass()));
    }
  }

  protected void testInternal(String arg, TranslatorAssertions assertions) {
    try {
      testInternal(new String[] {arg}, assertions);
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.toString());
    }
  }
}
