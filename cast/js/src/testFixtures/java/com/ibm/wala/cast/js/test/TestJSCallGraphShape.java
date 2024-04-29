/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.util.test.TestCallGraphShape;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import java.util.Collection;

public abstract class TestJSCallGraphShape extends TestCallGraphShape {

  @Override
  public Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier) {
    return JSCallGraphUtil.getNodes(CG, functionIdentifier);
  }
}
