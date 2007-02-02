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

import java.io.Reader;

import com.ibm.wala.util.debug.Assertions;

import polyglot.frontend.*;
import polyglot.frontend.goals.Goal;
import polyglot.util.ErrorQueue;

/**
 * A Polyglot extension descriptor for a test harness extension that generates DOMO IR for
 * the sources and class files in the classpath.
 * @author rfuhrer
 */
public class JavaIRTranslatorExtension extends JLExtensionInfo implements IRTranslatorExtension {
    protected PolyglotSourceLoaderImpl fSourceLoader;

    public void setSourceLoader(PolyglotSourceLoaderImpl sourceLoader) {
	fSourceLoader= sourceLoader;
    }

    public Goal getCompileGoal(Job job) {
	return new IRGoal(job, fSourceLoader);
    }

}
