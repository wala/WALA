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
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>)
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.spec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.scandroid.util.LoaderUtils;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public class AndroidSpecs implements ISpecs {
//	private AppModelMethod appEntrySummary;
	
	static String act = "Landroid/app/Activity";
	static String svc = "Landroid/app/Service";
	static String prv = "Landroid/content/ContentProvider";
	static String rslv = "Landroid/content/ContentResolver";
	static String ctx = "Landroid/content/Context";
	static String http = "Landroid/net/AndroidHttpClient";
	static String bnd = "Landroid/os/IBinder";
	static String lm = "Landroid/location/LocationManager";
	static String tm = "Landroid/telephony/TelephonyManager";
	static String sms = "android/telephony/SmsManager";
	static String smsGsm = "android/telephony/gsm/SmsManager";
	static String ll = "Landroid/location/LocationListener";
	static String gl = "Landroid/location/GpsStatus$Listener";
	static String nl = "Landroid/location/GpsStatus$NmeaListener";

	static MethodNamePattern actCreate =
			new MethodNamePattern(act, "onCreate");
	static MethodNamePattern actStart =
			new MethodNamePattern(act, "onStart");
	static MethodNamePattern actResume =
			new MethodNamePattern(act, "onResume");
	static MethodNamePattern actStop =
			new MethodNamePattern(act, "onStop");
	static MethodNamePattern actRestart =
			new MethodNamePattern(act, "onRestart");
	static MethodNamePattern actDestroy =
			new MethodNamePattern(act, "onDestroy");
	static MethodNamePattern actOnActivityResult =
			new MethodNamePattern(act, "onActivityResult");

	static MethodNamePattern actOnRestoreInstanceState =
			new MethodNamePattern(act, "onRestoreInstanceState");
	static MethodNamePattern actOnSaveInstanceState =
			new MethodNamePattern(act, "onSaveInstanceState");

	static MethodNamePattern actSetResult =
			new MethodNamePattern(act, "setResult");
	
	static MethodNamePattern actGetIntent =
			new MethodNamePattern(act, "getIntent");
	
	static MethodNamePattern actStartActivityForResult =
			new MethodNamePattern(act, "startActivityForResult");
	static MethodNamePattern actStartActivityIfNeeded =
			new MethodNamePattern(act, "startActivityIfNeeded");
	static MethodNamePattern actStartNextMatchingActivity =
			new MethodNamePattern(act, "startNextMatchingActivity");
	static MethodNamePattern actStartActivityFromChild =
			new MethodNamePattern(act, "startActivityFromChild");

	static MethodNamePattern svcCreate =
			new MethodNamePattern(svc, "onCreate");
	static MethodNamePattern svcStart =
			new MethodNamePattern(svc, "onStart");
	static MethodNamePattern svcStartCommand =
			new MethodNamePattern(svc, "onStartCommand");
	static MethodNamePattern svcBind =
			new MethodNamePattern(svc, "onBind");

	static MethodNamePattern rslvQuery =
			new MethodNamePattern(rslv, "query");
	static MethodNamePattern rslvInsert =
			new MethodNamePattern(rslv, "insert");
	static MethodNamePattern rslvUpdate =
			new MethodNamePattern(rslv, "update");

	static MethodNamePattern prvCreate =
			new MethodNamePattern(prv, "onCreate");
	static MethodNamePattern prvQuery =
			new MethodNamePattern(prv, "query");
	static MethodNamePattern prvInsert =
			new MethodNamePattern(prv, "insert");
	static MethodNamePattern prvUpdate =
			new MethodNamePattern(prv, "update");

	static MethodNamePattern ctxStartActivity =
			new MethodNamePattern(ctx, "startActivity");
	static MethodNamePattern ctxStartService =
			new MethodNamePattern(ctx, "startService");
	static MethodNamePattern ctxBindService =
			new MethodNamePattern(ctx, "bindService");

	static MethodNamePattern bndTransact =
			new MethodNamePattern(bnd, "transact");
	static MethodNamePattern bndOnTransact =
			new MethodNamePattern(bnd, "onTransact");    

	static MethodNamePattern httpExecute =
			new MethodNamePattern(http, "execute");

//	private static MethodNamePattern[] callbackModelEntry = {
//		new MethodNamePattern("Lcom/SCanDroid/AppModel", "entry")
//	};
	
	static MethodNamePattern llLocChanged =
	        new MethodNamePattern(ll, "onLocationChanged");
	static MethodNamePattern llProvDisabled =
	        new MethodNamePattern(ll, "onProviderDisabled");
	static MethodNamePattern llProvEnabled =
	        new MethodNamePattern(ll, "onProviderEnabled");
	static MethodNamePattern llStatusChanged =
	        new MethodNamePattern(ll, "onStatusChanged");
	static MethodNamePattern glStatusChanged =
	        new MethodNamePattern(gl, "onGpsStatusChanged");
    static MethodNamePattern nlNmeaRecvd =
            new MethodNamePattern(nl, "onNmeaReceived");

	private static MethodNamePattern[] defaultCallbacks = {
		actCreate,
		actStart,
		actResume,
		actStop,
		actRestart,
		actDestroy,
		actOnActivityResult,

		svcCreate,
		svcStart,
		svcStartCommand,
		svcBind,
		//svcTransact,

		prvCreate,
		prvQuery,
		prvInsert,
		prvUpdate,

		llLocChanged,
		llProvDisabled,
		llProvEnabled,
		llStatusChanged,
		glStatusChanged,
		nlNmeaRecvd,
	};
	@Override
	public MethodNamePattern[] getEntrypointSpecs() { return defaultCallbacks; }


	private static SourceSpec[] sourceSpecs = {
//		new EntryArgSourceSpec( actCreate, null ),
		//doesn't have any parameters
		// new EntryArgSourceSpec( actStart, null ),
		// new EntryArgSourceSpec( actResume, null ),
		// new EntryArgSourceSpec( actStop, null ),
		// new EntryArgSourceSpec( actRestart, null ),
		// new EntryArgSourceSpec( actDestroy, null ),
		//track all parameters?  or just the Intent data(3)
		new EntryArgSourceSpec( actOnActivityResult, new int[] {3}),
//		new EntryArgSourceSpec( actOnRestoreInstanceState, null ),
//		new EntryArgSourceSpec( actOnSaveInstanceState, null ),

//		new EntryArgSourceSpec( svcCreate, null ),
		new EntryArgSourceSpec( svcStart, new int[] { 1 } ),
		new EntryArgSourceSpec( svcStartCommand, new int[] { 1 } ),
		new EntryArgSourceSpec( svcBind, new int[] {1} ),
		
		new EntryArgSourceSpec(bndOnTransact, new int[] { 2 }),

		new EntryArgSourceSpec(llLocChanged, null),
		new EntryArgSourceSpec(llProvDisabled, null),
		new EntryArgSourceSpec(llProvEnabled, null),
		new EntryArgSourceSpec(llStatusChanged, null),
		new EntryArgSourceSpec(glStatusChanged, null),
		new EntryArgSourceSpec(nlNmeaRecvd, null),

		//doesn't exist
		// new EntryArgSourceSpec( svcTransact, null ),

		//no parameters
		//new EntryArgSourceSpec( prvCreate, null ),
//		new CallArgSourceSpec( prvQuery, new int[] { 2, 3, 4, 5 }, SourceType.PROVIDER_SOURCE),
//		new CallArgSourceSpec( prvInsert, new int[] { 2 }, SourceType.PROVIDER_SOURCE),
//		new CallArgSourceSpec( prvUpdate, new int[] { 2, 3, 4 }, SourceType.PROVIDER_SOURCE),
				
		new CallArgSourceSpec(bndTransact, new int[] { 3 }),
		
		new CallRetSourceSpec(rslvQuery, new int[] {}),
//		new CallRetSourceSpec(httpExecute, new int[] {}),
		new CallRetSourceSpec(actGetIntent, new int[] {}),
		
//		new CallRetSourceSpec(new MethodNamePattern("LTest/Apps/GenericSource", "getIntSource"), new int[]{}),
		new CallRetSourceSpec(new MethodNamePattern("LTest/Apps/GenericSource", "getStringSource"), new int[]{}),

        new CallRetSourceSpec(new MethodNamePattern(lm, "getProviders"), null),
        new CallRetSourceSpec(new MethodNamePattern(lm, "getProvider"), null),
        new CallRetSourceSpec(new MethodNamePattern(lm, "getLastKnownLocation"), null),
        new CallRetSourceSpec(new MethodNamePattern(lm, "isProviderEnabled"), null),
        new CallRetSourceSpec(new MethodNamePattern(lm, "getBestProvider"), null),
        new CallRetSourceSpec(new MethodNamePattern(tm, "getNeighboringCellInfo"), null),
        new CallRetSourceSpec(new MethodNamePattern(tm, "getCellLocation"), null),
		
	};
	
	@Override
	public SourceSpec[] getSourceSpecs() { return sourceSpecs; }

	/**
	 * TODO: document!
	 */
	private static SinkSpec[] sinkSpecs = {
		new CallArgSinkSpec(actSetResult, new int[] { 2 }),
//		new CallArgSinkSpec(bndTransact, new int[] { 2 }),
		
		new CallArgSinkSpec(rslvQuery, new int[] { 2, 3, 4, 5 }),
		new CallArgSinkSpec(rslvInsert, new int[] { 2 }),
//		new CallArgSinkSpec(rslvUpdate, new int[] { 2, 3, 4 }),
		
		new CallArgSinkSpec(ctxBindService, new int[] { 1 }),
		new CallArgSinkSpec(ctxStartService, new int[] { 1 }),
		
		new CallArgSinkSpec(ctxStartActivity, new int[] { 1 }),
		new CallArgSinkSpec(actStartActivityForResult, new int[] { 1 }),
		new CallArgSinkSpec(actStartActivityIfNeeded, new int[] { 1 }),		
		new CallArgSinkSpec(actStartNextMatchingActivity, new int[] { 1 }),
		new CallArgSinkSpec(actStartActivityFromChild, new int[] { 2 }),		

		
		new EntryArgSinkSpec( bndOnTransact, new int[] { 3 } ),
//		new EntryArgSinkSpec( actOnActivityResult, new int[] { 2 } ),
//		new EntryArgSinkSpec( actOnSaveInstanceState, new int[] { 0 } ),

		//new EntryRetSinkSpec(prvQuery),
		
		new CallArgSinkSpec(new MethodNamePattern("LTest/Apps/GenericSink", "setSink"), new int[]{ 1 }),
		
        new CallArgSinkSpec(new MethodNamePattern(smsGsm, "sendTextMessage"), null),
        new CallArgSinkSpec(new MethodNamePattern(sms, "sendMultipartTextMessage"), null),
        new CallArgSinkSpec(new MethodNamePattern(smsGsm, "sendDataMessage"), null),
        new CallArgSinkSpec(new MethodNamePattern(sms, "sendTextMessage"), null),
        new CallArgSinkSpec(new MethodNamePattern(smsGsm, "sendMultipartTextMessage"), null),
		new CallArgSinkSpec(new MethodNamePattern(sms, "sendDataMessage"), null),
	};

	@Override
	public SinkSpec[] getSinkSpecs() { return sinkSpecs; }

	private static MethodNamePattern[] callBacks = new MethodNamePattern[]{};
//	public MethodNamePattern[] getCallBacks() {
//		if (callBacks == null)
//			callBacks = new MethodNamePattern[] {};
//		return callBacks;
//	}
	public void addPossibleListeners(ClassHierarchy cha) {
		Set<String> ignoreMethods = new HashSet<>();
		ignoreMethods.add("<init>");
		ignoreMethods.add("<clinit>");
		ignoreMethods.add("registerNatives");
		ignoreMethods.add("getClass");
		ignoreMethods.add("hashCode");
		ignoreMethods.add("equals");
		ignoreMethods.add("clone");
		ignoreMethods.add("toString");
		ignoreMethods.add("notify");
		ignoreMethods.add("notifyAll");
		ignoreMethods.add("finalize");
		ignoreMethods.add("wait");		

		List<MethodNamePattern> moreEntryPointSpecs = new ArrayList<> ();
		
		//add default entrypoints from AndroidSpecs.entrypointSpecs
		//Currently adds methods even if they exist in the ignnoreMethods
		//set.
		for (MethodNamePattern mnp: defaultCallbacks) {
			moreEntryPointSpecs.add(mnp);
		}

		for (IClass ic:cha) {
			if (!LoaderUtils.fromLoader(ic, ClassLoaderReference.Application)) {
				continue;
			}

			//finds all *Listener classes and fetches all methods for the listener
			if (ic.getName().getClassName().toString().endsWith("Listener")) {
				for (IMethod im: ic.getAllMethods()) {
					//TODO: add isAbstract()?
					if (!ignoreMethods.contains(im.getName().toString()) && !im.isPrivate()) {
						moreEntryPointSpecs.add(
								new MethodNamePattern(ic.getName().toString(), 
										im.getName().toString()));
					}
				}
			}
			//not a listener, just find all the methods that start with "on____"
			else {
				for (IMethod im:ic.getAllMethods()) {
					//TODO: add isAbstract()?
					if (!ignoreMethods.contains(im.getName().toString()) &&
							im.getName().toString().startsWith("on") && !im.isPrivate()) {
						moreEntryPointSpecs.add(new MethodNamePattern(ic.getName().toString(),
								im.getName().toString()));
					}
				}
			}
		}

//		entrypointSpecs =
		callBacks = 
				moreEntryPointSpecs.toArray(new MethodNamePattern[moreEntryPointSpecs.size()]);

	}
	
	public MethodNamePattern[] getCallBacks() {
		return callBacks;
	}
	
//	public void setEntrySummary(AppModelMethod amm) {
//		this.appEntrySummary = amm;
//	}
//	public AppModelMethod getEntrySummary() {
//		return appEntrySummary;
//	}
	
}
