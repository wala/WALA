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

import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.AndroidPossibleEntryPoint;

/**
 *  Hardcoded EntryPoint-specifications for an Android-Application.
 *
 *  The specifications are read and handled by AndroidEntryPointLocator.
 *  see: http://developer.android.com/reference/android/app/Application.html
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class ApplicationEP {

    /**
     * Called when the application is starting, before any activity, service, or receiver objects (excluding content providers) have been created.
     */
	public static final AndroidPossibleEntryPoint onCreate = new AndroidPossibleEntryPoint("onCreate", 
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    ProviderEP.onCreate     /* Yes, ContentProviders come before App! */
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ActivityEP.onCreate,
                    ServiceEP.onCreate
                }
            ));

    /**
     *  Called by the system when the device configuration changes while your component is running.
     *
     *  Note that, unlike activities, other components are never restarted when a configuration changes: they must always deal with the 
     *  results of the change, such as by re-retrieving resources. 
     */
	public static final AndroidPossibleEntryPoint onConfigurationChanged = new AndroidPossibleEntryPoint("onConfigurationChanged", 
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ActivityEP.onConfigurationChanged,
                    ExecutionOrder.END_OF_LOOP
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_LAST
               }
            ));
    /**
     *  This is called when the overall system is running low on memory, and actively running processes should trim their memory usage.
     *
     *  While the exact point at which this will be called is not defined, generally it will happen when all background process have been killed.
     *  That is, before reaching the point of killing processes hosting service and foreground UI that we would like to avoid killing. 
     */
	public static final AndroidPossibleEntryPoint onLowMemory = new AndroidPossibleEntryPoint("onLowMemory", 
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP,
                    ActivityEP.onLowMemory
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP,
                    onConfigurationChanged  // XXX ??!
                }
            ));

    /**
     *  This method is for use in emulated process environments.
     */
    /*
	public static final AndroidPossibleEntryPoint onTerminate = new AndroidPossibleEntryPoint(AndroidComponent.APPLICATION, 
            "onTerminate",
			ExecutionOrder.AT_LAST
            );
    */

    /**
     *  Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process.
     */
	public static final AndroidPossibleEntryPoint onTrimMemory = new AndroidPossibleEntryPoint("onTrimMemory", 
            ExecutionOrder.directlyBefore(onLowMemory)     // may potentially come before onLowMemory 
            );

    /**
     *  Add the EntryPoint specifications defined in this file to the given list.
     *
     *  @param  possibleEntryPoints the list to extend.
     */
	public static void populate(List<? super AndroidPossibleEntryPoint> possibleEntryPoints) {
		possibleEntryPoints.add(onCreate);
		possibleEntryPoints.add(onConfigurationChanged);
        possibleEntryPoints.add(onLowMemory);
        //possibleEntryPoints.add(onTerminate);
		possibleEntryPoints.add(onTrimMemory);
	}
}
