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
package com.ibm.wala.client.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author unascribed
 *
 * TODO: document me
 */
public abstract class EclipseProjectAnalysisEngine extends AbstractAnalysisEngine {

  protected final Set<Module> userEntries = new HashSet<Module>();

  protected final Set<Module> sourceEntries = new HashSet<Module>();

  protected final Set<Module> systemEntries = new HashSet<Module>();

  private final IPath fRootPath;

  protected final IJavaProject project;

  public EclipseProjectAnalysisEngine() {
    super();
    this.project = null;
    this.fRootPath = null;
  }

  public EclipseProjectAnalysisEngine(IJavaProject project) {
    super();
    this.project = project;
    this.fRootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
  }

  protected IPath makeAbsolute(IPath p) {
    if (p.toFile().exists())
      return p;
    return fRootPath.append(p);
  }

  private void resolveClasspathEntry(IClasspathEntry entry, boolean user) throws JavaModelException, IOException {
    IClasspathEntry e = JavaCore.getResolvedClasspathEntry(entry);

    System.err.println("looking at " + e + " of type " + e.getEntryKind());

    if (e.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
      IClasspathContainer cont = JavaCore.getClasspathContainer(entry.getPath(), project);
      IClasspathEntry[] entries = cont.getClasspathEntries();
      resolveClasspathEntries(entries, cont.getKind() == IClasspathContainer.K_APPLICATION);
    } else if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
      File file = makeAbsolute(e.getPath()).toFile();
      (user ? userEntries : systemEntries).add(file.isDirectory() ? (Module) new BinaryDirectoryTreeModule(file)
          : (Module) new JarFileModule(new JarFile(file)));
    } else if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
      File file = makeAbsolute(e.getPath()).toFile();
      sourceEntries.add(new SourceDirectoryTreeModule(file));
      if (e.getOutputLocation() != null) {
        File output = makeAbsolute(e.getOutputLocation()).toFile();
        userEntries.add(new BinaryDirectoryTreeModule(output));
      }
    } else if (e.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
      IPath projectPath = e.getPath();
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = ws.getRoot();
      IProject project = (IProject) root.getContainerForLocation(projectPath);
      IJavaProject javaProject = JavaCore.create(project);
      resolveClasspathEntries(javaProject.getRawClasspath(), true);
    } else {
      throw new RuntimeException("unexpected entry " + e);
    }
  }

  protected void resolveClasspathEntries(IClasspathEntry[] entries, boolean user) throws JavaModelException, IOException {
    for (int i = 0; i < entries.length; i++) {
      resolveClasspathEntry(entries[i], user);
    }
  }

  protected void buildAnalysisScope() {
    try {
      if (project != null) {
        resolveClasspathEntries(project.getRawClasspath(), true);
        userEntries.add(new BinaryDirectoryTreeModule(makeAbsolute(project.getOutputLocation()).toFile()));

        super.setJ2SELibraries((Module[]) systemEntries.toArray(new Module[systemEntries.size()]));

        Set<Module> allUserEntries = new HashSet<Module>();
        allUserEntries.addAll(userEntries);
        allUserEntries.addAll(sourceEntries);
        super.setModuleFiles(allUserEntries);
      }

      super.buildAnalysisScope();
    } catch (JavaModelException e) {
      Assertions.UNREACHABLE();
    } catch (IOException e) {
      Assertions.UNREACHABLE();
    }
  }

  public void setJ2SELibraries(JarFile[] libs) {
    Assertions._assert(project == null);
    super.setJ2SELibraries(libs);
  }

  public void setJ2SELibraries(Module[] libs) {
    Assertions._assert(project == null);
    super.setJ2SELibraries(libs);
  }

  public void setModuleFiles(Collection moduleFiles) {
    Assertions._assert(project == null);
    super.setModuleFiles(moduleFiles);
  }

}
