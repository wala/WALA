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
package com.ibm.wala.stringAnalysis.translator;

import com.ibm.wala.automaton.grammar.string.IGrammar;
import com.ibm.wala.cfg.IBasicBlock;

public interface IBB2Grammar {
    ISSA2Rule getSSA2Rule();
    IGrammar translate(IBasicBlock bb, TranslationContext ctx);
}
