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
import java.util.Properties;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ecore.java.scope.EJavaAnalysisScope;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.emf.wrappers.JavaScopeUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;
import com.ibm.wala.viz.GhostviewUtil;

/**
 * 
 * This simple example application builds a WALA IR and fires off ghostview to
 * visualize a DOT representation.
 * 
 * @author sfink
 */
public class GVWalaIR {

  final public static String PS_FILE = "ir.ps";

  /**
   * Usage: GVWalaIR -appJar [jar file name] -sig [method signature] The "jar
   * file name" should be something like "c:/temp/testdata/java_cup.jar" The
   * signature should be something like "java_cup.lexer.advance()V"
   * 
   * @param args
   */
  public static void main(String[] args) {
    run(args);
  }

  /**
   * @param args
   *            -appJar [jar file name] -sig [method signature] The "jar file
   *            name" should be something like "c:/temp/testdata/java_cup.jar"
   *            The signature should be something like
   *            "java_cup.lexer.advance()V"
   */
  public static Process run(String[] args) {
    validateCommandLine(args);
    return run(args[1], args[3]);
  }

  /**
   * @param appJar
   *            should be something like "c:/temp/testdata/java_cup.jar"
   * @param methodSig
   *            should be something like "java_cup.lexer.advance()V"
   */
  public static Process run(String appJar, String methodSig) {
    try {
      if (SWTCallGraph.isDirectory(appJar)) {
        appJar = SWTCallGraph.findJarFiles(new String[] { appJar });
      }
      EJavaAnalysisScope escope = JavaScopeUtil.makeAnalysisScope(appJar);

      // generate a DOMO-consumable wrapper around the incoming scope object
      EMFScopeWrapper scope = EMFScopeWrapper.generateScope(escope);

      // invoke DOMO to build a DOMO class hierarchy object
      WarningSet warnings = new WarningSet();
      ClassHierarchy cha = ClassHierarchy.make(scope, warnings);

      MethodReference mr = StringStuff.makeMethodReference(methodSig);

      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        Assertions.UNREACHABLE("could not resolve " + mr);
      }
      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setUsePiNodes(true);
      IR ir = options.getSSACache().findOrCreateIR(m, Everywhere.EVERYWHERE, options.getSSAOptions(), new WarningSet());

      if (ir == null) {
        Assertions.UNREACHABLE("Null IR for " + m);
      }

      System.err.println(ir.toString());

      Properties wp = null;
      try {
        wp = WalaProperties.loadProperties();
        wp.putAll(WalaExamplesProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + GVWalaIR.PS_FILE;
      String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + GVTypeHierarchy.DOT_FILE;
      String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = wp.getProperty(WalaExamplesProperties.GHOSTVIEW_EXE);

      return GhostviewUtil.ghostviewIR(cha, ir, psFile, dotFile, dotExe, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   * 
   * Usage:
   * <ul>
   * <li> args[0] : "-appJar"
   * <li> args[1] : something like "c:/temp/testdata/java_cup.jar"
   * <li> args[2] : "-sig"
   * <li> args[3] : a method signature like "java_cup.lexer.advance()V" </ul?
   * 
   * @param args
   * @throws UnsupportedOperationException
   *             if command-line is malformed.
   */
  static void validateCommandLine(String[] args) {
    if (args.length != 4) {
      throw new UnsupportedOperationException("must have at exactly 4 command-line arguments");
    }
    if (!args[0].equals("-appJar")) {
      throw new UnsupportedOperationException("invalid command-line, args[0] should be -appJar, but is " + args[0]);
    }
    if (!args[2].equals("-sig")) {
      throw new UnsupportedOperationException("invalid command-line, args[2] should be -sig, but is " + args[0]);
    }
  }
}