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
package com.ibm.wala.examples.analysis;

import java.io.IOException;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.config.AnalysisScopeReader;

/**
 * This is a simple example WALA application.
 * 
 * This counts the number of parameters to each method in the primordial loader (the J2SE standard libraries), and
 * prints the result.
 * 
 * @author sfink
 */
public class CountParameters {
  
  private final static ClassLoader MY_CLASSLOADER = CountParameters.class.getClassLoader();
  
  /**
   * Use the 'CountParameters' launcher to run this program with the appropriate classpath
   */
  public static void main(String[] args) throws IOException, ClassHierarchyException {
    // build an analysis scope representing the standard libraries, excluding no classes
    AnalysisScope scope = AnalysisScopeReader.readJavaScope("primordial.txt", null, MY_CLASSLOADER);
    
    // build a class hierarchy
    System.err.print("Build class hierarchy...");
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);
    System.err.println("Done");
    
    int nClasses = 0;
    int nMethods = 0;
    int nParameters = 0;
    
    for (IClass c : cha) {
      nClasses++;
      for (IMethod m : c.getDeclaredMethods()) {
        nMethods++;
        nParameters += m.getNumberOfParameters();
      }
    }
  
    System.out.println(nClasses + " classes");
    System.out.println(nMethods + " methods");
    System.out.println((float)nParameters/(float)nMethods + " parameters per method");
    
  }
}
