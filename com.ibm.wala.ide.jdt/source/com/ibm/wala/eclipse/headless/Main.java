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
package com.ibm.wala.eclipse.headless;

import com.ibm.wala.ide.util.EclipseProjectPath.AnalysisScopeType;
import com.ibm.wala.ide.util.JavaEclipseProjectPath;
import com.ibm.wala.ide.util.JdtUtil;
import java.util.Collection;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.IJavaProject;

/**
 * A dummy main class that runs WALA in a headless Eclipse platform.
 *
 * <p>This is for expository purposes, and tests some WALA eclipse functionality.
 *
 * @author sjfink
 */
public class Main implements IApplication {

  @Override
  public Object start(IApplicationContext context) throws Exception {
    Collection<IJavaProject> jp = JdtUtil.getWorkspaceJavaProjects();
    for (IJavaProject p : jp) {
      System.out.println(p);
      JavaEclipseProjectPath path =
          JavaEclipseProjectPath.make(p, AnalysisScopeType.SOURCE_FOR_PROJ_AND_LINKED_PROJS);
      System.out.println("Path: " + path);
    }
    return null;
  }

  @Override
  public void stop() {}
}
