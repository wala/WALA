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
 *  Hardcoded EntryPoint-specifications for an Android-Activity.
 *
 *  The specifications are read and handled by AndroidEntryPointLocator.
 *  https://developer.android.com/reference/android/app/Fragment.html
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class FragmentEP {


    //
    //  Start-up sequence:
    //

    /**
     *  called once the fragment is associated with its activity.
     *
     *  Called before onCreate
     */
    public static final AndroidPossibleEntryPoint onAttach = new AndroidPossibleEntryPoint("onAttach",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    ApplicationEP.onCreate,
                    ProviderEP.onCreate
                    //ActivityEP.onCreate     // I's same time...
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    //ServiceEP.onCreate    // Shall we put that there?
                }
            ));

    /**
     *  called to do initial creation of the fragment.
     *
     *  Called before before onCreateView.
     *  Note that this can be called while the fragment's activity is still in the process of being created. 
     *  once the activity itself is created: onActivityCreated(Bundle).
     */
    public static final AndroidPossibleEntryPoint onCreate = new AndroidPossibleEntryPoint("onCreate",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    onAttach,
                    //ActivityEP.onCreate     // I's same time...
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                }
            ));
    
    /**
     *  creates and returns the view hierarchy associated with the fragment.
     *
     *  This will be called between onCreate(Bundle) and onActivityCreated(Bundle).     XXX: CONTRADICTING DOCUMENTATION!
     *  his is optional, and non-graphical fragments can return null.
     */
    public static final AndroidPossibleEntryPoint onCreateView = new AndroidPossibleEntryPoint("onCreateView",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    onCreate,
                    //ActivityEP.onCreate     // May still be same time...
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                }
            ));
    
    /**
     *  tells the fragment that its activity has completed its own Activity.onCreate().
     *
     *  Called when the fragment's activity has been created and this fragment's view hierarchy instantiated.
     *  This is called after onCreateView                                               XXX: CONTRADICTING DOCUMENTATION!
     *  and before onViewStateRestored(Bundle).
     */
    public static final AndroidPossibleEntryPoint onActivityCreated = new AndroidPossibleEntryPoint("onActivityCreated",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    onCreate,
                    onCreateView                    // XXX: Now which one is correct?
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    //onCreateView                  // XXX: Now which one is correct?
                }
            ));
    
    /**
     *  tells the fragment that all of the saved state of its view hierarchy has been restored.
     *
     *  Called when all saved state has been restored into the view hierarchy of the fragment.
     *  This is called after onActivityCreated(Bundle) and before onStart().
     */
    public static final AndroidPossibleEntryPoint onViewStateRestored = new AndroidPossibleEntryPoint("onViewStateRestored",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,    // TODO: Already Part of loop?
                    onCreateView,
                    onActivityCreated
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    // TODO: Start of the Activity at this point?
                }
            ));
    
    /**
     *  makes the fragment visible to the user (based on its containing activity being started).
     *
     *  Called when the Fragment is visible to the user. 
     *  This is generally tied to Activity.onStart of the containing Activity's lifecycle. 
     */
    public static final AndroidPossibleEntryPoint onStart = new AndroidPossibleEntryPoint("onStart",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    ActivityEP.onStart,     // TODO:    Verify
                    onViewStateRestored
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MIDDLE_OF_LOOP
                }
            ));
    
    /**
     *   makes the fragment interacting with the user (based on its containing activity being resumed).
     *
     *   Called when the fragment is visible to the user and actively running. 
     *   This is generally tied to Activity.onResume of the containing Activity's lifecycle. 
     */
    public static final AndroidPossibleEntryPoint onResume = new AndroidPossibleEntryPoint("onResume",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    onStart,
                    ActivityEP.onPostCreate,
                    ActivityEP.onRestoreInstanceState,
                    ActivityEP.onResume     // TODO: Rather after or before?
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MIDDLE_OF_LOOP
                }
            ));


    //
    //  Stop sequence
    //

    /**
     *  fragment is no longer interacting with the user either because its activity is being paused or a fragment operation 
     *  is modifying it in the activity.
     *
     *  Called when the Fragment is no longer resumed. 
     *  This is generally tied to Activity.onPause of the containing Activity's lifecycle. 
     */
    public static final AndroidPossibleEntryPoint onPause = new AndroidPossibleEntryPoint("onPause",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP,
                    ActivityEP.onPause      // TODO: Rather after or before?
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP
                }
            ));

    /**
     * fragment is no longer visible to the user either because its activity is being stopped or a fragment operation is 
     * modifying it in the activity.
     *
     * Called when the Fragment is no longer started. 
     * This is generally tied to Activity.onStop of the containing Activity's lifecycle. 
     */
    public static final AndroidPossibleEntryPoint onStop = new AndroidPossibleEntryPoint("onStop",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onPause,
                    ActivityEP.onStop      // TODO: Rather after or before?
                }
            ));
    
    /**
     *  allows the fragment to clean up resources associated with its View.
     *
     *  Called when the view previously created by onCreateView(LayoutInflater, ViewGroup, Bundle) has been detached from the fragment.
     *  This is called after onStop() and before onDestroy(). It is called regardless of whether onCreateView returned a non-null view.
     *
     *  Internally it is called after the view's state has been saved but before it has been removed from its parent. 
     */
    public static final AndroidPossibleEntryPoint onDestroyView = new AndroidPossibleEntryPoint("onDestroyView",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onStop,
                    ActivityEP.onStop      // TODO: Rather after or before?
                }
            ));
    
    /**
     *  called to do final cleanup of the fragment's state.
     *
     *  Called when the fragment is no longer in use. This is called after onStop() and before onDetach(). 
     */
    public static final AndroidPossibleEntryPoint onDestroy = new AndroidPossibleEntryPoint("onDestroy",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onDestroyView,
                    ActivityEP.onStop      // TODO: Rather after or before?
                }
            ));

    /**
     *  called immediately prior to the fragment no longer being associated with its activity.
     *
     *  Called when the fragment is no longer attached to its activity. This is called after onDestroy(). 
     */
    public static final AndroidPossibleEntryPoint onDetach = new AndroidPossibleEntryPoint("onDetach",
            ExecutionOrder.between( 
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_LAST, 
                    onDestroy
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ActivityEP.onDestroy
                }
            ));


    //
    //  Misc
    //

    /**
     *  @see ActivityEP#onActivityResult
     */
    public static final AndroidPossibleEntryPoint onActivityResult = new AndroidPossibleEntryPoint("onActivityResult",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP,
                    // If this Activity starts an notherone it most certainly goes into background so
                    // place this after onPause
                    onPause,
                    ActivityEP.onPause,
                    //ActivityEP.onActivityResult   // TODO: Resolve if to put here...
                }
            ));

    /**
     *  Unlike activities, other components are never restarted.
     */
    public static final AndroidPossibleEntryPoint onConfigurationChanged = new AndroidPossibleEntryPoint("onConfigurationChanged",
            ExecutionOrder.directlyAfter(ActivityEP.onConfigurationChanged)    // TODO: Verify
            );

    /**
     *  This hook is called whenever an item in a context menu is selected. 
     */
    public static final AndroidPossibleEntryPoint onContextItemSelected = new AndroidPossibleEntryPoint("onContextItemSelected",
            ExecutionOrder.directlyAfter(ActivityEP.onContextItemSelected) // TODO: Verify
            );

    /**
     *  Called when a fragment loads an animation. 
     */
    public static final AndroidPossibleEntryPoint onCreateAnimator = new AndroidPossibleEntryPoint("onCreateAnimator",
            ExecutionOrder.directlyAfter(onResume) // TODO: Here?
            );

    /**
     *  Called when a context menu for the view is about to be shown. 
     *
     *  Unlike onCreateOptionsMenu, this will be called every time the context menu is about to be shown
     */
    public static final AndroidPossibleEntryPoint onCreateContextMenu = new AndroidPossibleEntryPoint("onCreateContextMenu",
            ExecutionOrder.directlyAfter(ActivityEP.onCreateContextMenu)
            );

    /**
     *  Initialize the contents of the Activity's standard options menu. 
     */
    public static final AndroidPossibleEntryPoint onCreateOptionsMenu = new AndroidPossibleEntryPoint("onCreateOptionsMenu",
            ExecutionOrder.directlyAfter(ActivityEP.onCreateOptionsMenu)
            );

    /**
     *  Called when this fragment's option menu items are no longer being included in the overall options menu. 
     *  Receiving this call means that the menu needed to be rebuilt, but this fragment's items were not included in the newly 
     *  built menu (its onCreateOptionsMenu was not called). 
     */
    public static final AndroidPossibleEntryPoint onDestroyOptionsMenu = new AndroidPossibleEntryPoint("onDestroyOptionsMenu",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP, // TODO: Here?
                }
            ));

    /**
     *  Called when the hidden state has changed. 
     *
     *  Fragments start out not hidden.
     */
    public static final AndroidPossibleEntryPoint onHiddenChanged = new AndroidPossibleEntryPoint("onHiddenChanged",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MIDDLE_OF_LOOP, // TODO: Here?
                }
            ));

    /**
     *  Called when a fragment is being created as part of a view layout inflation, typically from setting the content view of an 
     *  activity. 
     *
     *  This may be called immediately after the fragment is created from a tag in a layout file. 
     *  Note this is before the fragment's onAttach(Activity) has been called...
     */
    public static final AndroidPossibleEntryPoint onInflate = new AndroidPossibleEntryPoint("onInflate",
            ExecutionOrder.between( 
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST, 
                    ApplicationEP.onCreate,     // TODO: Here?
                    ActivityEP.onCreate
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onAttach
                }
            ));

    /**
     *  @see    ActivityEP#onLowMemory
     *  @see    ApplicationEP#onLowMemory
     */
    public static final AndroidPossibleEntryPoint onLowMemory = new AndroidPossibleEntryPoint("onLowMemory",
            ExecutionOrder.directlyBefore(ActivityEP.onLowMemory)
            );
    /**
     *  @see    ActivityEP#onOptionsItemSelected
     */
    public static final AndroidPossibleEntryPoint onOptionsItemSelected = new AndroidPossibleEntryPoint("onOptionsItemSelected",
            ExecutionOrder.directlyAfter(ActivityEP.onOptionsItemSelected)  // TODO: After? Before?
            );

    /**
     *  @see    ActivityEP#onOptionsMenuClosed
     */
    public static final AndroidPossibleEntryPoint onOptionsMenuClosed = new AndroidPossibleEntryPoint("onOptionsMenuClosed",
            ExecutionOrder.directlyAfter(ActivityEP.onOptionsMenuClosed)  // TODO: After? Before?
            );
    
    /**
     *  This is called right before the menu is shown, every time it is shown. 
     *
     *  @see    ActivityEP#onPrepareOptionsMenu
     */
    public static final AndroidPossibleEntryPoint onPrepareOptionsMenu = new AndroidPossibleEntryPoint("onPrepareOptionsMenu",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.START_OF_LOOP,
                    onCreateOptionsMenu,
                    onResume
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onOptionsItemSelected,
                    onOptionsMenuClosed,
                    ExecutionOrder.AFTER_LOOP
                }
            ));

    /**
     *  Called to ask the fragment to save its current dynamic state. 
     *
     *  Bundle here will be available in the Bundle given to onCreate(Bundle), onCreateView(LayoutInflater, ViewGroup, Bundle), 
     *  and onActivityCreated(Bundle). 
     *
     *  This method may be called at any time before onDestroy(). There are many situations where a fragment may be mostly torn down, 
     *  but its state will not be saved until its owning activity actually needs to save its state.
     */
    public static final AndroidPossibleEntryPoint onSaveInstanceState = new AndroidPossibleEntryPoint("onSaveInstanceState",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onStop,
                    onDestroyView       // See comment there
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onDestroy,
                    ExecutionOrder.AT_LAST  // XXX: To early
                }
            ));

    /**
     *  @see ActivityEP#onTrimMemory
     */
    public static final AndroidPossibleEntryPoint onTrimMemory = new AndroidPossibleEntryPoint("onTrimMemory",
            ExecutionOrder.directlyBefore(ActivityEP.onTrimMemory)
            );

    /**
     *  Called immediately after onCreateView has returned, but before any saved state has been restored in to the view. 
     */
    public static final AndroidPossibleEntryPoint onViewCreated = new AndroidPossibleEntryPoint("onViewCreated",
            ExecutionOrder.between(
                onCreateView,
                new AndroidEntryPoint.IExecutionOrder[] {
                    onActivityCreated,
                    onViewStateRestored
                }
            ));
   


    /**
     *  Add the EntryPoint specifications defined in this file to the given list.
     *
     *  @param  possibleEntryPoints the list to extend.
     */
    public static void populate(List<? super AndroidPossibleEntryPoint> possibleEntryPoints) {
        possibleEntryPoints.add(onAttach);
        possibleEntryPoints.add(onCreate);
        possibleEntryPoints.add(onCreateView);
        possibleEntryPoints.add(onActivityCreated);
        possibleEntryPoints.add(onViewStateRestored);
        possibleEntryPoints.add(onStart);
        possibleEntryPoints.add(onResume);
        possibleEntryPoints.add(onPause);
        possibleEntryPoints.add(onStop);
        possibleEntryPoints.add(onDestroyView);
        possibleEntryPoints.add(onDestroy);
        possibleEntryPoints.add(onDetach);
        possibleEntryPoints.add(onActivityResult);
        possibleEntryPoints.add(onConfigurationChanged);
        possibleEntryPoints.add(onContextItemSelected);
        possibleEntryPoints.add(onCreateAnimator);
        possibleEntryPoints.add(onCreateContextMenu);
        possibleEntryPoints.add(onCreateOptionsMenu);
        possibleEntryPoints.add(onDestroyOptionsMenu);
        possibleEntryPoints.add(onHiddenChanged);
        possibleEntryPoints.add(onInflate);
        possibleEntryPoints.add(onLowMemory);
        possibleEntryPoints.add(onOptionsItemSelected);
        possibleEntryPoints.add(onOptionsMenuClosed);
        possibleEntryPoints.add(onPrepareOptionsMenu);
        possibleEntryPoints.add(onSaveInstanceState);
        possibleEntryPoints.add(onTrimMemory);
        possibleEntryPoints.add(onViewCreated);
    }
}
