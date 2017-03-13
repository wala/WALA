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

import com.ibm.wala.cast.tree.CAstNode;

/**
 * A node labeller keeps a mapping from nodes to integers to allow consistent labelling of nodes.
 * 
 * @author mschaefer
 *
 */
public class NodeLabeller {
	private ArrayList<CAstNode> nodes = new ArrayList<>();
	
	/**
	 * Adds a node to the mapping if it is not present yet.
	 * 
	 * @param node the node to add
	 * @return the node's label
	 */
	public int addNode(CAstNode node) {
		int label = getLabel(node);
		if(label == -1) {
			label = nodes.size();
			nodes.add(node);
		}
		return label;
	}
	
	/**
	 * Determines the label of a node in the mapping.
	 * 
	 * @param node the node whose label is to be determined
	 * @return if the node is mapped, returns its label; otherwise, returns -1
	 */
	public int getLabel(CAstNode node) {
		return nodes.indexOf(node);
	}
	
	/**
	 * Determines the node associated with a given label.
	 */
	public CAstNode getNode(int label) {
		return nodes.get(label);
	}
}
