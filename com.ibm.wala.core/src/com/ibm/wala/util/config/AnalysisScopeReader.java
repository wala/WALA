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
package com.ibm.wala.util.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.Plugin;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.core.plugin.CorePlugin;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

/**
 * Reads {@link AnalysisScope} from a text file.
 */
public class AnalysisScopeReader {

  private static final ClassLoader MY_CLASSLOADER = AnalysisScopeReader.class.getClassLoader();

  private static final String BASIC_FILE = "primordial.txt";

  public static AnalysisScope readJavaScope(String scopeFileName, File exclusionsFile, ClassLoader javaLoader) throws IOException {
    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
    return read(scope, scopeFileName, exclusionsFile, javaLoader, CorePlugin.getDefault());
  }

  private static AnalysisScope readJavaScope(String scopeFileName, File exclusionsFile, ClassLoader javaLoader, Plugin plugIn) throws IOException {
    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
    return read(scope, scopeFileName, exclusionsFile, javaLoader, plugIn);
  }

  private static AnalysisScope read(AnalysisScope scope, String scopeFileName, File exclusionsFile, ClassLoader javaLoader,
      Plugin plugIn) throws IOException {
    BufferedReader r = null;
    try {
      File scopeFile = (plugIn == null) ? FileProvider.getFile(scopeFileName, javaLoader) : FileProvider.getFileFromPlugin(plugIn,
          scopeFileName);
      assert scopeFile.exists();

      String line;
      r = new BufferedReader(new FileReader(scopeFile));
      while ((line = r.readLine()) != null) {
        processScopeDefLine(scope, javaLoader, line);
      }

      if (exclusionsFile != null) {
        scope.setExclusions(FileOfClasses.createFileOfClasses(exclusionsFile));
      }

    } finally {
      if (r != null) {
        try {
          r.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return scope;
  }

  public static void processScopeDefLine(AnalysisScope scope, ClassLoader javaLoader, String line) throws IOException {
    if (line == null) {
      throw new IllegalArgumentException("null line");
    }
    StringTokenizer toks = new StringTokenizer(line, "\n,");
    if (!toks.hasMoreTokens()) {
      return;
    }
    Atom loaderName = Atom.findOrCreateUnicodeAtom(toks.nextToken());
    ClassLoaderReference walaLoader = scope.getLoader(loaderName);

    @SuppressWarnings("unused")
    String language = toks.nextToken();
    String entryType = toks.nextToken();
    String entryPathname = toks.nextToken();
    if ("classFile".equals(entryType)) {
      File cf = FileProvider.getFile(entryPathname, javaLoader);
      scope.addClassFileToScope(walaLoader, cf);
    } else if ("sourceFile".equals(entryType)) {
      File sf = FileProvider.getFile(entryPathname, javaLoader);
      scope.addSourceFileToScope(walaLoader, sf, entryPathname);
    } else if ("binaryDir".equals(entryType)) {
      File bd = FileProvider.getFile(entryPathname, javaLoader);
      assert bd.isDirectory();
      scope.addToScope(walaLoader, new BinaryDirectoryTreeModule(bd));
    } else if ("sourceDir".equals(entryType)) {
      File sd = FileProvider.getFile(entryPathname, javaLoader);
      assert sd.isDirectory();
      scope.addToScope(walaLoader, new SourceDirectoryTreeModule(sd));
    } else if ("jarFile".equals(entryType)) {
      Module M = FileProvider.getJarFileModule(entryPathname, javaLoader);
      scope.addToScope(walaLoader, M);
    } else if ("loaderImpl".equals(entryType)) {
      scope.setLoaderImpl(walaLoader, entryPathname);
    } else if ("stdlib".equals(entryType)) {
      String[] stdlibs = WalaProperties.getJ2SEJarFiles();
      for (int i = 0; i < stdlibs.length; i++) {
        scope.addToScope(walaLoader, new JarFile(stdlibs[i]));
      }
    } else {
      Assertions.UNREACHABLE();
    }
  }

  /**
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IOException 
   * @throws IllegalStateException if there are problmes reading wala properties
   */
  public static AnalysisScope makePrimordialScope(File exclusionsFile) throws IOException {
    return readJavaScope(BASIC_FILE, exclusionsFile, MY_CLASSLOADER, CorePlugin.getDefault());
  }

  private static AnalysisScope makePrimordialScope(File exclusionsFile, Plugin plugIn) throws IOException {
    return readJavaScope(BASIC_FILE, exclusionsFile, MY_CLASSLOADER, plugIn);
  }

  /**
   * @param classPath class path to analyze, delimited by File.pathSeparator
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IOException 
   * @throws IllegalStateException if there are problems reading wala properties
   */
  public static AnalysisScope makeJavaBinaryAnalysisScope(String classPath, File exclusionsFile) throws IOException {
    return makeJavaBinaryAnalysisScope(classPath, exclusionsFile, CorePlugin.getDefault());
  }

  /**
   * @param classPath class path to analyze, delimited by File.pathSeparator
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IOException 
   * @throws IllegalStateException if there are problems reading wala properties
   */
  public static AnalysisScope makeJavaBinaryAnalysisScope(String classPath, File exclusionsFile, Plugin plugIn) throws IOException {
    if (classPath == null) {
      throw new IllegalArgumentException("classPath null");
    }
    AnalysisScope scope = makePrimordialScope(exclusionsFile, plugIn);
    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);

    addClassPathToScope(classPath, scope, loader);

    return scope;
  }

  public static void addClassPathToScope(String classPath, AnalysisScope scope, ClassLoaderReference loader) {
    if (classPath == null) {
      throw new IllegalArgumentException("null classPath");
    }
    try {
      StringTokenizer paths = new StringTokenizer(classPath, File.pathSeparator);
      while (paths.hasMoreTokens()) {
        String path = paths.nextToken();
        if (path.endsWith(".jar")) {
          scope.addToScope(loader, new JarFile(path));
        } else {
          File f = new File(path);
          if (f.isDirectory()) {
            scope.addToScope(loader, new BinaryDirectoryTreeModule(f));
          } else {
            scope.addClassFileToScope(loader, f);
          }
        }
      }
    } catch (IOException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }
}
