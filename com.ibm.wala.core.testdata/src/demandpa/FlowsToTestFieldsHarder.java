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
public class FlowsToTestFieldsHarder {

  public static void main(String[] args) {
    Object o1 = new FlowsToType();
    A a1 = new A();
    a1.f = o1;
    A a2 = new A();
    a2.f = a1;
    A a3 = (A) a2.f;
    Object o2 = a3.f;
    DemandPATestUtil.makeVarUsed(o2);
  }
}
