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

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.core.IIncludePathEntry;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.JavaScriptModelException;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ide.jsdt.Activator;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

public class JavaScriptEclipseProjectPath extends EclipseProjectPath<IIncludePathEntry, IJavaScriptProject> {

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
	
  private final Set<Pair<String, Plugin>> models = HashSetFactory.make();

	protected JavaScriptEclipseProjectPath(Set<Pair<String, Plugin>> models) {
		super(AnalysisScopeType.SOURCE_FOR_PROJ_AND_LINKED_PROJS);
		this.models.addAll(models);
		this.models.add(Pair.make("prologue.js", (Plugin)Activator.getDefault()));
	}

	public static JavaScriptEclipseProjectPath make(IJavaScriptProject p, Set<Pair<String, Plugin>> models) throws IOException, CoreException {
	  JavaScriptEclipseProjectPath path = new JavaScriptEclipseProjectPath(models);
	  path.create(p.getProject());  
	  return path;
	}

	
	@Override
  public EclipseProjectPath<?, ?> create(IProject project) throws CoreException, IOException {
    EclipseProjectPath<?, ?> path = super.create(project);
  
    Collection<Module> s = modules.get(JSLoader.JAVASCRIPT);
    for(Pair<String,Plugin> model : models) {
      URL modelFile = JsdtUtil.getPrologueFile(model.fst, model.snd);
      assert modelFile != null : "cannot find file for " + model;
      s.add(new JSCallGraphUtil.Bootstrap(model.fst, modelFile.openStream(), modelFile));
    }

    return path;
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
			resolveSourcePathEntry(JSLoader.JAVASCRIPT, true, cpeFromMainProject, e.getPath(), null, e.getExclusionPatterns(), "js");
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
