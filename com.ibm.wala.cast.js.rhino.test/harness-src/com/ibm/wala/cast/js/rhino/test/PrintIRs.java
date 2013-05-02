/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.rhino.test;

import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;

public class PrintIRs {

  /**
   * prints the IR of each function in the script 
   * @throws IOException 
   * @throws ClassHierarchyException 
   */
  public static void printIRs(String filename) throws IOException, ClassHierarchyException {
    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
    IClassHierarchy cha = JSCallGraphUtil.makeHierarchyForScripts(filename);
    IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();
    SSAOptions options = new SSAOptions();
    for (IClass klass : cha) {
      Collection<IMethod> allMethods = klass.getAllMethods();
      for (IMethod m : allMethods) {
        IR ir = factory.makeIR(m, Everywhere.EVERYWHERE, options);
        System.out.println(ir);
        if (m instanceof AstMethod) {
          System.out.println(((AstMethod)m).getSourcePosition());
        }
      }
    }
  }
  
  /**
   * 
   * @param args
   * @throws IOException 
   * @throws ClassHierarchyException 
   */
  public static void main(String[] args) throws ClassHierarchyException, IOException {
    printIRs(args[0]);

  }

}
