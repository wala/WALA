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

import com.ibm.wala.cast.tree.CAstEntity;

public abstract class ExtractionPolicyFactory {
  public abstract ExtractionPolicy createPolicy(CAstEntity entity);
}
