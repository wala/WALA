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
package com.ibm.wala.ide;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ide.util.JavaEclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;

/**
 * An Eclipse action that analyzes a Java selection
 */
public abstract class AbstractJavaAnalysisAction implements IObjectActionDelegate, IRunnableWithProgress {

  /**
   * The current {@link ISelection} highlighted in the Eclipse workspace
   */
  private ISelection currentSelection;

  public AbstractJavaAnalysisAction() {
    super();
  }

  /*
   * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
   */
  @Override
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  /**
   * Compute an analysis scope for the current selection
   */
  public static AnalysisScope computeScope(IStructuredSelection selection) throws IOException {
    return computeScope(selection, EclipseProjectPath.AnalysisScopeType.NO_SOURCE);
  }

  /**
   * Compute an analysis scope for the current selection
   * 
   * @param scopeType should analysis use the source files in the Eclipse projects rather than the class files.
   */
  public static AnalysisScope computeScope(final IStructuredSelection selection, 
		  final EclipseProjectPath.AnalysisScopeType scopeType) throws IOException 
  {
    if (selection == null) {
      throw new IllegalArgumentException("null selection");
    }
    final Collection<EclipseProjectPath<?, ?>> projectPaths = new LinkedList<>();
    Job job = new Job("Compute project paths") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        for (Object object : Iterator2Iterable.make((Iterator<?>) selection.iterator())) {
          if (object instanceof IJavaElement) {
            IJavaElement e = (IJavaElement) object;
            IJavaProject jp = e.getJavaProject();
            try {
              projectPaths.add(JavaEclipseProjectPath.make(jp, scopeType));
            } catch (CoreException e1) {
              e1.printStackTrace();
              // skip and continue
            } catch (IOException e2) {
              e2.printStackTrace();
              return new Status(IStatus.ERROR, "", 0, "", e2);
            }
          } else {
            Assertions.UNREACHABLE(object.getClass());
          }
        }
        return Status.OK_STATUS;
      }

    };
    // lock the whole workspace
    job.setRule(ResourcesPlugin.getWorkspace().getRoot());
    job.schedule();
    try {
      job.join();
      IStatus result = job.getResult();
      if (result.getSeverity() == IStatus.ERROR) {
        Throwable exception = result.getException();
        if (exception instanceof IOException) {
          throw (IOException) exception;
        } else if (exception instanceof RuntimeException) {
          throw (RuntimeException) exception;
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      assert false;
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
    for (Object object : Iterator2Iterable.make((Iterator<?>) selection.iterator())) {
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
  private static AnalysisScope mergeProjectPaths(Collection<EclipseProjectPath<?, ?>> projectPaths) throws IOException {
    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();

    Collection<Module> seen = HashSetFactory.make();
    // to avoid duplicates, we first add all application modules, then
    // extension
    // modules, then primordial
    buildScope(ClassLoaderReference.Application, projectPaths, scope, seen);
    buildScope(ClassLoaderReference.Extension, projectPaths, scope, seen);
    buildScope(ClassLoaderReference.Primordial, projectPaths, scope, seen);
    return scope;
  }

  /**
   * Enhance an {@link AnalysisScope} to include in a particular loader, elements from a set of Eclipse projects
   * 
   * @param loader the class loader in which new {@link Module}s will live
   * @param projectPaths Eclipse project paths to add to the analysis scope
   * @param scope the {@link AnalysisScope} under construction. This will be mutated.
   * @param seen set of {@link Module}s which have already been seen, and should not be added to the analysis scope
   */
  private static void buildScope(ClassLoaderReference loader, Collection<EclipseProjectPath<?, ?>> projectPaths, AnalysisScope scope,
      Collection<Module> seen) throws IOException {
    for (EclipseProjectPath<?, ?> path : projectPaths) {
      AnalysisScope pScope = path.toAnalysisScope((File) null);
      for (Module m : pScope.getModules(loader)) {
        if (!seen.contains(m)) {
          seen.add(m);
          scope.addToScope(loader, m);
        }
      }
    }
  }

  /*
   * @see IActionDelegate#run(IAction)
   */
  @Override
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

  /*
   * @see IActionDelegate#selectionChanged(IAction, ISelection)
   */
  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    currentSelection = selection;
  }

  /**
   * @return The current {@link ISelection} highlighted in the Eclipse workspace
   */
  public ISelection getCurrentSelection() {
    return currentSelection;
  }
}
