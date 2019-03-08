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
package com.ibm.wala.util.debug;

/**
 * An optional interface for data structures that provide a verbose option for debugging purposes.
 */
public interface VerboseAction {
  /** optional method used for performance debugging */
  void performVerboseAction();
}
