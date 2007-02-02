/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.stringAnalysis.js.test.translator;

import com.ibm.wala.stringAnalysis.translator.*;

public class TestLexicalGR2CFG extends TestGR2CFG {
  protected ISSA2Rule createSSA2Rule() {
    //return new SSA2LexicalRule(super.createSSA2Rule());
    return super.createSSA2Rule();
  }
}
