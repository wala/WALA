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
package com.ibm.wala.cast.tree.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;

public class DelegatingEntity implements CAstEntity {
    private final CAstEntity base;
    
    public DelegatingEntity(CAstEntity base) {
	this.base = base;
    }

    public int getKind() {
	return base.getKind();
    }

    public String getName() {
	return base.getName();
    }

    public String getSignature() {
	return base.getSignature();
    }

    public String[] getArgumentNames() {
	return base.getArgumentNames();
    }

    public CAstNode[] getArgumentDefaults() {
	return base.getArgumentDefaults();
    }

    public int getArgumentCount() {
	return base.getArgumentCount();
    }

    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
	return base.getAllScopedEntities();
    }

    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
	return base.getScopedEntities(construct);
    }

    public CAstNode getAST() {
	return base.getAST();
    }

    public CAstControlFlowMap getControlFlow() {
	return base.getControlFlow();
    }

    public CAstSourcePositionMap getSourceMap() {
	return base.getSourceMap();
    }

    public CAstSourcePositionMap.Position getPosition() {
	return base.getPosition();
    }

    public CAstNodeTypeMap getNodeTypeMap() {
	return base.getNodeTypeMap();
    }

    public Collection<CAstQualifier> getQualifiers() {
	return base.getQualifiers();
    }

    public CAstType getType() {
	return base.getType();
    }

    public Collection<CAstAnnotation> getAnnotations() {
      return base.getAnnotations();
    }

}
