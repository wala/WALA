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
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.VisitorGoal;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.AscriptionVisitor;

/**
 * Runs an AscriptionVisitor to make sure that empty array literals actually get a type.
 * @author rfuhrer
 */
public class AscriptionGoal extends VisitorGoal {
  /**
   * 
   */
  private static final long serialVersionUID = 7416951196743862079L;

  public AscriptionGoal(Job job) {
    super(job,
      new AscriptionVisitor(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory()) {
        @Override
        public Expr ascribe(Expr e, Type toType) throws SemanticException {
          if (e instanceof ArrayInit && e.type().isNull()) {
            return e.type(toType);
          }
          return super.ascribe(e, toType);
        }
    }
    );
    Scheduler scheduler= job.extensionInfo().scheduler();

	addPrereq(scheduler.TypeChecked(job));
  }
}
