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
import java.util.Collection;
import java.util.Map;

import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;

/**
 * This is a simple example WALA application.
 * 
 * For each method in the standard libraries, it maps the IMethod to the set of fields the method reads.
 * 
 * @author sfink
 */
public class GetLoadedFields {

  private final static ClassLoader MY_CLASSLOADER = GetLoadedFields.class.getClassLoader();

  /**
   * Use the 'GetLoadedFields' launcher to run this program with the appropriate classpath
   * @throws InvalidClassFileException
   */
  public static void main(String[] args) throws IOException, ClassHierarchyException, InvalidClassFileException {
    // build an analysis scope representing the standard libraries, excluding no classes
    AnalysisScope scope = AnalysisScopeReader.readJavaScope("primordial.txt", null, MY_CLASSLOADER);

    // build a class hierarchy
    System.err.print("Build class hierarchy...");
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);
    System.err.println("Done");

    int nMethods = 0;
    int nFields = 0;

    Map<IMethod, Collection<FieldReference>> method2Field = HashMapFactory.make();
    for (IClass c : cha) {
      for (IMethod m : c.getDeclaredMethods()) {
        nMethods++;
        Collection<FieldReference> fields = CodeScanner.getFieldsRead(m);
        nFields += fields.size();
        method2Field.put(m, fields);
      }
    }
    
    System.out.println(nMethods + " methods");
    System.out.println((float)nFields/(float)nMethods + " fields read per method");
  }
}
