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
package com.ibm.wala.examples.drivers;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.eclipse.emf.ecore.EObject;

import com.ibm.wala.ecore.java.ETypeHierarchy;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFBridge;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This is a simple example WALA application. It's neither efficient
 * nor concise, but is intended to demonstrate some basic framework concepts.
 * 
 * This application builds a type hierarchy and dumps it to an XML file. 
 * 
 * @author sfink
 */
public class ExportTypeHierarchyToXML {

  // This example takes one command-line argument, so args[1] should be the
  // "-classpath"
  // parameter
  final static int CLASSPATH_INDEX = 1;

  /**
   * The name of the file to which the TypeHierarchyWriter will dump its output.
   */
  private final static String FILENAME = "th.xml";

  /**
   * Usage: ExportTypeHierarchyToXML -classpath [classpath]
   * 
   * @param args
   */
  public static void main(String[] args) {
    // check that the command-line is kosher
    validateCommandLine(args);

    try {
      String classpath = args[CLASSPATH_INDEX];
      
      System.err.println("build type hierarchy...");
      WarningSet warnings = new WarningSet();
      ETypeHierarchy th = buildTypeHierarchy(classpath, warnings);
      if (th.getClasses().getNodes().getContents().size() <1) {
        System.err.println("PANIC: type hierarchy # classes=" + th.getClasses().getNodes().getContents().size());
        System.err.println(warnings.toString());
        System.exit(-1);
      }
     
      String file = getFileName();
      System.err.println("write to file " + file);
      write(th,file);
      
      Graph<EObject> g = GVTypeHierarchy.typeHierarchy2Graph(th);
      g = GVTypeHierarchy.pruneForAppLoader(g);
      if (g.getNumberOfNodes() == 0) {
        System.err.println("ERROR: The type hierarchy in " + ExportTypeHierarchyToXML.getFileName() + " has no nodes from the Application loader");
        System.err.println("Probably something's wrong with the input jars being analyzed.");
        System.err.println("check the files in the path: " + classpath);
        System.err.println("Also look at these warning messages:");
        System.err.println(warnings.toString());
        System.exit(-1);
      }

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static void write(ETypeHierarchy t, String filename) throws WalaException {

    if (filename == null) {
      throw new WalaException("internal error: null filename parameter");
    }
    
    // Create a collection of EObjects to persist
    Collection<EObject> persist = new LinkedList<EObject>();
    // Persist the type hierarchy itself
    persist.add(t);
    // Persist the classes as well, which are NOT contained in the ETypeHierarchy
    persist.add(t.getClasses().getNodes());
    // Persist the interfaces as well, which are NOT contained in the ETypeHierarchy
    persist.add(t.getInterfaces().getNodes());
    
    // Save everything to a file.
    EUtil.saveToFile(persist, filename);
  }

  static String getFileName() throws WalaException {
    Properties p = null;;
    try {
      p = WalaProperties.loadProperties();
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String file = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + FILENAME;
    return file;
  }

  public static ETypeHierarchy buildTypeHierarchy(String classpath, WarningSet warnings) throws WalaException {
    EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(classpath);
    
    // generate a WALA-consumable wrapper around the incoming scope object
    EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);
    
    // invoke WALA to build a class hierarchy
    ClassHierarchy cha = ClassHierarchy.make(scope, warnings);
    
    // Export the class hierarchy object to an EMF TypeHierarchy object
    com.ibm.wala.emf.wrappers.ETypeHierarchyWrapper t1 = EMFBridge.makeTypeHierarchy(cha);

    ETypeHierarchy th = (ETypeHierarchy) t1.toEMF();
    return th;
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage: args[0] : "-classpath" args[1] : String, a ";"-delimited class path
   * 
   * @param args
   * @throws UnsupportedOperationException
   *           if command-line is malformed.
   */
  static void validateCommandLine(String[] args) {
    if (args.length < 2) {
      throw new UnsupportedOperationException("must have at least 2 command-line arguments");
    }
    if (!args[0].equals("-classpath")) {
      throw new UnsupportedOperationException("invalid command-line, args[0] should be -classpath, but is " + args[0]);
    }
  }
}