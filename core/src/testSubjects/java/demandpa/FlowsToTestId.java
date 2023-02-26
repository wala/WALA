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
public class FlowsToTestId {

  static Object id(Object o) {
    return o;
  }

  public static void main(String[] args) {
    Object o1 = new FlowsToType();
    Object o2 = new Object();
    Object o3 = id(o1);
    Object o4 = id(o2);
    DemandPATestUtil.makeVarUsed(o3);
    DemandPATestUtil.makeVarUsed(o4);
  }
}
