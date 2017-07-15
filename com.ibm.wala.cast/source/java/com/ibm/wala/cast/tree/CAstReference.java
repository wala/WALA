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
package com.ibm.wala.cast.tree;

/**
 *  This interface is used to denote various kinds of references in
 * CAst structures.  It can be used to denote types for languages like
 * Java and PHP that have non-trivial mappings from names to actual
 * entities. 
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public interface CAstReference {

  CAstType type();

}
