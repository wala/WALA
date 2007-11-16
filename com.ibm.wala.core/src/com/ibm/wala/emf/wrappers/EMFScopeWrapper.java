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
package com.ibm.wala.emf.wrappers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource.XMLMap;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopePackageImpl;
import com.ibm.wala.ecore.java.scope.EBuiltInModule;
import com.ibm.wala.ecore.java.scope.EBuiltInResource;
import com.ibm.wala.ecore.java.scope.EClassFile;
import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.EModule;
import com.ibm.wala.ecore.java.scope.ESourceFile;
import com.ibm.wala.ecore.java.scope.EStandardClassLoader;
import com.ibm.wala.ecore.java.scope.JavaScopeFactory;
import com.ibm.wala.ecore.java.scope.impl.JavaScopePackageImpl;
import com.ibm.wala.ecore.java.scope.util.JavaScopeSwitch;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.config.FileProvider;
import com.ibm.wala.util.config.XMLSetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * Goal of this class is to generate a scope-wrapper around a given Jar or Class
 * file. The generated scope-wrapper currently provides the an additional
 * Primordial class loader with the primoirdial_jar_model built-in module. In
 * the future, additional information may be included in the generated
 * scope-wrapper.
 * 
 * The generator emulates internal structure created by scope files such as the
 * following example:
 * 
 * <verbatim> <loaders loaderName="Primordial"> <modules
 * xsi:type="com.ibm.wala.java.scope:BuiltInModule"/> <modules
 * xsi:type="com.ibm.wala.java.scope:BuiltInModule" id="primordial_jar_model"/>
 * </loaders> <loaders loaderName="Application"> <modules
 * url="$TESTDATA/Example.jar" xsi:type="com.ibm.wala.java.scope:JarFile"/>
 * </loaders> </verbatim>
 * 
 * @author Eran Yahav (yahave)
 * @author Stephen Fink
 * 
 */
@SuppressWarnings("unchecked")
public class EMFScopeWrapper extends AnalysisScope {

  private final static int DEBUG_LEVEL = 0;

  private final String exclusionsFile;

  private final String scopeFile;

  private final ClassLoader classloader;

  private static final Object SUCCESS = new Object();

  static {
    JavaScopePackageImpl.init();
    J2EEScopePackageImpl.init();
    Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
  }

  /**
   * Constructor enabling automated construction of the ScopeWrapper (with no
   * files provided)
   */
  protected EMFScopeWrapper(final ClassLoader loader) {
    this.classloader = loader;
    this.scopeFile = null;
    this.exclusionsFile = null;
  }

  public EMFScopeWrapper(String scopeFile, String exclusionsFile, ClassLoader loader, boolean scopeAsFile) {
    super();
    this.scopeFile = scopeFile;
    this.classloader = loader;

    if (DEBUG_LEVEL > 0) {
      Trace.println(getClass() + " ctor " + scopeFile);
    }
    this.exclusionsFile = exclusionsFile;
    EJavaAnalysisScope escope = null;
    try {
      if (scopeAsFile) {
        escope = readScopeAsFile();
      } else {
        escope = readScope();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    populate(escope);
    addDefaultBypassLoader();

    if (exclusionsFile != null) {
      setExclusions(new XMLSetOfClasses(exclusionsFile, loader));
    }
  }

  public EMFScopeWrapper(String scopeFile, String exclusionsFile, ClassLoader loader) {
    this(scopeFile, exclusionsFile, loader, false);
  }

  /**
   * add the default logic for synthetic bypass.
   */
  protected void addDefaultBypassLoader() {
    ClassLoaderReference synthLoader = classLoaderName2Ref(EStandardClassLoader.SYNTHETIC_LITERAL.getName());
    setLoaderImpl(synthLoader, com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader.class.getName());
  }

  /**
   * Set up internal data structures
   * 
   * @param escope
   */
  protected void populate(EJavaAnalysisScope escope) {
    for (Iterator<EClassLoader> it = escope.getLoaders().iterator(); it.hasNext();) {
      EClassLoader loader = it.next();
      ClassLoaderReference loaderRef = classLoaderName2Ref(loader.getLoaderName());
      for (Iterator<EModule> it2 = loader.getModules().iterator(); it2.hasNext();) {
        EModule m = it2.next();
        if (DEBUG_LEVEL > 0) {
          Trace.println("populate: " + m);
        }
        processModule(m, loaderRef);
      }
    }
  }


  private void processModule(EModule m, final ClassLoaderReference loader) {
    JavaScopeSwitch sw = new JavaScopeSwitch() {

      @Override
      public Object defaultCase(EObject object) {
        Assertions.UNREACHABLE(object.getClass().toString());
        return null;
      }

      @Override
      public Object caseEBuiltInModule(EBuiltInModule object) {
        processBuiltInModule(object, loader);
        return SUCCESS;
      }

      @Override
      public Object caseEJarFile(EJarFile object) {
        processJarFile(object, loader);
        return SUCCESS;
      }

      @Override
      public Object caseEClassFile(EClassFile object) {
        try {
          processClassFile(object, loader);
        } catch (IOException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE(e.toString());
        }
        return SUCCESS;
      }

      @Override
      public Object caseESourceFile(ESourceFile object) {
        try {
          processSourceFile(object, loader);
        } catch (IOException e) {
          e.printStackTrace();
          Assertions.UNREACHABLE(e.toString());
        }
        return SUCCESS;
      }

    };
    sw.doSwitch(m);
  }

  /**
   * @param m
   * @param loader
   */
  private void processSourceFile(ESourceFile m, ClassLoaderReference loader) throws IOException {
    String fileName = m.getUrl();
    Assertions.productionAssertion(fileName != null, "null file name specified");
    File file = FileProvider.getFile(fileName);
    addSourceFileToScope(loader, file, fileName);
  }

  /**
   * @param m
   * @param loader
   * @throws IOException
   */
  private void processClassFile(EClassFile m, ClassLoaderReference loader) throws IOException {
    String fileName = m.getUrl();
    Assertions.productionAssertion(fileName != null, "null file name specified");
    File file = FileProvider.getFile(fileName);
    addClassFileToScope(loader, file);
  }

  /**
   * @param m
   * @param loader
   */
  private void processJarFile(EJarFile m, ClassLoaderReference loader) throws IllegalArgumentException {
    String fileName = m.getUrl();
    Assertions.productionAssertion(fileName != null, "null jar file name specified");
    Module mod = null;
    try {
      mod = FileProvider.getJarFileModule(fileName);
      if (mod == null) {
        throw new IllegalArgumentException("failed to find jar file module " + fileName);
      }
    } catch (ZipException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("Error opening jar file: " + fileName);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("Error opening jar file: " + fileName);
    }
    addToScope(loader, mod);
  }

  /**
   * @param m
   */
  private void processBuiltInModule(EBuiltInModule m, ClassLoaderReference loader) {
    EBuiltInResource r = m.getId();
    switch (r.getValue()) {
    case EBuiltInResource.DEFAULT_J2SE_LIBS:
      String[] libNames = WalaProperties.getJ2SEJarFiles();
      if (DEBUG_LEVEL > 0) {
        Trace.println("DEFAULT_J2SE_LIBS:");
        for (int i = 0; i < libNames.length; i++) {
          Trace.println("  " + libNames[i]);
        }
      }
      addLibsToLoader(libNames, loader);
      break;
    case EBuiltInResource.DEFAULT_J2EE_LIBS:
      String[] jlibNames = WalaProperties.getJ2EEJarFiles();
      if (DEBUG_LEVEL > 0) {
        Trace.println("DEFAULT_J2EE_LIBS:");
        for (int i = 0; i < jlibNames.length; i++) {
          Trace.println("  " + jlibNames[i]);
        }
      }
      addLibsToLoader(jlibNames, loader);
      break;
    case EBuiltInResource.PRIMORDIAL_JAR_MODEL:
      addDefaultJarFileToScope(r.getName().replace('_', '.'), loader);
      if (DEBUG_LEVEL > 0) {
        Trace.println("PRIMORDIAL_JAR_MODEL: " + r.getName());
      }
      break;
    case EBuiltInResource.EXTENSION_JAR_MODEL:
      addDefaultJarFileToScope(r.getName().replace('_', '.'), loader);
      if (DEBUG_LEVEL > 0) {
        Trace.println("EXTENSION_JAR_MODEL: " + r.getName());
      }
      break;
    default:
      Assertions.UNREACHABLE();
      break;
    }
  }

  /*
   * This method loads the default jar file of this name from the
   * JarFileProvider
   * 
   * @param ch @param start @param length
   */
  private void addDefaultJarFileToScope(String fileName, ClassLoaderReference loader) {
    Module m = null;
    Assertions.productionAssertion(fileName.length() > 0, "null fileName");
    if (DEBUG_LEVEL > 0) {
      Trace.println("Load default jar file: " + fileName);
    }
    try {
      m = FileProvider.getJarFileModule(fileName);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.productionAssertion(m != null, "could not open " + fileName);
    }
    Assertions.productionAssertion(m != null, "could not open " + fileName);
    if (DEBUG_LEVEL > 0) {
      Trace.println("Add to scope: " + m);
    }
    addToScope(loader, m);
  }

  /**
   * @param libNames
   * @param loader
   */
  private void addLibsToLoader(String[] libNames, ClassLoaderReference loader) {
    for (int i = 0; i < libNames.length; i++) {
      Module m = null;
      String fileName = libNames[i];
      Assertions.productionAssertion(fileName.length() > 0, "null fileName");
      try {
        m = FileProvider.getJarFileModule(fileName);
      } catch (IOException e) {
        e.printStackTrace();
        Assertions.productionAssertion(m != null, "could not open " + fileName);
      }
      Assertions.productionAssertion(m != null, "could not open " + fileName);
      if (DEBUG_LEVEL > 0) {
        Trace.println("EMFScopeWrapper add file to loader " + fileName);
      }
      addToScope(loader, m);
    }
  }

  /**
   * @return an EMF scope object
   */
  private EJavaAnalysisScope readScope() {
    InputStream s = classloader.getResourceAsStream(scopeFile);
    if (s == null) {
      Assertions.UNREACHABLE("failed to open scope file: " + scopeFile);
    }
    ResourceSet resSet = new ResourceSetImpl();
    Resource r = resSet.createResource(URI.createURI(getClass() + "junk"));
    Assertions.productionAssertion(r != null);
    Map<String, XMLMap> options = HashMapFactory.make();
    try {
      r.load(s, options);
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("unable to parse " + scopeFile);
    }
    return (EJavaAnalysisScope) r.getContents().get(0);
  }

  /**
   * @return an EMF scope object
   */
  private EJavaAnalysisScope readScopeAsFile() {
    EJavaAnalysisScope result = null;
    try {
      File theScopeFile = new File(scopeFile);
      InputStream s = new FileInputStream(theScopeFile);

      ResourceSet resSet = new ResourceSetImpl();
      Resource r = resSet.createResource(URI.createURI(getClass().toString())); // +
      // "junk2"));
      Assertions.productionAssertion(r != null);
      Map<String, XMLMap> options = HashMapFactory.make();
      try {
        r.load(s, options);
      } catch (IOException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE("unable to parse " + scopeFile);
      }
      result = (EJavaAnalysisScope) r.getContents().get(0);
    } catch (IOException e) {
      Assertions.UNREACHABLE("scopeAsFile: failed to open scope file: " + scopeFile);
    }
    return result;
  }

  @Override
  protected Object getExclusionString() {
    return "Exclusions defined in file " + exclusionsFile;
  }

  /**
   * generate a EMFScopeWrapper for a given jar file
   * 
   * @param cl -
   *          application class loader
   * @param jarUrl -
   *          URL to the Jar file to be wrapped
   * @param exclusionsFile -
   *          exclusions file
   * @return a new EMFScopeWrapper wrapping the jar file
   * @throws WalaException
   */
  public static EMFScopeWrapper generateJarScope(ClassLoader cl, String jarUrl, String exclusionsFile) throws WalaException {
    EClassLoader primordial = JavaScopeUtil.createPrimordialLoader();
    EClassLoader application = JavaScopeUtil.createJarApplicationLoader(jarUrl);

    Collection<EClassLoader> loaders = new ArrayList<EClassLoader>();
    loaders.add(primordial);
    loaders.add(application);

    try {
      return generateScope(cl, loaders, exclusionsFile);
    } catch (Throwable e) {
      throw new WalaException("problem generating scope", e);
    }
  }

  /**
   * generate a EMFScopeWrapper for a given class file
   * 
   * @param cl -
   *          application class loader
   * @param classUrl -
   *          URL to the class file to be wrapped
   * @param exclusionsFile -
   *          exclusions file
   * @return a new EMFScopeWrapper wrapping the class file
   * @throws WalaException
   */
  public static EMFScopeWrapper generateClassScope(ClassLoader cl, String classUrl, String exclusionsFile) throws WalaException {
    EClassLoader primordial = JavaScopeUtil.createPrimordialLoader();
    EClassLoader application = JavaScopeUtil.createClassApplicationLoader(classUrl);

    Collection<EClassLoader> loaders = new ArrayList<EClassLoader>();
    loaders.add(primordial);
    loaders.add(application);

    try {
      return generateScope(cl, loaders, exclusionsFile);
    } catch (Throwable e) {
      throw new WalaException("problem generating scope", e);
    }
  }

  /**
   * generates a EMFScopeWrapper from a collection of loaders
   * 
   * @param cl -
   *          application class loader
   * @param loaders -
   *          collection of class loaders
   * @param exclusionsFile -
   *          exclusion file
   * @return a new EMFScopeWrapper comprising of the given loaders, and taking
   *         the provided exclusion file into account
   */
  static EMFScopeWrapper generateScope(ClassLoader cl, Collection<EClassLoader> loaders, String exclusionsFile)
      throws WalaException {
    EMFScopeWrapper csw = new EMFScopeWrapper(cl);

    EJavaAnalysisScope jas = JavaScopeFactory.eINSTANCE.createEJavaAnalysisScope();
    jas.getLoaders().clear();
    jas.getLoaders().addAll(loaders);

    csw.populate(jas);

    csw.addDefaultBypassLoader();

    if (exclusionsFile != null) {
      csw.setExclusions(new XMLSetOfClasses(exclusionsFile, cl));
    }

    return csw;
  }

  /**
   * generates a EMFScopeWrapper from am EJavaAnalysisScope
   * 
   * @param escope
   * @return a new EMFScopeWrapper comprising the given scope
   * @throws WalaException
   * @throws IllegalArgumentException
   *           if escope is null
   */
  public static EMFScopeWrapper generateScope(EJavaAnalysisScope escope) throws WalaException {
    if (escope == null) {
      throw new IllegalArgumentException("escope is null");
    }
    return generateScope(escope, new EMFScopeWrapper(EMFScopeWrapper.class.getClassLoader()));
  }

  private static EMFScopeWrapper generateScope(final EJavaAnalysisScope escope, final EMFScopeWrapper csw) {
    csw.populate(escope);
    csw.addDefaultBypassLoader();
    String exclusionsFile = escope.getExclusionFileName();
    if (exclusionsFile != null) {
      csw.setExclusions(new XMLSetOfClasses(exclusionsFile, EMFScopeWrapper.class.getClassLoader()));
    }

    return csw;
  }
}
