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
package com.ibm.wala.annotations;

/**
 * An annotation which indicates a method which although
 * public, should be considered an "internal" method, not exposed
 * to the outside world or an adversary.
 * 
 * This annotation is placed in the shrike project just so it's
 * at the top of the WALA project dependency hierarchy.  It doesn't have
 * anything to do with shrike.
 */
@Deprecated
public @interface Internal {

}
