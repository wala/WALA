/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.client.ECJJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.examples.ast.SynchronizedBlockDuplicator;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.SourceModuleTranslator;
import com.ibm.wala.cast.java.translator.jdt.JDTJava2CAstTranslator;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJSourceModuleTranslator;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.RangePosition;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.SetOfClasses;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ECJSyncDuplicatorTest extends SyncDuplicatorTests {

  private static final CallSiteReference guard =
      CallSiteReference.make(
          0,
          MethodReference.findOrCreate(
              TypeReference.findOrCreate(JavaSourceAnalysisScope.SOURCE, "LMonitor2"),
              "randomIsolate",
              "()Z"),
          Dispatch.STATIC);

  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    JavaSourceAnalysisEngine engine =
        new ECJJavaSourceAnalysisEngine() {
          @Override
          protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
            return Util.makeMainEntrypoints(
                JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
          }

          @Override
          protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
            return new ECJClassLoaderFactory(exclusions) {
              @Override
              protected ECJSourceLoaderImpl makeSourceLoader(
                  ClassLoaderReference classLoaderReference,
                  IClassHierarchy cha,
                  IClassLoader parent) {
                return new ECJSourceLoaderImpl(classLoaderReference, parent, cha) {
                  @Override
                  protected SourceModuleTranslator getTranslator() {
                    return new ECJSourceModuleTranslator(cha.getScope(), this) {
                      @Override
                      protected JDTJava2CAstTranslator<Position> makeCAstTranslator(
                          CompilationUnit astRoot, String fullPath) {
                        return new JDTJava2CAstTranslator<Position>(
                            sourceLoader, astRoot, fullPath, true) {
                          @Override
                          public CAstEntity translateToCAst() {
                            CAstEntity ast = super.translateToCAst();
                            SynchronizedBlockDuplicator unwind =
                                new SynchronizedBlockDuplicator(new CAstImpl(), true, guard);
                            return unwind.translate(ast);
                          }

                          @Override
                          public Position makePosition(int start, int end) {
                            try {
                              return new RangePosition(
                                  new URL("file://" + fullPath),
                                  this.cu.getLineNumber(start),
                                  start,
                                  end);
                            } catch (MalformedURLException e) {
                              throw new RuntimeException("bad file: " + fullPath, e);
                            }
                          }
                        };
                      }
                    };
                  }
                };
              }
            };
          }
        };
    engine.setExclusionsFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    populateScope(engine, sources, libs);
    return engine;
  }
}
