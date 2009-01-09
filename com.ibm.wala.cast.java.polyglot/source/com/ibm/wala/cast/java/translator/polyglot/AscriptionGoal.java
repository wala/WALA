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
package com.ibm.wala.cast.java.translator.polyglot;

import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Pass;
import polyglot.frontend.Scheduler;
import polyglot.frontend.VisitorPass;
import polyglot.frontend.goals.AbstractGoal;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.ErrorInfo;
import polyglot.visit.AscriptionVisitor;

/**
 * Runs an AscriptionVisitor to make sure that empty array literals actually get a type.
 * @author rfuhrer
 */
public class AscriptionGoal extends AbstractGoal {
    public AscriptionGoal(Job job) {
	super(job);
	try {
	    Scheduler scheduler= job.extensionInfo().scheduler();

	    addPrerequisiteGoal(scheduler.TypeChecked(job), scheduler);
	} catch (CyclicDependencyException e) {
	    job.compiler().errorQueue().enqueue(ErrorInfo.INTERNAL_ERROR, "Cycle encountered in goal graph?");
	    throw new IllegalStateException(e.getMessage());
	}
    }

    public Pass createPass(ExtensionInfo extInfo) {
	return new VisitorPass(this,
	    new AscriptionVisitor(job(), extInfo.typeSystem(), extInfo.nodeFactory()) {
	    	public Expr ascribe(Expr e, Type toType) throws SemanticException {
	    	    if (e instanceof ArrayInit && e.type().isNull()) {
	    		return e.type(toType);
	    	    }
	    	    return super.ascribe(e, toType);
	    	}
	});
    }
}
