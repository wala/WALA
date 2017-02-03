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
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * A {@link Statement} representing the normal return value in a callee,
 * immediately before returning to the caller.
 */
public class NormalReturnCallee extends Statement {

  public NormalReturnCallee(CGNode node) {
    super(node);
  }

  @Override
  public Kind getKind() {
    return Kind.NORMAL_RET_CALLEE;
  }
}
