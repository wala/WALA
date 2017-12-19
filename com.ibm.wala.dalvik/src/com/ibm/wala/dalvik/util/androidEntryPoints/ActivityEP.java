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
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointLocator
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class ActivityEP {
	// see: http://developer.android.com/reference/android/app/Activity.html

	//
    //  The entrypoints for the start and stop of the activity come first.
    //  Then some view-handling ones
    //  they are followed by user interaction stuff
    //
    
    
    /**
     *  Called after App.onCreate - assumed to be before Service.onCreate.
     *
     *  This does not have to be called before Service.onCreate but the user assumably starts most 
     *  apps with an activity we place it slightly before the Services
     */
    public static final AndroidPossibleEntryPoint onCreate = new AndroidPossibleEntryPoint("onCreate", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    // ApplicationEP.onCreate,      // Uncommenting would create a ring-dependency
                }));
            //,
            //    new AndroidEntryPoint.IExecutionOrder[] {
            //        //ServiceEP.onCreate                // Not necessarily but let's assume it // XXX: Causes Loop
            //    }
            //));

    /**
     *  Called a view steps before the Activity gets visible.
     *
     *  Called after onCreate(Bundle) — or after onRestart() when the activity had been stopped, but is now again 
     *  being displayed to the user. It will be followed by onResume(). 
     */
	public static final AndroidPossibleEntryPoint onStart = new AndroidPossibleEntryPoint("onStart", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreate, 
                    ExecutionOrder.START_OF_LOOP
            }));

    /**
     *  Restores the View-State (and may do other stuff).
     *
     *  This method is called after onStart() when the activity is being re-initialized from a previously saved state, 
     *  given here in savedInstanceState. 
     *
     *  The default implementation of this method performs a restore of any view state that had previously been frozen 
     *  by onSaveInstanceState(Bundle). 
     *
     *  This method is called between onStart() and onPostCreate(Bundle).
     */
    public static final AndroidPossibleEntryPoint onRestoreInstanceState = new AndroidPossibleEntryPoint("onRestoreInstanceState", 
            ExecutionOrder.after(onStart));

    /**
     *  Called when activity start-up is complete.
     *
     *  Called after onStart() and onRestoreInstanceState(Bundle)
     */
    public static final AndroidPossibleEntryPoint onPostCreate = new AndroidPossibleEntryPoint("onPostCreate", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onRestoreInstanceState,
                    onStart,
                    ExecutionOrder.START_OF_LOOP
            }));

    /**
     *  Activity starts interacting with the user.
     *
     *  Called after onRestoreInstanceState(Bundle), onRestart(), or onPause()
     *  Use onWindowFocusChanged(boolean) to know for certain that your activity is visible to the user.
     */
	public static final AndroidPossibleEntryPoint onResume = new AndroidPossibleEntryPoint("onResume", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
				    onPostCreate,
                    onRestoreInstanceState,
                    // onRestart defined later..
                    // onPause defined later
				    // ExecutionOrder.MIDDLE_OF_LOOP
				} ));

    /**
     *  Called when activity resume is complete.
     */
    public static final AndroidPossibleEntryPoint onPostResume = new AndroidPossibleEntryPoint("onPostResume", 
            ExecutionOrder.after(onResume));

    /**
     *  Activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started. 
     *  onNewIntent() will be called on the existing instance with the Intent that was used to re-launch it.
     *
     *  An activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method. 
     */
    public static final AndroidPossibleEntryPoint onNewIntent = new AndroidPossibleEntryPoint("onNewIntent",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onPostCreate,
                    onRestoreInstanceState,
                },
                onResume
            ));

    /**
     *  Called when you are no longer visible to the user. 
     *
     *  Next call will be either onRestart(), onDestroy(), or nothing, depending on later user activity. 
     */
	public static final AndroidPossibleEntryPoint onStop = new AndroidPossibleEntryPoint("onStop", 
            ExecutionOrder.after(
            ExecutionOrder.END_OF_LOOP
            ));

    /**
     *  Current activity is being re-displayed to the user.
     *
     *  Called after onStop(), followed by onStart() and then onResume(). 
     */
	public static final AndroidPossibleEntryPoint onRestart = new AndroidPossibleEntryPoint("onRestart", 
            ExecutionOrder.after(onStop) );


    /**
     *  Called to retrieve per-instance state from an activity before being killed.
     *
     *  It will get restored by onCreate(Bundle) or onRestoreInstanceState(Bundle).
     *
     *  If called, this method will occur before onStop(). There are no guarantees about whether it will 
     *  occur before or after onPause().
     */
   public static final AndroidPossibleEntryPoint onSaveInstanceState = new AndroidPossibleEntryPoint("onSaveInstanceState", 
           ExecutionOrder.after(onPostResume));

   /**
    *   Activity is going to the background.
    *
    *   Activity has not (yet) been killed. The counterpart to onResume(). 
    *
    *   In situations where the system needs more memory it may kill paused processes to reclaim resources.
    *   In general onSaveInstanceState(Bundle) is used to save per-instance state in the activity and this 
    *   method is used to store global persistent data (in content providers, files, etc.) 
    *
    *   After receiving this call you will usually receive a following call to onStop()
    *   however in some cases there will be a direct call back to onResume() without going through the stopped state.
    */
	public static final AndroidPossibleEntryPoint onPause = new AndroidPossibleEntryPoint("onPause", 
            ExecutionOrder.after(new AndroidEntryPoint.IExecutionOrder[] {
				onResume,
				onSaveInstanceState,
				ExecutionOrder.MIDDLE_OF_LOOP
				} ));
    /**
     *  Perform any final cleanup before an activity is destroyed. 
     *
     *  Someone called finish() on the Activity, or the system is temporarily destroying this Activity to save space.
     *  There are situations where the system will simply kill the activity's hosting process without calling this method.
     */
	public static final AndroidPossibleEntryPoint onDestroy = new AndroidPossibleEntryPoint("onDestroy", 
            ExecutionOrder.after(new AndroidEntryPoint.IExecutionOrder[] {
                onStop,
    			ExecutionOrder.AT_LAST
            }));

    /**
     *  Called when an Activity started by this one returns its result.
     */
	public static final AndroidPossibleEntryPoint onActivityResult = new AndroidPossibleEntryPoint("onActivityResult", 
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP,
                    // If this Activity starts an notherone it most certainly goes into background so
                    // place this after onPause
                    onPause
            }));

    //
    //  View stuff
    //
     /**
     * Accessibility events that are sent by the system when something notable happens in the user interface. For example, 
     * when a Button is clicked, a View is focused, etc. 
     *
     * TODO: Assert included everywhere
     */
    public static final AndroidPossibleEntryPoint dispatchPopulateAccessibilityEvent = new AndroidPossibleEntryPoint("dispatchPopulateAccessibilityEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AT_FIRST,
                    onCreate    // TODO: where to put?
                }
           ));

    /** Helper */
    private static final ExecutionOrder getVisible = ExecutionOrder.after(
        new AndroidEntryPoint.IExecutionOrder[] {
            ExecutionOrder.START_OF_LOOP,
            onResume,
            onPostCreate,
            dispatchPopulateAccessibilityEvent
        });

    private static final ExecutionOrder allInitialViewsSetUp = ExecutionOrder.between(
        getVisible,
        new AndroidEntryPoint.IExecutionOrder[] {
            ExecutionOrder.MIDDLE_OF_LOOP,
            onStop,
            onPause,
            onSaveInstanceState
        });



    /**
     *  Callback for creating dialogs that are managed (saved and restored) for you by the activity. 
     *
     *  If you would like an opportunity to prepare your dialog before it is shown, override onPrepareDialog.
     *
     *  This method was deprecated in API level 13.
     */
    public static final AndroidPossibleEntryPoint onCreateDialog = new AndroidPossibleEntryPoint("onCreateDialog",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    getVisible,
                    //ExecutionOrder.MIDDLE_OF_LOOP,  // Why?
                },
                allInitialViewsSetUp
            ));

    /**
     *  Provides an opportunity to prepare a managed dialog before it is being shown. 
     *
     *  This method was deprecated in API level 13.
     */
    public static final AndroidPossibleEntryPoint onPrepareDialog = new AndroidPossibleEntryPoint("onPrepareDialog",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateDialog,
                    getVisible,
                },
                allInitialViewsSetUp
            ));
 
    /**
     *  used when inflating with the LayoutInflater returned by getSystemService(String). 
     *
     * TODO: More info 
     *  This implementation handles tags to embed fragments inside of the activity.
     */
    public static final AndroidPossibleEntryPoint onCreateView = new AndroidPossibleEntryPoint("onCreateView",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    getVisible,
                    onPostCreate    // Create or Post?
                },
                allInitialViewsSetUp
            ));

    /**
     *  Called when a Fragment is being attached to this activity, immediately after the call to its 
     *  Fragment.onAttach() method and before Fragment.onCreate(). 
     */
    public static final AndroidPossibleEntryPoint onAttachFragment = new AndroidPossibleEntryPoint("onAttachFragment",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateView,
                    getVisible
                },
                allInitialViewsSetUp
            ));

    /**
     *  Called when the main window associated with the activity has been attached to the window manager. 
     *  See View.onAttachedToWindow() for more information. # TODO: See
     *
     *  Note that this function is guaranteed to be called before View.onDraw
     *  including before or after onMeasure(int, int).
     */
    public static final AndroidPossibleEntryPoint onAttachedToWindow = new AndroidPossibleEntryPoint("onAttachedToWindow",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateView,
                    getVisible
                },
                allInitialViewsSetUp
            ));

    /**
     *  Called when the main window associated with the activity has been detached from the window manager. 
     *  See View.onDetachedFromWindow() for more information.   # TODO See
     */
     public static final AndroidPossibleEntryPoint onDetachedFromWindow = new AndroidPossibleEntryPoint("onDetachedFromWindow",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onAttachedToWindow,
                    //ExecutionOrder.END_OF_LOOP,   // XXX: Why doesn't this work? 
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP,
                    onStop,
                    onSaveInstanceState,
                    onPause
                }
            ));
  
    /**
     *  This hook is called whenever the content view of the screen changes.
     *
     *  Due to a call to Window.setContentView or Window.addContentView
     */
     public static final AndroidPossibleEntryPoint onContentChanged = new AndroidPossibleEntryPoint("onContentChanged",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateView,
                    getVisible      // TODO
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP,  // TODO
                    onStop,
                    onPause,
                    onSaveInstanceState
                }
            ));

     /**
      * Called by setTheme(int) and getTheme() to apply a theme resource to the current Theme object.
      *
      * TODO: Do we have to register an entrypoint for this?
      */
     public static final AndroidPossibleEntryPoint onApplyThemeResource = new AndroidPossibleEntryPoint("onApplyThemeResource",
             ExecutionOrder.directlyAfter(onStart)   // Narf
             );

   
    /**
     *  TODO: GET MORE INFO ON THIS!.
     *
     *  This simply returns null so that all panel sub-windows will have the default menu behavior. 
     */
    public static final AndroidPossibleEntryPoint onCreatePanelView = new AndroidPossibleEntryPoint("onCreatePanelView",
            ExecutionOrder.between(
                getVisible,
                allInitialViewsSetUp
            ));

    /**
     *  TODO: GET MORE INFO ON THIS!.
     *
     *  This calls through to the new onCreateOptionsMenu(Menu) method for the FEATURE_OPTIONS_PANEL panel
     */
    public static final AndroidPossibleEntryPoint onCreatePanelMenu = new AndroidPossibleEntryPoint("onCreatePanelMenu",
            ExecutionOrder.between(
                getVisible,
                allInitialViewsSetUp
            ));

    /**
     *  TODO: GET MORE INFO ON THIS!.
     *
     *  This calls through to the new onPrepareOptionsMenu(Menu) method for the FEATURE_OPTIONS_PANEL 
     */
    public static final AndroidPossibleEntryPoint onPreparePanel = new AndroidPossibleEntryPoint("onPreparePanel",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    getVisible,
                    onCreatePanelMenu,                  // TODO Verify
                    onCreatePanelView,
                    //ExecutionOrder.MIDDLE_OF_LOOP,      // TODO: Do Multiple times?
                },
                allInitialViewsSetUp
            ));

    /**
     *  TODO: GET MORE INFO ON THIS!.
     *
     *  This calls through to onOptionsMenuClosed(Menu) method for the FEATURE_OPTIONS_PANEL.
     *  For context menus (FEATURE_CONTEXT_MENU), the onContextMenuClosed(Menu) will be called. 
     */
    public static final AndroidPossibleEntryPoint onPanelClosed = new AndroidPossibleEntryPoint("onPanelClosed",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreatePanelMenu,                  
                    onCreatePanelView,
                    onPreparePanel,
                    ExecutionOrder.MIDDLE_OF_LOOP,      // TODO: Do Later?
                    getVisible,
                    allInitialViewsSetUp
                },
                ExecutionOrder.AFTER_LOOP
            ));

    /**
     * Called when a context menu for the view is about to be shown. 
     *
     * Unlike onCreateOptionsMenu(Menu), this will be called every time the context menu is about to be shown.
     *
     * Use onContextItemSelected(android.view.MenuItem) to know when an item has been selected. 
     */
    public static final AndroidPossibleEntryPoint onCreateContextMenu = new AndroidPossibleEntryPoint("onCreateContextMenu",
            ExecutionOrder.between(
                getVisible,
                allInitialViewsSetUp
            ));

    /**
     *  TODO: How does this correlate to onMenuItemSelected.
     * 
     *  You can use this method for any items for which you would like to do processing without those other facilities. 
     */
    public static final AndroidPossibleEntryPoint onContextItemSelected = new AndroidPossibleEntryPoint("onContextItemSelected",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateContextMenu,
                    getVisible,
                    ExecutionOrder.MIDDLE_OF_LOOP,      // TODO: Do Later?
                },
                onPanelClosed   // XXX??
            ));

    /**
     * This hook is called whenever the context menu is being closed.
     *
     * either by the user canceling the menu with the back/menu button, or when an item is selected.
     */
    public static final AndroidPossibleEntryPoint onContextMenuClosed = new AndroidPossibleEntryPoint("onContextMenuClosed",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateContextMenu,
                    onContextItemSelected,
                    getVisible
                },
                ExecutionOrder.AFTER_LOOP   // To much? XXX
            ));
 
    public static final AndroidPossibleEntryPoint onCreateOptionsMenu = new AndroidPossibleEntryPoint("onCreateOptionsMenu",
            ExecutionOrder.directlyAfter(onCreateContextMenu)   // TODO: Well it behaves different! See onPrepareOptionsMenu, 
            );
    public static final AndroidPossibleEntryPoint onOptionsItemSelected = new AndroidPossibleEntryPoint("onOptionsItemSelected",
            ExecutionOrder.directlyAfter(onContextItemSelected)
            );
    
    public static final AndroidPossibleEntryPoint onPrepareOptionsMenu = new AndroidPossibleEntryPoint("onPrepareOptionsMenu",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateOptionsMenu,
                    getVisible
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onOptionsItemSelected,
                    allInitialViewsSetUp
                }
            ));

    public static final AndroidPossibleEntryPoint onOptionsMenuClosed = new AndroidPossibleEntryPoint("onOptionsMenuClosed",
            ExecutionOrder.directlyAfter(onContextMenuClosed)
            );

    
    /** TODO: More Info */
    public static final AndroidPossibleEntryPoint onMenuOpened = new AndroidPossibleEntryPoint("onMenuOpened",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateOptionsMenu,
                    onPrepareOptionsMenu,
                    onCreateContextMenu,
                    getVisible
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onOptionsItemSelected,
                    onContextItemSelected,
                    allInitialViewsSetUp
                }
            ));

    /**
     * TODO More info.
     *
     * This calls through to the new onOptionsItemSelected(MenuItem) 
     */
     public static final AndroidPossibleEntryPoint onMenuItemSelected = new AndroidPossibleEntryPoint("onMenuItemSelected",
            ExecutionOrder.between(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateContextMenu,
                    onPrepareOptionsMenu,
                    onMenuOpened,
                    getVisible
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onOptionsItemSelected,  // Calls through to this functions
                    onContextItemSelected,
                    allInitialViewsSetUp
                }
            ));


    public static final AndroidPossibleEntryPoint onTitleChanged = new AndroidPossibleEntryPoint("onTitleChanged",
            ExecutionOrder.directlyAfter(getVisible)  // TODO: What placement to choose?
    );
    public static final AndroidPossibleEntryPoint onChildTitleChanged = new AndroidPossibleEntryPoint("onChildTitleChanged",
            ExecutionOrder.directlyAfter(onTitleChanged)
    );
   
   
    //
    //  User Interaction
    //


    /**
     *  Called whenever a key, touch, or trackball event is dispatched to the activity. 
     *
     *  This callback and onUserLeaveHint() are intended to help activities manage status bar notifications intelligently; 
     *  specifically, for helping activities determine the proper time to cancel a notification.
     *
     *  All calls to your activity's onUserLeaveHint() callback will be accompanied by calls to onUserInteraction(). 
     *
     *  Note that this callback will be invoked for the touch down action that begins a touch gesture, but may not be invoked for 
     *  the touch-moved and touch-up actions that follow.
     */
    public static final AndroidPossibleEntryPoint onUserInteraction = new AndroidPossibleEntryPoint("onUserInteraction",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    getVisible,
                    allInitialViewsSetUp,
                    dispatchPopulateAccessibilityEvent,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));

    public static final AndroidPossibleEntryPoint dispatchTouchEvent = new AndroidPossibleEntryPoint("dispatchTouchEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    // TODO: Relation to onGenericMotionEvent
                    onUserInteraction,  // TODO: Verify
                    getVisible,
                    allInitialViewsSetUp,
                    dispatchPopulateAccessibilityEvent,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));

    /**
     *  Called when a touch screen event was not handled by any of the views under it. 
     *
     *  This is most useful to process touch events that happen outside of your window bounds, where there is no view to receive it.
     */
    public static final AndroidPossibleEntryPoint onTouchEvent = new AndroidPossibleEntryPoint("onTouchEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    // TODO: Relation to onGenericMotionEvent
                    onUserInteraction,  // TODO: Verify
                    dispatchPopulateAccessibilityEvent,
                    dispatchTouchEvent
                }
            ));

    /**
     *  You can override this to intercept all generic motion events before they are dispatched to the window.
     *
     *  TODO: Verify before on... stuff
     */
    public static final AndroidPossibleEntryPoint dispatchGenericMotionEvent = new AndroidPossibleEntryPoint("dispatchGenericMotionEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onUserInteraction,  // TODO: Verify
                    getVisible,
                    dispatchPopulateAccessibilityEvent,
                    allInitialViewsSetUp,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));
    /**
     *  Called when a generic motion event was not handled by any of the views inside of the activity.
     *
     *  Generic motion events with source class SOURCE_CLASS_POINTER are delivered to the view under the pointer. 
     *  All other generic motion events are delivered to the focused view. 
     *
     *  TODO: After onUserInteraction?
     */
    public static final AndroidPossibleEntryPoint onGenericMotionEvent = new AndroidPossibleEntryPoint("onGenericMotionEvent",
            ExecutionOrder.after(
                dispatchGenericMotionEvent
            ));

    
    public static final AndroidPossibleEntryPoint dispatchTrackballEvent = new AndroidPossibleEntryPoint("dispatchTrackballEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchPopulateAccessibilityEvent,
                    onUserInteraction,  // TODO: Verify
                    onGenericMotionEvent,   // TODO: Verify
                    getVisible,
                    allInitialViewsSetUp,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));

    /**
     *  Called when the trackball was moved and not handled by any of the views inside of the activity.
     *
     *  The call here happens before trackball movements are converted to DPAD key events, which then get sent 
     *  back to the view hierarchy, and will be processed at the point for things like focus navigation.
     */
    public static final AndroidPossibleEntryPoint onTrackballEvent = new AndroidPossibleEntryPoint("onTrackballEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchTrackballEvent,
                    onUserInteraction,  // TODO: Verify
                    onGenericMotionEvent,   // TODO: Verify
                }
            ));

    /**
     *  You can override this to intercept all key events before they are dispatched to the window.
     *  TODO: Verify before on... stuff
     */
    public static final AndroidPossibleEntryPoint dispatchKeyEvent = new AndroidPossibleEntryPoint("dispatchKeyEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onUserInteraction,  // TODO: Verify
                    onTrackballEvent,   // DPAD key events TODO: Verify
                    getVisible,
                    allInitialViewsSetUp,
                    dispatchPopulateAccessibilityEvent,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));

    public static final AndroidPossibleEntryPoint dispatchKeyShortcutEvent = new AndroidPossibleEntryPoint("dispatchKeyShortcutEvent",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    onUserInteraction,  // TODO: Verify
                    onTrackballEvent,   // DPAD key events TODO: Verify
                    getVisible,
                    allInitialViewsSetUp,
                    dispatchPopulateAccessibilityEvent,
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                }
            ));

    /**
     * The default implementation takes care of KEYCODE_BACK by calling onBackPressed(), though the behavior varies based 
     * on the application compatibility mode: for ECLAIR or later applications, it will set up the dispatch to call 
     * onKeyUp(int, KeyEvent) where the action will be performed; for earlier applications, it will perform the action 
     * immediately in on-down, as those versions of the platform behaved. 
     *
     * TODO: After onUserInteraction?
     */
    public static final AndroidPossibleEntryPoint onKeyDown = new AndroidPossibleEntryPoint("onKeyDown",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    onUserInteraction,
                    dispatchKeyEvent,
                    onTrackballEvent    // DPAD key events
                }
            ));

    public static final AndroidPossibleEntryPoint onKeyLongPress = new AndroidPossibleEntryPoint("onKeyLongPress",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    onKeyDown
                }
            ));

    public static final AndroidPossibleEntryPoint onKeyMultiple = new AndroidPossibleEntryPoint("onKeyMultiple",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    onKeyDown
                }
            ));
 
    public static final AndroidPossibleEntryPoint onKeyShortcut = new AndroidPossibleEntryPoint("onKeyShortcut",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    dispatchKeyShortcutEvent,
                    onKeyDown
                }
            ));
  
    /**
     * The default implementation handles KEYCODE_BACK to stop the activity and go back.
     */
    public static final AndroidPossibleEntryPoint onKeyUp = new AndroidPossibleEntryPoint("onKeyUp",
            ExecutionOrder.after(
                new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    onKeyDown,
                    onKeyLongPress,
                    onKeyMultiple,
                    onKeyShortcut
                }
            ));

    public static final AndroidPossibleEntryPoint onBackPressed = new AndroidPossibleEntryPoint("onBackPressed",
            ExecutionOrder.after(   // TODO Why is this so late?
                 new AndroidEntryPoint.IExecutionOrder[] {
                    dispatchKeyEvent,
                    onKeyDown,  // May  be both of them depending on version
                    onKeyUp
                 }
            ));


    /**
     *   This method will be invoked by the default implementation of onNavigateUp() 
     */
    public static final AndroidPossibleEntryPoint onCreateNavigateUpTaskStack = new AndroidPossibleEntryPoint("onCreateNavigateUpTaskStack",
            ExecutionOrder.between(
                 new AndroidEntryPoint.IExecutionOrder[] {
                   ExecutionOrder.START_OF_LOOP 
                     //onBackPressed      // TODO: Verify
                 },
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onPause,
                    onSaveInstanceState,
                    ExecutionOrder.AFTER_LOOP  // TODO This is to LATE!
                 }
            ));

    /**
     * Prepare the synthetic task stack that will be generated during Up navigation from a different task. 
     */
    public static final AndroidPossibleEntryPoint onPrepareNavigateUpTaskStack = new AndroidPossibleEntryPoint("onPrepareNavigateUpTaskStack",
            ExecutionOrder.between(
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateNavigateUpTaskStack,
                    //onBackPressed      // TODO: Verify
                 },
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onPause,
                    onSaveInstanceState,
                    ExecutionOrder.END_OF_LOOP  // TODO This is to LATE!
                 }
            ));

    /**
     *  This is called when a child activity of this one attempts to navigate up. 
     *  The default implementation simply calls onNavigateUp() on this activity (the parent).
     */
    public static final AndroidPossibleEntryPoint onNavigateUpFromChild = new AndroidPossibleEntryPoint("onNavigateUpFromChild",
            ExecutionOrder.between(
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateNavigateUpTaskStack, //No
                     //onBackPressed,     // TODO: Verify
                 },
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateNavigateUpTaskStack,
                    onPrepareNavigateUpTaskStack,    
                    onSaveInstanceState,
                    onPause
                 }
            ));

    /**
     *  This method is called whenever the user chooses to navigate Up within your application's activity hierarchy from the action bar. 
     */
    public static final AndroidPossibleEntryPoint onNavigateUp = new AndroidPossibleEntryPoint("onNavigateUp",
            ExecutionOrder.between(
                 new AndroidEntryPoint.IExecutionOrder[] {
                    //onBackPressed,     // TODO: Verify
                    onNavigateUpFromChild
                 },
                 new AndroidEntryPoint.IExecutionOrder[] {
                    onCreateNavigateUpTaskStack,
                    onPrepareNavigateUpTaskStack,    
                    onSaveInstanceState,
                    onPause
                 }
            ));
  
    /**
     *  This hook is called when the user signals the desire to start a search.
     *
     *  ..in response to a menu item, search button, or other widgets within your activity.
     */
    public static final AndroidPossibleEntryPoint  onSearchRequested = new AndroidPossibleEntryPoint("onSearchRequested",
            ExecutionOrder.after(
                 new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP,
                    onKeyUp,
                    onTrackballEvent,
                    onOptionsItemSelected, 
                    onContextItemSelected,
                    onMenuItemSelected
                 }
            ));
 
   
    //
    //  Misc stuff
    //

    /**
     *  Menus may depend on it..
     */
    public static final AndroidPossibleEntryPoint onActionModeStarted = new AndroidPossibleEntryPoint("onActionModeStarted",
            ExecutionOrder.MULTIPLE_TIMES_IN_LOOP // TODO where to put??
        );

    public static final AndroidPossibleEntryPoint onActionModeFinished = new AndroidPossibleEntryPoint("onActionModeFinished",
            ExecutionOrder.after(onActionModeStarted)
            );
    /**
     * Give the Activity a chance to control the UI for an action mode requested by the system. 
     */
    public static final AndroidPossibleEntryPoint onWindowStartingActionMode = new AndroidPossibleEntryPoint("onWindowStartingActionMode",
            ExecutionOrder.between(
                ExecutionOrder.MULTIPLE_TIMES_IN_LOOP, // TODO where to put??
                onActionModeStarted
            ));
    /**
     *  Will be called if you have selected configurations you would like to handle with the configChanges attribute in your manifest. 
     *  If any configuration change occurs that is not selected to be reported by that attribute, then instead of reporting it the system 
     *  will stop and restart the activity 
     */
    public static final AndroidPossibleEntryPoint onConfigurationChanged = new AndroidPossibleEntryPoint("onConfigurationChanged",
            ExecutionOrder.between( // TODO: Find a nice position
                new AndroidEntryPoint.IExecutionOrder[] {
                    onStop
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onRestart
                }
            ));

    public static final AndroidPossibleEntryPoint onSharedPreferenceChanged = new AndroidPossibleEntryPoint("onSharedPreferenceChanged",
            ExecutionOrder.between( // TODO: Find a nice position
                new AndroidEntryPoint.IExecutionOrder[] {
                    onStop
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    onRestart
                }
            ));

    /**
     * This method is called before pausing 
     */
    public static final AndroidPossibleEntryPoint onCreateDescription = new AndroidPossibleEntryPoint("onCreateDescription",
            ExecutionOrder.between(
                 onSaveInstanceState,
                 onPause
             ));
    /**
     * This method is called before pausing 
     */
    public static final AndroidPossibleEntryPoint onCreateThumbnail = new AndroidPossibleEntryPoint("onCreateThumbnail",
            ExecutionOrder.directlyBefore(onCreateDescription)
            );
    /**
     *  This function will be called after any global assist callbacks.
     *
     *  Assit is requested by the user.
     *  TODO: WTF is this?
     */
    public static final AndroidPossibleEntryPoint onProvideAssistData = new AndroidPossibleEntryPoint("onProvideAssistData",
            ExecutionOrder.between(
                allInitialViewsSetUp,
                ExecutionOrder.MIDDLE_OF_LOOP
            ));

    /**
     *  Called by the system, as part of destroying an activity due to a configuration change, when it is known that a 
     *  new instance will immediately be created for the new configuration.
     *
     *  The function will be called between onStop() and onDestroy().
     *  A new instance of the activity will always be immediately created after this one's onDestroy() is called.
     */
    public static final AndroidPossibleEntryPoint onRetainNonConfigurationInstance = new AndroidPossibleEntryPoint("onRetainNonConfigurationInstance",
            ExecutionOrder.between(
                    onStop,
                    onDestroy
             ));
   
    /**
     * While the exact point at which this will be called is not defined, generally it will happen when all background process have been killed. 
     * That is, before reaching the point of killing processes hosting service and foreground UI that we would like to avoid killing. 
     */
    public static final AndroidPossibleEntryPoint onLowMemory = new AndroidPossibleEntryPoint("onLowMemory",
            ExecutionOrder.between( // TODO: find a nice position
                ExecutionOrder.END_OF_LOOP,
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.AFTER_LOOP,
                    onConfigurationChanged  // XXX ??!!
                }
            ));

    /**
     *  This will happen for example when it goes in the background and there is not enough memory to keep as many 
     *  background processes running as desired. 
     */
    public static final AndroidPossibleEntryPoint onTrimMemory = new AndroidPossibleEntryPoint("onTrimMemory",
            ExecutionOrder.directlyBefore(onLowMemory)     // may potentially come before onLowMemory but they are near enough...
            );
    
    /** Called as part of the activity lifecycle when an activity is about to go into the background as the result of user choice. 
     * For example, when the user presses the Home key.
     *
     * this method is called right before the activity's onPause() callback. 
     */
    public static final AndroidPossibleEntryPoint onUserLeaveHint = new AndroidPossibleEntryPoint("onUserLeaveHint",
            ExecutionOrder.directlyBefore(onPause)
            );
    /**
     * This is called whenever the current window attributes change. 
     */
    public static final AndroidPossibleEntryPoint onWindowAttributesChanged = new AndroidPossibleEntryPoint("onWindowAttributesChanged",
            ExecutionOrder.between( 
                new AndroidEntryPoint.IExecutionOrder[] {   
                    ExecutionOrder.MULTIPLE_TIMES_IN_LOOP
                },
                new AndroidEntryPoint.IExecutionOrder[] {
                    ExecutionOrder.END_OF_LOOP
                }
            ));

    /**
     *  Called when the current Window of the activity gains or loses focus.
     *
     *  Note that this provides information about global focus state, which is managed independently of activity lifecycles. 
     *  As such, while focus changes will generally have some relation to lifecycle changes, you should not rely on any particular 
     *  order between the callbacks here and those in the other lifecycle methods such as onResume(). 
     */
    public static final AndroidPossibleEntryPoint onWindowFocusChanged = new AndroidPossibleEntryPoint("onWindowFocusChanged",
            ExecutionOrder.directlyAfter(onResume)    // TODO see above...
            );

    /**
     *  Add the EntryPoint specifications defined in this file to the given list.
     *
     *  @param  possibleEntryPoints the list to extend.
     */
	public static void populate(List<? super AndroidPossibleEntryPoint> possibleEntryPoints) {
        possibleEntryPoints.add(onCreate);
        possibleEntryPoints.add(onStart);
        possibleEntryPoints.add(onRestoreInstanceState);
        possibleEntryPoints.add(onPostCreate);
        possibleEntryPoints.add(onResume);
        possibleEntryPoints.add(onPostResume);
        possibleEntryPoints.add(onNewIntent);
        possibleEntryPoints.add(onStop);
        possibleEntryPoints.add(onRestart);
        possibleEntryPoints.add(onSaveInstanceState);
        possibleEntryPoints.add(onPause);
        possibleEntryPoints.add(onDestroy);
        possibleEntryPoints.add(onActivityResult);
        possibleEntryPoints.add(dispatchPopulateAccessibilityEvent);
        possibleEntryPoints.add(onCreateDialog);
        possibleEntryPoints.add(onPrepareDialog);
        possibleEntryPoints.add(onCreateView);
        possibleEntryPoints.add(onAttachFragment);
        possibleEntryPoints.add(onAttachedToWindow);
        possibleEntryPoints.add(onDetachedFromWindow);
        possibleEntryPoints.add(onContentChanged);
        possibleEntryPoints.add(onApplyThemeResource);
        possibleEntryPoints.add(onCreatePanelView);
        possibleEntryPoints.add(onCreatePanelMenu);
        possibleEntryPoints.add(onPreparePanel);
        possibleEntryPoints.add(onPanelClosed);
        possibleEntryPoints.add(onCreateContextMenu);
        possibleEntryPoints.add(onContextItemSelected);
        possibleEntryPoints.add(onContextMenuClosed);
        possibleEntryPoints.add(onCreateOptionsMenu);
        possibleEntryPoints.add(onOptionsItemSelected);
        possibleEntryPoints.add(onPrepareOptionsMenu);
        possibleEntryPoints.add(onOptionsMenuClosed);
        possibleEntryPoints.add(onMenuOpened);
        possibleEntryPoints.add(onMenuItemSelected);
        possibleEntryPoints.add(onTitleChanged);
        possibleEntryPoints.add(onChildTitleChanged);
        possibleEntryPoints.add(onUserInteraction);
        possibleEntryPoints.add(dispatchTouchEvent);
        possibleEntryPoints.add(onTouchEvent);
        possibleEntryPoints.add(dispatchGenericMotionEvent);
        possibleEntryPoints.add(onGenericMotionEvent);
        possibleEntryPoints.add(dispatchTrackballEvent);
        possibleEntryPoints.add(onTrackballEvent);
        possibleEntryPoints.add(dispatchKeyEvent);
        possibleEntryPoints.add(dispatchKeyShortcutEvent);
        possibleEntryPoints.add(onKeyDown);
        possibleEntryPoints.add(onKeyLongPress);
        possibleEntryPoints.add(onKeyMultiple);
        possibleEntryPoints.add(onKeyShortcut);
        possibleEntryPoints.add(onKeyUp);
        possibleEntryPoints.add(onBackPressed);
        possibleEntryPoints.add(onCreateNavigateUpTaskStack);
        possibleEntryPoints.add(onPrepareNavigateUpTaskStack);
        possibleEntryPoints.add(onNavigateUpFromChild);
        possibleEntryPoints.add(onNavigateUp);
        possibleEntryPoints.add(onSearchRequested);
        possibleEntryPoints.add(onActionModeStarted);
        possibleEntryPoints.add(onActionModeFinished);
        possibleEntryPoints.add(onWindowStartingActionMode);
        possibleEntryPoints.add(onConfigurationChanged);
        possibleEntryPoints.add(onCreateDescription);
        possibleEntryPoints.add(onCreateThumbnail);
        possibleEntryPoints.add(onProvideAssistData);
        possibleEntryPoints.add(onRetainNonConfigurationInstance);
        possibleEntryPoints.add(onLowMemory);
        possibleEntryPoints.add(onTrimMemory);
        possibleEntryPoints.add(onUserLeaveHint);
        possibleEntryPoints.add(onWindowAttributesChanged);
        possibleEntryPoints.add(onWindowFocusChanged);
        possibleEntryPoints.add(onSharedPreferenceChanged);

	}
}
