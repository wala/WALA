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
package com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent.IntentType;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * Contains Information on functions that start Android-Components based on an Intent.
 *
 * This is used by the IntentContextSelector to add an IntentContext to this Methods.
 *
 * TODO:
 * TODO: Fill in better values for targetAccuracy and componentType
 * TODO: Add declaring class
 * @author  Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 * @since   1013-10-16
 */
public class IntentStarters {
    /**
     *  The flags influence the later model.
     */
    public enum StarterFlags {
        /**
         *  Call the function onActivityResult of the calling Activity.
         */
        CALL_ON_ACTIVITY_RESULT,
        /**
         *  The intent is started using other permissions as the caller has.
         *
         *  This is used with startIntentSender
         */
        QUENCH_PERMISSIONS,
        /**
         *  For internal use only.
         *
         *  Used during the "boot process" and when installing the context free overrides.
         */
        CONTEXT_FREE,
    }

    /** Handling IntentSenders causes issues */
    private final boolean doIntentSender = true;


    /**
     *  Contains information on how to call a starter-function.
     */
    public static class StartInfo {
        private final Set<IntentType> targetAccuracy;
        /** used to dispatch to the correct MiniModel if intent-target could not be retreived */
        private final Set<AndroidComponent> componentType;
        /** relevant for the IntentContextSelector */
        private final int[] relevantParameters;
        private final Set<StarterFlags> flags;
        private final TypeReference declaringClass;

        StartInfo (final TypeReference declaringClass, final Set<IntentType> targetAccuracy, final Set<AndroidComponent> componentType, 
                final int[] relevantParameters) {
            this(declaringClass, targetAccuracy, componentType, relevantParameters, EnumSet.noneOf(StarterFlags.class));
        }

        StartInfo (final TypeReference declaringClass, final Set<IntentType> targetAccuracy, final Set<AndroidComponent> componentType, 
                final int[] relevantParameters, final Set<StarterFlags> flags) {
            this.declaringClass = declaringClass;
            this.targetAccuracy = targetAccuracy;
            this.componentType = componentType;
            this.relevantParameters = relevantParameters;
            this.flags = flags;
        }

        public static StartInfo makeContextFree(final AndroidComponent component) {
            final Set<AndroidComponent> compo = (component == null)?Collections.EMPTY_SET:EnumSet.of(component);
            return new IntentStarters.StartInfo(null, EnumSet.of(IntentType.UNKNOWN_TARGET), compo, new int[0],
                    EnumSet.of(StarterFlags.CONTEXT_FREE));
        }

        /**
         *  Set the CONTEXT_FREE Flag.
         */
        public void setContextFree() {
            this.flags.add(StarterFlags.CONTEXT_FREE);
        }

        /**
         *  The parameters the ContextSelecor shall remember.
         */
        public int[] getRelevant() {
            return relevantParameters;
        }

        public TypeReference getDeclaringClass() {
            return this.declaringClass;
        }

        /**
         *  These influence how the model is built.
         *
         *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentStarters.StarterFlags
         */
        public Set<StarterFlags> getFlags() {
            return this.flags;
        }

        /**
         *  Target-Types that may started by this. 
         *
         *  Although a set of multiple Components may be returned in most cases it is only one
         *  type.
         */
        public Set<AndroidComponent> getComponentsPossible() {
            return this.componentType;
        }

        public boolean isSystemService() {
            return this.targetAccuracy.contains(IntentType.SYSTEM_SERVICE); // XXX Nah.. not so nice :(
        }

        @Override
        public String toString() {
            return "<StartInfo flags=" + flags + " to possible " + componentType + " with allowed Accuracies of " + targetAccuracy + "/>";
        }
    }

    private final HashMap<Selector, StartInfo> starters = HashMapFactory.make();

    public boolean isStarter(MethodReference mRef) {
        return starters.containsKey(mRef.getSelector());
    }

    public StartInfo getInfo(MethodReference mRef) {
        return starters.get(mRef.getSelector());
    }

    public StartInfo getInfo(Selector mSel) {
        return starters.get(mSel);
    }


    public Set<Selector> getKnownMethods() {
        return starters.keySet();
    }

    public IntentStarters(IClassHierarchy cha) {
        final ClassLoaderReference searchLoader = ClassLoaderReference.Primordial;
        final TypeReference tContextWrapper = TypeReference.find(searchLoader, "Landroid/content/ContextWrapper");
        final TypeReference tContext = TypeReference.find(searchLoader, "Landroid/content/Context");
        final TypeReference tActivity = TypeReference.find(searchLoader, "Landroid/app/Activity");
        
        // Stubs may be to old for:
        final boolean doFragments = (cha.lookupClass(AndroidTypes.Fragment) != null); 
        final boolean doUsers = (cha.lookupClass(AndroidTypes.UserHandle) != null);

        if (! doFragments) {
            System.out.println("WARNING: IntentStarters skipping starters with Fragments - Stubs to old!");
        }
        if (! doUsers) {
            System.out.println("WARNING: IntentStarters skipping starters with UserHandles - Stubs to old!");
        }


        // This does not belong here:
        /*
        starters.put(Selector.make("getSystemService(Ljava/lang/String;)Ljava/lang/Object;"),
                new StartInfo(tContext, EnumSet.of(IntentType.EXTERNAL_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));
        starters.put(Selector.make("getSystemService(Ljava/lang/String;)Ljava/lang/Object;"),
                new StartInfo(tActivity, EnumSet.of(IntentType.EXTERNAL_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));
        */


        // android.content.ContextWrapper.bindService(Intent service, ServiceConnection conn, int flags)
        starters.put(/*MethodReference.findOrCreate(tContextWrapper,*/ Selector.make("bindService(Landroid/content/Intent;Landroid/content/ServiceConnection;I)Z"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));
                // Delegates directly to android.content.Context.bindService
        // android.content.ContextWrapper.sendBroadcast(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendBroadcast(Landroid/content/Intent;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                // Delegates directly to android.content.Context.
        // android.content.ContextWrapper.sendBroadcast(Intent intent, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendBroadcast(Landroid/content/Intent;Ljava/lang/String;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        if (doUsers) {
        // android.content.ContextWrapper.sendBroadcastAsUser(Intent intent, UserHandle user)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        // android.content.ContextWrapper.sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Ljava/lang/String;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        }
        // android.content.ContextWrapper.sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, 
        //          Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(
                "sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;Landroid/content/BroadcastReceiver;Landroid/os/Handler;I" +
                "Ljava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        // android.content.ContextWrapper.sendOrderedBroadcast(Intent intent, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        if (doUsers) {
        // android.content.ContextWrapper.sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, 
        //          BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(
                "sendOrderedBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Ljava/lang/String;Landroid/content/BroadcastReceiver;" + 
                "Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        }
        // android.content.ContextWrapper.sendStickyBroadcast(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendStickyBroadcast(Landroid/content/Intent;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        if (doUsers) {
        // android.content.ContextWrapper.sendStickyBroadcastAsUser(Intent intent, UserHandle user)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("sendStickyBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                // Delegates directly to android.content.Context.
        }
        // android.content.ContextWrapper.sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, 
        //          String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(
                        "sendStickyOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;Landroid/content/BroadcastReceiver;Landroid/os/Handler;" + 
                        "ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        if (doUsers) {
        // android.content.ContextWrapper.sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, 
        //          Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(
                        "sendStickyOrderedBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Landroid/content/BroadcastReceiver;" +
                        "Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
                //  Delegates directly to android.content.Context.
        }
        // android.content.ContextWrapper.startActivities(Intent[] intents)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("startActivities([Landroid/content/Intent;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
                //  Delegates directly to android.content.Context.
        // android.content.ContextWrapper.startActivities(Intent[] intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("startActivities([Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
                //  Delegates directly to android.content.Context.
        // android.content.ContextWrapper.startActivity(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("startActivity(Landroid/content/Intent;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
                //  Delegates directly to android.content.Context.
        // android.content.ContextWrapper.startActivity(Intent intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("startActivity(Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
                //  Delegates directly to android.content.Context.
        if (doIntentSender) {
            //  android.content.ContextWrapper.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, 
            //           int extraFlags, Bundle options)
            starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;IIILandroid/os/Bundle;)V"),
                    new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
                    //  Delegates directly to android.content.Context.
            // android.content.ContextWrapper.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;III)V"),
                    new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
                    //  Delegates directly to android.content.Context.
        }
        // android.content.ContextWrapper.startService(Intent service)
        starters.put( /* MethodReference.findOrCreate(tContextWrapper, */ Selector.make("startService(Landroid/content/Intent;)Landroid/content/ComponentName;"),
                new StartInfo(tContextWrapper, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));
                //  Delegates directly to android.content.Context.


        // android.content.Context.bindService(Intent service, ServiceConnection conn, int flags)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("bindService(Landroid/content/Intent;Landroid/content/ServiceConnection;I)Z"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));
        // android.content.Context.sendBroadcast(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendBroadcast(Landroid/content/Intent;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        // android.content.Context.sendBroadcast(Intent intent, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendBroadcast(Landroid/content/Intent;Ljava/lang/String;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        if (doUsers) {
        // android.content.Context.sendBroadcastAsUser(Intent intent, UserHandle user)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        // android.content.Context.sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Ljava/lang/String;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        }
        // android.content.Context.sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver, 
        //          Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(
                "sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;Landroid/content/BroadcastReceiver;Landroid/os/Handler;I" +
                "Ljava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        // android.content.Context.sendOrderedBroadcast(Intent intent, String receiverPermission)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        if (doUsers) {
        // android.content.Context.sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission, 
        //          BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(
                "sendOrderedBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Ljava/lang/String;Landroid/content/BroadcastReceiver;" + 
                "Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        // android.content.Context.sendStickyBroadcast(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendStickyBroadcast(Landroid/content/Intent;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        // android.content.Context.sendStickyBroadcastAsUser(Intent intent, UserHandle user)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("sendStickyBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        }
        // android.content.Context.sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, 
        //          String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(
                        "sendStickyOrderedBroadcast(Landroid/content/Intent;Ljava/lang/String;Landroid/content/BroadcastReceiver;Landroid/os/Handler;" + 
                        "ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        if (doUsers) {
        // android.content.Context.sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, 
        //          Handler scheduler, int initialCode, String initialData, Bundle initialExtras)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(
                        "sendStickyOrderedBroadcastAsUser(Landroid/content/Intent;Landroid/os/UserHandle;Landroid/content/BroadcastReceiver;" +
                        "Landroid/os/Handler;ILjava/lang/String;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.BROADCAST), EnumSet.of(AndroidComponent.BROADCAST_RECEIVER), new int[] {1}));
        }
        // android.content.Context.startActivities(Intent[] intents)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("startActivities([Landroid/content/Intent;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.content.Context.startActivities(Intent[] intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("startActivities([Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.content.Context.startActivity(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("startActivity(Landroid/content/Intent;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.content.Context.startActivity(Intent intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("startActivity(Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        if (doIntentSender) {
            //  android.content.Context.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, 
            //           int extraFlags, Bundle options)
            starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;IIILandroid/os/Bundle;)V"),
                    new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
            // android.content.Context.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;III)V"),
                    new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
        }
        // android.content.Context.startService(Intent service)
        starters.put( /* MethodReference.findOrCreate(tContext, */ Selector.make("startService(Landroid/content/Intent;)Landroid/content/ComponentName;"),
                new StartInfo(tContext, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.SERVICE), new int[] {1}));


        // android.app.Activity.startActivities(Intent[] intents)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivities([Landroid/content/Intent;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivities(Intent[] intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivities([Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivity(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivity(Landroid/content/Intent;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivity(Intent intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivity(Landroid/content/Intent;Landroid/os/Bundle;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivityForResult(Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityForResult(Landroid/content/Intent;I)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1},
                        EnumSet.of(StarterFlags.CALL_ON_ACTIVITY_RESULT)));
        // android.app.Activity.startActivityForResult(Intent intent, int requestCode, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityForResult(Landroid/content/Intent;ILandroid/os/Bundle;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1},
                        EnumSet.of(StarterFlags.CALL_ON_ACTIVITY_RESULT)));
        // android.app.Activity.startActivityFromChild(Activity child, Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityFromChild(Landroid/app/Activity;Landroid/content/Intent;I)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivityFromChild(Activity child, Intent intent, int requestCode, Bundle options) 
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityFromChild(Landroid/app/Activity;Landroid/content/Intent;ILandroid/os/Bundle;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        if (doFragments) {
        // android.app.Activity.startActivityFromFragment(Fragment fragment, Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityFromFragment(Landroid/app/Fragment;Landroid/content/Intent;I)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivityFromFragment(Fragment fragment, Intent intent, int requestCode, Bundle options) 
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityFromFragment(Landroid/app/Fragment;Landroid/content/Intent;ILandroid/os/Bundle;)V"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        }
        // android.app.Activity.startActivityIfNeeded(Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityIfNeeded(Landroid/content/Intent;I)Z"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivityIfNeeded(Intent intent, int requestCode, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityIfNeeded(Landroid/content/Intent;ILandroid/os/Bundle;)Z"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        if (doIntentSender) {
            //  android.app.Activity.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, 
            //           int extraFlags, Bundle options)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;IIILandroid/os/Bundle;)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
            // android.app.Activity.startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSender(Landroid/content/IntentSender;Landroid/content/Intent;III)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
            // android.app.Activity.startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, 
            //          int flagsMask, int flagsValues, int extraFlags, Bundle options)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.CALL_ON_ACTIVITY_RESULT, StarterFlags.QUENCH_PERMISSIONS)));
            // android.app.Activity.startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, 
            //          int flagsMask, int flagsValues, int extraFlags)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSenderForResult(Landroid/content/IntentSender;ILandroid/content/Intent;III)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.CALL_ON_ACTIVITY_RESULT, StarterFlags.QUENCH_PERMISSIONS)));
            // android.app.Activity.startIntentSenderFromChild(Activity child, IntentSender intent, int requestCode, 
            //          Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSenderFromChild(Landroid/app/Activity;Landroid/content/IntentSender;ILandroid/content/Intent;III)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
            // android.app.Activity.startIntentSenderFromChild(Activity child, IntentSender intent, int requestCode, 
            //          Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options)
            starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make(   // TODO
                            "startIntentSenderFromChild(Landroid/app/Activity;Landroid/content/IntentSender;ILandroid/content/Intent;IIILandroid/os/Bundle;)V"),
                    new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1, 2},
                            EnumSet.of(StarterFlags.QUENCH_PERMISSIONS)));
        }
        // android.app.Activity.startNextMatchingActivity(Intent intent)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startNextMatchingActivity(Landroid/content/Intent;)Z"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        // android.app.Activity.startActivity(Intent intents, Bundle options)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startNextMatchingActivity(Landroid/content/Intent;Landroid/os/Bundle;)Z"),
                new StartInfo(tActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));

        if (doFragments) {
        final TypeReference tFragmentActivity = TypeReference.find(searchLoader, "Landroid/support/v4/app/FragmentActivity");
        // android.support.v4.app.FragmentActivity.startActivityForResult(Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityForResult(Landroid/content/Intent;I)Z"),
                new StartInfo(tFragmentActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1},
                        EnumSet.of(StarterFlags.CALL_ON_ACTIVITY_RESULT)));
        // android.support.v4.app.startActivityFromFragment(Fragment fragment, Intent intent, int requestCode)
        starters.put( /* MethodReference.findOrCreate(tActivity, */ Selector.make("startActivityFromFragment(Landroid/app/Fragment;Landroid/content/Intent;I)Z"),
                new StartInfo(tFragmentActivity, EnumSet.of(IntentType.UNKNOWN_TARGET), EnumSet.of(AndroidComponent.ACTIVITY), new int[] {1}));
        }

    }
}
