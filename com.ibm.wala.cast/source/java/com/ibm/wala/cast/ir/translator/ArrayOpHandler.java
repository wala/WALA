/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.tree.CAstNode;

public interface ArrayOpHandler {
  void doArrayRead(
      WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues);

  void doArrayWrite(
      WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval);
}
