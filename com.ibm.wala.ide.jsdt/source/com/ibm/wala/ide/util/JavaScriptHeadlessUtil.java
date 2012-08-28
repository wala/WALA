package com.ibm.wala.ide.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.jsdt.core.IJavaScriptProject;
import org.eclipse.wst.jsdt.core.JavaScriptCore;

import com.ibm.wala.util.functions.Function;

public class JavaScriptHeadlessUtil extends HeadlessUtil {

	  public static IJavaScriptProject getJavaScriptProjectFromWorkspace(final String projectName) {
		    IJavaScriptProject jp = getProjectFromWorkspace(new Function<IProject, IJavaScriptProject>() {
		      public IJavaScriptProject apply(IProject p) {
		        try {
		          if (p.hasNature(JavaScriptCore.NATURE_ID)) {
		            IJavaScriptProject jp = JavaScriptCore.create(p);
		             if (jp != null && jp.getElementName().equals(projectName)) {
		              return jp;
		            }
		          }
		        } catch (CoreException e) {
		        }
		        // failed to match
		        return null;
		      }
		    });
		    return jp;
		  }

}
