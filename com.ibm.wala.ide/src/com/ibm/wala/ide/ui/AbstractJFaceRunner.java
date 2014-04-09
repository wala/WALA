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
package com.ibm.wala.ide.ui;

import org.eclipse.jface.window.ApplicationWindow;

/**
 * abstract base class for launching a JFace application
 * 
 * TODO: unify with other launchers?
 */
public abstract class AbstractJFaceRunner  {

  protected ApplicationWindow applicationWindow = null;

  protected boolean blockInput = false;

  protected AbstractJFaceRunner() {
    super();
  }

  public ApplicationWindow getApplicationWindow() {
    return applicationWindow;
  }

  public void setApplicationWindow(ApplicationWindow newApplicationWindow) {
    applicationWindow = newApplicationWindow;
  }

  public boolean isBlockInput() {
    return blockInput;
  }

  public void setBlockInput(boolean newBlockInput) {
    blockInput = newBlockInput;
  }


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
} 
