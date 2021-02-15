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
/*
 * Created on Oct 6, 2005
 */
package com.ibm.wala.cast.java.translator;

import com.ibm.wala.classLoader.ModuleEntry;
import java.util.Set;

/**
 * An interface used by the JavaSourceLoaderImpl to encapsulate the loading of source entities on
 * the compile-time classpath into the DOMO analysis infrastructure.
 *
 * @author rfuhrer
 */
public interface SourceModuleTranslator {
  void loadAllSources(Set<ModuleEntry> modules);
}
