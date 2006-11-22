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

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import com.ibm.wala.ecore.java.scope.EBuiltInModule;
import com.ibm.wala.ecore.java.scope.EBuiltInResource;
import com.ibm.wala.ecore.java.scope.EClassFile;
import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EJarFile;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.ecore.java.scope.JavaScopeFactory;
import com.ibm.wala.util.io.FileSuffixes;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * Utilities to help manipulate EMF objects representing Java analysis scopes.
 * 
 * 
 * @author Eran Yahav (yahave)
 */
public class JavaScopeUtil {
  // codes used to classify classpath entries
  private final static byte JAR_FILE = 0;

  /**
   * name of the Primordial class loader
   */
  private static final String PRIMORDIAL_LOADER_NAME = "Primordial";

  /**
   * name of the application class loader
   */
  private static final String APPLICATION_LOADER_NAME = "Application";

  /**
   * name of the jar file providing the primordial model
   */
  private static final String PRIMORDIAL_JAR_FILE = "primordial_jar_model";

  /**
   * Creates a new EClassLoader representing the application class loader
   * 
   * @return a new EClassLoader
   */
  public static EClassLoader createApplicationLoader() {
    EClassLoader application = JavaScopeFactory.eINSTANCE.createEClassLoader();
    application.setLoaderName(APPLICATION_LOADER_NAME);
    return application;
  }

  /**
   * Creates a new EClassLoader representing the application class loader, and
   * containing the given Jar file as a module.
   * 
   * @param jarUrl -
   *          URL to the jar file to be included as module\
   * @return a new ClassLoader containing the provided Jar file as a module
   */
  @SuppressWarnings("unchecked")
  public static EClassLoader createJarApplicationLoader(String jarUrl) {
    EClassLoader application = createApplicationLoader();
    EJarFile jf = createJarModule(jarUrl);
    application.getModules().clear();
    application.getModules().add(jf);
    return application;
  }

  /**
   * Creates a new EClassLoader representing the application class loader, and
   * containing the given class file as a module.
   * 
   * @param classUrl -
   *          URL to the class file to be included as module
   * @return a new ClassLoader containing the provided class file as a module
   */
  @SuppressWarnings("unchecked")
  public static EClassLoader createClassApplicationLoader(String classUrl) {
    EClassLoader application = JavaScopeFactory.eINSTANCE.createEClassLoader();
    application.setLoaderName(APPLICATION_LOADER_NAME);
    EClassFile cf = createClassModule(classUrl);
    application.getModules().clear();
    application.getModules().add(cf);
    return application;
  }

  /**
   * Creates a new Primordial class loader
   * 
   * @return a new EClassLoader representing the Primordial class loader, and
   *         containing the primordial_jar_model as a module.
   */
  @SuppressWarnings("unchecked")
  public static EClassLoader createPrimordialLoader() {
    EClassLoader primordialLoader = JavaScopeFactory.eINSTANCE.createEClassLoader();
    primordialLoader.setLoaderName(PRIMORDIAL_LOADER_NAME);
    EBuiltInModule bim1 = JavaScopeFactory.eINSTANCE.createEBuiltInModule();
    EBuiltInModule bim2 = JavaScopeFactory.eINSTANCE.createEBuiltInModule();
    EBuiltInResource bir = EBuiltInResource.get(PRIMORDIAL_JAR_FILE);
    bim2.setId(bir);
    Collection<EBuiltInModule> modules = new ArrayList<EBuiltInModule>();
    modules.add(bim1);
    modules.add(bim2);

    primordialLoader.getModules().clear();
    primordialLoader.getModules().addAll(modules);
    return primordialLoader;
  }

  /**
   * create a JarFile from a given url
   * 
   * @param urlString -
   *          url to the jar file
   * @return a JarFile from the url
   */
  public static EJarFile createJarModule(String urlString) {
    EJarFile aJarFile = JavaScopeFactory.eINSTANCE.createEJarFile();
    aJarFile.setUrl(urlString);
    return aJarFile;
  }

  /**
   * create a ClassFile from a given url
   * 
   * @param urlString -
   *          url to the class file
   * @return a ClassFile from the url
   */
  public static EClassFile createClassModule(String urlString) {
    EClassFile aClassFile = JavaScopeFactory.eINSTANCE.createEClassFile();
    aClassFile.setUrl(urlString);
    return aClassFile;
  }

  public static EJavaAnalysisScope makeAnalysisScope(String classpath) throws WalaException {

    // create an EJavaAnalysisScope and deposit it into the output PAS
    EJavaAnalysisScope scope = JavaScopeFactory.eINSTANCE.createEJavaAnalysisScope();

    // populate the analysis scope according to the contents of the classpath
    populateScope(scope, classpath);

    return scope;
  }

  public static EJavaAnalysisScope makePrimordialScope() throws WalaException {
    return makeAnalysisScope("");
  }

  /**
   * Populate an analysis scope object according to the contents of a String
   * which represents the classpath
   * 
   * @param scope
   *          a scope object to populate
   * @param string
   *          a ';'-delimited classpath string
   * @throws WalaException
   *           if there's a problem
   */
  @SuppressWarnings("unchecked")
  private static void populateScope(EJavaAnalysisScope scope, String string) throws WalaException {
    // This component assumes that the analysis scope describes the application
    // loader. Create an object to represent the application loader.
    EClassLoader appLoader = createApplicationLoader();
    scope.getLoaders().add(appLoader);

    // Parse the String which represents the classpath
    StringTokenizer tokenizer = new StringTokenizer(string, ";");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken();
      byte kind = classifyToken(token);
      switch (kind) {
      case JAR_FILE:
        // Add a jar-file module to the analysis scope
        EJarFile j = createJarModule(token);
        appLoader.getModules().add(j);
        break;
      default:
        // TODO: implement more cases
        throw new WalaException("unexpected kind: " + kind);
      }
    }

    // add the primordial class loader to the analysis scope
    scope.getLoaders().add(createPrimordialLoader());
  }

  /**
   * @param token
   *          an entry in a classpath
   * @return JAR_FILE if it's a jar file name
   * @throws WalaException
   *           if we cannot deduce the kind of the token
   */
  public static byte classifyToken(String token) throws WalaException {
    if (FileSuffixes.isJarFile(token)) {
      return JAR_FILE;
    } else {
      throw new WalaException("could not parse classpath token: " + token);
    }
  }

}