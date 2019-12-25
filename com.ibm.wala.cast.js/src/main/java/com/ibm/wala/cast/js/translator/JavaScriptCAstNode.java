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
package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.tree.CAstNode;

public interface JavaScriptCAstNode extends CAstNode {

  public static final int ENTER_WITH = SUB_LANGUAGE_BASE + 1;

  public static final int EXIT_WITH = SUB_LANGUAGE_BASE + 2;
}
