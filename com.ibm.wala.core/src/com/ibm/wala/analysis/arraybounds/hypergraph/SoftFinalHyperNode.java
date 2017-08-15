/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.arraybounds.hypergraph;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;

public class SoftFinalHyperNode<T> extends HyperNode<T> {

  public SoftFinalHyperNode(T value) {
    super(value);
  }

  @Override
  public void setWeight(Weight weight) {
    if (weight.equals(Weight.NOT_SET) || this.getWeight().equals(Weight.NOT_SET)) {
      super.setWeight(weight);
    }
  }
}
