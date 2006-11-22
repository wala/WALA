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
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EJface Application Runner</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.viz.EJfaceApplicationRunner#getApplicationWindow <em>Application Window</em>}</li>
 *   <li>{@link com.ibm.wala.viz.EJfaceApplicationRunner#isBlockInput <em>Block Input</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public abstract class EJfaceApplicationRunner  {
  /**
   * The default value of the '{@link #getApplicationWindow() <em>Application Window</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getApplicationWindow()
   * @generated
   * @ordered
   */
  protected static final ApplicationWindow APPLICATION_WINDOW_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getApplicationWindow() <em>Application Window</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getApplicationWindow()
   * @generated
   * @ordered
   */
  protected ApplicationWindow applicationWindow = APPLICATION_WINDOW_EDEFAULT;

  /**
   * The default value of the '{@link #isBlockInput() <em>Block Input</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isBlockInput()
   * @generated
   * @ordered
   */
  protected static final boolean BLOCK_INPUT_EDEFAULT = false;

  /**
   * The cached value of the '{@link #isBlockInput() <em>Block Input</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isBlockInput()
   * @generated
   * @ordered
   */
  protected boolean blockInput = BLOCK_INPUT_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
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
