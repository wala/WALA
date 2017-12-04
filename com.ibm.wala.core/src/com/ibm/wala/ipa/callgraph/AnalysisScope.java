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
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.wala.classLoader.ArrayClassLoader;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.ClassFileModule;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.RtJar;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;

/**
 * Base class that represents a set of files to analyze.
 * 
 * The analysis scope is partitioned by class loader. There are three pre-defined class loader scopes:
 * <ul>
 * <li>Primordial (for <code>rt.jar</code>, the core classes)
 * <li>Extension (for extension libraries in $JRE/lib/ext)
 * <li>Application (for the classes of the application)
 * </ul>
 * 
 * Each class loader will load a set of classes described by a {@link Module}.
 */
public class AnalysisScope {

  private final static int DEBUG_LEVEL = 0;

  public static final Atom PRIMORDIAL = Atom.findOrCreateUnicodeAtom("Primordial");

  public static final Atom EXTENSION = Atom.findOrCreateUnicodeAtom("Extension");

  public static final Atom APPLICATION = Atom.findOrCreateUnicodeAtom("Application");

  public static final Atom SYNTHETIC = Atom.findOrCreateUnicodeAtom("Synthetic");

  /**
   * Create an analysis scope initialized for analysis of Java
   */
  public static AnalysisScope createJavaAnalysisScope() {
    AnalysisScope scope = new AnalysisScope(Collections.singleton(Language.JAVA));
    scope.initForJava();
    return scope;
  }

  /**
   * Initialize a scope for java analysis
   */
  protected void initForJava() {
    initCoreForJava();
    initSynthetic(loadersByName.get(APPLICATION));
  }

  /**
   * Initialize the standard 3 class loaders for java analysis
   */
  protected void initCoreForJava() {
    ClassLoaderReference primordial = new ClassLoaderReference(PRIMORDIAL, ClassLoaderReference.Java, null);
    ClassLoaderReference extension = new ClassLoaderReference(EXTENSION, ClassLoaderReference.Java, primordial);
    ClassLoaderReference application = new ClassLoaderReference(APPLICATION, ClassLoaderReference.Java, extension);

    loadersByName.put(PRIMORDIAL, primordial);
    loadersByName.put(EXTENSION, extension);
    loadersByName.put(APPLICATION, application);
  }

  /**
   * Create the class loader for synthetic classes.
   */
  protected void initSynthetic(ClassLoaderReference parent) {
    ClassLoaderReference synthetic = new ClassLoaderReference(SYNTHETIC, ClassLoaderReference.Java, parent);
    setLoaderImpl(synthetic, "com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader");
    loadersByName.put(SYNTHETIC, synthetic);
  }

  /**
   * A set of classes to exclude from the analysis entirely.
   */
  private SetOfClasses exclusions;

  final protected LinkedHashMap<Atom, ClassLoaderReference> loadersByName = new LinkedHashMap<>();

  /**
   * Special class loader for array instances
   */
  private final ArrayClassLoader arrayClassLoader = new ArrayClassLoader();

  final private Map<ClassLoaderReference, List<Module>> moduleMap = HashMapFactory.make(3);

  private final Map<Atom, Language> languages;

  protected AnalysisScope(Collection<? extends Language> languages) {
    super();
    this.languages = new HashMap<>();
    for (Language l : languages) {
      this.languages.put(l.getName(), l);
    }
  }

  public Language getLanguage(Atom name) {
    return languages.get(name);
  }

  public boolean isApplicationLoader(IClassLoader loader) {
    return loader.getReference().equals(getLoader(APPLICATION));
  }

  /**
   * Return the information regarding the primordial loader.
   */
  public ClassLoaderReference getPrimordialLoader() {
    return getLoader(PRIMORDIAL);
  }

  /**
   * Return the information regarding the extension loader.
   */
  public ClassLoaderReference getExtensionLoader() {
    return getLoader(EXTENSION);
  }

  /**
   * Return the information regarding the application loader.
   */
  public ClassLoaderReference getApplicationLoader() {
    return getLoader(APPLICATION);
  }

  /**
   * Return the information regarding the application loader.
   */
  public ClassLoaderReference getSyntheticLoader() {
    return getLoader(SYNTHETIC);
  }

  /**
   * @return the set of languages to be processed during this analysis session.
   */
  public Collection<Language> getLanguages() {
    return languages.values();
  }

  /**
   * @return the set of "base languages," each of which defines a family of compatible languages, and therefore induces a distinct
   *         ClassHierarchy
   */
  public Set<Language> getBaseLanguages() {
    Set<Language> result = HashSetFactory.make();
    for (Language language : getLanguages()) {
      if (language.getBaseLanguage() == null) {
        result.add(language);
      }
    }
    return result;
  }

  /**
   * Add a class file to the scope for a loader
   */
  public void addSourceFileToScope(ClassLoaderReference loader, File file, String fileName) throws IllegalArgumentException {
    List<Module> s = MapUtil.findOrCreateList(moduleMap, loader);
    s.add(new SourceFileModule(file, fileName, null));
  }

  /**
   * Add a class file to the scope for a loader
   * @throws InvalidClassFileException 
   */
  public void addClassFileToScope(ClassLoaderReference loader, File file) throws IllegalArgumentException, InvalidClassFileException {
    List<Module> s = MapUtil.findOrCreateList(moduleMap, loader);
    s.add(new ClassFileModule(file, null));
  }

  /**
   * Add a jar file to the scope for a loader
   */
  @SuppressWarnings("unused")
  public void addToScope(ClassLoaderReference loader, JarFile file) {
    List<Module> s = MapUtil.findOrCreateList(moduleMap, loader);
    if (DEBUG_LEVEL > 0) {
      System.err.println(("AnalysisScope: add JarFileModule " + file.getName()));
    }
    s.add(new JarFileModule(file));
  }

  /**
   * Add a module to the scope for a loader
   */
  @SuppressWarnings("unused")
  public void addToScope(ClassLoaderReference loader, Module m) {
    if (m == null) {
      throw new IllegalArgumentException("null m");
    }
    List<Module> s = MapUtil.findOrCreateList(moduleMap, loader);
    if (DEBUG_LEVEL > 0) {
      System.err.println(("AnalysisScope: add module " + m));
    }
    s.add(m);
  }

  /**
   * Add all modules from another scope
   */
  public void addToScope(AnalysisScope other) {
    if (other == null) {
      throw new IllegalArgumentException("null other");
    }
    for (ClassLoaderReference loader : other.getLoaders()) {
      for (Module m : other.getModules(loader)) {
        addToScope(loader, m);
      }
    }
  }

  /**
   * Add a module file to the scope for a loader. The classes in the added jar file will override classes added to the scope so far.
   */
  @SuppressWarnings("unused")
  public void addToScopeHead(ClassLoaderReference loader, Module m) {
    if (m == null) {
      throw new IllegalArgumentException("null m");
    }
    List<Module> s = MapUtil.findOrCreateList(moduleMap, loader);
    if (DEBUG_LEVEL > 0) {
      System.err.println(("AnalysisScope: add overriding module " + m));
    }
    s.add(0, m);
  }

  /**
   * @return the ClassLoaderReference specified by <code>name</code>.
   * @throws IllegalArgumentException if name is null
   */
  public ClassLoaderReference getLoader(Atom name) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("name is null");
    }
    if (name.length() == 0) {
      throw new IllegalArgumentException("empty atom is not a legal class loader name");
    }
    /*
     * if (Assertions.verifyAssertions) { if (name.getVal(0) > 'Z') { Assertions._assert(name.getVal(0) <= 'Z',
     * "Classloader name improperly capitalised?  (" + name + ")"); } }
     */
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
    if (ref == null) {
      throw new IllegalArgumentException("null ref");
    }
    if (implClass == null) {
      throw new IllegalArgumentException("null implClass");
    }
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
    if (desc == null) {
      throw new IllegalArgumentException("null desc");
    }
    ClassLoaderReference clr = getLoader(loader);
    Descriptor ddesc = Descriptor.findOrCreate(languages.get(clr.getLanguage()), desc);
    TypeReference type = TypeReference.findOrCreate(clr, TypeName.string2TypeName(klass));
    return MethodReference.findOrCreate(type, name, ddesc);
  }

  public List<Module> getModules(ClassLoaderReference loader) {
    List<Module> result = moduleMap.get(loader);
    List<Module> empty = Collections.emptyList();
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
    return RtJar.getRtJar(
        new MapIterator<Module,JarFile>(
            new FilterIterator<>(getModules(getPrimordialLoader()).iterator(), JarFileModule.class::isInstance), M -> ((JarFileModule) M).getJarFile()));
  }

  public String getJavaLibraryVersion() throws IllegalStateException {
    try (final JarFile rtJar = getRtJar()) {
      if (rtJar == null) {
        throw new IllegalStateException("cannot find runtime libraries");
      }
      Manifest man = rtJar.getManifest();
      assert man != null : "runtime library has no manifest!";
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

  public boolean isJava18Libraries() throws IllegalStateException {
    return getJavaLibraryVersion().startsWith("1.8");
  }

  public boolean isJava17Libraries() throws IllegalStateException {
    return getJavaLibraryVersion().startsWith("1.7");
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

  /**
   * Creates a "serializable" version of the analysis scope.
   * 
   * @return a "serializable" version of the analysis scope.
   * @throws NotSerializableException
   */
  public ShallowAnalysisScope toShallowAnalysisScope() throws NotSerializableException {

    if (getArrayClassLoader().getNumberOfClasses() != 0) {
      throw new NotSerializableException("Scope was already used for building array classes");
    }
    // Note: 'arrayClassLoader' object will be built from scratch in remote process

    // represent modules map as a set of strings (corresponding to analysis scope file lines.
    List<String> moduleLines = new ArrayList<>();
    for (Map.Entry<ClassLoaderReference, List<Module>> e : moduleMap.entrySet()) {
      ClassLoaderReference lrReference = e.getKey();
      String moduleLdr = lrReference.getName().toString();
      String moduleLang = lrReference.getLanguage().toString();
      assert Language.JAVA.getName().equals(lrReference.getLanguage()) : "Java language only is currently supported";

      for (Module m : e.getValue()) {
        String moduleType;
        String modulePath;
        if (m instanceof JarFileModule) {
          moduleType = "jarFile";
          modulePath = ((JarFileModule) m).getAbsolutePath();
        } else if (m instanceof BinaryDirectoryTreeModule) {
          moduleType = "binaryDir";
          modulePath = ((BinaryDirectoryTreeModule) m).getPath();
        } else if (m instanceof SourceDirectoryTreeModule) {
          moduleType = "sourceDir";
          modulePath = ((SourceDirectoryTreeModule) m).getPath();
        } else if (m instanceof SourceFileModule) {
          moduleType = "sourceFile";
          modulePath = ((SourceFileModule) m).getAbsolutePath();
        } else {
          Assertions.UNREACHABLE("Module type isn't supported - " + m);
          continue;
        }
        modulePath.replace("\\", "/");
        String moduleDescrLine = String.format("%s,%s,%s,%s", moduleLdr, moduleLang, moduleType, modulePath);
        moduleLines.add(moduleDescrLine);
      }
    }

    // represent loaderImplByRef map as set of strings
    List<String> ldrImplLines = new ArrayList<>();
    for (Map.Entry<ClassLoaderReference, String> e : loaderImplByRef.entrySet()) {
      ClassLoaderReference lrReference = e.getKey();
      String ldrName = lrReference.getName().toString();
      String ldrLang = lrReference.getLanguage().toString();
      assert Language.JAVA.getName().equals(lrReference.getLanguage()) : "Java language only is currently supported";
      String ldrImplName = e.getValue();
      String ldrImplDescrLine = String.format("%s,%s,%s,%s", ldrName, ldrLang, "loaderImpl", ldrImplName);
      ldrImplLines.add(ldrImplDescrLine);
    }

    ShallowAnalysisScope shallowScope = new ShallowAnalysisScope(getExclusions(), moduleLines, ldrImplLines);
    return shallowScope;
  }
}
