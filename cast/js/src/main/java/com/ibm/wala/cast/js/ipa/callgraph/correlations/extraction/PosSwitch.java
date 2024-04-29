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

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

public abstract class PosSwitch<A> {
  public abstract A caseRootPos(RootPos pos);

  public abstract A caseChildPos(ChildPos pos);

  public abstract A caseForInLoopBodyPos(ExtractionPos pos);

  public abstract A caseLabelPos(LabelPos pos);
}
