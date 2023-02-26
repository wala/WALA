/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
package com.ibm.wala.dalvik.util;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.jar.JarFile;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

public class AndroidAnalysisScope {

  private static final String BASIC_FILE = "primordial.txt";

  /**
   * Creates an Android Analysis Scope
   *
   * @param codeFileName the name of a .oat|.apk|.dex file
   * @param exclusions the name of the exclusions file (nullable)
   * @param loader the classloader to use
   * @param androidLib an array of libraries (e.g. the Android SDK jar) to add to the scope
   * @return a {@link AnalysisScope}
   */
  public static AnalysisScope setUpAndroidAnalysisScope(
      URI codeFileName, String exclusions, ClassLoader loader, URI... androidLib)
      throws IOException {
    return setUpAndroidAnalysisScope(
        codeFileName, DexFileModule.AUTO_INFER_API_LEVEL, exclusions, loader, androidLib);
  }

  public static AnalysisScope setUpAndroidAnalysisScope(
      URI codeFileName, int apiLevel, String exclusions, ClassLoader loader, URI... androidLib)
      throws IOException {
    AnalysisScope scope;
    File exclusionsFile = exclusions != null ? new File(exclusions) : null;

    if (androidLib == null || androidLib.length == 0) {
      scope = AnalysisScopeReader.instance.readJavaScope(BASIC_FILE, exclusionsFile, loader);
    } else {
      scope = AnalysisScope.createJavaAnalysisScope();

      if (exclusionsFile != null) {
        try (final InputStream fs =
            exclusionsFile.exists()
                ? new FileInputStream(exclusionsFile)
                : FileProvider.class
                    .getClassLoader()
                    .getResourceAsStream(exclusionsFile.getName())) {
          scope.setExclusions(new FileOfClasses(fs));
        }
      }

      scope.setLoaderImpl(
          ClassLoaderReference.Primordial, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

      for (URI al : androidLib) {
        try {
          scope.addToScope(ClassLoaderReference.Primordial, DexFileModule.make(new File(al)));
        } catch (Exception e) {
          scope.addToScope(
              ClassLoaderReference.Primordial, new JarFileModule(new JarFile(new File(al))));
        }
      }
    }

    scope.setLoaderImpl(
        ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

    File codeFile = new File(codeFileName);
    boolean isContainerFile =
        codeFile.getName().endsWith(".oat") || codeFile.getName().endsWith(".apk");

    if (isContainerFile) {
      MultiDexContainer<? extends DexBackedDexFile> multiDex =
          DexFileFactory.loadDexContainer(
              codeFile,
              apiLevel == DexFileModule.AUTO_INFER_API_LEVEL ? null : Opcodes.forApi(apiLevel));

      for (String dexEntry : multiDex.getDexEntryNames()) {
        scope.addToScope(
            ClassLoaderReference.Application, new DexFileModule(codeFile, dexEntry, apiLevel));
      }
    } else {
      scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(codeFile, apiLevel));
    }

    return scope;
  }

  /** Handle .apk file. */
  public static void addClassPathToScope(
      String classPath, AnalysisScope scope, ClassLoaderReference loader) {
    if (classPath == null) {
      throw new IllegalArgumentException("null classPath");
    }
    try {
      String[] paths = classPath.split(File.pathSeparator);

      for (String path : paths) {
        if (path.endsWith(".jar")
            || path.endsWith(".apk")
            || path.endsWith(".dex")) { // Handle android file.
          File f = new File(path);
          scope.addToScope(loader, DexFileModule.make(f));
        } else {
          File f = new File(path);
          if (f.isDirectory()) { // handle directory FIXME not working
            // for .dex and .apk files into that
            // directory
            scope.addToScope(loader, new BinaryDirectoryTreeModule(f));
          } else { // handle java class file.
            try {
              scope.addClassFileToScope(loader, f);
            } catch (InvalidClassFileException e) {
              throw new IllegalArgumentException("Invalid class file", e);
            }
          }
        }
      }

    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }
}
