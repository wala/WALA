/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ide.classloader;

import com.ibm.wala.classLoader.FileModule;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.ide.util.EclipseProjectPath;
import java.io.File;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

public class EclipseSourceDirectoryTreeModule extends SourceDirectoryTreeModule {

  private final IPath rootIPath;
  private final Pattern[] excludePatterns;

  private static Pattern interpretPattern(IPath pattern) {
    return Pattern.compile(
        '^'
            + pattern
                .toString()
                .replace(".", "\\.")
                .replace("**", "~~~")
                .replace("*", "[^/]*")
                .replace("~~~", ".*")
            + '$');
  }

  private static Pattern[] interpretExcludes(IPath[] excludes) {
    if (excludes == null) {
      return null;
    } else {
      Pattern[] stuff = new Pattern[excludes.length];
      for (int i = 0; i < excludes.length; i++) {
        stuff[i] = interpretPattern(excludes[i]);
      }
      return stuff;
    }
  }

  public EclipseSourceDirectoryTreeModule(IPath root, IPath[] excludePaths) {
    super(EclipseProjectPath.makeAbsolute(root).toFile());
    this.rootIPath = root;
    this.excludePatterns = interpretExcludes(excludePaths);
  }

  public EclipseSourceDirectoryTreeModule(IPath root, IPath[] excludePaths, String fileExt) {
    super(EclipseProjectPath.makeAbsolute(root).toFile(), fileExt);
    this.rootIPath = root;
    this.excludePatterns = interpretExcludes(excludePaths);
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
  protected boolean includeFile(File file) {
    if (!super.includeFile(file)) {
      return false;
    } else {
      if (excludePatterns != null) {
        IPath p = rootIPath.append(file.getPath().substring(root.getPath().length()));
        for (Pattern exclude : excludePatterns) {
          if (exclude.matcher(p.toOSString()).matches()) {
            return false;
          }
        }
      }
      return true;
    }
  }

  @Override
  public String toString() {
    return "EclipseSourceDirectoryTreeModule:" + rootIPath;
  }
}
