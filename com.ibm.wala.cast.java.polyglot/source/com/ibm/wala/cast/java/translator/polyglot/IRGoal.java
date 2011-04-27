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
 * 
 * @author rfuhrer
 */
public class IRGoal extends AbstractGoal implements EndGoal {
  private JavaSourceLoaderImpl fSourceLoader;

  public IRGoal(Job job, JavaSourceLoaderImpl sourceLoader) {
    super(job);
    fSourceLoader = sourceLoader;
    try {
      Scheduler scheduler = job.extensionInfo().scheduler();

      addPrerequisiteGoal(scheduler.TypeChecked(job), scheduler);
      // Need ConstantsChecked in order to make sure that case statements have non-zero labels.
      addPrerequisiteGoal(scheduler.ConstantsChecked(job), scheduler);
      // Need to add an AscriptionGoal as a prereq to make sure that empty array initializers get a type ascribed.
      addPrerequisiteGoal(new AscriptionGoal(job), scheduler);
    } catch (CyclicDependencyException e) {
      job.compiler().errorQueue().enqueue(ErrorInfo.INTERNAL_ERROR, "Cycle encountered in goal graph?");
      throw new IllegalStateException(e.getMessage());
    }
  }

  public Pass createPass(polyglot.frontend.ExtensionInfo extInfo) {
    return new JavaIRPass(this, job(), new Java2IRTranslator(new PolyglotJava2CAstTranslator(fSourceLoader.getReference(), extInfo
        .nodeFactory(), extInfo.typeSystem(), new PolyglotIdentityMapper(fSourceLoader.getReference(), this.job.extensionInfo()
        .typeSystem())), fSourceLoader, ((IRTranslatorExtension) extInfo).getCAstRewriterFactory()));
  }

  public String name() {
    return "<DOMO IR goal for " + job().source().path() + ">";
  }
}
