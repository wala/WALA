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

import org.eclipse.jface.window.ApplicationWindow;

/**
 * abstract base class for launching a JFace application
 * 
 * TODO: unify with other launchers?
 */
public abstract class EJfaceApplicationRunner  {

  /**
   * The cached value of the '{@link #getApplicationWindow() <em>Application Window</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getApplicationWindow()
   * @generated
   * @ordered
   */
  protected ApplicationWindow applicationWindow = null;

  protected boolean blockInput = false;

  protected EJfaceApplicationRunner() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ApplicationWindow getApplicationWindow() {
    return applicationWindow;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setApplicationWindow(ApplicationWindow newApplicationWindow) {
    applicationWindow = newApplicationWindow;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean isBlockInput() {
    return blockInput;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setBlockInput(boolean newBlockInput) {
    blockInput = newBlockInput;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (applicationWindow: ");
    result.append(applicationWindow);
    result.append(", blockInput: ");
    result.append(blockInput);
    result.append(')');
    return result.toString();
  }

} //EJfaceApplicationRunner
