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
package com.ibm.wala.classLoader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.io.FileSuffixes;
import com.ibm.wala.util.shrike.ShrikeClassReaderHandle;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * A class loader that reads class definitions from a set of Modules.
 */
public class ClassLoaderImpl implements IClassLoader {
  public static final int DEBUG_LEVEL = 0;

  private static final boolean OPTIMIZE_JAR_FILE_IO = true;

  /**
   * classes to ignore
   */
  final private SetOfClasses exclusions;

  /**
   * Identity for this class loader
   */
  final private ClassLoaderReference loader;

  /**
   * A mapping from class name (TypeName) to IClass
   */
  protected final Map<TypeName, IClass> loadedClasses = HashMapFactory.make();

  /**
   * A mapping from class name (TypeName) to String (source file name)
   */
  private final Map<TypeName, ModuleEntry> sourceMap = HashMapFactory.make();

  /**
   * Parent classloader
   */
  final private IClassLoader parent;

  /**
   * Governing class hierarchy
   */
  protected final IClassHierarchy cha;

  /**
   * an object to delegate to for loading of array classes
   */
  private final ArrayClassLoader arrayClassLoader;

  /**
   * @param loader class loader reference identifying this loader
   * @param parent parent loader for delegation
   * @param exclusions set of classes to exclude from loading
   */
  @SuppressWarnings("unused")
  public ClassLoaderImpl(ClassLoaderReference loader, ArrayClassLoader arrayClassLoader, IClassLoader parent,
      SetOfClasses exclusions, IClassHierarchy cha) {

    if (loader == null) {
      throw new IllegalArgumentException("null loader");
    }

    this.arrayClassLoader = arrayClassLoader;
    this.parent = parent;
    this.loader = loader;
    this.exclusions = exclusions;
    this.cha = cha;

    if (DEBUG_LEVEL > 0) {
      System.err.println("Creating class loader for " + loader);
    }
  }

  /**
   * Return the Set of (ModuleEntry) source files found in a module.
   * 
   * @param M the module
   * @return the Set of source files in the module
   * @throws IOException
   */
  @SuppressWarnings("unused")
  private Set<ModuleEntry> getSourceFiles(Module M) throws IOException {
    if (DEBUG_LEVEL > 0) {
      System.err.println("Get source files for " + M);
    }
    HashSet<ModuleEntry> result = HashSetFactory.make();
    for (ModuleEntry entry : Iterator2Iterable.make(M.getEntries())) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("consider entry for source information: " + entry);
      }
      if (entry.isSourceFile()) {
        if (DEBUG_LEVEL > 0) {
          System.err.println("found source file: " + entry);
        }
        result.add(entry);
      } else if (entry.isModuleFile()) {
        result.addAll(getSourceFiles(entry.asModule()));
      }
    }
    return result;
  }

  /**
   * Return the Set of (ModuleEntry) class files found in a module.
   * 
   * @param M the module
   * @return the Set of class Files in the module
   * @throws IOException
   */
  @SuppressWarnings("unused")
  private Set<ModuleEntry> getClassFiles(Module M) throws IOException {
    if (DEBUG_LEVEL > 0) {
      System.err.println("Get class files for " + M);
    }
    HashSet<ModuleEntry> result = HashSetFactory.make();
    for (ModuleEntry entry : Iterator2Iterable.make(M.getEntries())) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("ClassLoaderImpl.getClassFiles:Got entry: " + entry);
      }
      if (entry.isClassFile()) {
        if (DEBUG_LEVEL > 0) {
          System.err.println("result contains: " + entry);
        }
        result.add(entry);
      } else if (entry.isModuleFile()) {
        Set<ModuleEntry> s = getClassFiles(entry.asModule());
        removeClassFiles(s, result);
        result.addAll(s);
      } else {
        if (DEBUG_LEVEL > 0) {
          System.err.println("Ignoring entry: " + entry);
        }
      }
    }
    return result;
  }

  /**
   * Remove from s any class file module entries which already are in t
   */
  private static void removeClassFiles(Set<ModuleEntry> s, Set<ModuleEntry> t) {
    Set<String> old = HashSetFactory.make();
    for (ModuleEntry m : t) {
      old.add(m.getClassName());
    }
    HashSet<ModuleEntry> toRemove = HashSetFactory.make();
    for (ModuleEntry m : s) {
      if (old.contains(m.getClassName())) {
        toRemove.add(m);
      }
    }
    s.removeAll(toRemove);
  }

  /**
   * Return a Set of IClasses, which represents all classes this class loader can load.
   */
  private Collection<IClass> getAllClasses() {
    assert loadedClasses != null;

    return loadedClasses.values();
  }

  static class ByteArrayReaderHandle extends ShrikeClassReaderHandle {
    public ByteArrayReaderHandle(ModuleEntry entry, byte[] contents) {
      super(entry);
      assert contents != null && contents.length > 0;
      this.contents = contents;
    }

    private byte[] contents;

    private boolean cleared;

    @Override
    public ClassReader get() throws InvalidClassFileException {
      if (cleared) {
        return super.get();
      } else {
        return new ClassReader(contents);
      }
    }

    @Override
    public void clear() {
      if (cleared) {
        super.clear();
      } else {
        contents = null;
        cleared = true;
      }
    }

  }

  /**
   * Set up the set of classes loaded by this object.
   */
  @SuppressWarnings("unused")
  private void loadAllClasses(Collection<ModuleEntry> moduleEntries, Map<String, Object> fileContents) {
    for (ModuleEntry entry : moduleEntries) {
      if (!entry.isClassFile()) {
        continue;
      }

      String className = entry.getClassName().replace('.', '/');

      if (DEBUG_LEVEL > 0) {
        System.err.println("Consider " + className);
      }

      if (exclusions != null && exclusions.contains(className)) {
        if (DEBUG_LEVEL > 0) {
          System.err.println("Excluding " + className);
        }
        continue;
      }

      ShrikeClassReaderHandle entryReader = new ShrikeClassReaderHandle(entry);

      className = "L" + className;
      if (DEBUG_LEVEL > 0) {
        System.err.println("Load class " + className);
      }
      try {
        TypeName T = TypeName.string2TypeName(className);
        if (loadedClasses.get(T) != null) {
          Warnings.add(MultipleImplementationsWarning.create(className));
        } else if (parent != null && parent.lookupClass(T) != null) {
          Warnings.add(MultipleImplementationsWarning.create(className));
        } else {
          // try to read from memory
          ShrikeClassReaderHandle reader = entryReader;
          if (fileContents != null) {
            final Object contents = fileContents.get(entry.getName());
            if (contents != null) {
              // reader that uses the in-memory bytes
              reader = new ByteArrayReaderHandle(entry, (byte[]) contents);
            }
          }
          ShrikeClass tmpKlass = new ShrikeClass(reader, this, cha);
          if (tmpKlass.getReference().getName().equals(T)) {
            // always used the reader based on the entry after this point,
            // so we can null out and re-read class file contents
            loadedClasses.put(T, new ShrikeClass(entryReader, this, cha));
            if (DEBUG_LEVEL > 1) {
              System.err.println("put " + T + " ");
            }
          } else {
            Warnings.add(InvalidClassFile.create(className));
          }
        }
      } catch (InvalidClassFileException e) {
        if (DEBUG_LEVEL > 0) {
          System.err.println("Ignoring class " + className + " due to InvalidClassFileException");
        }
        Warnings.add(InvalidClassFile.create(className));
      }
    }
  }

  @SuppressWarnings("unused")
  private Map<String, Object> getAllClassAndSourceFileContents(byte[] jarFileContents, String fileName,
      Map<String, Map<String, Long>> entrySizes) {
    if (jarFileContents == null) {
      return null;
    }
    Map<String, Long> entrySizesForFile = entrySizes.get(fileName);
    if (entrySizesForFile == null) {
      return null;
    }
    Map<String, Object> result = HashMapFactory.make();
    try (final JarInputStream s = new JarInputStream(new ByteArrayInputStream(jarFileContents), false)) {
      JarEntry entry = null;
      while ((entry = s.getNextJarEntry()) != null) {
        byte[] entryBytes = getEntryBytes(entrySizesForFile.get(entry.getName()), s);
        if (entryBytes == null) {
          return null;
        }
        String name = entry.getName();
        if (FileSuffixes.isJarFile(name) || FileSuffixes.isWarFile(name)) {
          Map<String, Object> nestedResult = getAllClassAndSourceFileContents(entryBytes, name, entrySizes);
          if (nestedResult == null) {
            return null;
          }
          for (String entryName : nestedResult.keySet()) {
            if (!result.containsKey(entryName)) {
              result.put(entryName, nestedResult.get(entryName));
            }
          }
        } else if (FileSuffixes.isClassFile(name) || FileSuffixes.isSourceFile(name)) {
          result.put(name, entryBytes);
        }
      }
    } catch (IOException e) {
      assert false;
    }
    return result;
  }

  private static byte[] getEntryBytes(Long size, InputStream is) throws IOException {
    if (size == null) {
      return null;
    }
    ByteArrayOutputStream S = new ByteArrayOutputStream();
    int n = 0;
    long count = 0;
    byte[] buffer = new byte[1024];
    while (n > -1 && count < size) {
      n = is.read(buffer, 0, 1024);
      if (n > -1) {
        S.write(buffer, 0, n);
        count += n;
      }
    }
    return S.toByteArray();
  }

  /**
   * A warning when we find more than one implementation of a given class name
   */
  private static class MultipleImplementationsWarning extends Warning {

    final String className;

    MultipleImplementationsWarning(String className) {
      super(Warning.SEVERE);
      this.className = className;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + className;
    }

    public static MultipleImplementationsWarning create(String className) {
      return new MultipleImplementationsWarning(className);
    }
  }

  /**
   * A warning when we encounter InvalidClassFileException
   */
  private static class InvalidClassFile extends Warning {

    final String className;

    InvalidClassFile(String className) {
      super(Warning.SEVERE);
      this.className = className;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + className;
    }

    public static InvalidClassFile create(String className) {
      return new InvalidClassFile(className);
    }
  }

  /**
   * Set up mapping from type name to Module Entry
   */
  @SuppressWarnings("unused")
  protected void loadAllSources(Set<ModuleEntry> sourceModules) {
    for (ModuleEntry entry : sourceModules) {
      String className = entry.getClassName().replace('.', '/');
      className = className.replace(File.separatorChar, '/');
      className = "L" + ((className.startsWith("/")) ? className.substring(1) : className);
      TypeName T = TypeName.string2TypeName(className);

      // Note: entry.getClassName() may not return the correct class name, for example, 
      // com.ibm.wala.classLoader.SourceFileModule.getClassName()
      // {
      //  return FileSuffixes.stripSuffix(fileName).replace(File.separator.charAt(0), '/');
      // }
      // If fileName includes the full path, such as 
      // C:\TestApps\HelloWorld\src\main\java\com\ibm\helloworld\MyClass.java
      // Then above method would return the class name as:
      // C:/TestApps/HelloWorld/src/main/java/com/ibm/helloworld/MyClass
      // However, in WALA, we represent class name as:
      // PackageName.className, such as com.ibm.helloworld.MyClass
      //
      // In method "void init(List<Module> modules)", We loadAllClasses firstly, then 
      // loadAllSources. Therefore, all the classes should be in the map loadedClasses
      // when this method loadAllSources is called. To ensure we add the correct class 
      // name to the sourceMap, here we look up the class name from the map "loadedClasses"
      // before adding the source info to the sourceMap. If we could not find the class, 
      // we edit the className and try again.
      
      boolean success = false;
      if(loadedClasses.get(T) != null){
        if (DEBUG_LEVEL > 0) {
          System.err.println("adding to source map: " + T + " -> " + entry.getName());
        }
        sourceMap.put(T, entry);
        success = true;
      }
      //Class does not exist
      else{
        // look at substrings starting after '/' characters, in the hope
        // that we find a known class name
        while (className.indexOf('/') > 0) {
          className = "L" + className.substring(className.indexOf('/') + 1, className.length());
          TypeName T2 = TypeName.string2TypeName(className);
          if (loadedClasses.get(T2) != null) {
            if (DEBUG_LEVEL > 0) {
              System.err.println("adding to source map: " + T2 + " -> " + entry.getName());
            }
            sourceMap.put(T2, entry);
            success = true;
            break;
          }
        }
      }
      if(success == false){
        //Add the T and entry to the sourceMap anyway, just in case in some special
        //cases, we add new classes later.
        if (DEBUG_LEVEL > 0) {
          System.err.println("adding to source map: " + T + " -> " + entry.getName());
        }
        sourceMap.put(T, entry);
      }
    }
  }

  /**
   * Initialize internal data structures
   * 
   * @throws IllegalArgumentException if modules is null
   */
  @SuppressWarnings("unused")
  @Override
  public void init(List<Module> modules) throws IOException {

    if (modules == null) {
      throw new IllegalArgumentException("modules is null");
    }

    // module are loaded according to the given order (same as in Java VM)
    Set<ModuleEntry> classModuleEntries = HashSetFactory.make();
    Set<ModuleEntry> sourceModuleEntries = HashSetFactory.make();
    for (Module archive : modules) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("add archive: " + archive);
      }
      // byte[] jarFileContents = null;
      if (OPTIMIZE_JAR_FILE_IO && archive instanceof JarFileModule) {
        // if we have a jar file, we read the whole thing into memory and operate on that; enables more
        // efficient sequential I/O
        // this is work in progress; for now, we read the file into memory and throw away the contents, which
        // still gives a speedup for large jar files since it reads sequentially and warms up the FS cache. we get a small slowdown
        // for smaller jar files or for jar files already in the FS cache. eventually, we should
        // actually use the bytes read and eliminate the slowdown
        // 11/22/10: I can't figure out a way to actually use the bytes without hurting performance.  Apparently,
        // extracting files from a jar stored in memory via a JarInputStream is really slow compared to using
        // a JarFile.  Will leave this as is for now.  --MS
        // jarFileContents = archive instanceof JarFileModule ? getJarFileContents((JarFileModule) archive) : null;
        getJarFileContents((JarFileModule) archive);
      }
      Set<ModuleEntry> classFiles = getClassFiles(archive);
      removeClassFiles(classFiles, classModuleEntries);
      Set<ModuleEntry> sourceFiles = getSourceFiles(archive);
      Map<String, Object> allClassAndSourceFileContents = null;
      if (OPTIMIZE_JAR_FILE_IO) {
        // work in progress --MS
        // if (archive instanceof JarFileModule) {
        // final JarFileModule jfModule = (JarFileModule) archive;
        // final String name = jfModule.getJarFile().getName();
        // Map<String, Map<String, Long>> entrySizes = getEntrySizes(jfModule, name);
        // allClassAndSourceFileContents = getAllClassAndSourceFileContents(jarFileContents, name, entrySizes);
        // }
        // jarFileContents = null;
      }
      loadAllClasses(classFiles, allClassAndSourceFileContents);
      loadAllSources(sourceFiles);
      for (ModuleEntry file : classFiles) {
        classModuleEntries.add(file);
      }
      for (ModuleEntry file : sourceFiles) {
        sourceModuleEntries.add(file);
      }
    }
  }

  @SuppressWarnings("unused")
  private Map<String, Map<String, Long>> getEntrySizes(Module module, String name) {
    Map<String, Map<String, Long>> result = HashMapFactory.make();
    Map<String, Long> curFileResult = HashMapFactory.make();
    for (ModuleEntry e : Iterator2Iterable.make(module.getEntries())) {
      if (e.isModuleFile()) {
        result.putAll(getEntrySizes(e.asModule(), e.getName()));
      } else {
        if (e instanceof JarFileEntry) {
          curFileResult.put(e.getName(), ((JarFileEntry) e).getSize());
        }
      }
    }
    result.put(name, curFileResult);
    return result;
  }

  /**
   * get the contents of a jar file. if any IO exceptions occur, catch and return null.
   */
  private static void getJarFileContents(JarFileModule archive) {
    String jarFileName = archive.getJarFile().getName();
    InputStream s = null;
    try {
      File jarFile = (new FileProvider()).getFile(jarFileName);
      int bufferSize = 65536;
      s = new BufferedInputStream(new FileInputStream(jarFile), bufferSize);
      byte[] b = new byte[1024];
      int n = s.read(b);
      while (n != -1) {
        n = s.read(b);
      }
    } catch (IOException e) {
    } finally {
      try {
        if (s != null) {
          s.close();
        }
      } catch (IOException e) {
      }
    }
  }

  @Override
  public ClassLoaderReference getReference() {
    return loader;
  }

  @Override
  public Iterator<IClass> iterateAllClasses() {
    return getAllClasses().iterator();
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#lookupClass(com.ibm.wala.types.TypeName)
   */
  @SuppressWarnings("unused")
  @Override
  public IClass lookupClass(TypeName className) {
    if (className == null) {
      throw new IllegalArgumentException("className is null");
    }
    if (DEBUG_LEVEL > 1) {
      System.err.println(this + ": lookupClass " + className);
    }

    // treat arrays specially:
    if (className.isArrayType()) {
      return arrayClassLoader.lookupClass(className, this, cha);
    }

    // try delegating first.
    IClassLoader parent = getParent();
    if (parent != null) {
      IClass result = parent.lookupClass(className);
      if (result != null) {
        return result;
      }
    }
    // delegating failed. Try our own namespace.
    IClass result = loadedClasses.get(className);
    return result;
  }

  /**
   * Method getParent.
   */
  @Override
  public IClassLoader getParent() {
    return parent;
  }

  @Override
  public Atom getName() {
    return loader.getName();
  }

  @Override
  public Language getLanguage() {
    return Language.JAVA;
  }

  @Override
  public String toString() {
    return getName().toString();
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#getNumberOfClasses()
   */
  @Override
  public int getNumberOfClasses() {
    return getAllClasses().size();
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#getNumberOfMethods()
   */
  @Override
  public int getNumberOfMethods() {
    int result = 0;
    for (IClass klass : Iterator2Iterable.make(iterateAllClasses())) {
      result += klass.getDeclaredMethods().size();
    }
    return result;
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#getSourceFileName(com.ibm.wala.classLoader.IClass)
   */
  @Override
  public String getSourceFileName(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    ModuleEntry e = sourceMap.get(klass.getName());
    return e == null ? null : e.getName();
  }

  @Override
  public Reader getSource(IMethod method, int offset) {
    return getSource(method.getDeclaringClass());
  }

  @Override
  public String getSourceFileName(IMethod method, int offset) {
    return getSourceFileName(method.getDeclaringClass());
  }

  @Override
  public Reader getSource(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    ModuleEntry e = sourceMap.get(klass.getName());
    return e == null ? null : new InputStreamReader(e.getInputStream());
  }

  /*
   * @see com.ibm.wala.classLoader.IClassLoader#removeAll(java.util.Collection)
   */
  @SuppressWarnings("unused")
  @Override
  public void removeAll(Collection<IClass> toRemove) {
    if (toRemove == null) {
      throw new IllegalArgumentException("toRemove is null");
    }
    for (IClass klass : toRemove) {
      if (DEBUG_LEVEL > 0) {
        System.err.println("removing " + klass.getName());
      }
      loadedClasses.remove(klass.getName());
      sourceMap.remove(klass.getName());
    }
  }

  @Override
  public SSAInstructionFactory getInstructionFactory() {
    return getLanguage().instructionFactory();
  }
}
