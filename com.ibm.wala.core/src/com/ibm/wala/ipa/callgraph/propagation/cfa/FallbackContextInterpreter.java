/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.EmptyIterator;


/**
 * This ContextInterpreter can be used when using another WALA frontend than the shrike frontend. WALA's standard ContextInterpreters, like
 * e.g. DefaultSSAInterpreter delegate to CodeScanner, which assumes, that the provided methods are instances of shrike classes.
 * When using these ContextInterpreter with another frontend than shrike, this leads to ClassCastExceptions. This class can be used to
 * work around this issue. It delegates to a given ContextInterpreter, if the CGNode's IMethod is a Shrike class. 
 * Otherwise, it retrieves the required information from the CGNode's IR, which should always work.
 * 
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 * 
 * This class is provided by the JOANA project (joana.ipd.kit.edu).
 */
public class FallbackContextInterpreter implements SSAContextInterpreter {

	private SSAContextInterpreter shrikeCI;

	public FallbackContextInterpreter(SSAContextInterpreter shrikeCI) {
		this.shrikeCI = shrikeCI;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateNewSites(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
		if (node.getMethod() instanceof SyntheticMethod || node.getMethod() instanceof ShrikeCTMethod) {
			return shrikeCI.iterateNewSites(node);
		} else {
			IRView ir = getIR(node);
		    if (ir == null) {
		      return EmptyIterator.instance();
		    } else {
		      return ir.iterateNewSites();
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateFieldsRead(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
		return shrikeCI.iterateFieldsRead(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateFieldsWritten(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
		return shrikeCI.iterateFieldsWritten(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter#iterateCallSites(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
		if (node.getMethod() instanceof SyntheticMethod || node.getMethod() instanceof ShrikeCTMethod) {
			return shrikeCI.iterateCallSites(node);
		} else {
			IRView ir = getIR(node);
		    if (ir == null) {
		      return EmptyIterator.instance();
		    } else {
		      return ir.iterateCallSites();
		    }
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.IClass)
	 */
	@Override
	public boolean recordFactoryType(CGNode node, IClass klass) {
		return shrikeCI.recordFactoryType(node, klass);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.cha.CHAContextInterpreter#understands(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public boolean understands(CGNode node) {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getIR(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public IR getIR(CGNode node) {
		return shrikeCI.getIR(node);
	}

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getDU(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public DefUse getDU(CGNode node) {
		return shrikeCI.getDU(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public int getNumberOfStatements(CGNode node) {
		return shrikeCI.getNumberOfStatements(node);
	}

	/* (non-Javadoc)
	 * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getCFG(com.ibm.wala.ipa.callgraph.CGNode)
	 */
	@Override
	public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
		return shrikeCI.getCFG(n);
	}
}
