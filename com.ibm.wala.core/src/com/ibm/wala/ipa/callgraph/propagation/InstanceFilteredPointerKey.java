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
package com.ibm.wala.ipa.callgraph.propagation;

/**
 * A PointerKey which carries a type filter, used during pointer analysis
 * 
 * @author Julian dolby (dolby@us.ibm.com)
 */
public interface InstanceFilteredPointerKey extends FilteredPointerKey {
    
    /**
     * @return the class which should govern filtering of instances to
     * which this pointer points, or null if no filtering needed
     */
    public InstanceKey getInstanceFilter();
}
