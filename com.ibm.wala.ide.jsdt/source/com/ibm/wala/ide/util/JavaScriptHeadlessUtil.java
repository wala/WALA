/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

public class JavaScriptHeadlessUtil extends HeadlessUtil {

	  public static IJavaScriptProject getJavaScriptProjectFromWorkspace(final String projectName) {
		    IJavaScriptProject jp = getProjectFromWorkspace(p -> {
          try {
            if (p.hasNature(JavaScriptCore.NATURE_ID)) {
              IJavaScriptProject jp1 = JavaScriptCore.create(p);
               if (jp1 != null && jp1.getElementName().equals(projectName)) {
                return jp1;
              }
            }
          } catch (CoreException e) {
          }
          // failed to match
          return null;
        });
		    return jp;
		  }

}
