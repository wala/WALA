/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.util;

import static com.ibm.wala.ide.util.JdtUtil.getAST;
import static com.ibm.wala.ide.util.JdtUtil.getOriginalNode;

import java.lang.ref.SoftReference;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.dom.ASTNode;

import com.ibm.wala.util.collections.HashMapFactory;

public class ASTNodeFinder {

	private final Map<IFile, SoftReference<ASTNode>> fileASTs = HashMapFactory.make();
	
	public ASTNode getASTNode(JdtPosition pos) {
		IFile sourceFile = pos.getEclipseFile();
		if (!fileASTs.containsKey(sourceFile) || fileASTs.get(sourceFile).get() == null) {
			fileASTs.put(sourceFile, new SoftReference<>(getAST(sourceFile)));
		}
		
		return getOriginalNode(fileASTs.get(sourceFile).get(), pos);
	}
	
}
