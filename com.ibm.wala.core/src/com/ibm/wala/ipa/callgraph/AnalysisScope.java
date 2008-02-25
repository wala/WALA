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
package com.ibm.wala.ipa.callgraph;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.wala.classLoader.ArrayClassLoader;
import com.ibm.wala.classLoader.ClassFileModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;

/**
 * 
 * Base class that represents a set of files to analyze.
 * 
 * The analysis scope is partitioned by class loader. There are three
 * pre-defined class loader scopes:
 * <ul>
 * <li>Primordial (for <code>rt.jar</code>, the core classes)
 * <li>Extension (for extension libraries in $JRE/lib/ext)
 * <li>Application (for the classes of the application)
 * </ul>
 * 
 * @author sfink
 */
public class AnalysisScope {

  private final static int DEBUG_LEVEL = 0;

  public static final Atom PRIMORDIAL = Atom.findOrCreateUnicodeAtom("Primordial");

  public static final Atom EXTENSION = Atom.findOrCreateUnicodeAtom("Extension");

  public static final Atom APPLICATION = Atom.findOrCreateUnicodeAtom("Application");

  public static final Atom SYNTHETIC = Atom.findOrCreateUnicodeAtom("Synthetic");

  public static AnalysisScope createAnalysisScope(Set<Language> languages) {
    return new AnalysisScope(languages);
  }

  /**
   * A set of classes to exclude from the analysis entirely.
   */
  private SetOfClasses exclusions;

  /**
   * map: Atom -> ClassLoaderReference
   */
  final protected LinkedHashMap<Atom, ClassLoaderReference> loadersByName = new LinkedHashMap<Atom, ClassLoaderReference>();

  /**
   * Special class loader for array instances
   */
  private final ArrayClassLoader arrayClassLoader = new ArrayClassLoader();

  /**
   * map: ClassLoaderReference -> Set <Modules>
   */
  final private Map<ClassLoaderReference, Set<Module>> moduleMap = HashMapFactory.make(3);

  private final Collection<Language> languages;

  protected AnalysisScope(Collection<Language> languages) {
    super();
    this.languages = languages;
    ClassLoaderReference primordial = new ClassLoaderReference(PRIMORDIAL, ClassLoaderReference.Java);
    ClassLoaderReference extension = new ClassLoaderReference(EXTENSION, ClassLoaderReference.Java);
    ClassLoaderReference application = new ClassLoaderReference(APPLICATION, ClassLoaderReference.Java);
    ClassLoaderReference synthetic = new ClassLoaderReference(SYNTHETIC, ClassLoaderReference.Java);
    extension.setParent(primordial);
    application.setParent(extension);
    synthetic.setParent(application);

    setLoaderImpl(synthetic, "com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader");

    loadersByName.put(PRIMORDIAL, primordial);
    loadersByName.put(EXTENSION, extension);
    loadersByName.put(APPLICATION, application);
    loadersByName.put(SYNTHETIC, synthetic);
  }

  /**
   * Return the information regarding the primordial loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getPrimordialLoader() {
    return getLoader(PRIMORDIAL);
  }

  /**
   * Return the information regarding the extension loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getExtensionLoader() {
    return getLoader(EXTENSION);
  }

  /**
   * Return the information regarding the application loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getApplicationLoader() {
    return getLoader(APPLICATION);
  }

  /**
   * Return the information regarding the application loader.
   * 
   * @return ClassLoaderReference
   */
  public ClassLoaderReference getSyntheticLoader() {
    return getLoader(SYNTHETIC);
  }

  /**
   * @return the set of languages to be processed during this analysis session.
   */
  public Collection<Language> getLanguages() {
    return Collections.unmodifiableCollection(languages);
  }

  /**
   * @return the set of "base languages," each of which defines a family of
   *         compatible languages, and therefore induces a distinct
   *         ClassHierarchy
   */
  public Set<Language> getBaseLanguages() {
    Set<Language> result = HashSetFactory.make();
    for (Language language : languages) {
      if (language.getBaseLanguage() == null) {
        result.add(language);
      }
    }
    return result;
  }

  /**
   * Add a class file to the scope for a loader
   * 
   * @param loader
   * @param file
   */
  public void addSourceFileToScope(ClassLoaderReference loader, File file, String fileName) throws IllegalArgumentException {
    Set<Module> s = MapUtil.findOrCreateSet(moduleMap, loader);
    s.add(new SourceFileModule(file, fileName));
  }

  /**
   * Add a class file to the scope for a loader
   * 
   * @param loader
   * @param file
   */
  public void addClassFileToScope(ClassLoaderReference loader, File file) throws IllegalArgumentException {
    Set<Module> s = MapUtil.findOrCreateSet(moduleMap, loader);
    s.add(new ClassFileModule(file));
  }

  /**
   * Add a jar file to the scope for a loader
   * 
   * @param loader
   * @param file
   */
  public void addToScope(ClassLoaderReference loader, JarFile file) {
    Set<Module> s = MapUtil.findOrCreateSet(moduleMap, loader);
    if (DEBUG_LEVEL > 0) {
      Trace.println("AnalysisScope: add JarFileModule " + file.getName());
    }
    s.add(new JarFileModule(file));
  }

  /**
   * Add a module to the scope for a loader
   * 
   * @param loader
   * @param m
   */
  public void addToScope(ClassLoaderReference loader, Module m) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(m != null);
    }
    Set<Module> s = MapUtil.findOrCreateSet(moduleMap, loader);
    if (DEBUG_LEVEL > 0) {
      Trace.println("AnalysisScope: add module " + m);
    }
    s.add(m);
  }

  /**
   * @return the ClassLoaderReference specified by <code>name</code>.
   * @throws IllegalArgumentException
   *             if name is null
   */
  public ClassLoaderReference getLoader(Atom name) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("empty atom is not a legal class loader name");
    }
    if (Assertions.verifyAssertions) {
      if (name.getVal(0) > 'Z') {
        Assertions._assert(name.getVal(0) <= 'Z', "Classloader name improperly capitalised?  (" + name + ")");
      }
    }
    return loadersByName.get(name);
  }

  protected ClassLoaderReference classLoaderName2Ref(String clName) {
    return getLoader(Atom.findOrCreateUnicodeAtom(clName));
  }

  private final HashMap<ClassLoaderReference, String> loaderImplByRef = HashMapFactory.make();

  public String getLoaderImpl(ClassLoaderReference ref) {
    return loaderImplByRef.get(ref);
  }

  public void setLoaderImpl(ClassLoaderReference ref, String implClass) {
    loaderImplByRef.put(ref, implClass);
  }

  public Collection<ClassLoaderReference> getLoaders() {
    return Collections.unmodifiableCollection(loadersByName.values());
  }

  public int getNumberOfLoaders() {
    return loadersByName.values().size();
  }

  public SetOfClasses getExclusions() {
    return exclusions;
  }

  public void setExclusions(SetOfClasses classes) {
    exclusions = classes;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (ClassLoaderReference loader : loadersByName.values()) {
      result.append(loader.getName());
      result.append("\n");
      for (Module m : getModules(loader)) {
        result.append(" ");
        result.append(m);
        result.append("\n");
      }
    }
    result.append(getExclusionString());
    result.append("\n");
    return result.toString();
  }

  /**
   * @return a String that describes exclusions from the analysis scope.
   */
  protected Object getExclusionString() {
    return "Exclusions: " + exclusions;
  }

  /**
   * Utility function. Useful when parsing input.
   */
  public MethodReference findMethod(Atom loader, String klass, Atom name, ImmutableByteArray desc) {
    ClassLoaderReference clr = getLoader(loader);
    Descriptor ddesc = Descriptor.findOrCreate(desc);
    TypeReference type = TypeReference.findOrCreate(clr, TypeName.string2TypeName(klass));
    return MethodReference.findOrCreate(type, name, ddesc);
  }

  public Set<Module> getModules(ClassLoaderReference loader) {
    Set<Module> result = moduleMap.get(loader);
    Set<Module> empty = Collections.emptySet();
    return result == null ? empty : result;
  }

  /**
   * @return Returns the arrayClassLoader.
   */
  public ArrayClassLoader getArrayClassLoader() {
    return arrayClassLoader;
  }

  /**
   * @return the rt.jar (1.4) or core.jar (1.5) file, or null if not found.
   */
  private JarFile getRtJar() {
    for (Iterator MS = getModules(getPrimordialLoader()).iterator(); MS.hasNext();) {
      Module M = (Module) MS.next();
      if (M instanceof JarFileModule) {
        JarFile JF = ((JarFileModule) M).getJarFile();
        if (JF.getName().endsWith(File.separator + "rt.jar")) {
          return JF;
        }
        if (JF.getName().endsWith(File.separator + "core.jar")) {
          return JF;
        }
        // hack for Mac
        if (JF.getName().equals("/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar")) {
          return JF;
        }
      }
    }
    return null;
  }

  public String getJavaLibraryVersion() throws IllegalStateException {
    JarFile rtJar = getRtJar();
    if (rtJar == null) {
      throw new IllegalStateException("cannot find runtime libraries");
    }
    try {
      Manifest man = rtJar.getManifest();
      Assertions._assert(man != null, "runtime library has no manifest!");
      String result = man.getMainAttributes().getValue("Specification-Version");
      if (result == null) {
        Attributes att = man.getMainAttributes();
        System.err.println("main attributes:" + att);
        Assertions.UNREACHABLE("Manifest for " + rtJar.getName() + " has no value for Specification-Version");
      }
      return result;
    } catch (java.io.IOException e) {
      Assertions.UNREACHABLE("error getting rt.jar manifest!");
      return null;
    }
  }

  public boolean isJava16Libraries() throws IllegalStateException {
    return getJavaLibraryVersion().startsWith("1.6");
  }

  public boolean isJava15Libraries() throws IllegalStateException {
    return getJavaLibraryVersion().startsWith("1.5");
  }

  public boolean isJava14Libraries() throws IllegalStateException {
    return getJavaLibraryVersion().startsWith("1.4");
  }

}
