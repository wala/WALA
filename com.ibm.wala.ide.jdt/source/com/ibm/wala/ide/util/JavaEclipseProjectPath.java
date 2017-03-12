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
package com.ibm.wala.ide.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;

public class JavaEclipseProjectPath extends EclipseProjectPath<IClasspathEntry, IJavaProject> {

	public enum JavaSourceLoader implements ILoader {
		SOURCE(JavaSourceAnalysisScope.SOURCE);
		
	    private ClassLoaderReference ref;

	    JavaSourceLoader(ClassLoaderReference ref) {
	      this.ref = ref;
	    }

		@Override
		public ClassLoaderReference ref() {
			return ref;
		}
	}
	
  protected JavaEclipseProjectPath(com.ibm.wala.ide.util.EclipseProjectPath.AnalysisScopeType scopeType)
      throws IOException, CoreException {
    super(scopeType);
  }

  public static JavaEclipseProjectPath make(IJavaProject p, AnalysisScopeType scopeType) throws IOException, CoreException {
    JavaEclipseProjectPath path = new JavaEclipseProjectPath(scopeType);
    path.create(p.getProject());
    return path;
  }

  @Override
  protected IJavaProject makeProject(IProject p) {
    try {
      if (p.hasNature(JavaCore.NATURE_ID)) {
        return JavaCore.create(p);
      }
    } catch (CoreException e) {
      // not a Java project
    } 
    return null;
  }

  @Override
  protected IClasspathEntry resolve(IClasspathEntry entry) {
    return JavaCore.getResolvedClasspathEntry(entry);
  }

  @Override
  protected void resolveClasspathEntry(IJavaProject project, IClasspathEntry entry, ILoader loader, boolean includeSource, boolean cpeFromMainProject) {
	  entry = JavaCore.getResolvedClasspathEntry(entry);
	  switch (entry.getEntryKind()) {
	  case IClasspathEntry.CPE_SOURCE: {
		  resolveSourcePathEntry(includeSource? JavaSourceLoader.SOURCE: Loader.APPLICATION, includeSource, cpeFromMainProject, entry.getPath(), entry.getOutputLocation(), entry.getExclusionPatterns()
, "java");
		  break;
	  }
	  case IClasspathEntry.CPE_LIBRARY: {
		  resolveLibraryPathEntry(loader, entry.getPath());
		  break;
	  }
	  case IClasspathEntry.CPE_PROJECT: {
		  resolveProjectPathEntry(loader, includeSource, entry.getPath());
		  break;
	  }
	  case IClasspathEntry.CPE_CONTAINER: {
		  try {
		    IClasspathContainer cont = JavaCore.getClasspathContainer(entry.getPath(), project);
		    IClasspathEntry[] entries = cont.getClasspathEntries();
		    resolveClasspathEntries(project, Arrays.asList(entries), cont.getKind() == IClasspathContainer.K_APPLICATION ? loader : Loader.PRIMORDIAL,
		        includeSource, false);
		  } catch (CoreException e) {
			  System.err.println(e);
			  Assertions.UNREACHABLE();
		  }
	  }
	  }
  }

  @Override
  protected void resolveProjectClasspathEntries(IJavaProject project, boolean includeSource) {
	try {
		resolveClasspathEntries(project, Arrays.asList(project.getRawClasspath()), Loader.EXTENSION, includeSource, true);
		File output = makeAbsolute(project.getOutputLocation()).toFile();
		if (!includeSource) {
			if (output.exists()) {
				List<Module> s = MapUtil.findOrCreateList(modules, Loader.APPLICATION);
				s.add(new BinaryDirectoryTreeModule(output));
			}
		}
	} catch (JavaModelException e) {
		e.printStackTrace();
		Assertions.UNREACHABLE();
	}
  }
}

