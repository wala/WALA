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

package com.ibm.wala.ipa.cha;

import com.ibm.wala.util.internationalization.StringBundle;

/**
 * Exception class that indicates that construction of class hierarchy has been
 * cancelled by a progress monitor.
 * 
 * @author egeay
 */
public final class CancelCHAConstructionException extends ClassHierarchyException {

  private static final long serialVersionUID = -1987107302523285889L;

  public CancelCHAConstructionException() {
    super(StringBundle.getInstance().get("CancelCHAConstructionException.cancelation_message")); //$NON-NLS-1$
  }

}
