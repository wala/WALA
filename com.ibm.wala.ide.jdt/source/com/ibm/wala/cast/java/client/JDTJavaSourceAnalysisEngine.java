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
package com.ibm.wala.cast.java.client;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.jdt.JDTClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ide.client.EclipseProjectSourceAnalysisEngine;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ide.util.EclipseProjectPath.AnalysisScopeType;
import com.ibm.wala.ide.util.JavaEclipseProjectPath;
import com.ibm.wala.ide.util.JdtUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.SetOfClasses;

public class JDTJavaSourceAnalysisEngine<I extends InstanceKey> extends EclipseProjectSourceAnalysisEngine<IJavaProject, I> {
  private boolean dump;
  
  public JDTJavaSourceAnalysisEngine(IJavaProject project) {
    super(project);
  }

  public JDTJavaSourceAnalysisEngine(String projectName) {
    this(JdtUtil.getNamedProject(projectName));
  }

  public void setDump(boolean dump) {
    this.dump = dump;
  }
  
  @Override
  protected ClassLoaderFactory makeClassLoaderFactory(SetOfClasses exclusions) {
	  return new JDTClassLoaderFactory(exclusions, dump);	
  }

  @Override
  protected AnalysisScope makeAnalysisScope() {
	  return new JavaSourceAnalysisScope();
  }
  
  @Override
  protected ClassLoaderReference getSourceLoader() {
	  return JavaSourceAnalysisScope.SOURCE;
  }

  @Override
  public IAnalysisCacheView makeDefaultCache() {
    return new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory());
  }

  @Override
  protected EclipseProjectPath<?, IJavaProject> createProjectPath(
		  IJavaProject project) throws IOException, CoreException {
    project.open(new NullProgressMonitor());
	  return JavaEclipseProjectPath.make(project, AnalysisScopeType.SOURCE_FOR_PROJ_AND_LINKED_PROJS);	
  }

  @Override
  protected CallGraphBuilder<I> getCallGraphBuilder(IClassHierarchy cha,
		  AnalysisOptions options, IAnalysisCacheView cache) {
	    return new ZeroCFABuilderFactory().make(options, cache, cha, scope);
  }

}
