/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.config;

import java.io.Serializable;
import java.util.Set;

/**
 * Logically, a set of {@link Class}.
 * 
 * TODO: why does this not extend {@link Set}? Is there a good reason anymore?
 */
public abstract class SetOfClasses implements Serializable {

  public abstract boolean contains(String klassName);

  public abstract void add(String klass);

}
