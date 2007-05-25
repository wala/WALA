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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;

import com.ibm.wala.classLoader.ArrayClassLoader;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.collections.FifoQueueNoDuplicates;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WalaException;

/**
 * This class is used to build the complete analysis scope for analyzing an
 * Eclipse plugin.
 * <p>
 * Given an Eclipse plugin, this class detects all the other plugins that should
 * be part of the analysis scope when analyzing the given plugin.
 * 
 * This is buggy in that it ignores the Import-Package declarations in the manifest.
 * This is a problem.   Probably the way to go is to recode this whole thing using
 * a headless Eclipse instance, and rely on Eclipse APIs to resolve
 * dependencies the right way.
 * 
 * @author Marco Pistoia
 * @author Stephen Fink
 */
@Deprecated
public class EclipseAnalysisScope extends AnalysisScope {

  public static final boolean DEBUG = true;

  private static final String ECLIPSE_PRODUCT_NAME = ".eclipseproduct";

  /**
   * We delegate tracking the analysis scope to this object
   */
  private final EMFScopeWrapper delegate;

  /**
   * <code>byte</code> value indicating the Eclipse version.
   */
  private byte version;

  /**
   * <code>byte</code> value indicating the Eclipse subversion. Default value
   * is 0.
   */
  private byte subversion = 0;

  /**
   * A Set containing String objects, each String object representing the plugin
   * id of a plug-in, fragment, or bundle being analyzed.
   */
  private Set<String> pluginIds = HashSetFactory.make();
  
  /**
   * name of plugin being analyzed
   */
  private final String pluginName;
  
  /**
   * absolute name of plugins directory in Eclipse installation
   */
  private final String pluginsDirName;


  /**
   * @param pluginName
   *          name of the plugin to be analyzed
   * @throws WalaException
   */
  public EclipseAnalysisScope(String pluginName) throws WalaException {
    
    this.pluginName = pluginName;
    
    Properties wp = WalaProperties.loadProperties();
    this.pluginsDirName = wp.getProperty(WalaProperties.ECLIPSE_PLUGINS_DIR);
    Assertions.productionAssertion(pluginsDirName != null, "eclipse_plugins_dir property is not set");
    
    File pluginDirectory = new File(pluginsDirName);

    initVersionInfo(pluginDirectory);

    this.delegate = buildDelegate(pluginDirectory);
  }

  public EMFScopeWrapper buildDelegate(File pluginDirectory) throws WalaException {
    pluginIds.add(pluginName);
    
    File plugIn = findPluginDirOrJAR(pluginName, pluginsDirName, pluginDirectory);
    if (plugIn == null) {
      throw new WalaException("EclipseAnalysisScopeBuilder: Unable to identify " + pluginName);
    }

    Collection<JarFile> relevantJars = findRequiredPluginsFragmentsAndBundles(plugIn, pluginDirectory);
    // Scan all plug-in manifests for a Fragment-host whose id matches
    // the eclipse plugin ids (Eclipse.plugin.nnn from RootDetector.config).
    relevantJars.addAll(getContributingFragments(pluginDirectory));
    // Build the analysis scope
    return createScope(relevantJars);

  }

  private EMFScopeWrapper createScope(Collection<JarFile> analysisScopeJars) throws WalaException {
    EMFScopeWrapper scope = EMFScopeWrapper.generateScope(JavaScopeUtil.makePrimordialScope());
    for (JarFile jarFile : analysisScopeJars) {
      scope.addToScope(ClassLoaderReference.Application, jarFile);
    }
    if (DEBUG) {
      printConfigurationInformation(scope);
    }
    return scope;
  }

  /**
   * initialize information about the Eclipse version
   */
  private void initVersionInfo(File pluginDirectory) {
    File eclipseRoot = pluginDirectory.getParentFile();
    File[] contents = eclipseRoot.listFiles();
    Assertions.productionAssertion(contents != null, "no files in eclipse root " + eclipseRoot);
    for (int i = 0; i < contents.length; i++) {
      File content = contents[i];
      if (content.getName().equals(ECLIPSE_PRODUCT_NAME)) {
        Properties props = new Properties();
        try {
          props.load(new FileInputStream(content));
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          throw new RuntimeException("EclipseAnalysisScopeBuilder: File " + ECLIPSE_PRODUCT_NAME
              + " not found in Eclipse root directory, " + eclipseRoot.getName() + ".");
        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException("EclipseAnalysisScopeBuilder: File " + ECLIPSE_PRODUCT_NAME
              + " could not be accessed in Eclipse root directory, " + eclipseRoot.getName() + ".");
        }
        String eclipseVersion = (String) props.get("version");
        if (eclipseVersion == null) {
          throw new RuntimeException("EclipseAnalysisScopeBuilder: Unable to detect Eclipse version from file "
              + ECLIPSE_PRODUCT_NAME + " in directory " + eclipseRoot.getName() + ".");
        }
        eclipseVersion = eclipseVersion.replace(".", ":"); // "." is not
        // accepted as a
        // separator
        String[] versions = eclipseVersion.split(":");
        version = (new Byte(versions[0])).byteValue();
        if (versions.length >= 2) {
          subversion = (new Byte(versions[1])).byteValue();
        }
        break; // We found what we needed
      }
    }
  }

  /**
   * printConfigurationInformation - Print out the contents of the specified
   * Properties object.
   * 
   * @param p
   *          (in) - The configuration properties file for the
   *          EclipseAnalysisScopeBuilder.
   */
  private static void printConfigurationInformation(AnalysisScope scope) {
    System.out.println("CONFIGURATION INFORMATION:");
    Set modules;
    // Primordial
    modules = scope.getModules(ClassLoaderReference.Primordial);
    System.out.println("PRIMORDIAL:\n" + modules);
    // Extension
    modules = scope.getModules(ClassLoaderReference.Extension);
    System.out.println("EXTENSION:\n" + modules);
    // Application
    modules = scope.getModules(ClassLoaderReference.Application);
    System.out.println("APPLICATION:\n" + modules);
  }

  /**
   * findRequiredPluginsFragmentsAndBundles - Find all the plugins necessary to
   * the analysis scope.
   * 
   * @param workQ
   *          A list of plugins and fragments which are required by the plugin
   *          being analyzed.
   */
  private Collection<JarFile> findRequiredPluginsFragmentsAndBundles(File plugIn, File pluginDirectory) {
    Collection<JarFile> result = HashSetFactory.make();
    FifoQueueNoDuplicates<File> workQ = new FifoQueueNoDuplicates<File>();
    // Find all the plugins necessary to the analysis scope.
    workQ.push(plugIn);
    while (!workQ.isEmpty()) {
      File f = (File) workQ.pop();
      Manifest mf = null;
      try {
        if (f.isDirectory()) {
          Set<JarFile> plugInJars = findJars(f);
          result.addAll(plugInJars);
          mf = new Manifest(getInputStream(f.getPath() + File.separator + "META-INF" + File.separator + "MANIFEST.MF"));
        } else {
          JarFile plugInJar = new JarFile(f);
          mf = plugInJar.getManifest();
          result.add(plugInJar);
        }
      } catch (IOException ioe) {
        System.err.println("EclipseAnalysisScopebuilder: Unable to access manifest file for " + f.getName());
        continue;
      }
      Set<File> requiredPlugins = findRequiredBundles(mf, pluginsDirName, pluginDirectory);
      if (requiredPlugins != null) {
        System.out.println("EclipseAnalysisScopeBuilder: " + f.getName() + " requires bundles:");
        Iterator requiredPluginsIter = requiredPlugins.iterator();
        while (requiredPluginsIter.hasNext()) {
          File requiredPlugIn = (File) requiredPluginsIter.next();
          System.out.println("  " + requiredPlugIn.getName());
        }
      }
      if (requiredPlugins != null) {
        workQ.push(requiredPlugins.iterator());
      }
    }
    return result;
  }

  /**
   * Converts a plugin name into an actual name in the file system. For example,
   * "org.eclipse.help.appserver" would be converted to something like
   * "E:\eclipse3.0M7\eclipse\plugins\org.eclipse.help.appserver_3.0.0" if the
   * plugin is available in a directory or
   * "E:\eclipse3.0M7\eclipse\plugins\org.eclipse.help.appserver_3.0.0.jar" if
   * the plugin is available as a JAR file.
   * 
   * @param libName
   *          a String representing the name of a plugin
   * @return a File representing the directory or JAR file containing the
   *         plugin. This method returns <code>null</code> if no directory or
   *         JAR file was found for this plugin.
   * @throws IllegalArgumentException  if pluginDirectory is null
   */
  public static File findPluginDirOrJAR(String libName, String pluginsDirName, File pluginDirectory) throws IllegalArgumentException {
    if (pluginDirectory == null) {
      throw new IllegalArgumentException("pluginDirectory is null");
    }
    String pathName = pluginsDirName + "/" + libName;
    File path = new File(pathName);
    if (path.isDirectory()) // libName corresponds to a real directory
      return path;
    // libName does not corresponds to a real directory. Search the
    // plugins directory for a corresponding subdirectory containing
    // this plugin.
    File[] contents = pluginDirectory.listFiles();
    if (contents == null) {
      throw new IllegalArgumentException("bad plugin directory " + pluginDirectory.getAbsolutePath());
    }
    for (int i = 0; i < contents.length; i++) {
      String subName = contents[i].getName();
      if (subName.startsWith(libName + "_"))
        return contents[i]; // found
    }
    return null; // the plugin directory was not found
  }

  /**
   * Finds all the JAR files in a given directory and all its subdirectories
   * recursively.
   * 
   * @param baseDir
   *          a File representing the base directory of the plugin.
   * @return a Set of Strings, each of which represents the name of a JAR file
   *         in the given directory.
   */
  public static Set<JarFile> findJars(File baseDir) throws IllegalArgumentException {
    FifoQueueNoDuplicates<File> workQueue = new FifoQueueNoDuplicates<File>();
    Set<JarFile> jars = HashSetFactory.make();
    workQueue.push(baseDir);
    while (!workQueue.isEmpty()) {
      File dir = (File) workQueue.pop();
      File[] contents = dir.listFiles();
      if (contents == null) {
        throw new IllegalArgumentException("bad file " + dir.getAbsolutePath());
      }
      for (int i = 0; i < contents.length; i++) {
        if (contents[i].isDirectory()) {
          workQueue.push(contents[i]);
        } else if (contents[i].getName().endsWith(".jar")) {
          try {
            jars.add(new JarFile(contents[i]));
          } catch (IOException ioe) {
            System.err.println("EclipseAnalysisScopeBuilder: File " + contents[i].getName() + " is not a valid JAR file.");
          }
        }
      }
    }
    return jars;
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

  /**
   * Finds all the bundles required by a given bundle or fragment as specified
   * in its MANIFEST.MF file.
   * 
   * @param manifestFileName
   *          a String representing the fully qualified name of a manifest file.
   * @return a TreeSet of Strings each of which represents the fully qualified
   *         name of a required plugin's directory. The returned TreeSet is
   *         <code>null</code> if it was not possible to have access to the
   *         XML file.
   */
  private Set<File> findRequiredBundles(Manifest mf, String pluginDirsName, File pluginDirectory) {
    Set<File> requiredBundles = HashSetFactory.make();
    Attributes a = mf.getMainAttributes();
    String pluginId = a.getValue(Constants.BUNDLE_SYMBOLICNAME);
    if (pluginId != null) {
      // save off the plugin IDs, so that contributing fragments can be
      // found.
      pluginId = parseFragmentHost(pluginId);
      pluginIds.add(pluginId);
    }
    String requireBundle = a.getValue(Constants.REQUIRE_BUNDLE);
    if (requireBundle != null) {
      String splittingFactor = ",";
      String requiredArray[] = requireBundle.split(splittingFactor);
      if (requiredArray != null) {
        for (int i = 0; i < requiredArray.length; i++) {
          String requiredBundleName = (requiredArray[i].trim()).split(";")[0];
          if ((version > 3 || version == 3 && subversion >= 2) && Character.isDigit(requiredBundleName.charAt(0)))
            continue;
          File requiredBundle = findPluginDirOrJAR(requiredBundleName, pluginDirsName,pluginDirectory);
          if (requiredBundle == null) {
            System.err.println("EclipseAnalysisScopeBuilder: " + requiredBundleName + " was not found.");
            continue;
          }
          requiredBundles.add(requiredBundle);
        }
      }
    }
    // If FRAGMENT_HOST is set, then this is a fragment, not a plugin.
    String fragmentHost = a.getValue(Constants.FRAGMENT_HOST);
    if (fragmentHost != null) {
      String hostPluginName = parseFragmentHost(fragmentHost);
      if (hostPluginName != null) {
        // If this is a fragment, add the plugin that hosts the fragment to the
        // list.
        File hostPlugIn = findPluginDirOrJAR(hostPluginName, pluginDirsName, pluginDirectory);
        if (hostPlugIn == null) {
          System.err.println("EclipseAnalysisScopeBuilder: " + hostPluginName + " was not found.");
        } else {
          requiredBundles.add(hostPlugIn);
        }
      }
    }
    return requiredBundles;
  }

  /**
   * findManifests - Find all MANIFEST.MF files under specified base directory.
   * 
   * @param baseDirName
   * @return Set - A set of full paths to manifest files.
   */
  private static Set<String> findManifests(String baseDirName) {
    FifoQueueNoDuplicates<File> workQueue = new FifoQueueNoDuplicates<File>();
    Set<String> manifests = HashSetFactory.make();
    workQueue.push(new File(baseDirName));
    while (!workQueue.isEmpty()) {
      File dir = (File) workQueue.pop();
      String dirName = dir.getAbsolutePath().replace('\\', '/');
      String[] contents = dir.list();
      for (int i = 0; i < contents.length; i++) {
        String fullName = dirName + "/" + contents[i];
        if (contents[i].equalsIgnoreCase("MANIFEST.MF")) {
          manifests.add(fullName);
          continue;
        }
        File file = new File(fullName);
        if (file.isDirectory())
          workQueue.push(file);
      }
    }
    return manifests;
  }

  /**
   * getContributingFragments - Using the pluginIds Set find all fragments which
   * contribute to the list of loaded plugins, and add their associated jars to
   * the analysisScopeJars Set.
   */
  private Collection<JarFile> getContributingFragments(File pluginDirectory) {
    Collection<JarFile> result = HashSetFactory.make();
    Set manifests = findManifests(pluginsDirName);
    Iterator manifestsIter = manifests.iterator();
    while (manifestsIter.hasNext()) {
      String manifestName = (String) manifestsIter.next();
      Iterator pluginIdsIter = pluginIds.iterator();
      while (pluginIdsIter.hasNext()) {
        String fragmentHost = (String) pluginIdsIter.next();
        result.addAll(getContributingFragment(fragmentHost, manifestName, pluginsDirName, pluginDirectory));
      }
    }
    return result;
  }

  /**
   * addContributingFragment - Determine if the supplied manifest.mf file
   * contributes to the specified fragmentHost. If so, add the associated jar,
   * and any required jars to the analysisScopeJars Set.
   * 
   * @param fragmentHost -
   *          The plugin id value to search for in fragment manifests.
   * @param manifestFileName -
   *          The name of the current manifest file being scanned.
   * 
   */
  private Collection<JarFile> getContributingFragment(String fragmentHost, String manifestFileName, String pluginsDirName, File pluginDirectory) {
    Collection<JarFile> result = HashSetFactory.make();
    InputStream is = getInputStream(manifestFileName);
    if (is == null) {
      System.err.println("EclipseAnalysisScopeBuilder: " + manifestFileName + " was not found.");
    } else {
      try {
        Manifest m = new Manifest(is);
        Attributes a = m.getMainAttributes();
        // If FRAGMENT_HOST is set, then this is a fragment, not a plugin.
        String hostPlugin = a.getValue(Constants.FRAGMENT_HOST);

        if (hostPlugin != null) {
          hostPlugin = parseFragmentHost(hostPlugin);
          if (hostPlugin.equalsIgnoreCase(fragmentHost)) {
            String requireBundle = a.getValue(Constants.REQUIRE_BUNDLE);
            if (requireBundle != null) {
              String requiredArray[] = requireBundle.split(",");
              if (requiredArray != null) {
                for (int i = 0; i < requiredArray.length; i++) {
                  String pluginName = (requiredArray[i].trim()).split(";")[0];
                  File plugIn = findPluginDirOrJAR(pluginName, pluginsDirName, pluginDirectory);
                  if (plugIn == null) {
                    System.out.println("EclipseAnalysisScopeBuilder: " + pluginName + " was not found.");
                    continue;
                  }
                  Set<JarFile> pluginJars = findJars(plugIn);
                  if (pluginJars == null || pluginJars.isEmpty()) {
                    System.out.println("EclipseAnalysisScopeBuilder: Informational: no jars found in plugin root folder.");
                  } else
                    result.addAll(pluginJars);
                }
              }
            }

            // Add all jars for this plugin to the analysis scope since it
            // contributes as a fragment.
            String fragmentSymbolicName = a.getValue(Constants.BUNDLE_SYMBOLICNAME);
            fragmentSymbolicName = parseFragmentHost(fragmentSymbolicName);
            if (fragmentSymbolicName != null) {
              File plugIn = findPluginDirOrJAR(fragmentSymbolicName, pluginsDirName, pluginDirectory);
              if (plugIn == null) {
                System.out.println("EclipseAnalysisScopeBuilder: " + hostPlugin + " was not found.");
              } else {
                Set<JarFile> pluginJars = findJars(plugIn);
                result.addAll(pluginJars);
              }
            }
          }
        }
      } catch (Exception e) {
        System.out.println("EclipseAnalysisScopeBuilder: " + e.getLocalizedMessage());
      }
    }
    return result;
  }

  /**
   * Return an InputStream to the <code>plugin.xml</code> file describing the
   * plugin configuration. The InputStream returned is <code>null</code> if
   * the XML file does not exist or cannot be opened.
   * 
   * @param xmlFileName
   *          a String representing the name of the <code>plugin.xml</code>
   *          file describing the plugin configuration.
   * @return an InputStream to the <code>plugin.xml</code> file.
   */
  public static InputStream getInputStream(String xmlFileName) {
    InputStream is = null;
    try {
      is = new FileInputStream(xmlFileName);
    } catch (IOException e) {
      System.out.println("EclipseAnalysisScopeBuilder: Unable to access file " + xmlFileName + ": " + e);
    }
    return is;
  }

  public void addClassFileToScope(ClassLoaderReference loader, File file) {
    delegate.addClassFileToScope(loader, file);
  }

  public void addSourceFileToScope(ClassLoaderReference loader, File file, String fileName) {
    delegate.addSourceFileToScope(loader, file, fileName);
  }

  public void addToScope(ClassLoaderReference loader, JarFile file) {
    delegate.addToScope(loader, file);
  }

  public void addToScope(ClassLoaderReference loader, Module m) {
    delegate.addToScope(loader, m);
  }

  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  public MethodReference findMethod(Atom loader, String klass, Atom name, ImmutableByteArray desc) {
    return delegate.findMethod(loader, klass, name, desc);
  }

  public ClassLoaderReference getApplicationLoader() {
    return delegate.getApplicationLoader();
  }

  public ArrayClassLoader getArrayClassLoader() {
    return delegate.getArrayClassLoader();
  }

  public SetOfClasses getExclusions() {
    return delegate.getExclusions();
  }

  public ClassLoaderReference getExtensionLoader() {
    return delegate.getExtensionLoader();
  }

  public String getJavaLibraryVersion() {
    return delegate.getJavaLibraryVersion();
  }

  public ClassLoaderReference getLoader(Atom name) {
    return delegate.getLoader(name);
  }

  public String getLoaderImpl(ClassLoaderReference ref) {
    return delegate.getLoaderImpl(ref);
  }

  public Collection<ClassLoaderReference> getLoaders() {
    return delegate.getLoaders();
  }

  public Set<Module> getModules(ClassLoaderReference loader) {
    return delegate.getModules(loader);
  }

  public int getNumberOfLoaders() {
    return delegate.getNumberOfLoaders();
  }

  public ClassLoaderReference getPrimordialLoader() {
    return delegate.getPrimordialLoader();
  }

  public ClassLoaderReference getSyntheticLoader() {
    return delegate.getSyntheticLoader();
  }

  public int hashCode() {
    return delegate.hashCode();
  }

  public boolean isJava14Libraries() {
    return delegate.isJava14Libraries();
  }

  public boolean isJava15Libraries() {
    return delegate.isJava15Libraries();
  }

  public void setExclusions(SetOfClasses classes) {
    delegate.setExclusions(classes);
  }

  public String toString() {
    return delegate.toString();
  }

  public String getPluginName() {
    return pluginName;
  }

  public String getPluginsDirName() {
    return pluginsDirName;
  }
}
