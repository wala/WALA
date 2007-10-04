/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.java.test;

import java.io.File;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.examples.ast.SynchronizedBlockDuplicator;
import com.ibm.wala.cast.java.translator.polyglot.IRTranslatorExtension;
import com.ibm.wala.cast.java.translator.polyglot.JavaIRTranslatorExtension;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.impl.CAstRewriter;
import com.ibm.wala.cast.tree.impl.CAstRewriterFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;

public class SyncDuplicatorTest extends IRTests {

  public SyncDuplicatorTest() {
    super("SyncDuplicatorTest");
  }

  private final static CallSiteReference testMethod =
    CallSiteReference.make(
      0, 
      MethodReference.findOrCreate(
        TypeReference.findOrCreate(
	  EclipseProjectPath.SOURCE_REF,
	  TypeName.string2TypeName("LMonitor2")),
	Atom.findOrCreateUnicodeAtom("test"),
	Descriptor.findOrCreateUTF8("(Ljava/lang/Object;)Z")),
      IInvokeInstruction.Dispatch.STATIC);

  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
    return new JavaSourceAnalysisEngine() {
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(EclipseProjectPath.SOURCE_REF, cha, mainClassDescriptors);
      }

      public IRTranslatorExtension getTranslatorExtension() {
	JavaIRTranslatorExtension ext =  new JavaIRTranslatorExtension();
	ext.setCAstRewriterFactory(new CAstRewriterFactory() {
	  public CAstRewriter createCAstRewriter(CAst ast) {
	    return new SynchronizedBlockDuplicator(ast, true, testMethod);
	  }
	}); 
	return ext;
      }

    };
  }

  protected String singleInputForTest() {
    return getName().substring(4) + ".java";
  }

  protected String singlePkgInputForTest(String pkgName) {
    return pkgName + File.separator + getName().substring(4) + ".java";
  }

  public void testMonitor2() {
    runTest(singleTestSrc(), rtJar, simpleTestEntryPoint(), emptyList, true);
  }

}
