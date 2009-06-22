/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.classloader;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.ibm.wala.classLoader.FileModule;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ide.util.EclipseProjectPath;

public class EclipseSourceDirectoryTreeModule extends SourceDirectoryTreeModule {

  private final IPath rootIPath;
  
  public EclipseSourceDirectoryTreeModule(IPath root) {
    super(EclipseProjectPath.makeAbsolute(root).toFile());
    this.rootIPath = root;
  }

  public EclipseSourceDirectoryTreeModule(IPath root, String fileExt) {
    super(EclipseProjectPath.makeAbsolute(root).toFile(), fileExt);
    this.rootIPath = root;
}

  @Override
  protected FileModule makeFile(File file) {
    IPath p = rootIPath.append(file.getPath().substring(root.getPath().length()));
    IWorkspace ws = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = ws.getRoot();
    IFile ifile = root.getFile(p);
    assert ifile.exists();
    return EclipseSourceFileModule.createEclipseSourceFileModule(ifile);
  }
  
  @Override
  public String toString() {
    return "EclipseSourceDirectoryTreeModule:" + rootIPath;
  }

}
