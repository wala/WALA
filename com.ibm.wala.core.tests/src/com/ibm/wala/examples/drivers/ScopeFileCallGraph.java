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
package com.ibm.wala.examples.drivers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.util.warnings.Warnings;

/**
 * Driver that constructs a call graph for an application specified via a scope file.  
 * Useful for getting some code to copy-paste.    
 */
public class ScopeFileCallGraph {

  /**
   * Usage: ScopeFileCallGraph -scopeFile file_path [-entryClass class_name |
   * -mainClass class_name]
   * 
   * If given -mainClass, uses main() method of class_name as entrypoint. If
   * given -entryClass, uses all public methods of class_name.
   * 
   * @throws IOException
   * @throws ClassHierarchyException
   * @throws CallGraphBuilderCancelException
   * @throws IllegalArgumentException
   */
  public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException,
      CallGraphBuilderCancelException {
    long start = System.currentTimeMillis();
    Properties p = CommandLine.parse(args);
    String scopeFile = p.getProperty("scopeFile");
    String entryClass = p.getProperty("entryClass");
    String mainClass = p.getProperty("mainClass");
    String dump = p.getProperty("dump");
    if (mainClass != null && entryClass != null) {
      throw new IllegalArgumentException("only specify one of mainClass or entryClass");
    }
    // use exclusions to eliminate certain library packages
    File exclusionsFile = null;
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, exclusionsFile, ScopeFileCallGraph.class.getClassLoader());
    IClassHierarchy cha = ClassHierarchyFactory.make(scope);
    System.out.println(cha.getNumberOfClasses() + " classes");
    System.out.println(Warnings.asString());
    Warnings.clear();
    AnalysisOptions options = new AnalysisOptions();
    Iterable<Entrypoint> entrypoints = entryClass != null ? makePublicEntrypoints(cha, entryClass) : Util.makeMainEntrypoints(scope, cha, mainClass);
    options.setEntrypoints(entrypoints);
    // you can dial down reflection handling if you like
    options.setReflectionOptions(ReflectionOptions.NONE);
    IAnalysisCacheView cache = new AnalysisCacheImpl();
    // other builders can be constructed with different Util methods
    CallGraphBuilder<InstanceKey> builder = Util.makeZeroOneContainerCFABuilder(options, cache, cha, scope);
//    CallGraphBuilder builder = Util.makeNCFABuilder(2, options, cache, cha, scope);
//    CallGraphBuilder builder = Util.makeVanillaNCFABuilder(2, options, cache, cha, scope);
    System.out.println("building call graph...");
    CallGraph cg = builder.makeCallGraph(options, null);
    long end = System.currentTimeMillis();
    System.out.println("done");
    if (dump != null) {
      System.err.println(cg);
    }
    System.out.println("took " + (end-start) + "ms");
    System.out.println(CallGraphStats.getStats(cg));
  }

  private static Iterable<Entrypoint> makePublicEntrypoints(IClassHierarchy cha, String entryClass) {
    Collection<Entrypoint> result = new ArrayList<>();
    IClass klass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application,
        StringStuff.deployment2CanonicalTypeString(entryClass)));
    for (IMethod m : klass.getDeclaredMethods()) {
      if (m.isPublic()) {
        result.add(new DefaultEntrypoint(m, cha));
      }
    }
    return result;
  }
}
