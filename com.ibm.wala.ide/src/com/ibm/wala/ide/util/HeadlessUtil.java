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
package com.ibm.wala.ide.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.classloader.EclipseSourceFileModule;
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
    if (cmdLine.length == 0) {
      throw new IllegalArgumentException("cmdLine must have at least one parameter");
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

  protected static <X> X getProjectFromWorkspace(Function<IProject, X> pred) {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IPath workspaceRootPath = workspaceRoot.getLocation();
    System.out.println("workspace: " + workspaceRootPath.toOSString());
  
    for (IProject p : workspaceRoot.getProjects()) {
      X result = pred.apply(p);
      if (result != null) {
        return result;
      }
    }
    Assertions.UNREACHABLE();
    return null;
  }

  public interface EclipseCompiler<Unit> {
    Unit getCompilationUnit(IFile file);
    
    Parser<Unit> getParser();
  }
  
  public interface Parser<Unit> {
    void setProject(IProject project);
    
    void processASTs(Map<Unit,EclipseSourceFileModule> files, Function<Object[], Boolean> errors);
  }
    
  public static <Unit> void parseModules(Set<ModuleEntry> modules, EclipseCompiler<Unit> compiler) {
    // sort files into projects
    Map<IProject, Map<Unit,EclipseSourceFileModule>> projectsFiles = new HashMap<>();
    for (ModuleEntry m : modules) {
      if (m instanceof EclipseSourceFileModule) {
        EclipseSourceFileModule entry = (EclipseSourceFileModule) m;
        IProject proj = entry.getIFile().getProject();
        if (!projectsFiles.containsKey(proj)) {
          projectsFiles.put(proj, new HashMap<Unit,EclipseSourceFileModule>());
        }
        projectsFiles.get(proj).put(compiler.getCompilationUnit(entry.getIFile()), entry);
      }
    }

  final Parser<Unit> parser = compiler.getParser();

  for (final Map.Entry<IProject,Map<Unit,EclipseSourceFileModule>> proj : projectsFiles.entrySet()) {
    parser.setProject(proj.getKey());
    parser.processASTs(proj.getValue(), problems -> {
      int length = problems.length;
      if (length > 0) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
          buffer.append(problems[i].toString());
          buffer.append('\n');
        }
        if (length != 0) {
          System.err.println(buffer.toString());
          return true;
        }
      }
      return false;
   });
  }
  }
}
