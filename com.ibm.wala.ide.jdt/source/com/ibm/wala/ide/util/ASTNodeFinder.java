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
			fileASTs.put(sourceFile, new SoftReference<ASTNode>(getAST(sourceFile)));
		}
		
		return getOriginalNode(fileASTs.get(sourceFile).get(), pos);
	}
	
}
