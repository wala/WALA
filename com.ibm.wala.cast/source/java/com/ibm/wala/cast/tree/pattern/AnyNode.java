/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.tree.pattern;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * A node pattern that matches any AST node.
 *
 * @author mschaefer
 */
public class AnyNode implements NodePattern {
  @Override
  public boolean matches(CAstNode node) {
    return true;
  }
}
