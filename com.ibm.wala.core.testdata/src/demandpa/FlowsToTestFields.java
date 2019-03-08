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
public class FlowsToTestFields {

  public static void main(String[] args) {
    Object o1 = new FlowsToType();
    Object o2 = new Object();
    A a1 = new A();
    A a2 = new A();
    a1.f = o1;
    a2.f = o2;
    Object o3 = a1.f;
    Object o4 = a2.f;
    DemandPATestUtil.makeVarUsed(o3);
    DemandPATestUtil.makeVarUsed(o4);
  }
}
