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
package com.ibm.wala.cast.tree.rewrite;

import com.ibm.wala.cast.tree.CAst;

public interface CAstRewriterFactory<
    C extends CAstRewriter.RewriteContext<K>, K extends CAstRewriter.CopyKey<K>> {

  public CAstRewriter<C, K> createCAstRewriter(CAst ast);
}
