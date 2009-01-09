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
/*
 * Created on Oct 6, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import polyglot.frontend.AbstractPass;
import polyglot.frontend.Job;
import polyglot.frontend.goals.Goal;

import com.ibm.wala.cast.java.translator.Java2IRTranslator;

/**
 * A Pass that creates DOMO IR for the given Java compilation unit.
 * @author rfuhrer
 */
public final class JavaIRPass extends AbstractPass {
    private final Job fJob;
    private final Java2IRTranslator fTranslator;

    public JavaIRPass(Goal goal, Job job, Java2IRTranslator translator) {
	super(goal);
	this.fJob= job;
	this.fTranslator= translator;
    }

    public boolean run() {
	fTranslator.translate(fJob.ast(), fJob.source().name());
	return true;
    }
}
