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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorLimitError;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StdErrorQueue;

/**
 * Enhancement of core Polyglot compiler that takes as input a Collection of StreamSources. Identical to Compiler in all other
 * respects.
 * 
 * @author rfuhrer
 */
public final class PolyglotFrontEnd extends Compiler {
  public PolyglotFrontEnd(ExtensionInfo info, ErrorQueue eq) {
    super(info, eq);
  }

  public PolyglotFrontEnd(ExtensionInfo info) {
    this(info, new StdErrorQueue(System.err, 1000 * 1000, info.compilerName()));
  }

  @SuppressWarnings("unchecked")
  public boolean compile(Collection/* <StreamSource> */sources) {
    boolean okay = false;

    try {
      try {
        Scheduler scheduler = sourceExtension().scheduler();
        List<Job> jobs = new ArrayList<Job>();

        // First, create a goal to compile every source file.
        for (Iterator i = sources.iterator(); i.hasNext();) {
          StreamSource source = (StreamSource) i.next();

          // mark this source as being explicitly specified by the user.
          source.setUserSpecified(true);

          // Add a new SourceJob for the given source. If a Job for the source
          // already exists, then we will be given the existing job.
          Job job = scheduler.addJob(source);
          jobs.add(job);

          // Now, add a goal for completing the job.
          scheduler.addGoal(sourceExtension().getCompileGoal(job));
        }

        scheduler.setCommandLineJobs(jobs);

        // Then, compile the files to completion.
        okay = scheduler.runToCompletion();
      } catch (InternalCompilerError e) {
        // Report it like other errors, but rethrow to get the stack trace.
        try {
          errorQueue().enqueue(ErrorInfo.INTERNAL_ERROR, e.message(), e.position());
        } catch (ErrorLimitError e2) {
        }
        errorQueue().flush();
        throw e;
      } catch (RuntimeException e) {
        // Flush the error queue, then rethrow to get the stack trace.
        errorQueue().enqueue(ErrorInfo.INTERNAL_ERROR, "Internal polyglot compiler error: " + e.getMessage(),
            Position.COMPILER_GENERATED);
        errorQueue().flush();
        throw e;
      }
    } catch (ErrorLimitError e) {
    }
    errorQueue().flush();

    for (Iterator i = allExtensions().iterator(); i.hasNext();) {
      ExtensionInfo ext = (ExtensionInfo) i.next();
      ext.getStats().report();
    }
    return okay;
  }
}
