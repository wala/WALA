/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package demandpa;

import java.util.HashSet;

/**
 * @author manu
 *
 */
public class FlowsToTestHashSet {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    HashSet s1 = new HashSet();
    HashSet s2 = new HashSet();
    s1.add(new FlowsToType());
    s2.add(new Object());
    Object o1 = s1.iterator().next();
    Object o2 = s2.iterator().next();
    TestUtil.makeVarUsed(o1);
    TestUtil.makeVarUsed(o2);
  }
}
