/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.eclipse.tests;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Bundle;

public class EclipseTestUtil {

	public static void importZippedProject(String zipFileName) {
		ZipFile zipFile = getZipFile(zipFileName);
		ZipFileStructureProvider zp = new ZipFileStructureProvider(zipFile);
		List children = zp.getChildren(zp.getRoot());
		Object element = children.get(0);
		String projectName = zp.getLabel(element);
		createOpenProject(projectName);
		importZipfile(zipFile,zp);
	}
	
	public static void createOpenProject(String projectName) {
		IWorkspaceRoot root = getWorkspace();
		IProject project = root.getProject(projectName);
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected static void importZipfile(ZipFile sourceZip, ZipFileStructureProvider provider) {
		IPath containerPath = getWorkspacePath();

		ImportOperation importOp = new ImportOperation(containerPath,provider.getRoot(), provider,
					new IOverwriteQuery() {
						public String queryOverwrite(String pathString) {
							return IOverwriteQuery.ALL;
					}
				}
			);
		
		importOp.setCreateContainerStructure(true);
		importOp.setOverwriteResources(true);
		try {
			importOp.run(new NullProgressMonitor());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			sourceZip.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static File getTestDataFile(String filename) {
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		IPath path = new Path("testdata").append(filename);
		
		URL url = FileLocator.find(bundle, path, null);
		try {
			URL fileURL = FileLocator.toFileURL(url);
			File file = new File(fileURL.getPath());
			return file;
		} catch (IOException e) {
			reportException(e);
		}
		
		return null;
	}
	
	public static ZipFile getZipFile(String testArchive) {
		File file = getTestDataFile(testArchive);
		if(file != null) {
			try {
				return new ZipFile(file);
			} catch (ZipException e) {
				reportException(e);
			} catch (IOException e) {
				reportException(e);
			}
		}
		
		return null;
	}
	
	public static IWorkspaceRoot getWorkspace() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	private static IPath getWorkspacePath() {
		return ResourcesPlugin.getWorkspace().getRoot().getFullPath();
	}
	
	private static void reportException(Exception e) {
		// TODO: add to appropriate error log?  Report differently ??
		e.printStackTrace();
	}

}
