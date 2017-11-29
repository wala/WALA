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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

/**
 * Reads {@link AnalysisScope} from a text file.
 */
public class AnalysisScopeReader {

  private static final ClassLoader MY_CLASSLOADER = AnalysisScopeReader.class.getClassLoader();

  protected static final String BASIC_FILE = "primordial.txt";

  /**
   * read in an analysis scope for a Java application from a text file
   * @param scopeFileName the text file specifying the scope
   * @param exclusionsFile a file specifying code to be excluded from the scope; can be <code>null</code>
   * @param javaLoader the class loader used to read in files referenced in the scope file, via {@link ClassLoader#getResource(String)}
   * @return the analysis scope
   * @throws IOException
   */
  public static AnalysisScope readJavaScope(String scopeFileName, File exclusionsFile, ClassLoader javaLoader) throws IOException {
    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
    return read(scope, scopeFileName, exclusionsFile, javaLoader);
  }


  public static AnalysisScope read(AnalysisScope scope, String scopeFileName, File exclusionsFile, ClassLoader javaLoader) throws IOException {
    BufferedReader r = null;
    try {
      // Now reading from jar is included in WALA, but we can't use their version, because they load from
      // jar by default and use filesystem as fallback. We want it the other way round. E.g. to deliver default
      // configuration files with the jar, but use userprovided ones if present in the working directory.
      // InputStream scopeFileInputStream = fp.getInputStreamFromClassLoader(scopeFileName, javaLoader);
      File scopeFile = new File(scopeFileName);

      String line;
      // assume the scope file is UTF-8 encoded; ASCII files will also be handled properly
      // TODO allow specifying encoding as a parameter?
      if (scopeFile.exists()) {
        r = new BufferedReader(new InputStreamReader(new FileInputStream(scopeFile), "UTF-8"));
      } else {
        // try to read from jar
        InputStream inFromJar = javaLoader.getResourceAsStream(scopeFileName);
        if (inFromJar == null) {
            throw new IllegalArgumentException("Unable to retreive " + scopeFileName + " from the jar using " + javaLoader);
        }
        r = new BufferedReader(new InputStreamReader(inFromJar));
      }
      while ((line = r.readLine()) != null) {
        processScopeDefLine(scope, javaLoader, line);
      }

      if (exclusionsFile != null) {
        try (InputStream fs = exclusionsFile.exists()? new FileInputStream(exclusionsFile): FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
          scope.setExclusions(new FileOfClasses(fs));
        }
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

  protected static AnalysisScope read(AnalysisScope scope, final URI scopeFileURI, final File exclusionsFile, ClassLoader javaLoader) throws IOException {
    BufferedReader r = null;
    try {
      String line;
      final InputStream inStream = scopeFileURI.toURL().openStream();
      if (inStream == null) {
        throw new IllegalArgumentException("Unable to retrieve URI " + scopeFileURI.toString());
      }
      r = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      while ((line = r.readLine()) != null) {
        processScopeDefLine(scope, javaLoader, line);
      }

      if (exclusionsFile != null) {
        try (final InputStream fs = exclusionsFile.exists()
            ? new FileInputStream(exclusionsFile)
            : FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
          scope.setExclusions(new FileOfClasses(fs));
        }
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
    FileProvider fp = (new FileProvider());
    if ("classFile".equals(entryType)) {
      File cf = fp.getFile(entryPathname, javaLoader);
      try {
        scope.addClassFileToScope(walaLoader, cf);
      } catch (InvalidClassFileException e) {
        Assertions.UNREACHABLE(e.toString());
      }
    } else if ("sourceFile".equals(entryType)) {
      File sf = fp.getFile(entryPathname, javaLoader);
      scope.addSourceFileToScope(walaLoader, sf, entryPathname);
    } else if ("binaryDir".equals(entryType)) {
      File bd = fp.getFile(entryPathname, javaLoader);
      assert bd.isDirectory();
      scope.addToScope(walaLoader, new BinaryDirectoryTreeModule(bd));
    } else if ("sourceDir".equals(entryType)) {
      File sd = fp.getFile(entryPathname, javaLoader);
      assert sd.isDirectory();
      scope.addToScope(walaLoader, new SourceDirectoryTreeModule(sd));
    } else if ("jarFile".equals(entryType)) {
      Module M = fp.getJarFileModule(entryPathname, javaLoader);
      scope.addToScope(walaLoader, M);
    } else if ("loaderImpl".equals(entryType)) {
      scope.setLoaderImpl(walaLoader, entryPathname);
    } else if ("stdlib".equals(entryType)) {
      String[] stdlibs = WalaProperties.getJ2SEJarFiles();
      for (String stdlib : stdlibs) {
        scope.addToScope(walaLoader, new JarFile(stdlib, false));
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
    return readJavaScope(BASIC_FILE, exclusionsFile, MY_CLASSLOADER);
  }



  /**
   * @param classPath class path to analyze, delimited by {@link File#pathSeparator}
   * @param exclusionsFile file holding class hierarchy exclusions. may be null
   * @throws IOException 
   * @throws IllegalStateException if there are problems reading wala properties
   */
  public static AnalysisScope makeJavaBinaryAnalysisScope(String classPath, File exclusionsFile) throws IOException {
    if (classPath == null) {
      throw new IllegalArgumentException("classPath null");
    }
    AnalysisScope scope = makePrimordialScope(exclusionsFile);
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
          JarFile jar = new JarFile(path, false);
          scope.addToScope(loader, jar);
          try {
            if (jar.getManifest() != null) {
              String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
              if (cp != null) {
                for(String cpEntry : cp.split(" ")) { 
                  addClassPathToScope(new File(path).getParent() + File.separator + cpEntry, scope, loader);
                }
              }
            }
          } catch (RuntimeException e) {
            System.err.println("warning: trouble processing class path of " + path);
          }
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
    } catch (InvalidClassFileException e) {
      Assertions.UNREACHABLE(e.toString());
    }
  }
}
