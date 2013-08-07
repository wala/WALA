package com.ibm.wala.ide.jsdt.tests;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;

import com.ibm.wala.cast.js.client.EclipseJavaScriptAnalysisEngine;
import com.ibm.wala.cast.js.client.EclipseWebAnalysisEngine;
import com.ibm.wala.ide.util.EclipseWebProjectPath;

public class WLProjectWebScopeTest extends WLProjectScopeTest {

  @Override
  protected EclipseWebProjectPath makeProjectPath(IJavaScriptProject p) throws IOException, CoreException {
    return EclipseWebProjectPath.make(p, Collections.EMPTY_SET);
  }

  @Override
  protected EclipseJavaScriptAnalysisEngine makeAnalysisEngine(IJavaScriptProject p) throws IOException, CoreException {
    return new EclipseWebAnalysisEngine(p, Collections.EMPTY_SET);
  }

 
}
