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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.translator.jdt.JDTClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ide.classloader.EclipseSourceFileModule;
import com.ibm.wala.ide.util.JdtUtil;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;

public class JDTJavaSourceAnalysisEngine extends JavaSourceAnalysisEngine {
  protected final IJavaProject project;

  public JDTJavaSourceAnalysisEngine(IJavaProject project) {
    super();
    this.project = project;
  }

  public JDTJavaSourceAnalysisEngine(String projectName) {
    this(JdtUtil.getNamedProject(projectName));
  }

  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
    return new JDTClassLoaderFactory(exclusions);
  }

  public void addSourceModule(IResource file) {
    IProject proj = project.getProject();
    IPath path = file.getProjectRelativePath();
    if (file.getType() == IResource.FILE) {
      addSourceModule(EclipseSourceFileModule.createEclipseSourceFileModule(proj.getFile(path)));    
    } else {
      assert file.getType() == IResource.FOLDER;
      IFolder dir = proj.getFolder(path);
      try {
        for(IResource x : dir.members()) {
          assert x.getType() == IResource.FILE || x.getType() == IResource.FOLDER;
          addSourceModule(x);
        }
      } catch (CoreException e) {
        throw new RuntimeException("trouble with " + file, e);
      }
    }
  }
  
  public void addSourceModule(String fileName) {
    IResource file = project.getProject().findMember(fileName);
    assert file != null;
    addSourceModule(file);
  }

}
