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
package com.ibm.wala.automaton.string;

import java.util.*;

public interface ITransition extends Cloneable {
    public IState getPreState();
    public IState getPostState();
    public void setPreState(IState state);
    public void setPostState(IState state);
    public ISymbol getInputSymbol();
    public void setInputSymbol(ISymbol s);
    public Iterator getOutputSymbols();
    public boolean hasOutputSymbols();
    public void appendOutputSymbols(List outputs);
    public void prependOutputSymbols(List outputs);
    public boolean isEpsilonTransition();
    public boolean accept(ISymbol symbol, IMatchContext ctx);
    public List transit(ISymbol symbol);
    public ITransition copy(ITransitionCopier copier);
    public Object clone();
}
