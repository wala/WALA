package com.ibm.wala.ide.jsdt.tests;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.js.client.EclipseJavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.client.EclipseWebAnalysisEngine;
import com.ibm.wala.ide.util.EclipseWebProjectPath;
import com.ibm.wala.ide.util.JavaScriptEclipseProjectPath;

public class WLProjectWebScopeTest extends WLProjectScopeTest {

  @Override
  protected JavaScriptEclipseProjectPath makeProjectPath(IJavaScriptProject p) throws IOException, CoreException {
    return new EclipseWebProjectPath(p);
  }

  @Override
  protected EclipseJavaScriptAnalysisEngine makeAnalysisEngine(IJavaScriptProject p) throws IOException, CoreException {
    return new EclipseWebAnalysisEngine(p);
  }

 
}
