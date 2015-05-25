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
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.AndroidPossibleEntryPoint;

/**
 *  Hardcoded EntryPoint-specifications for an Android-Service.
 *
 *  The specifications are read and handled by AndroidEntryPointLocator.
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 */
public final class ServiceEP {
    /**
     *  Called by the system when the service is first created. 
     */
    public static final AndroidPossibleEntryPoint onCreate = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onCreate",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    ProviderEP.onCreate,
                    //ApplicationEP.onCreate,      // Uncommenting would create a ring-dependency
                }
            ));

    /**
     *  Called by the system every time a client explicitly starts the service by calling startService(Intent).
     *  For backwards compatibility, the default implementation calls onStart.
     *
     *  startService-Services are not informed when they are stopped.
     */
	public static final AndroidPossibleEntryPoint onStartCommand = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onStartCommand",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] { 
                    ExecutionOrder.AT_FIRST,
                    onCreate
                }
			));

    
    /**
     *  Only for backwards compatibility.
     */
    public static final AndroidPossibleEntryPoint onStart = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onStart",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    onCreate,
                    onStartCommand      // onStartCommand usually calls onStart
                }
            ));


    /**
     * Return the communication channel to the service. May return null if clients can not bind to the service.
     *
     * Services started this way can be notified before they get stopped via onUnbind
     */
	public static final AndroidPossibleEntryPoint onBind = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onBind",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreate, 
                    ExecutionOrder.BEFORE_LOOP
                }
            ));

    /**
     * Called when all clients have disconnected from a particular interface published by the service. 
     * Return true if you would like to have the service's onRebind(Intent) method later called when new clients bind to it. 
     */
    public static final AndroidPossibleEntryPoint onUnbind = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onUnbind",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onBind, 
                    ExecutionOrder.END_OF_LOOP
                }
            ));

    /**
     *  Called when new clients have connected to the service, after it had previously been notified that all had disconnected in its 
     *  onUnbind(Intent). This will only be called if the implementation of onUnbind(Intent) was overridden to return true.
     */
    public static final AndroidPossibleEntryPoint onRebind = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onRebind",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onBind, 
                    onUnbind
                }
            ));


    /**
     *  Called by the system to notify a Service that it is no longer used and is being removed. 
     *  Upon return, there will be no more calls in to this Service object and it is effectively dead. 
     */
    public static final AndroidPossibleEntryPoint onDestroy = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE, 
            "onDestroy",
			ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onUnbind, 
                    onStart,
                    onBind,
                    onStartCommand, 
                    ExecutionOrder.AT_LAST
                }
            ));

    /**
     *  This is called if the service is currently running and the user has removed a task that comes from the service's application. 
     *  If you have set ServiceInfo.FLAG_STOP_WITH_TASK then you will not receive this callback; instead, the service will simply be stopped.
     */
    public static final AndroidPossibleEntryPoint onTaskRemoved = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE,
            "onTaskRemoved",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onUnbind, 
                    onStart, 
                    onBind,
                    onStartCommand, 
                    ExecutionOrder.AT_LAST
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onDestroy
                }
            ));

    /**
     * Called by the system when the device configuration changes while your component is running. 
     */
    public static final AndroidPossibleEntryPoint onConfigurationChanged = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE,
            "onConfigurationChanged",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {   // TODO: Position
                    onCreate
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP
                }
            ));
    
    /**
     * This is called when the overall system is running low on memory, and actively running processes should trim their memory usage. 
     */
    public static final AndroidPossibleEntryPoint onLowMemory = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE,
            "onLowMemory",
            ExecutionOrder.between( // TODO: find a nice position
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP,
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP,
                    //onConfigurationChanged  // XXX ??!!
                }
            ));

    /**
     * Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process.
     */
    public static final AndroidPossibleEntryPoint onTrimMemory = new AndroidPossibleEntryPoint(AndroidComponent.SERVICE,
            "onTrimMemory",
            ExecutionOrder.between( // TODO: find a nice position
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP,
                    //onConfigurationChanged  // XXX ??!!
                }
            ));

    public static final AndroidPossibleEntryPoint onHandleIntent = new AndroidPossibleEntryPoint(AndroidComponent.INTENT_SERVICE,
            "onHandleIntent", 
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreate
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onStart
                }
            ));

    public static final AndroidPossibleEntryPoint onCreateInputMethodInterface = new AndroidPossibleEntryPoint(
            AndroidComponent.ABSTRACT_INPUT_METHOD_SERVICE, "onCreateInputMethodInterface",
            ExecutionOrder.directlyAfter(onCreate));

    public static final AndroidPossibleEntryPoint onCreateInputMethodSessionInterface = new AndroidPossibleEntryPoint(
            AndroidComponent.ABSTRACT_INPUT_METHOD_SERVICE, "onCreateInputMethodSessionInterface",
            ExecutionOrder.after(onCreateInputMethodInterface)); // TODO: Place

    public static final AndroidPossibleEntryPoint onGenericMotionEvent = new AndroidPossibleEntryPoint(
            AndroidComponent.ABSTRACT_INPUT_METHOD_SERVICE, "onGenericMotionEvent",
            ExecutionOrder.directlyAfter(ActivityEP.onGenericMotionEvent));

    public static final AndroidPossibleEntryPoint onTrackballEvent = new AndroidPossibleEntryPoint(
            AndroidComponent.ABSTRACT_INPUT_METHOD_SERVICE, "onTrackballEvent",
            ActivityEP.onTrackballEvent);

    public static final AndroidPossibleEntryPoint onAccessibilityEvent = new AndroidPossibleEntryPoint(
            AndroidComponent.ACCESSIBILITY_SERVICE, "onAccessibilityEvent",
            ExecutionOrder.after(onTrackballEvent)); // TODO: Place

    public static final AndroidPossibleEntryPoint onInterrupt = new AndroidPossibleEntryPoint(
            AndroidComponent.ACCESSIBILITY_SERVICE, "onInterrupt",
            ExecutionOrder.after(onAccessibilityEvent));
   
    public static final AndroidPossibleEntryPoint onActionModeFinished = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onActionModeFinished",
            ActivityEP.onActionModeFinished);

    public static final AndroidPossibleEntryPoint onActionModeStarted = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onActionModeStarted",
            ActivityEP.onActionModeStarted);

    public static final AndroidPossibleEntryPoint onAttachedToWindow = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onAttachedToWindow",
            ActivityEP.onAttachedToWindow);
   
    public static final AndroidPossibleEntryPoint onContentChanged = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onContentChanged",
            ActivityEP.onContentChanged);

    public static final AndroidPossibleEntryPoint onCreatePanelMenu = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onCreatePanelMenu",
            ActivityEP.onCreatePanelMenu);

    public static final AndroidPossibleEntryPoint onCreatePanelView = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onCreatePanelView",
            ActivityEP.onCreatePanelView);

    public static final AndroidPossibleEntryPoint onDetachedFromWindow = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onDetachedFromWindow",
            ActivityEP.onDetachedFromWindow);
   
    public static final AndroidPossibleEntryPoint onDreamingStarted = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onDreamingStarted",
            ExecutionOrder.after(onStart)); // TODO: Place

    public static final AndroidPossibleEntryPoint onDreamingStopped = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onDreamingStopped",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {   // TODO: Place
                    onDreamingStarted,
                    onBind,
                    onStartCommand,
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onDestroy,
                    onUnbind
                }));

    public static final AndroidPossibleEntryPoint onMenuItemSelected = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onMenuItemSelected",
            ActivityEP.onMenuItemSelected);
    
    public static final AndroidPossibleEntryPoint onMenuOpened = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onMenuOpened",
            ActivityEP.onMenuOpened);

    public static final AndroidPossibleEntryPoint onPanelClosed = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onPanelClosed",
            ActivityEP.onPanelClosed);

    public static final AndroidPossibleEntryPoint onPreparePanel = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onPreparePanel",
            ActivityEP.onPreparePanel);

    public static final AndroidPossibleEntryPoint onSearchRequested = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onSearchRequested",
            ActivityEP.onSearchRequested);

    public static final AndroidPossibleEntryPoint onWindowAttributesChanged = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onWindowAttributesChanged",
            ActivityEP.onWindowAttributesChanged);
    
    public static final AndroidPossibleEntryPoint onWindowFocusChanged = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onWindowFocusChanged",
            ActivityEP.onWindowFocusChanged);

    public static final AndroidPossibleEntryPoint onWindowStartingActionMode = new AndroidPossibleEntryPoint(
            AndroidComponent.DREAM_SERVICE, "onWindowStartingActionMode",
            ActivityEP.onWindowStartingActionMode);

    public static final AndroidPossibleEntryPoint onDeactivated = new AndroidPossibleEntryPoint(
            AndroidComponent.HOST_APDU_SERVICE, "onDeactivated",
            ExecutionOrder.directlyBefore(ActivityEP.onPause));

    public static final AndroidPossibleEntryPoint onCreateMediaRouteProvider = new AndroidPossibleEntryPoint(
            AndroidComponent.MEDIA_ROUTE_PROVIDER_SERVICE, "onCreateMediaRouteProvider",
            onCreate);

    public static final AndroidPossibleEntryPoint onNotificationPosted = new AndroidPossibleEntryPoint(
            AndroidComponent.NOTIFICATION_LISTENER_SERVICE, "onNotificationPosted",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);

    public static final AndroidPossibleEntryPoint onNotificationRemoved = new AndroidPossibleEntryPoint(
            AndroidComponent.NOTIFICATION_LISTENER_SERVICE, "onNotificationRemoved",
            ExecutionOrder.after(onNotificationPosted));

    public static final AndroidPossibleEntryPoint onConnected = new AndroidPossibleEntryPoint(
            AndroidComponent.PRINT_SERVICE, "onConnected",
            ExecutionOrder.after(onStart));
    
    public static final AndroidPossibleEntryPoint onCreatePrinterDiscoverySession = new AndroidPossibleEntryPoint(
            AndroidComponent.PRINT_SERVICE, "onCreatePrinterDiscoverySession",
            ExecutionOrder.between(onStart, onConnected));
    
    public static final AndroidPossibleEntryPoint onDisconnected = new AndroidPossibleEntryPoint(
            AndroidComponent.PRINT_SERVICE, "onDisconnected",
            ExecutionOrder.between(onConnected, onDestroy));    // XXX: Section hop

    public static final AndroidPossibleEntryPoint onPrintJobQueued = new AndroidPossibleEntryPoint(
            AndroidComponent.PRINT_SERVICE, "onPrintJobQueued",
            ExecutionOrder.between(onConnected, onDisconnected)); // XXX: Section hop

    public static final AndroidPossibleEntryPoint onRequestCancelPrintJob = new AndroidPossibleEntryPoint(
            AndroidComponent.PRINT_SERVICE, "onRequestCancelPrintJob",
            ExecutionOrder.between(onPrintJobQueued, onDisconnected)); // XXX: Section hop
    
    public static final AndroidPossibleEntryPoint onCancel = new AndroidPossibleEntryPoint(
            AndroidComponent.RECOGNITION_SERVICE, "onCancel",
            ExecutionOrder.between(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP, ExecutionOrder.END_OF_LOOP));
   
    public static final AndroidPossibleEntryPoint onStartListening = new AndroidPossibleEntryPoint(
            AndroidComponent.RECOGNITION_SERVICE, "onStartListening",
            ExecutionOrder.between(ExecutionOrder.MULTIPLE_TIMES_IN_LOOP, onCancel));

    public static final AndroidPossibleEntryPoint onStopListening = new AndroidPossibleEntryPoint(
            AndroidComponent.RECOGNITION_SERVICE, "onStopListening",
            ExecutionOrder.between(onCancel, ExecutionOrder.END_OF_LOOP));
    
    public static final AndroidPossibleEntryPoint onGetViewFactory = new AndroidPossibleEntryPoint(
            AndroidComponent.REMOTE_VIEWS_SERVICE, "onGetViewFactory",
            ExecutionOrder.after(onStart)); // TODO: Position

    public static final AndroidPossibleEntryPoint onGetEnabled = new AndroidPossibleEntryPoint(
            AndroidComponent.SETTING_INJECTOR_SERVICE, "onGetEnabled",
            ExecutionOrder.after(onStart)); // TODO: Position
    
    public static final AndroidPossibleEntryPoint onGetSummary = new AndroidPossibleEntryPoint(
            AndroidComponent.SETTING_INJECTOR_SERVICE, "onGetSummary",
            ExecutionOrder.after(onStart)); // TODO: Position

    public static final AndroidPossibleEntryPoint onGetFeaturesForLanguage = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onGetFeaturesForLanguage",
            ExecutionOrder.after(onStart)); // TODO: Position

    public static final AndroidPossibleEntryPoint onGetLanguage = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onGetLanguage",
            ExecutionOrder.directlyBefore(onGetFeaturesForLanguage));

    public static final AndroidPossibleEntryPoint onLoadLanguage = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onLoadLanguage",
            ExecutionOrder.directlyBefore(onGetLanguage));

    public static final AndroidPossibleEntryPoint onIsLanguageAvailable = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onIsLanguageAvailable",
            ExecutionOrder.directlyBefore(onLoadLanguage));

    public static final AndroidPossibleEntryPoint onSynthesizeText = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onSynthesizeText",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onGetLanguage,
                    onLoadLanguage,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }));

    public static final AndroidPossibleEntryPoint onStop = new AndroidPossibleEntryPoint(
            AndroidComponent.TEXT_TO_SPEECH_SERVICE, "onStop",
            ExecutionOrder.directlyBefore(ActivityEP.onStop));


    public static final AndroidPossibleEntryPoint onRevoke = new AndroidPossibleEntryPoint(
            AndroidComponent.VPN_SERVICE, "onRevoke",
            ExecutionOrder.between(ExecutionOrder.END_OF_LOOP, onDestroy));
    
    public static final AndroidPossibleEntryPoint onCreateEngine = new AndroidPossibleEntryPoint(
            AndroidComponent.WALLPAPER_SERVICE, "onCreateEngine",
            ExecutionOrder.between(onCreate, onStart)); // TODO: Position
    
    public static final AndroidPossibleEntryPoint onAppPrivateCommand = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onAppPrivateCommand",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);     // TODO: Position
    
    /**
     *  to find out about switching to a new client.
     */
    public static final AndroidPossibleEntryPoint onBindInput = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onBindInput",
            ExecutionOrder.after(ActivityEP.onResume));

    /**
     *  Compute the interesting insets into your UI.
     */
    public static final AndroidPossibleEntryPoint onComputeInsets = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onComputeInsets",
            ExecutionOrder.after(onStart));

    public static final AndroidPossibleEntryPoint onConfigureWindow = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onConfigureWindow",
            ExecutionOrder.after(onComputeInsets));

    public static final AndroidPossibleEntryPoint onCreateCandidatesView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onCreateCandidatesView",
            ExecutionOrder.between(onStart, onComputeInsets));
    
    /**
     *  non-demand generation of the UI.
     */
    public static final AndroidPossibleEntryPoint onCreateExtractTextView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onCreateExtractTextView",
            ExecutionOrder.after(onCreateCandidatesView));

    /**
     *  non-demand generation of the UI.
     */
    public static final AndroidPossibleEntryPoint onCreateInputView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onCreateInputView",
            onCreateExtractTextView);

    /**
     *  non-demand generation of the UI.
     */
    public static final AndroidPossibleEntryPoint onStartCandidatesView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onStartCandidatesView",
            onCreateExtractTextView);

    //public static final AndroidPossibleEntryPoint onCreateInputMethodInterface = new AndroidPossibleEntryPoint(
    //        AndroidComponent.INPUT_METHOD_SERVICE, "onCreateInputMethodInterface",
    //        ExecutionOrder.after(onCreate));

    //public static final AndroidPossibleEntryPoint onCreateInputMethodSessionInterface = new AndroidPossibleEntryPoint(
    //        AndroidComponent.INPUT_METHOD_SERVICE, "onCreateInputMethodSessionInterface",
    //        ExecutionOrder.directlyAfter(onCreateInputMethodInterface)); 

    public static final AndroidPossibleEntryPoint onDisplayCompletions = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onDisplayCompletions",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);
    
    public static final AndroidPossibleEntryPoint onEvaluateFullscreenMode = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onEvaluateFullscreenMode",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);

    public static final AndroidPossibleEntryPoint onEvaluateInputViewShown = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onEvaluateInputViewShown",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);
    
    public static final AndroidPossibleEntryPoint onExtractTextContextMenuItem = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onExtractTextContextMenuItem",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);

    public static final AndroidPossibleEntryPoint onExtractedCursorMovement = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onExtractedCursorMovement",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);

    public static final AndroidPossibleEntryPoint onExtractedSelectionChanged = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onExtractedSelectionChanged",
            ExecutionOrder.directlyAfter(onExtractedCursorMovement));

    public static final AndroidPossibleEntryPoint onExtractedTextClicked = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onExtractedTextClicked",
            ExecutionOrder.directlyAfter(onExtractedCursorMovement));
    
    public static final AndroidPossibleEntryPoint onExtractingInputChanged = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onExtractingInputChanged",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onExtractedTextClicked,
                    onExtractedSelectionChanged,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }));

    public static final AndroidPossibleEntryPoint onFinishInput = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onFinishInput",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onExtractingInputChanged,
                }));

    public static final AndroidPossibleEntryPoint onFinishInputView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onFinishInputView",
            ExecutionOrder.after(onFinishInput));

    public static final AndroidPossibleEntryPoint onFinishCandidatesView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onFinishCandidatesView",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onExtractingInputChanged,
                    onFinishInput,
                    onStartCandidatesView
                }));

    //public static final AndroidPossibleEntryPoint onGenericMotionEvent = new AndroidPossibleEntryPoint(
    //        AndroidComponent.INPUT_METHOD_SERVICE, "onGenericMotionEvent",
    //        ExecutionOrder.directlyBefore(ActivityEP.onGenericMotionEvent));
    
    /**
     *  for user-interface initialization, in particular to deal with configuration changes while the service is running.
     */
    public static final AndroidPossibleEntryPoint onInitializeInterface = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onInitializeInterface",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP); // TODO: Position

    public static final AndroidPossibleEntryPoint onKeyDown = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onKeyDown",
            ExecutionOrder.directlyBefore(ActivityEP.onKeyDown));

    public static final AndroidPossibleEntryPoint onKeyLongPress = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onKeyLongPress",
            ExecutionOrder.directlyBefore(ActivityEP.onKeyLongPress));

    public static final AndroidPossibleEntryPoint onKeyMultiple = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onKeyMultiple",
            ExecutionOrder.directlyBefore(ActivityEP.onKeyMultiple));

    public static final AndroidPossibleEntryPoint onKeyUp = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onKeyUp",
            ExecutionOrder.directlyBefore(ActivityEP.onKeyUp));
    
    public static final AndroidPossibleEntryPoint onShowInputRequested = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onShowInputRequested",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP);

    /**
     * deal with an input session starting with the client. 
     */
    public static final AndroidPossibleEntryPoint onStartInput = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onStartInput",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateInputMethodSessionInterface,
                    ActivityEP.onResume
                }));

    /**
     * deal with input starting within the input area of the IME. 
     */
    public static final AndroidPossibleEntryPoint onStartInputView = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onStartInputView",
            onStartInput);
    
    // InputMethodService.onTrackballEvent
    public static final AndroidPossibleEntryPoint onUnbindInput = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onUnbindInput",
            onUnbind);  // TODO: Position
    
    public static final AndroidPossibleEntryPoint onUpdateCursor = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onUpdateCursor",
            ExecutionOrder.directlyBefore(onGenericMotionEvent));
    
    /**
     * Called when the application has reported new extracted text to be shown due to changes in its current text state. 
     */
    public static final AndroidPossibleEntryPoint onUpdateExtractedText = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onUpdateExtractedText",
            ExecutionOrder.directlyAfter(onExtractedTextClicked));
    
    public static final AndroidPossibleEntryPoint onUpdateExtractingViews = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onUpdateExtractingViews",
            ExecutionOrder.after(onExtractingInputChanged));
    
    public static final AndroidPossibleEntryPoint onUpdateExtractingVisibility = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onUpdateExtractingVisibility",
            ExecutionOrder.after(onUpdateExtractingViews));
    
    public static final AndroidPossibleEntryPoint onUpdateSelection = new AndroidPossibleEntryPoint(
             AndroidComponent.INPUT_METHOD_SERVICE, "onUpdateSelection",
             ExecutionOrder.after(onExtractedSelectionChanged));
    
    public static final AndroidPossibleEntryPoint onViewClicked = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onViewClicked",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onGenericMotionEvent,
                    onTrackballEvent,
                    onKeyUp
                }));
    
    public static final AndroidPossibleEntryPoint onWindowShown = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onWindowShown",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onConfigureWindow,
                    onCreateCandidatesView
                }));
    
    public static final AndroidPossibleEntryPoint onWindowHidden = new AndroidPossibleEntryPoint(
            AndroidComponent.INPUT_METHOD_SERVICE, "onWindowHidden",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onWindowShown,
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onDestroy,
                    ActivityEP.onPause
                }));
    
    /**
     *  After return of this method the BroadcastReceiver is assumed to have stopped.
     *
     *  As a BroadcastReceiver is oftain used in conjunction with a service it's defined here...
     */
    public static final AndroidPossibleEntryPoint onReceive = new AndroidPossibleEntryPoint(AndroidComponent.BROADCAST_RECEIVER,
            "onReceive", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST, 
                    ProviderEP.onCreate,
                    //ApplicationEP.onCreate
                }));
            //,
            //    new AndroidEntryPoint.IExecutionOrder[] {
            //        // BroadcastReceivers oftain use a Service - so place it before them.
            //        ServiceEP.onCreate
            //    }
            //)); 

    /**
     *  Add the EntryPoint specifications defined in this file to the given list.
     *
     *  @param  possibleEntryPoints the list to extend.
     */
	public static void populate(List<? super AndroidPossibleEntryPoint> possibleEntryPoints) {
		possibleEntryPoints.add(onCreate);
		possibleEntryPoints.add(onStart);
		possibleEntryPoints.add(onStartCommand);
        possibleEntryPoints.add(onBind);
        possibleEntryPoints.add(onUnbind);
        possibleEntryPoints.add(onRebind);
        possibleEntryPoints.add(onTaskRemoved);
        possibleEntryPoints.add(onConfigurationChanged);
        possibleEntryPoints.add(onLowMemory);
        possibleEntryPoints.add(onTrimMemory);
        possibleEntryPoints.add(onDestroy);

        possibleEntryPoints.add(onHandleIntent);
        possibleEntryPoints.add(onCreateInputMethodInterface);
        possibleEntryPoints.add(onCreateInputMethodSessionInterface);
        possibleEntryPoints.add(onGenericMotionEvent);
        possibleEntryPoints.add(onTrackballEvent);
        possibleEntryPoints.add(onAccessibilityEvent);
        possibleEntryPoints.add(onInterrupt);
        possibleEntryPoints.add(onActionModeFinished);
        possibleEntryPoints.add(onActionModeStarted);
        possibleEntryPoints.add(onAttachedToWindow);
        possibleEntryPoints.add(onContentChanged);
        possibleEntryPoints.add(onCreatePanelMenu);
        possibleEntryPoints.add(onCreatePanelView);
        possibleEntryPoints.add(onDetachedFromWindow);
        possibleEntryPoints.add(onDreamingStarted);
        possibleEntryPoints.add(onDreamingStopped);
        possibleEntryPoints.add(onMenuItemSelected);
        possibleEntryPoints.add(onMenuOpened);
        possibleEntryPoints.add(onPanelClosed);
        possibleEntryPoints.add(onPreparePanel);
        possibleEntryPoints.add(onSearchRequested);
        possibleEntryPoints.add(onWindowAttributesChanged);
        possibleEntryPoints.add(onWindowFocusChanged);
        possibleEntryPoints.add(onWindowStartingActionMode);
        possibleEntryPoints.add(onDeactivated); 
        possibleEntryPoints.add(onCreateMediaRouteProvider);
        possibleEntryPoints.add(onNotificationPosted);
        possibleEntryPoints.add(onNotificationRemoved);
        possibleEntryPoints.add(onConnected);
        possibleEntryPoints.add(onCreatePrinterDiscoverySession);
        possibleEntryPoints.add(onDisconnected);
        possibleEntryPoints.add(onPrintJobQueued);
        possibleEntryPoints.add(onRequestCancelPrintJob);
        possibleEntryPoints.add(onCancel);
        possibleEntryPoints.add(onStartListening);
        possibleEntryPoints.add(onStopListening);
        possibleEntryPoints.add(onGetViewFactory);
        possibleEntryPoints.add(onGetEnabled);
        possibleEntryPoints.add(onGetSummary);
        possibleEntryPoints.add(onGetFeaturesForLanguage);
        possibleEntryPoints.add(onGetLanguage);
        possibleEntryPoints.add(onIsLanguageAvailable);
        possibleEntryPoints.add(onLoadLanguage);
        possibleEntryPoints.add(onStop);
        possibleEntryPoints.add(onSynthesizeText);
        possibleEntryPoints.add(onRevoke);
        possibleEntryPoints.add(onCreateEngine);
        possibleEntryPoints.add(onAppPrivateCommand);
        possibleEntryPoints.add(onBindInput);
        possibleEntryPoints.add(onComputeInsets);
        possibleEntryPoints.add(onConfigureWindow);
        possibleEntryPoints.add(onCreateCandidatesView);
        possibleEntryPoints.add(onCreateExtractTextView);
        possibleEntryPoints.add(onCreateInputMethodInterface);
        possibleEntryPoints.add(onCreateInputMethodSessionInterface);
        possibleEntryPoints.add(onCreateInputView);
        possibleEntryPoints.add(onDisplayCompletions);
        possibleEntryPoints.add(onEvaluateFullscreenMode);
        possibleEntryPoints.add(onEvaluateInputViewShown);
        possibleEntryPoints.add(onExtractTextContextMenuItem);
        possibleEntryPoints.add(onExtractedCursorMovement);
        possibleEntryPoints.add(onExtractedSelectionChanged);
        possibleEntryPoints.add(onExtractedTextClicked);
        possibleEntryPoints.add(onExtractingInputChanged);
        possibleEntryPoints.add(onFinishCandidatesView);
        possibleEntryPoints.add(onFinishInput);
        possibleEntryPoints.add(onFinishInputView);
        possibleEntryPoints.add(onGenericMotionEvent);
        possibleEntryPoints.add(onInitializeInterface);
        possibleEntryPoints.add(onKeyDown);
        possibleEntryPoints.add(onKeyLongPress);
        possibleEntryPoints.add(onKeyMultiple);
        possibleEntryPoints.add(onKeyUp);
        possibleEntryPoints.add(onShowInputRequested);
        possibleEntryPoints.add(onStartCandidatesView);
        possibleEntryPoints.add(onStartInput);
        possibleEntryPoints.add(onStartInputView);
        possibleEntryPoints.add(onTrackballEvent);
        possibleEntryPoints.add(onUnbindInput);
        possibleEntryPoints.add(onUpdateCursor);
        possibleEntryPoints.add(onUpdateExtractedText);
        possibleEntryPoints.add(onUpdateExtractingViews);
        possibleEntryPoints.add(onUpdateExtractingVisibility);
        possibleEntryPoints.add(onUpdateSelection);
        possibleEntryPoints.add(onViewClicked);
        possibleEntryPoints.add(onWindowHidden);
        possibleEntryPoints.add(onWindowShown);

        possibleEntryPoints.add(onReceive);
	}
}
