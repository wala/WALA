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
public class FlowsToTestArraySetIter {

  public static void main(String[] args) {
    ArraySet s1 = new ArraySet();
    ArraySet s2 = new ArraySet();
    s1.add(new FlowsToType());
    s2.add(new B());
    A a = (A) s1.iterator().next();
    B b = (B) s2.iterator().next();
    DemandPATestUtil.makeVarUsed(b);
    DemandPATestUtil.makeVarUsed(a);
  }
}
