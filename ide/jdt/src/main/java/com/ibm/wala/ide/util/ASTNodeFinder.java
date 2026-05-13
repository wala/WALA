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
package com.ibm.wala.ide.util;

import static com.ibm.wala.ide.util.JdtUtil.getAST;
import static com.ibm.wala.ide.util.JdtUtil.getOriginalNode;

import com.ibm.wala.util.collections.HashMapFactory;
import java.lang.ref.SoftReference;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;
import org.jspecify.annotations.NonNull;

public class ASTNodeFinder {

  private final Map<IFile, @NonNull SoftReference<ASTNode>> fileASTs = HashMapFactory.make();

  public ASTNode getASTNode(JdtPosition pos) {
    return getOriginalNode(
        fileASTs
            .compute(
                pos.getEclipseFile(),
                (key, priorReference) ->
                    priorReference == null || priorReference.get() == null
                        ? new SoftReference<>(getAST(key))
                        : priorReference)
            .get(),
        pos);
  }
}
