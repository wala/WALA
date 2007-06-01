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
package com.ibm.wala.util.scope;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.ArgumentTypeEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.BasicEntrypoints;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * All public and protected methods in the "root packages" are considered
 * entrypoints, except not inner classes
 * 
 * @author pistoia
 * @author sfink
 */
@Deprecated
public class EclipseEntrypoints extends BasicEntrypoints {

  private final static boolean DEBUG = false;

  /**
   * @param scope
   *          governing analysis scope
   * @param cha
   *          governing class hierarchy
   * @param internalEntrypoints
   *          This boolean flag is used to decide whether internal packages
   *          (those containing the substring ".internal" in their names) should
   *          contribute entry points to the analysis (value <code>true</code>)
   *          or not (value <code>false</code>). The default value is
   *          <code>false</code> because internal packages are not supposed to
   *          be invoked from external bundles.
   * @throws WalaException 
   * @throws IllegalArgumentException  if cha is null
   * @throws IllegalArgumentException  if scope is null
   */
  public EclipseEntrypoints(EclipseAnalysisScope scope, final IClassHierarchy cha, boolean internalEntrypoints) throws WalaException {
    if (scope == null) {
      throw new IllegalArgumentException("scope is null");
    }
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    String pluginName = scope.getPluginName();
    String pluginsDirName = scope.getPluginsDirName();
    
    // Build the set of root packages
    Set<JarFile> rootJars = findRootJars(pluginName, pluginsDirName);
    // Build a set of root packages.
    Set rootPackages = findRootPackages(internalEntrypoints, rootJars);
    for (IClass klass : cha) {
      String className = StringStuff.jvmToReadableType(klass.getName().toString());
      if (className.contains("$")) {
        continue;
      }
      String packageName = computePackageName(className);
      if (!rootPackages.contains(packageName)) {
        continue;
      }
      if (!klass.isInterface()) {
        if (isApplicationClass(scope, klass)) {
          for (Iterator methodIt = klass.getDeclaredMethods().iterator(); methodIt.hasNext();) {
            IMethod method = (IMethod) methodIt.next();
            if (!method.isAbstract() && method.isPublic() || method.isProtected()) {
              add(new ArgumentTypeEntrypoint(method, cha));
            }
          }
        }
      }
    }
    if (DEBUG) {
      Trace.println(getClass() + "Number of EntryPoints:" + size());
    }
  }

  /**
   * @param scope
   *          an <code>AnalysisScope</code> object representing the governing
   *          analysis scope
   * @param klass
   *          the <code>IClass</code> object whose class loading membership is
   *          being tested
   * @return <code>true</code> iff <code>klass</code> is loaded by the
   *         application loader.
   */
  private boolean isApplicationClass(AnalysisScope scope, IClass klass) {
    return scope.getApplicationLoader().equals(klass.getClassLoader().getReference());
  }

  private String computePackageName(String className) {
    int lastDot = className.lastIndexOf('.');
    if (lastDot == -1) {
      return "";
    }
    return className.substring(0, lastDot);
  }

  /**
   * findRootJars - All the plugins listed in the configuration file must be
   * considered for the analysis scope as well as for finding the root nodes.
   * 
   * @param p
   *          The configuration properties file for the
   *          EclipseAnalysisScopeBuilder.
   * @return A Set containing entry <code>JARFile</code> objects. An <i>entry
   *         JAR file</i> is a JAR file containing some class files whose
   *         methods could be used as entry points. By default, all the public
   *         and protected methods in non-internal packages are considered entry
   *         points.
   * @throws WalaException
   */
  private static Set<JarFile> findRootJars(String pluginName, String pluginsDirName) throws WalaException {
    Set<JarFile> rootJars = HashSetFactory.make();

    File pluginDirectory = new File(pluginsDirName);
    File plugIn = EclipseAnalysisScope.findPluginDirOrJAR(pluginName, pluginsDirName, pluginDirectory);
    if (plugIn == null) {
      throw new WalaException("EclipseAnalysisScopeBuilder: Unable to identify " + pluginName);
    }
    String realName = plugIn.getPath();
    if (plugIn.isDirectory()) {
      System.out.println("EclipseAnalysisScopeBuilder: " + pluginName + " converted to " + realName);
      // Find all the JAR files in the plugin directory
      Set<JarFile> pluginAllJars = EclipseAnalysisScope.findJars(plugIn);
      // Among them, find the root JAR files for this plugin
      Set<JarFile> pluginRootJars = findBundleRootJars(realName, pluginAllJars);
      // Add the root JARs for this plugin to the set of all the root
      // JAR files for the whole analysis
      if (pluginRootJars == null || pluginRootJars.isEmpty()) {
        System.out.println("EclipseAnalysisScopeBuilder: Plugin or fragment contains no JAR files.  Nothing to analyze.");
      } else {
        rootJars.addAll(pluginRootJars);
      }
    } else {
      try {
        JarFile jarFile = new JarFile(plugIn);
        System.out.println("EclipseAnalysisScopeBuilder: JAR file " + pluginsDirName + "/" + pluginName + ".jar found");
        rootJars.add(jarFile);
      } catch (IOException ioe) {
        System.out.println("EclipseAnalysisScopeBuilder: JAR file " + pluginsDirName + "/" + pluginName + ".jar not found");
      }
    }
    return rootJars;
  }

  /**
   * findRootPackages - Builds up a list of package names from the rootJars Set
   * and saves the results in private TreeSet var rootPackages.
   * 
   * @return A Set containing String objects, each String object representing
   *         the name of a package that should be considered a root package. A
   *         <i>root package </i> is a package such that all its classes' public
   *         and protected methods are considered root methods. By default, a
   *         root package does not contain the String ".internal." in its name.
   */
  private static Set<String> findRootPackages(boolean internalEntrypoints, Set<JarFile> rootJars) {
    Set<String> rootPackages = HashSetFactory.make();
    if (DEBUG) {
      System.out.println("ROOT JARS:");
    }
    for (JarFile rootJarFile : rootJars) {
      if (DEBUG) {
        System.out.println(rootJarFile.getName());
      }
      Enumeration entries = rootJarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = (JarEntry) entries.nextElement();
        String entryName = entry.getName();
        // Filter out the internal packages, which should not
        // be invoked by client code.

        if (!internalEntrypoints && entryName.indexOf("/internal/") != -1) {
          continue;
        }
        if (entryName.endsWith(".class")) {
          // Indentify the package name
          int lastSlash = entryName.lastIndexOf('/');
          String tempPackageName = entryName.substring(0, lastSlash);
          String packageName = tempPackageName.replace('/', '.');
          rootPackages.add(packageName);
        }
      }
    }
    if (rootPackages.isEmpty()) {
      StringBuffer buf = new StringBuffer();
      int numberOfRootJars = rootJars.size();
      buf.append("EclipseAnalysisScopeBuilder: No entry points in JAR file");
      buf.append((numberOfRootJars == 1) ? ":\n" : "s:\n");
      Iterator<JarFile> rootJarsIter = rootJars.iterator();
      while (rootJarsIter.hasNext()) {
        buf.append("\t" + rootJarsIter.next() + "\n");
      }
      buf.append("The analysis is terminating.");
      System.out.println(buf.toString());
      System.exit(0);
    }
    if (DEBUG) {
      System.out.println("ROOT PACKAGES: " + rootPackages);
    }
    return rootPackages;
  }

  /**
   * Find all the root JAR files for a plugin, given the plugin directory. The
   * root JAR files are returned as String objects, each String representing the
   * fully qualified name of the JAR file. The information is based on the
   * plugin configuration file.
   * 
   * @param dirName
   *          a String representing the fully qualified name of the directory
   *          where a plugin is stored.
   * @param pluginAllJars
   *          all the JAR files found in the plugin directory.
   * @return a TreeSet of String object, each object representing the fully
   *         qualified name of a plugin JAR file.
   */
  private static Set<JarFile> findBundleRootJars(String dirName, Set<JarFile> pluginAllJars) {
    String manifestFileName = dirName + File.separator + "META-INF" + File.separator + "MANIFEST.MF";
    Set<JarFile> pluginRootJars = HashSetFactory.make();
    InputStream is = EclipseAnalysisScope.getInputStream(manifestFileName);
    if (is == null) {
      System.out.println("EclipseAnalysisScopeBuilder: " + manifestFileName + " was not found.");
      return pluginAllJars;
    } else {
      try {
        Manifest m = new Manifest(is);
        Attributes a = m.getMainAttributes();
        // If FRAGMENT_HOST is set, then this is a fragment, not a plugin.
        String hostPlugin = a.getValue(Constants.FRAGMENT_HOST);
        if (hostPlugin != null) {
          hostPlugin = parseFragmentHost(hostPlugin);
        }
        String bundleClasspath = a.getValue(Constants.BUNDLE_CLASSPATH);
        if (bundleClasspath != null) {
          String requiredArray[] = bundleClasspath.split(",");
          if (requiredArray != null) {
            for (int i = 0; i < requiredArray.length; i++) {
              String libraryName = (requiredArray[i].trim()).split(";")[0];
              // Verify that the JAR file is really there
              String fullLibraryName = dirName + File.separator + libraryName;
              Iterator pluginAllJarsIter = pluginAllJars.iterator();
              while (pluginAllJarsIter.hasNext()) {
                JarFile jarFile = (JarFile) pluginAllJarsIter.next();
                String jarFileName = jarFile.getName();
                if (jarFileName.equals(fullLibraryName)) {
                  pluginRootJars.add(jarFile);
                  break;
                }
              }
            }
          }
        }
      } catch (Exception e) {
        System.out.println("EclipseAnalysisScopeBuilder: " + e.getLocalizedMessage());
      }
    }
    return pluginRootJars;
  }

  /**
   * Parses the name of an Eclipse 3.0 fragment host plugin from the value
   * returned by the manifest.
   * 
   * e.g. if value contains: BadPlugin2;bundle-version="1.0.0" this method
   * returns "BadPlugin2".
   * 
   * @param value
   * @return String - the name of the fragment's plugin host.
   * 
   */
  private static String parseFragmentHost(String value) {
    String result = null;
    String elements[] = value.split(";");
    if (elements != null)
      result = elements[0];
    return result;
  }
}