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
package com.ibm.wala.automaton.tree;

import java.util.List;

import com.ibm.wala.automaton.string.*;

public class FilteredTreeTransition extends FilteredTransition implements ITreeTransition {

  public FilteredTreeTransition(IBinaryTree input, IBinaryTree output, IFilter filter, ICondition condition) {
    super(TreeTransition.createCompositeState(input),
      TreeTransition.createCompositeState(output),
      input, new ISymbol[]{output}, filter, condition);
  }

  public FilteredTreeTransition(IBinaryTree input, IBinaryTree output, IFilter filter) {
    super(TreeTransition.createCompositeState(input),
      TreeTransition.createCompositeState(output),
      input, new ISymbol[]{output}, filter);
  }

  public FilteredTreeTransition(IBinaryTree input, IBinaryTree output, ICondition condition) {
    super(TreeTransition.createCompositeState(input),
      TreeTransition.createCompositeState(output),
      input, new ISymbol[]{output}, condition);
  }

  public IBinaryTree transit(IBinaryTree tree) {
    List result = super.transit(tree);
    if (result == null) {
      return null;
    }
    else {
      return (IBinaryTree) result.get(0);
    }
  }

}
