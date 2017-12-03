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
package com.ibm.wala.ide.tests.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Bundle;

public class EclipseTestUtil {

  public static class ZippedProjectData {
    public final Plugin sourcePlugin;
    public final String projectName;
    public final String zipFileName;
 
    public ZippedProjectData(Plugin sourcePlugin, String projectName, String zipFileName) throws IOException {
      this.sourcePlugin = sourcePlugin;
      this.projectName = projectName;
      this.zipFileName = zipFileName;
      open();
    }
    
    private void open() throws IOException {
      importZippedProject(sourcePlugin, projectName, zipFileName, new NullProgressMonitor());
    }
    
    public void close() {
      destroyProject(projectName);
    }
  }
  
  public static void importZippedProject(Plugin plugin, String projectName, String zipFileName, IProgressMonitor monitor) throws IOException {
    try (final ZipFile zipFile = getZipFile(plugin, zipFileName)) {
      createOpenProject(projectName);
      importZipfile(projectName, zipFile, monitor);
    }
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

  public static void destroyProject(String projectName) {
    IWorkspaceRoot root = getWorkspace();
    IProject project = root.getProject(projectName);
    try {
      project.delete(true, null);
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  public static void importProjectFromFilesystem(String projectName, File root, IProgressMonitor monitor) {
    FileSystemStructureProvider fs = FileSystemStructureProvider.INSTANCE;
    importProject(fs, monitor, projectName, root);
  }
  
  public static void importZipfile(String projectName, ZipFile zipFile, IProgressMonitor monitor) {
    ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);

    importProject(provider, monitor, projectName, provider.getRoot());

    try {
      zipFile.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected static <T> void importProject(IImportStructureProvider provider, IProgressMonitor monitor, String projectName, T root) {
    IPath containerPath = getWorkspacePath().append(projectName).addTrailingSeparator();

    ImportOperation importOp = new ImportOperation(containerPath, root, provider, pathString -> IOverwriteQuery.ALL);

    importOp.setCreateContainerStructure(false);
    importOp.setOverwriteResources(true);
    try {
      importOp.run(monitor);
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static File getTestDataFile(Plugin plugin, String filename) {
    Bundle bundle = plugin.getBundle();
    IPath path = new Path("testdata").append(filename);

    URL url = FileLocator.find(bundle, path, null);
    assert url != null : bundle.toString() + " path " + path.toString();
    try {
      URL fileURL = FileLocator.toFileURL(url);
      File file = new File(fileURL.getPath());
      return file;
    } catch (IOException e) {
      reportException(e);
    }

    return null;
  }

  public static ZipFile getZipFile(Plugin plugin, String testArchive) {
    File file = getTestDataFile(plugin, testArchive);
    if (file != null) {
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
    // TODO: add to appropriate error log? Report differently ??
    e.printStackTrace();
  }

}
