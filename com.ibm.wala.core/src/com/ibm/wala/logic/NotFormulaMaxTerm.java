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


/**
 * An irreducible Not Formula.
 * 
 * use carefully.
 * 
 * @author sjfink
 */
public class NotFormulaMaxTerm extends NotFormula implements IMaxTerm {

  public static NotFormulaMaxTerm createNotFormulaMaxTerm(RelationFormula f) {
    return new NotFormulaMaxTerm(f);
  }

  private NotFormulaMaxTerm(IFormula f) {
    super(f);
  }
  
  public Collection<? extends IMaxTerm> getMaxTerms() {
    return Collections.singleton(this);
  }

 
}
