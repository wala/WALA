package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.jdt.test.Activator;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public abstract class JDTJavaTest extends IRTests {

  @BeforeClass
  public static void beforeClass() {
    EclipseTestUtil.importZippedProject(Activator.getDefault(), JDTJavaIRTests.PROJECT_NAME, JDTJavaIRTests.PROJECT_ZIP, new NullProgressMonitor());
    System.err.println("finish importing project");
  }

  @AfterClass
  public static void afterClass() {
    EclipseTestUtil.destroyProject(JDTJavaIRTests.PROJECT_NAME);
  }

  public JDTJavaTest(String projectName) {
    super(projectName);
  }

  @Override
  protected AbstractAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    return makeAnalysisEngine(mainClassDescriptors, sources, libs);
  }
  
  static AbstractAnalysisEngine makeAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    AbstractAnalysisEngine engine;
    try {
      engine = new JDTJavaSourceAnalysisEngine(JDTJavaIRTests.PROJECT_NAME) {
        @Override
        protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
          return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
        }
      };
  
      try {
        engine.setExclusionsFile((new EclipseFileProvider())
            .getFileFromPlugin(Activator.getDefault(), CallGraphTestUtil.REGRESSION_EXCLUSIONS).getAbsolutePath());
      } catch (IOException e) {
        Assert.assertFalse("Cannot find exclusions file", true);
      }
  
      return engine;
    } catch (IOException e1) {
      Assert.fail(e1.getMessage());
      return null;
    } catch (CoreException e1) {
      Assert.fail(e1.getMessage());
      return null;
    }
  }

}