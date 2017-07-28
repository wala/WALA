/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.java.test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.io.TemporaryFile;

public abstract class JDTJavaTest extends IRTests {

  static {
    System.setProperty("wala.jdt.quiet", "true");
  }

  private final ZippedProjectData project;
  
  public JDTJavaTest(ZippedProjectData project) {
    super(project.projectName);
    this.project = project;
    this.dump = Boolean.parseBoolean(System.getProperty("wala.cast.dump", "false"));
   }

  @Override
  protected <I extends InstanceKey> AbstractAnalysisEngine<I> getAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    return makeAnalysisEngine(mainClassDescriptors, project);
  }
  
  static <I extends InstanceKey> AbstractAnalysisEngine<I> makeAnalysisEngine(final String[] mainClassDescriptors, ZippedProjectData project) {
    AbstractAnalysisEngine<I> engine;
    engine = new JDTJavaSourceAnalysisEngine<I>(project.projectName) {
      {
        setDump(Boolean.parseBoolean(System.getProperty("wala.cast.dump", "false")));
      }
      
      @Override
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
      }
    };
 
    try {
      File tf = TemporaryFile.urlToFile("exclusions.txt", CallGraphTestUtil.class.getClassLoader().getResource(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
      engine.setExclusionsFile(tf.getAbsolutePath());
      tf.deleteOnExit();
    } catch (IOException e) {
      Assert.assertFalse("Cannot find exclusions file: " + e.toString(), true);
    }
 
    return engine;
  }

}
