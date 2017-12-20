/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.dalvik.util.androidEntryPoints;

import java.util.List;

import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.AndroidPossibleEntryPoint;

/**
 *  Hardcoded specifications of androids location handling call-backs.
 *  
 *  The specifications are read and handled by AndroidEntryPointLocator if the flags of
 *  AndroidEntryPointLocator are set to include call-backs.
 *  
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class LocationEP {
	public static final AndroidPossibleEntryPoint onLocationChanged = new AndroidPossibleEntryPoint("onLocationChanged", ExecutionOrder.MIDDLE_OF_LOOP);

	public static final AndroidPossibleEntryPoint onProviderDisabled = new AndroidPossibleEntryPoint("onProviderDisabled", ExecutionOrder.MIDDLE_OF_LOOP);

	public static final AndroidPossibleEntryPoint onProviderEnabled =  new AndroidPossibleEntryPoint("onProviderEnabled", ExecutionOrder.MIDDLE_OF_LOOP);

	public static final AndroidPossibleEntryPoint onStatusChanged =    new AndroidPossibleEntryPoint("onStatusChanged", ExecutionOrder.MIDDLE_OF_LOOP);

	public static final AndroidPossibleEntryPoint onGpsStatusChanged = new AndroidPossibleEntryPoint("onGpsStatusChanged", ExecutionOrder.MIDDLE_OF_LOOP);

	public static final AndroidPossibleEntryPoint onNmeaReceived =    new AndroidPossibleEntryPoint("onNmeaReceived", ExecutionOrder.MIDDLE_OF_LOOP);

    /**
     *  Add the EntryPoint specifications defined in this file to the given list.
     *
     *  @param  possibleEntryPoints the list to extend.
     */
	public static void populate(List<? super AndroidPossibleEntryPoint> possibleEntryPoints) {
		possibleEntryPoints.add(onLocationChanged);
		possibleEntryPoints.add(onProviderDisabled);
		possibleEntryPoints.add(onProviderEnabled);
		possibleEntryPoints.add(onStatusChanged);
		possibleEntryPoints.add(onGpsStatusChanged);
		possibleEntryPoints.add(onNmeaReceived);
	}
}
