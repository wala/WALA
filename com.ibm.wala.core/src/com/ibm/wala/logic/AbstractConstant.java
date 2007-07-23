/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

import java.util.Collection;
import java.util.Collections;


public abstract class AbstractConstant implements IConstant {
  
  public Kind getKind() {
    return Kind.CONSTANT;
   }

  public String prettyPrint(ILogicDecorator d) {
    return d.prettyPrint(this);
  }

  public Collection<Variable> getFreeVariables() {
    return Collections.emptySet();
  }

  public Collection<? extends IConstant> getConstants() {
    return Collections.singleton(this);
  }

  public Collection<? extends ITerm> getAllTerms() {
    return Collections.singleton(this);
  }
  

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();
  
}
