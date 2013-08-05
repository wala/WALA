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
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

import com.ibm.wala.cast.js.JavaScriptPlugin;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.io.FileProvider;

public class JavaScriptEclipseProjectPath extends EclipseProjectPath<IIncludePathEntry, IJavaScriptProject> {

  protected File getProlgueFile(String file) {
    try {
      FileProvider fileProvider = new EclipseFileProvider(JavaScriptPlugin.getDefault());
      JavaScriptLoader.addBootstrapFile(file);
      return fileProvider.getFile("dat/" + file, getClass().getClassLoader());
    } catch (IOException e) {
      assert false : "cannot find " + file;
      return null;
    }
  }

	public enum JSLoader implements ILoader {
		JAVASCRIPT(JavaScriptTypes.jsLoader);
		
	    private ClassLoaderReference ref;

	    JSLoader(ClassLoaderReference ref) {
	      this.ref = ref;
	    }

		@Override
		public ClassLoaderReference ref() {
			return ref;
		}
	}
	
	protected JavaScriptEclipseProjectPath(IJavaScriptProject p) throws IOException,
			CoreException {
		super(p.getProject(), AnalysisScopeType.SOURCE_FOR_PROJ_AND_LINKED_PROJS);
		
    List<Module> s = MapUtil.findOrCreateList(modules, JSLoader.JAVASCRIPT);
    File preamble = getProlgueFile("prologue.js");
    s.add(new SourceFileModule(preamble, "prologue.js", null));
	}

	public static JavaScriptEclipseProjectPath make(IJavaScriptProject p) throws IOException, CoreException {
		return new JavaScriptEclipseProjectPath(p);
	}

	@Override
	protected IJavaScriptProject makeProject(IProject p) {
		try {
			if (p.hasNature(JavaScriptCore.NATURE_ID)) {
				return JavaScriptCore.create(p);
			} else {
				return null;
			}
		} catch (CoreException e) {
			return null;
		}
	}

	@Override
	protected IIncludePathEntry resolve(IIncludePathEntry entry) {
		return JavaScriptCore.getResolvedIncludepathEntry(entry);
	}

	@Override
	protected void resolveClasspathEntry(IJavaScriptProject project,
			IIncludePathEntry entry,
			ILoader loader,
			boolean includeSource, boolean cpeFromMainProject) {
		IIncludePathEntry e = JavaScriptCore.getResolvedIncludepathEntry(entry);
		switch (e.getEntryKind()) {
		case IIncludePathEntry.CPE_SOURCE:
			resolveSourcePathEntry(JSLoader.JAVASCRIPT, true, cpeFromMainProject, e.getPath(), null, "js");
		}
	}

	@Override
	protected void resolveProjectClasspathEntries(IJavaScriptProject project, boolean includeSource) {
	  try {
			resolveClasspathEntries(project, Arrays.asList(project.getRawIncludepath()), Loader.EXTENSION, includeSource, true);
		} catch (JavaScriptModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
