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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.*;

import java.util.*;

public interface AstPointerKeyFactory extends PointerKeyFactory {

  Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F);

  Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F);

  PointerKey getPointerKeyForObjectCatalog(InstanceKey I);

}
