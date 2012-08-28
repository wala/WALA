package com.ibm.wala.ide.util;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class JavaEclipseProjectPath extends EclipseProjectPath<IClasspathEntry, IJavaProject> {

  protected JavaEclipseProjectPath(IProject project, com.ibm.wala.ide.util.EclipseProjectPath.AnalysisScopeType scopeType)
      throws IOException, CoreException {
    super(project, scopeType);
  }

  public static JavaEclipseProjectPath make(IJavaProject p, AnalysisScopeType scopeType) throws IOException, CoreException {
    return new JavaEclipseProjectPath(p.getProject(), scopeType);
  }

  @Override
  protected IJavaProject makeProject(IProject p) {
    try {
      if (p.hasNature(JavaCore.NATURE_ID)) {
        return JavaCore.create(p);
      }
    } catch (CoreException e) {
      // not a Java project
    } 
    return null;
  }

  @Override
  protected IClasspathEntry resolve(IClasspathEntry entry) {
    return JavaCore.getResolvedClasspathEntry(entry);
  }

  @Override
  protected void resolveClasspathEntry(IJavaProject project, IClasspathEntry entry,
      com.ibm.wala.ide.util.EclipseProjectPath.ILoader loader, boolean includeSource, boolean cpeFromMainProject) {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void resolveProjectClasspathEntries(IJavaProject project, boolean includeSource) {
    // TODO Auto-generated method stub
    
  }

}
