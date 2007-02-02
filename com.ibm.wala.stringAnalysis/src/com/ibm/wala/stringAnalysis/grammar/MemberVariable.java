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
package com.ibm.wala.stringAnalysis.grammar;

import com.ibm.wala.automaton.string.*;

public class MemberVariable extends PrefixedSymbol implements IVariable {
  public MemberVariable(IVariable prefix, ISymbol member) {
      super(prefix, member);
  }
  
  public IVariable getReceiver() {
    return (IVariable) getPrefix();
  }
  
  public ISymbol getMember() {
    return getLocal();
  }
  
  public String toString() {
      return getPrefix().toString() + "." + getLocal().toString();
  }
}

/*
public class MemberVariable extends VariableWrapper implements IVariable {
  private ISymbol member;
    
  public MemberVariable(IVariable prefix, ISymbol member) {
    super(prefix);
    this.member = member;
  }
  
  public ISymbol getMember() {
    return member;
  }
  
  public IVariable getReceiver() {
    return getVariable();
  }

  public String toString() {
      return super.toString() + "." + member.toString();
  }
}
*/
