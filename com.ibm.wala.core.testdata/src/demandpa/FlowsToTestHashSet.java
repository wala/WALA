/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package demandpa;

/** @author manu */
public class FlowsToTestHashSet {

  public static void main(String[] args) {
    DummyHashSet s1 = new DummyHashSet();
    DummyHashSet s2 = new DummyHashSet();
    s1.add(new FlowsToType());
    s2.add(new Object());
    Object o1 = s1.iterator().next();
    Object o2 = s2.iterator().next();
    DemandPATestUtil.makeVarUsed(o1);
    DemandPATestUtil.makeVarUsed(o2);
  }
}
