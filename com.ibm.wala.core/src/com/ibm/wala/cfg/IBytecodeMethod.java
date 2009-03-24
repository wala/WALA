/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cfg;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * @author omert
 *
 */
public interface IBytecodeMethod extends IMethod {

  int getBytecodeIndex(int index) throws InvalidClassFileException;

  ExceptionHandler[][] getHandlers() throws InvalidClassFileException;

  IInstruction[] getInstructions() throws InvalidClassFileException;

}
