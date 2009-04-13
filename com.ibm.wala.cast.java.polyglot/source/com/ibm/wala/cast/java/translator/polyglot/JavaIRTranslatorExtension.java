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

import polyglot.frontend.JLExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.goals.Goal;

import com.ibm.wala.cast.tree.impl.CAstRewriterFactory;

/**
 * A Polyglot extension descriptor for a test harness extension that generates DOMO IR for the sources and class files in the
 * classpath.
 * 
 * @author rfuhrer
 */
public class JavaIRTranslatorExtension extends JLExtensionInfo implements IRTranslatorExtension {
  protected PolyglotSourceLoaderImpl fSourceLoader;

  protected PolyglotIdentityMapper fMapper;

  @SuppressWarnings("unchecked")
  protected CAstRewriterFactory rewriterFactory;

  public void setSourceLoader(PolyglotSourceLoaderImpl sourceLoader) {
    fSourceLoader = sourceLoader;
    fMapper = new PolyglotIdentityMapper(sourceLoader.getReference(), typeSystem());
  }

  public Goal getCompileGoal(Job job) {
    return new IRGoal(job, fSourceLoader);
  }

  public PolyglotIdentityMapper getIdentityMapper() {
    return fMapper;
  }

  @SuppressWarnings("unchecked")
  public void setCAstRewriterFactory(CAstRewriterFactory factory) {
    rewriterFactory = factory;
  }

  @SuppressWarnings("unchecked")
  public CAstRewriterFactory getCAstRewriterFactory() {
    return rewriterFactory;
  }
}
