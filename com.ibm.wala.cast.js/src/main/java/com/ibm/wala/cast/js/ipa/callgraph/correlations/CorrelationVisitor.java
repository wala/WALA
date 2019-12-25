/*
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.cast.js.ipa.callgraph.correlations;

/**
 * Visitor class for performing case analysis on {@link Correlation}s.
 *
 * @author mschaefer
 */
public interface CorrelationVisitor<T> {
  public T visitReadWriteCorrelation(ReadWriteCorrelation rwc);

  public T visitEscapeCorrelation(EscapeCorrelation ec);
}
