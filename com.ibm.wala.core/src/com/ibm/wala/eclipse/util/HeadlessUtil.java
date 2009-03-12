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
package com.ibm.wala.eclipse.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.CommandLine;

public class HeadlessUtil {

  /**
   * create a Properties object representing the properties set by the command
   * line args. if args[i] is "-foo" and args[i+1] is "bar", then the result
   * will define a property with key "foo" and value "bar"
   */
  public static Properties parseCommandLine(String[] cmdLine) {
    if (cmdLine == null) {
      throw new IllegalArgumentException("null cmdLine");
    }
    Properties p = null;
    assert cmdLine[0].equals("-pdelaunch");
    String[] x = new String[cmdLine.length - 1];
    System.arraycopy(cmdLine, 1, x, 0, x.length);
    try {
      p = CommandLine.parse(x);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      System.err.println("Length " + x.length);
      for (String s : x) {
        System.err.println(s);
      }
      Assertions.UNREACHABLE();
    }
    return p;
  }

  /**
   * compute the analysis scope for a project in the current workspace
   * @throws IOException 
   * @throws CoreException 
   */
  public static AnalysisScope computeScope(String projectName) throws IOException, CoreException {
    IJavaProject jp = getProjectFromWorkspace(projectName);
    EclipseProjectPath path = EclipseProjectPath.make(jp);
    return path.toAnalysisScope((File)null);
  }

  private static IJavaProject getProjectFromWorkspace(String projectName) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IPath workspaceRootPath = workspaceRoot.getLocation();
    System.out.println("workspace: " + workspaceRootPath.toOSString());
  
    for (IProject p : workspaceRoot.getProjects()) {
      try {
        if (p.hasNature(JavaCore.NATURE_ID)) {
          IJavaProject jp = JavaCore.create(p);
          if (jp != null && jp.getElementName().equals(projectName)) {
            return jp;
          }
        }
      } catch (CoreException e) {
        // do nothing and continue
      }
    }
    Assertions.UNREACHABLE();
    return null;
  }

}
