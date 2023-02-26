/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.examples.drivers;

import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.core.util.strings.StringStuff;
import com.ibm.wala.core.viz.PDFViewUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.viz.DotUtil;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * This simple example application builds a WALA CDG and fires off ghostview to viz a DOT
 * representation.
 *
 * @author sfink
 */
public class PDFControlDependenceGraph {

  public static final boolean SANITIZE_CFG = false;

  public static final String PDF_FILE = "cdg.pdf";

  /**
   * Usage: GVControlDependenceGraph -appJar [jar file name] -sig [method signature] The "jar file
   * name" should be something like "c:/temp/testdata/java_cup.jar" The signature should be
   * something like "java_cup.lexer.advance()V"
   */
  public static void main(String[] args) throws IOException {

    run(args);
  }

  /**
   * @param args -appJar [jar file name] -sig [method signature] The "jar file name" should be
   *     something like "c:/temp/testdata/java_cup.jar" The signature should be something like
   *     "java_cup.lexer.advance()V"
   */
  public static Process run(String[] args) throws IOException {
    validateCommandLine(args);
    return run(args[1], args[3]);
  }

  /**
   * @param appJar should be something like "c:/temp/testdata/java_cup.jar"
   * @param methodSig should be something like "java_cup.lexer.advance()V"
   */
  public static Process run(String appJar, String methodSig) throws IOException {
    try {
      if (PDFCallGraph.isDirectory(appJar)) {
        appJar = PDFCallGraph.findJarFiles(new String[] {appJar});
      }
      AnalysisScope scope =
          AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(
              appJar, new FileProvider().getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));

      ClassHierarchy cha = ClassHierarchyFactory.make(scope);

      MethodReference mr = StringStuff.makeMethodReference(methodSig);

      IMethod m = cha.resolveMethod(mr);
      if (m == null) {
        System.err.println("could not resolve " + mr);
        throw new RuntimeException();
      }
      AnalysisOptions options = new AnalysisOptions();
      options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
      IAnalysisCacheView cache = new AnalysisCacheImpl(options.getSSAOptions());
      IR ir = cache.getIR(m, Everywhere.EVERYWHERE);

      if (ir == null) {
        Assertions.UNREACHABLE("Null IR for " + m);
      }

      System.err.println(ir);
      ControlDependenceGraph<ISSABasicBlock> cdg =
          new ControlDependenceGraph<>(ir.getControlFlowGraph());

      Properties wp = null;
      try {
        wp = WalaProperties.loadProperties();
        wp.putAll(WalaExamplesProperties.loadProperties());
      } catch (WalaException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
      String psFile =
          wp.getProperty(WalaProperties.OUTPUT_DIR)
              + File.separatorChar
              + PDFControlDependenceGraph.PDF_FILE;
      String dotFile =
          wp.getProperty(WalaProperties.OUTPUT_DIR)
              + File.separatorChar
              + PDFTypeHierarchy.DOT_FILE;
      String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
      String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

      DotUtil.<ISSABasicBlock>dotify(cdg, PDFViewUtil.makeIRDecorator(ir), dotFile, psFile, dotExe);

      return PDFViewUtil.launchPDFView(psFile, gvExe);

    } catch (WalaException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Validate that the command-line arguments obey the expected usage.
   *
   * <p>Usage:
   *
   * <ul>
   *   <li>args[0] : "-appJar"
   *   <li>args[1] : something like "c:/temp/testdata/java_cup.jar"
   *   <li>args[2] : "-sig"
   *   <li>args[3] : a method signature like "java_cup.lexer.advance()V"
   * </ul>
   *
   * @throws UnsupportedOperationException if command-line is malformed.
   */
  static void validateCommandLine(String[] args) {
    if (args.length != 4) {
      throw new UnsupportedOperationException("must have at exactly 4 command-line arguments");
    }
    if (!args[0].equals("-appJar")) {
      throw new UnsupportedOperationException(
          "invalid command-line, args[0] should be -appJar, but is " + args[0]);
    }
    if (!args[2].equals("-sig")) {
      throw new UnsupportedOperationException(
          "invalid command-line, args[2] should be -sig, but is " + args[0]);
    }
  }
}
