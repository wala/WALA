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

package com.ibm.wala.classLoader;

import java.util.Iterator;

/**
 * A {@link Module} represents a set of files to analyze. eg., a Jar file. These are persistent
 * (hung onto by {@link ClassLoaderImpl}) .. so, a Module should not hold onto a lot of data.
 */
public interface Module {

  /** @return an Iterator of the ModuleEntries in this Module. */
  Iterator<? extends ModuleEntry> getEntries();
}
