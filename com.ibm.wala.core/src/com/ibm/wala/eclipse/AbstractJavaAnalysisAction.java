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
package com.ibm.wala.eclipse;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * An Eclipse action that analyzes a Java selection
 * 
 * @author sjfink
 */
public abstract class AbstractJavaAnalysisAction implements IObjectActionDelegate, IRunnableWithProgress {

  private ISelection currentSelection;
  
  private final AnalysisCache cache;

  public AbstractJavaAnalysisAction(AnalysisCache cache) {
    super();
    this.cache = cache;
  }

  /**
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * Compute an analysis scope for the current selection
   */
  protected AnalysisScope computeScope() {
    IStructuredSelection selection = (IStructuredSelection) currentSelection;
    Collection<EclipseProjectPath> projectPaths = HashSetFactory.make();
    for (Iterator it = selection.iterator(); it.hasNext();) {
      Object object = it.next();
      if (object instanceof IJavaElement) {
        IJavaElement e = (IJavaElement) object;
        IJavaProject jp = e.getJavaProject();
        projectPaths.add(EclipseProjectPath.make(ResourcesPlugin.getWorkspace().getRoot().getLocation(), jp));
      } else {
        Assertions.UNREACHABLE(object.getClass());
      }
    }

    AnalysisScope scope = mergeProjectPaths(projectPaths);
    return scope;
  }

  /**
   * compute the java projects represented by the current selection
   */
  protected Collection<IJavaProject> computeJavaProjects() {
    IStructuredSelection selection = (IStructuredSelection) currentSelection;
    Collection<IJavaProject> projects = HashSetFactory.make();
    for (Iterator it = selection.iterator(); it.hasNext();) {
      Object object = it.next();
      if (object instanceof IJavaElement) {
        IJavaElement e = (IJavaElement) object;
        IJavaProject jp = e.getJavaProject();
        projects.add(jp);
      } else {
        Assertions.UNREACHABLE(object.getClass());
      }
    }
    return projects;
  }

  /**
   * create an analysis scope as the union of a bunch of EclipseProjectPath
   */
  private AnalysisScope mergeProjectPaths(Collection<EclipseProjectPath> projectPaths) {
    AnalysisScope scope = new AnalysisScope();
    Collection<Module> seen = HashSetFactory.make();
    // to avoid duplicates, we first add all application modules, then extension
    // modules, then primordial
    buildScope(ClassLoaderReference.Application, projectPaths, scope, seen);
    buildScope(ClassLoaderReference.Extension, projectPaths, scope, seen);
    buildScope(ClassLoaderReference.Primordial, projectPaths, scope, seen);
    return scope;
  }

  private void buildScope(ClassLoaderReference loader, Collection<EclipseProjectPath> projectPaths, AnalysisScope scope,
      Collection<Module> seen) {
    for (EclipseProjectPath path : projectPaths) {
      AnalysisScope pScope = path.toAnalysisScope(null);
      for (Module m : pScope.getModules(loader)) {
        if (!seen.contains(m)) {
          seen.add(m);
          scope.addToScope(loader, m);
        }
      }
    }
  }

  /**
   * @see IActionDelegate#run(IAction)
   */
  public void run(IAction action) {
    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
    try {
      progressService.busyCursorWhile(this);
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  /**
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    currentSelection = selection;
  }

  public ISelection getCurrentSelection() {
    return currentSelection;
  }

  public AnalysisCache getAnalysisCache() {
    return cache;
  }

}
