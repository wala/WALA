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
package com.ibm.wala.dynamic;

import java.io.File;
import java.util.Iterator;

import org.eclipse.emf.common.util.EList;

import com.ibm.wala.util.warnings.WalaException;

/**
 * A Java process launcher
 */
public class JavaLauncher extends Launcher {
  /**
   * The default value of the '{@link #getProgramArgs() <em>Program Args</em>}'
   * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getProgramArgs()
   * @generated
   * @ordered
   */
  protected static final String PROGRAM_ARGS_EDEFAULT = "";

  /**
   * The cached value of the '{@link #getProgramArgs() <em>Program Args</em>}'
   * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getProgramArgs()
   * @generated
   * @ordered
   */
  protected String programArgs = PROGRAM_ARGS_EDEFAULT;

  /**
   * The default value of the '{@link #getMainClass() <em>Main Class</em>}'
   * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getMainClass()
   * @generated
   * @ordered
   */
  protected static final String MAIN_CLASS_EDEFAULT = "";

  /**
   * The cached value of the '{@link #getMainClass() <em>Main Class</em>}'
   * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getMainClass()
   * @generated
   * @ordered
   */
  protected String mainClass = MAIN_CLASS_EDEFAULT;

  /**
   * The cached value of the '{@link #getClasspathEntries() <em>Classpath Entries</em>}'
   * attribute list. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @see #getClasspathEntries()
   * @generated
   * @ordered
   */
  protected EList classpathEntries = null;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public JavaLauncher() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getProgramArgs() {
    return programArgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setProgramArgs(String newProgramArgs) {
    programArgs = newProgramArgs;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setMainClass(String newMainClass) {
    mainClass = newMainClass;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public EList getClasspathEntries() {
    return classpathEntries;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (programArgs: ");
    result.append(programArgs);
    result.append(", mainClass: ");
    result.append(mainClass);
    result.append(", classpathEntries: ");
    result.append(classpathEntries);
    result.append(')');
    return result.toString();
  }

  /**
   * @return the string that identifies the java.exe file
   */
  protected String getJavaExe() {
    String java = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java.exe";
    return java;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.capa.core.EAnalysisEngine#processImpl()
   */
  public void launch() throws WalaException {

    String cp = makeClasspath();

    String heap = " -Xmx800M ";

    String cmd = getJavaExe() + heap + cp + getMainClass() + " " + getProgramArgs();

    // TODO: factor out the following!
    Process p = spawnProcess(cmd);
    Thread d1 = isCaptureOutput() ? captureStdOut(p) : drainStdOut(p);
    Thread d2 = drainStdErr(p);
    try {
      d1.join();
      d2.join();
    } catch (InterruptedException e) {
      throw new WalaException("Internal error", e);
    }
    if (isCaptureOutput()) {
      Drainer d = (Drainer)d1;
      setOutput(d.getCapture().toByteArray());
    }
  }

  private String makeClasspath() {
    if (getClasspathEntries() == null || getClasspathEntries().isEmpty()) {
      return "";
    } else {
      String cp = " -classpath ";
      for (Iterator it = getClasspathEntries().iterator(); it.hasNext();) {
        cp += (String) it.next();
        if (it.hasNext()) {
          cp += ";";
        }
      }
      cp += " ";
      return cp;
    }
  }

} // JavaLauncher
