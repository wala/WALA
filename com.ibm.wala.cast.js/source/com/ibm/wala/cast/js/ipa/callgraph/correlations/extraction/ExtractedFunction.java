/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.Rewrite;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * A simple implementation of {@link CAstEntity} used by the {@link ClosureExtractor}.
 * 
 * @author mschaefer
 *
 */
class ExtractedFunction implements CAstEntity {
	private final String name;
	private String[] parms;
	private final ExtractionPos pos;
	private Rewrite r;
	private CAstNode root;
	private CAstControlFlowMap cfg;
	private CAstSourcePositionMap posmap;
	private CAstNodeTypeMap types;
	private Map<CAstNode, Collection<CAstEntity>> scopedEntities;
	
	public ExtractedFunction(String name, ExtractionPos pos) {
		this.name = name;
		this.pos = pos;
	}
	
	public void setRewrite(Rewrite r) {
		assert this.r == null : "Rewrite shouldn't be set more than once.";
		this.r = r;
		this.root = r.newRoot();
		this.cfg = r.newCfg();
		this.posmap = r.newPos();
		this.types = r.newTypes();
	}
	
	@Override
  public int getKind() {
		return CAstEntity.FUNCTION_ENTITY;
	}

	@Override
  public String getName() {
		return name;
	}

	@Override
  public String getSignature() {
		return null;
	}

	@Override
  public String[] getArgumentNames() {
		computeParms();
		return parms;
	}

	@Override
  public CAstNode[] getArgumentDefaults() {
		return new CAstNode[0];
	}

	@Override
  public int getArgumentCount() {
		computeParms();
		return parms.length;
	}
	
	private void computeParms() {
		if(this.parms == null) {
		  ArrayList<String> parms = new ArrayList<>();
		  parms.add(name);
		  parms.add("this");
		  parms.addAll(pos.getParameters());
			if(pos.containsThis())
			  parms.add(pos.getThisParmName());
			this.parms = parms.toArray(new String[0]);
		}
	}

	@Override
  public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
		if(scopedEntities == null) {
			scopedEntities = HashMapFactory.make();
			// first, add all existing entities inside the body of this for loop
			for(Entry<CAstNode, Collection<CAstEntity>> e : r.newChildren().entrySet()) {
				if(NodePos.inSubtree(e.getKey(), root)) {
					Collection<CAstEntity> c = scopedEntities.get(e.getKey());
					if(c == null)
						scopedEntities.put(e.getKey(), c = HashSetFactory.make());
					c.addAll(e.getValue());					
				}
			}
			// now, add all new entities which arise from extracted nested for-in loops
			for(ExtractionPos nested_loop : Iterator2Iterable.make(pos.getNestedLoops())) {
				CAstNode callsite = nested_loop.getCallSite();
				CAstEntity scoped_entity = nested_loop.getExtractedEntity();
				Collection<CAstEntity> c = scopedEntities.get(callsite);
				if(c == null)
					scopedEntities.put(callsite, c = HashSetFactory.make());
				c.add(scoped_entity);
			}
		}
		return scopedEntities;
	}

	@Override
  public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
		if(getAllScopedEntities().containsKey(construct)) {
	        return getAllScopedEntities().get(construct).iterator();							
		} else {
			return EmptyIterator.instance();
		}
	}

	@Override
  public CAstNode getAST() {
		return root;
	}

	@Override
  public CAstControlFlowMap getControlFlow() {
		return cfg;
	}

	@Override
  public CAstSourcePositionMap getSourceMap() {
		return posmap;
	}

	@Override
  public Position getPosition() {
		return getSourceMap().getPosition(root);
	}

	@Override
  public CAstNodeTypeMap getNodeTypeMap() {
		return types;
	}

	@Override
  public Collection<CAstQualifier> getQualifiers() {
		return null;
	}

	@Override
  public CAstType getType() {
		return null;
	}

	@Override
  public Collection<CAstAnnotation> getAnnotations() {
   return null;
  }

  @Override
	public String toString() {
		return "<JS function " + name + ">";
	}
}
