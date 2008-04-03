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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;

/**
 * A {@link ContextSelector} to handle default reflection logic.
 * 
 * @author sjfink
 */
public class ReflectionContextSelector extends DelegatingContextSelector {

  public static ReflectionContextSelector createReflectionContextSelector() {
    return new ReflectionContextSelector();
  }

  /**
   * checks (1) logic for Constructor.newInstance and Method.invoke, (2) logic for other methods of Class, (3) logic for
   * Class.forName, (4) logic for Object.getClass, (5) logic for Class.newInstance, and finally (6) logic for synthetic
   * factories.
   */
  private ReflectionContextSelector() {
    super(new ReflectiveInvocationSelector(), new DelegatingContextSelector(new JavaLangClassContextSelector(),
        new DelegatingContextSelector(new DelegatingContextSelector(new DelegatingContextSelector(new ForNameContextSelector(),
            new GetClassContextSelector()), new ClassNewInstanceContextSelector()), new FactoryContextSelector())));
  }

}
