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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import com.ibm.wala.client.impl.AbstractAnalysisEngine;
import com.ibm.wala.emf.wrappers.EMFScopeWrapper;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.debug.Assertions;

/**
 * Representation of an analysis scope from an Eclipse project.
 * 
 * We set up classloaders as follows:
 * <ul>
 * <li> The project being analyzed is in the Application Loader
 * <li> Projects on which the main project depends are in the Extension loader
 * <li> System libraries are in the primordial loader.
 * <li> Source modules go in a special Source loader.
 * </ul>
 * 
 * @author sjfink
 * @author jdolby (some code moved here from EclipseProjectAnalysisEngine)
 * 
 */
public class EclipseProjectPath {

  /**
   * TODO: do we really need this? Why shouldn't source files come from a
   * "normal" class loader like any other resource?
   */

  public static final Atom SOURCE = Atom.findOrCreateUnicodeAtom("Source");

  public static final ClassLoaderReference SOURCE_REF = new ClassLoaderReference(EclipseProjectPath.SOURCE);

  public enum Loader {
    APPLICATION(ClassLoaderReference.Application),
    EXTENSION(ClassLoaderReference.Extension),
    PRIMORDIAL(ClassLoaderReference.Primordial),
    SOURCE(SOURCE_REF);

    private ClassLoaderReference ref;

    Loader(ClassLoaderReference ref) {
      this.ref = ref;
    }
  };

  /**
   * path to the root of the workspace containing a project
   */
  private final IPath workspaceRootPath;

  /**
   * The project whose path this object represents
   */
  private final IJavaProject project;

  private final Map<Loader, Set<Module>> binaryModules = HashMapFactory.make();

  private final Map<Loader, Set<Module>> sourceModules = HashMapFactory.make();

  private final Collection<IClasspathEntry> alreadyResolved = HashSetFactory.make();

  private EclipseProjectPath(IPath workspaceRootPath, IJavaProject project) {
    this.workspaceRootPath = workspaceRootPath;
    this.project = project;
    assert workspaceRootPath != null;
    assert project != null;
    for (Loader loader : Loader.values()) {
      MapUtil.findOrCreateSet(binaryModules, loader);
      MapUtil.findOrCreateSet(sourceModules, loader);
    }
  }

  public static EclipseProjectPath make(IPath workspaceRootPath, IJavaProject project) {
    if (workspaceRootPath == null) {
      throw new IllegalArgumentException("workspaceRootPath is null");
    }
    return new EclipseProjectPath(workspaceRootPath, project);
  }

  /**
   * Figure out what a classpath entry means and add it to the appropriate set
   * of modules
   */
  @SuppressWarnings("restriction")
  private void resolveClasspathEntry(IClasspathEntry entry, Loader loader) throws JavaModelException, IOException {
    IClasspathEntry e = JavaCore.getResolvedClasspathEntry(entry);
    if (alreadyResolved.contains(e)) {
      return;
    } else {
      alreadyResolved.add(e);
    }

    if (e.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
      IClasspathContainer cont = JavaCore.getClasspathContainer(entry.getPath(), project);
      IClasspathEntry[] entries = cont.getClasspathEntries();
      resolveClasspathEntries(entries, cont.getKind() == IClasspathContainer.K_APPLICATION ? loader : Loader.PRIMORDIAL);
    } else if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
      File file = makeAbsolute(e.getPath()).toFile();
      JarFile j;
      try {
        j = new JarFile(file);
      } catch (ZipException z) {
        // a corrupted file. ignore it.
        return;
      }
      Set<Module> s = MapUtil.findOrCreateSet(binaryModules, loader);
      s.add(file.isDirectory() ? (Module) new BinaryDirectoryTreeModule(file) : (Module) new JarFileModule(j));
    } else if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
      File file = makeAbsolute(e.getPath()).toFile();
      Set<Module> s = MapUtil.findOrCreateSet(sourceModules, Loader.SOURCE);
      s.add(new SourceDirectoryTreeModule(file));
      if (e.getOutputLocation() != null) {
        File output = makeAbsolute(e.getOutputLocation()).toFile();
        s = MapUtil.findOrCreateSet(binaryModules, loader);
        s.add(new BinaryDirectoryTreeModule(output));
      }
    } else if (e.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
      IPath projectPath = makeAbsolute(e.getPath());
      IWorkspace ws = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot root = ws.getRoot();
      IProject project = (IProject) root.getContainerForLocation(projectPath);
      try {
        if (project.hasNature(JavaCore.NATURE_ID)) {
          IJavaProject javaProject = JavaCore.create(project);
          resolveClasspathEntries(javaProject.getRawClasspath(), loader);
          File output = makeAbsolute(javaProject.getOutputLocation()).toFile();
          Set<Module> s = MapUtil.findOrCreateSet(binaryModules, loader);
          s.add(new BinaryDirectoryTreeModule(output));
        }
      } catch (CoreException e1) {
        e1.printStackTrace();
        Assertions.UNREACHABLE();
      }
    } else {
      throw new RuntimeException("unexpected entry " + e);
    }
  }

  protected void resolveClasspathEntries(IClasspathEntry[] entries, Loader loader) throws JavaModelException, IOException {
    for (int i = 0; i < entries.length; i++) {
      resolveClasspathEntry(entries[i], loader);
    }
  }

  protected IPath makeAbsolute(IPath p) {
    if (p.toFile().exists()) {
      return p;
    }
    return workspaceRootPath.append(p);
  }

  public void resolveProjectClasspathEntries() throws JavaModelException, IOException {
    resolveClasspathEntries(project.getRawClasspath(), Loader.EXTENSION);
  }

  /**
   * Convert this path to a WALA analysis scope
   */
  public AnalysisScope toAnalysisScope(String exclusionsFile) {
    try {
      resolveProjectClasspathEntries();
      Set<Module> s = MapUtil.findOrCreateSet(binaryModules, Loader.APPLICATION);
      s.add(new BinaryDirectoryTreeModule(makeAbsolute(project.getOutputLocation()).toFile()));

      AnalysisScope scope = new EMFScopeWrapper(AbstractAnalysisEngine.BASIC_FILE, exclusionsFile, getClass().getClassLoader());

      for (Loader loader : Loader.values()) {
        for (Module m : binaryModules.get(loader)) {
          scope.addToScope(loader.ref, m);
        }
        for (Module m : sourceModules.get(loader)) {
          scope.addToScope(loader.ref, m);
        }
      }
      return scope;
    } catch (JavaModelException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public Collection<Module> getModules(Loader loader, boolean binary) {
    if (binary) {
      return Collections.unmodifiableCollection(binaryModules.get(loader));
    } else {
      return Collections.unmodifiableCollection(sourceModules.get(loader));
    }
  }

  @Override
  public String toString() {
    return toAnalysisScope(null).toString();
  }

}
