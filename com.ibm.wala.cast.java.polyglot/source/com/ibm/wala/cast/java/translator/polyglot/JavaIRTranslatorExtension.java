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

import java.util.List;

import polyglot.frontend.Goal;
import polyglot.frontend.JLExtensionInfo;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;

import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;

/**
 * A Polyglot extension descriptor for a test harness extension that generates WALA IR for
 * the sources and class files in the classpath.
 * @author rfuhrer
 */
public class JavaIRTranslatorExtension extends JLExtensionInfo implements IRTranslatorExtension {
  protected PolyglotSourceLoaderImpl fSourceLoader;

  protected PolyglotIdentityMapper fMapper;

  protected CAstRewriterFactory<?,?> rewriterFactory;

  //PORT1.7 getCompileGoal() no longer exists; set the compile goal by manipulating the End goal for the job
  @Override
  protected Scheduler createScheduler() {
    return new JLScheduler(this) {
      @Override
      public List<Goal> goals(Job job) {
        List<Goal> goals= super.goals(job);
        Goal endGoal = goals.get(goals.size()-1);
        if (!(endGoal.name().equals("End"))) {
          throw new IllegalStateException("Last goal is not an End goal?");
        }
        endGoal.addPrereq(new IRGoal(job, fSourceLoader));
        return goals;
      }
    };
  }

  public void setSourceLoader(PolyglotSourceLoaderImpl sourceLoader) {
    fSourceLoader= sourceLoader;
    fMapper= new PolyglotIdentityMapper(sourceLoader.getReference());
  }

  public PolyglotIdentityMapper getIdentityMapper() {
    return fMapper;
  }

  public void setCAstRewriterFactory(CAstRewriterFactory<?,?> factory) {
    rewriterFactory = factory;
  }

  public CAstRewriterFactory<?,?> getCAstRewriterFactory() {
    return rewriterFactory;
  }

  public boolean getReplicateForDoLoops() {
    return false;
  }
}
