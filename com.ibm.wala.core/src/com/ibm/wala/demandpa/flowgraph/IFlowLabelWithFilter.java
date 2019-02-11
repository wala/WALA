/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.flowgraph;

import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;

public interface IFlowLabelWithFilter extends IFlowLabel {

  public TypeFilter getFilter();
}
