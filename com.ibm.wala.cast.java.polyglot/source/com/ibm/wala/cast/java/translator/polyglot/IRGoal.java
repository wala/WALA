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

import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.SourceGoal_c;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.Java2IRTranslator;

/**
 * A kind of EndGoal that indicates that WALA IR has been generated for the given compilation unit.
 * @author rfuhrer
 */
public class IRGoal extends SourceGoal_c /* PORT1.7 removed 'implements EndGoal' */ {
  /**
   * 
   */
  private static final long serialVersionUID = -8023929848709826817L;

  private JavaSourceLoaderImpl fSourceLoader;

  protected Java2IRTranslator fTranslator;

  public IRGoal(Job job, JavaSourceLoaderImpl sourceLoader) {
    super(job);
    fSourceLoader = sourceLoader;

    Scheduler scheduler= job.extensionInfo().scheduler();

    addPrereq(scheduler.TypeChecked(job));
    // PORT1.7 - TypeChecked will suffice for what used to require ConstantsChecked.
    // Need ConstantsChecked in order to make sure that case statements have non-zero labels.
//  addPrereq(scheduler.ConstantsChecked(job));
    // Need to add an AscriptionGoal as a prereq to make sure that empty array initializers get a type ascribed.
    addPrereq(new AscriptionGoal(job));
  }

  @Override
  public boolean runTask() {
    ExtensionInfo extInfo= job.extensionInfo();

    fTranslator= new Java2IRTranslator(
            fSourceLoader,
            ((IRTranslatorExtension)extInfo).getCAstRewriterFactory());
    ModuleSource src = (ModuleSource) job.source();
    fTranslator.translate( 
        src.getModule(),
        new PolyglotJava2CAstTranslator(
            job.ast(),
            fSourceLoader.getReference(),
            extInfo.nodeFactory(),
            extInfo.typeSystem(),
            new PolyglotIdentityMapper(fSourceLoader.getReference()),
            ((IRTranslatorExtension)extInfo).getReplicateForDoLoops()).translateToCAst());
    return true;
  }

  @Override
  public String name() {
    return "<WALA IR goal for " + job.source().path() + ">";
  }
}
