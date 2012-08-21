/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.test.ide.IDEIRTestUtil;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.plugin.CoreTestsPlugin;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class JDTJavaIRTests extends JavaIRTests {

  public static final String PROJECT_NAME = "com.ibm.wala.cast.java.test.data";
 
  public static final String PROJECT_ZIP = "test_project.zip";
  
  public JDTJavaIRTests() {
    super(PROJECT_NAME);
  }

  @Override
  protected void populateScope(JavaSourceAnalysisEngine engine, Collection<String> sources, List<String> libs) throws IOException {
	  IDEIRTestUtil.populateScope(projectName, (JDTJavaSourceAnalysisEngine)engine, sources, libs);
  }

  @BeforeClass
  public static void beforeClass() {
    EclipseTestUtil.importZippedProject(TestPlugin.getDefault(), PROJECT_NAME, PROJECT_ZIP, new NullProgressMonitor());
    System.err.println("finish importing project");
  }

  @AfterClass
  public static void afterClass() {
    EclipseTestUtil.destroyProject(PROJECT_NAME);
  }

  @Override
  protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
    JavaSourceAnalysisEngine engine = new JDTJavaSourceAnalysisEngine(PROJECT_NAME) {
      @Override
      protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
        return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
      }
    };

    try {
      engine.setExclusionsFile((new EclipseFileProvider())
          .getFileFromPlugin(CoreTestsPlugin.getDefault(), CallGraphTestUtil.REGRESSION_EXCLUSIONS).getAbsolutePath());
    } catch (IOException e) {
      Assert.assertFalse("Cannot find exclusions file", true);
    }

    return engine;
  }
}
