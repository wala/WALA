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

import com.ibm.wala.cast.java.jdt.test.Activator;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class JDTJavaIRTests extends JavaIRTests {
  
  public static final String PROJECT_NAME = "com.ibm.wala.cast.java.test.data";
 
  public static final String PROJECT_ZIP = "test_project.zip";
  
  public static final ZippedProjectData PROJECT;
  static {
    try {
      PROJECT = new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  private final ZippedProjectData project;

  public JDTJavaIRTests() {
    this(PROJECT);
  }
  
  private JDTJavaIRTests(ZippedProjectData project) {
    super(project.projectName);
    this.project = project;
    this.dump = Boolean.parseBoolean(System.getProperty("wala.cast.dump", "false"));
  }

  @Override
  protected <I extends InstanceKey> AbstractAnalysisEngine<I> getAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    return JDTJavaTest.makeAnalysisEngine(mainClassDescriptors, sources, libs, project);
  }
}
