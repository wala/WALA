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
package com.ibm.wala.viz;

import java.io.IOException;
import java.util.Arrays;

import com.ibm.wala.util.warnings.WalaException;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>GS View Launcher</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.viz.GSViewLauncherImpl#getDescription <em>Description</em>}</li>
 *   <li>{@link com.ibm.wala.viz.GSViewLauncherImpl#getVendor <em>Vendor</em>}</li>
 *   <li>{@link com.ibm.wala.viz.GSViewLauncherImpl#getVersion <em>Version</em>}</li>
 *   <li>{@link com.ibm.wala.viz.GSViewLauncherImpl#getPsfile <em>Psfile</em>}</li>
 *   <li>{@link com.ibm.wala.viz.GSViewLauncherImpl#getGvExe <em>Gv Exe</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GSViewLauncherImpl  {
  
  private  Process process;
  
  /**
   * The default value of the '{@link #getDescription() <em>Description</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getDescription()
   * @generated
   * @ordered
   */
  protected static final String DESCRIPTION_EDEFAULT = "Launch gsview on a postscript file";

  /**
   * The cached value of the '{@link #getDescription() <em>Description</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getDescription()
   * @generated
   * @ordered
   */
  protected String description = DESCRIPTION_EDEFAULT;

  /**
   * The default value of the '{@link #getVendor() <em>Vendor</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getVendor()
   * @generated
   * @ordered
   */
  protected static final String VENDOR_EDEFAULT = "IBM";

  /**
   * The cached value of the '{@link #getVendor() <em>Vendor</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getVendor()
   * @generated
   * @ordered
   */
  protected String vendor = VENDOR_EDEFAULT;

  /**
   * The default value of the '{@link #getVersion() <em>Version</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getVersion()
   * @generated
   * @ordered
   */
  protected static final String VERSION_EDEFAULT = "0.01";

  /**
   * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getVersion()
   * @generated
   * @ordered
   */
  protected String version = VERSION_EDEFAULT;

  /**
   * The default value of the '{@link #getPsfile() <em>Psfile</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getPsfile()
   * @generated
   * @ordered
   */
  protected static final String PSFILE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPsfile() <em>Psfile</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getPsfile()
   * @generated
   * @ordered
   */
  protected String psfile = PSFILE_EDEFAULT;

  /**
   * The default value of the '{@link #getGvExe() <em>Gv Exe</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getGvExe()
   * @generated
   * @ordered
   */
  protected static final String GV_EXE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getGvExe() <em>Gv Exe</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getGvExe()
   * @generated
   * @ordered
   */
  protected String gvExe = GV_EXE_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected GSViewLauncherImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getDescription() {
    return description;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getVendor() {
    return vendor;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getVersion() {
    return version;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getPsfile() {
    return psfile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setPsfile(String newPsfile) {
    psfile = newPsfile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getGvExe() {
    return gvExe;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setGvExe(String newGvExe) {
    gvExe = newGvExe;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (description: ");
    result.append(description);
    result.append(", vendor: ");
    result.append(vendor);
    result.append(", version: ");
    result.append(version);
    result.append(", psfile: ");
    result.append(psfile);
    result.append(", gvExe: ");
    result.append(gvExe);
    result.append(')');
    return result.toString();
  }

  private WalaException exception = null;
  
  /* 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    String[] cmdarray = { getGvExe(), getPsfile() };
    try {
      Process p = Runtime.getRuntime().exec(cmdarray);
      setProcess(p);
    } catch (IOException e) {
      e.printStackTrace();
      exception = new WalaException("gv invocation failed for\n" + Arrays.toString(cmdarray));
    }
  }
  /**
   * @return Returns the exception.
   */
  public WalaException getException() {
    return exception;
  }

  public Process getProcess() {
    return process;
  }

  public void setProcess(Process process) {
    this.process = process;
  }
} //GSViewLauncherImpl
