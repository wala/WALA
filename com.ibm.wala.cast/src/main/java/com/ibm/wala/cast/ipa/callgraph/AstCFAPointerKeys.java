/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultPointerKeyFactory;

public class AstCFAPointerKeys extends DelegatingAstPointerKeys {

  public AstCFAPointerKeys() {
    super(new DefaultPointerKeyFactory());
  }
}
