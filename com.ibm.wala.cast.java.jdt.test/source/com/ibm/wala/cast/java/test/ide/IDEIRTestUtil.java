/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 3, 2005
 */
package com.ibm.wala.cast.java.test.ide;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

import junit.framework.Assert;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ide.classloader.EclipseSourceFileModule;
import com.ibm.wala.ide.tests.util.EclipseTestUtil;

public class IDEIRTestUtil {

	public static void populateScope(String projectName, JavaSourceAnalysisEngine engine, Collection<String> sources, List<String> libs) throws IOException {
		boolean foundLib = false;
		for (String lib : libs) {
			File libFile = new File(lib);
			if (libFile.exists()) {
				foundLib = true;
				engine.addSystemModule(new JarFileModule(new JarFile(libFile)));
			}
		}
		assert foundLib : "couldn't find library file from " + libs;

		IWorkspace w = null;
		IJavaProject project = null;
		try {
			if (projectName != null) {
				w = ResourcesPlugin.getWorkspace();
				project = EclipseTestUtil.getNamedProject(projectName);
			}
		} catch (IllegalStateException e) {
			// use Workspace only if it exists
		}

		for (String srcFilePath : sources) {

			if (w != null) {
				IFile file = project.getProject().getFile(srcFilePath);
				try {
					engine.addSourceModule(EclipseSourceFileModule.createEclipseSourceFileModule(file));
				} catch (IllegalArgumentException e) {
					Assert.assertTrue(e.getMessage(), false);
				}

			} else {
				String srcFileName = srcFilePath.substring(srcFilePath.lastIndexOf(File.separator) + 1);
				File f = new File(srcFilePath);
				Assert.assertTrue("couldn't find " + srcFilePath, f.exists());
				if (f.isDirectory()) {
					engine.addSourceModule(new SourceDirectoryTreeModule(f));
				} else {
					engine.addSourceModule(new SourceFileModule(f, srcFileName));
				}
			}
		}
	}
}
