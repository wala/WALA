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
package com.ibm.wala.eclipse.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;

/**
 * Convenience methods to get information from JDT IJavaElement model.
 *  
 * @author aying
 */
public class JdtUtil {

	public static String getFilePath(IJavaElement javaElt) {
		String filePath = javaElt.getPath().toString();		
		return filePath;
	}
			
	public static String getPackageName(ICompilationUnit cu) {
		try { 
			IPackageDeclaration[] pkgDecl = cu.getPackageDeclarations();
			
			// TODO: handle default package?
			if( pkgDecl != null && pkgDecl.length > 0 ) {
				String packageName = pkgDecl[0].getElementName();
				return packageName;
			}
		} catch (JavaModelException e) {
			
		}
		return "";
		
	}
	
	public static String getFullyQualifiedClassName(IType type) {
		ICompilationUnit cu = (ICompilationUnit)type.getParent();
		String packageName = getPackageName(cu); 			
		String className = type.getElementName();
		String fullyQName = packageName + "." + className;
		return fullyQName;
	}
	
	public static String getClassName(IType type) {
		String className = type.getElementName();
		return className;
	}
		
	public static String getMethodSignature(IMethod method) {
		try {
			String methodParamReturnInfo = method.getSignature();
			String methodName = method.getElementName();
			String methodSignature = methodName + " " + methodParamReturnInfo;
			return methodSignature;
		} catch (JavaModelException e) {
		}
		return "";
	}
	
	/**
	 * Return a unique string representing the specified
	 * Java element across projects in the workspace.  
	 * The returned string can be used as a handle to create 
	 * JavaElement by 'JavaCore.create(String)'
	 * 
	 * For example, suppose we have the method
	 *    'fooPackage.barPackage.FooClass.fooMethod(int)'
	 * which is in the 'FooProject' and source folder 'src'
	 * the handle would be
	 *    '=FooProject/src<fooPackage.barPackage{FooClass.java[FooClass~fooMethod~I'   
	 *   
	 * @param javaElt
	 * @return
	 */
	public static String getJdtHandleString(IJavaElement javaElt) {
		return javaElt.getHandleIdentifier();
	}
	
	public static IJavaElement createJavaElementFromJdtHandle(String jdtHandle) {
		return JavaCore.create(jdtHandle);
	}

	public static List<ICompilationUnit> getJavaCompilationUnits(IJavaProject javaProject) {
		List<ICompilationUnit> cuResult = new ArrayList<ICompilationUnit>();
		try {
			if( javaProject.hasChildren() ) { 
	            IPackageFragmentRoot[] packFragRoots = javaProject.getPackageFragmentRoots();
				for (int i = 0; i < packFragRoots.length; i++) {
	                IPackageFragmentRoot packFragRoot = packFragRoots[i];
	                if ( packFragRoot instanceof JarPackageFragmentRoot == false
	                		&& packFragRoot.hasChildren()) {
		                IJavaElement[] packFrags = packFragRoot.getChildren();
		                for (int j = 0; j < packFrags.length; j++) {
		                    ICompilationUnit[] cus = ((IPackageFragment) packFrags[j]).getCompilationUnits();
		                    if (cus != null) {
		                    	Collections.addAll(cuResult, cus);
		                    }
		                }
	                }
				}
			}	
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cuResult;
	}

	public static IType[] getClasses(ICompilationUnit cu) {			
		try {
			return cu.getAllTypes();
		}
		catch(JavaModelException e) {
			
		}
		return null;
	}

	public static IJavaProject getProject(IJavaElement javaElt) {
		IJavaProject javaProject = javaElt.getJavaProject();
		return javaProject;
	}

	public static String getProjectName(IJavaProject javaProject) {
		return javaProject.getElementName();
	}

	
	 /**
	  * @param typeSignature Some of the type signatures examples are "QString;" (String)
	  * and "I" (int)
	  * The type signatures may be either unresolved (for source types)
	  * or resolved (for binary types), and either basic (for basic types)
	  * or rich (for parameterized types). See {@link Signature} for details.
	  */
	public static String getHumanReadableType(String typeSignature) {
		String simpleName = Signature.getSignatureSimpleName(typeSignature);
		return simpleName;
	}
	
	  
	  public static IJavaProject getJavaProject(IFile appJar) {
		  String projectName = appJar.getProject().getName();
		  IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		  IJavaModel javaModel = JavaCore.create(workspaceRoot);
		  IJavaProject javaProject = javaModel.getJavaProject(projectName);
		  return javaProject;
	  }

		public static IJavaProject getHelloWorldProject() {
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IJavaModel javaModel = JavaCore.create(workspaceRoot);
			IJavaProject helloWorldProject = javaModel.getJavaProject("HelloWorld");
			return helloWorldProject;
		}

		public static IFile getHelloWorldJar() {
			IJavaProject project = getHelloWorldProject();
			IFile helloWorldJar = project.getProject().getFile("helloWorld.jar");
			return helloWorldJar;
		}
}
