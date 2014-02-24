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
package com.ibm.wala.dalvik.util;

import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;

import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.Selector;
import com.ibm.wala.dalvik.util.AndroidTypes;

import java.util.List;
import java.util.Set;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Does optional checks before building the CallGraph.
 *
 *  The android-model expects some configuration to be of specific settings to unfold it's 
 *  full performance. 
 *
 *  These checks may be run before building the CallGraph to verify everything is in place.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-11-01
 */
public class AndroidPreFlightChecks {
    private final Logger logger = LoggerFactory.getLogger(AndroidPreFlightChecks.class);

    private final AndroidEntryPointManager manager;
    private final AnalysisOptions options;
    private final IClassHierarchy cha;

    public AndroidPreFlightChecks(AndroidEntryPointManager manager, AnalysisOptions options, IClassHierarchy cha) {
        this.manager = manager;
        this.options = options;
        this.cha = cha;
    }

    public static enum Test {
        OVERRIDES,
        INTENTS,
        REUSE,
        STUBS_VERSION
    }

    /**
     *  Perform all checks defined in this class.
     *
     *  @return if the checks passed
     */
    public boolean all() {
        return allBut(EnumSet.noneOf(Test.class));
    }

    /**
     *  Perform all checks defined in this class but the listed ones.
     *
     *  @param skip checks not to perform
     *  @return if the checks passed
     */
    public boolean allBut(Set<Test> skip) {
        boolean pass = true;

        if (! skip.contains(Test.OVERRIDES)) {
            pass &= checkOverridesInPlace();
        }
        if (! skip.contains(Test.INTENTS)) {
            pass &= checkIntentSpecs();
        }
        if (! skip.contains(Test.REUSE)) {
            pass &= checkAllComponentsReuse();
        }
        if (! skip.contains(Test.STUBS_VERSION)) {
            pass &= checkStubsVersion();
        }

        return pass;
    }

    /**
     *  The Overrides are needed to resolve intents in the startComponent-Calls.
     *
     *  Without these overrides the startComponent-Calls will not be overridden. In a 
     *  static analysis there won't be a way to resolve the actual target of the call.
     *
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.stubs.Overrides.StartComponentMethodTargetSelector
     *
     *  @return if check passed
     *  @todo this doesn't check anything yet
     */
    public boolean checkOverridesInPlace() {
        boolean pass = true;

        // TODO: Check StartComponentMethodTargetSelector
        // TODO: Check IntentContextInterpreter
        // TODO: Check IntentContextSelector

        return pass;
    }

    /**
     *  Checks whether stubs are recent enough to contain some used functions.
     *
     *  If the stubs are to old some parts that rely on these functions may be 
     *  skipped entirely.
     *
     *  @return if check passed
     */
    public boolean checkStubsVersion() {
        boolean pass = true;

        if (this.cha.lookupClass(AndroidTypes.Fragment) == null) {
            logger.warn("Stubs to old to contain Fragments or class not found");
            pass = false;
        }

        if (this.cha.lookupClass(AndroidTypes.UserHandle) == null) {
            logger.warn("Stubs to old to contain UserHandles or class not found");
            pass = false;
        }

        if (this.cha.resolveMethod(
                        this.cha.lookupClass(AndroidTypes.Activity),
                        Selector.make("getLoaderManager()Landroid/app/LoaderManager;")) == null) {
            logger.warn("Stubs to old to contain function Activity.getLoaderManager()");
            pass = false;
        }

        if (this.cha.resolveMethod(
                        this.cha.lookupClass(AndroidTypes.Activity),
                        Selector.make("getSystemService(Ljava/lang/String;)Ljava/lang/Object;")) == null) {
            logger.warn("Stubs do not contain function Activity.getSystemService");
            pass = false;
        }

        if (this.cha.resolveMethod(
                        this.cha.lookupClass(AndroidTypes.Context),
                        Selector.make("getSystemService(Ljava/lang/String;)Ljava/lang/Object;")) == null) {
            logger.warn("Stubs do not contain function Context.getSystemService");
            pass = false;
        }


        return pass;
    }

    /**
     *  Is enough info present to resolve Intents.
     *
     *  This information is needed by the startComponent-functions in order to resolve the 
     *  target of the call (if enough context is present).
     *
     *  If this information is unavailable the call will be resolved to the function
     *  AndroidModel.Class.startUNKNOWNComponent which will call all Components of the
     *  specific type (Activity, Service, ..) present in the application.
     *
     *  @return if check passed
     */
    public boolean checkIntentSpecs() {
        boolean pass = true;

        final List <AndroidEntryPoint> entrypoits = this.manager.ENTRIES;

        for (AndroidEntryPoint ep : entrypoits) {
            final TypeName test = ep.getMethod().getDeclaringClass().getName();
            if (! this.manager.existsIntentFor(test)) {
                if (test.toString().startsWith("Landroid/")) {
                    continue;
                }
                logger.warn("No Intent-Specification was found for {}", test);
                pass = false;
            }
        }

        return pass;
    }

    /**
     *  In order to for the startComponent-calls to work components should be set reuse.
     *
     *  If components are _not_ set reuse the following will happen:
     *
     *      * The caller context can not be set 
     *      * No result will be transmitted back to onActivityResult
     *
     *  @return if the test passed
     */
    public boolean checkAllComponentsReuse() {
        boolean pass = true;

        final IInstantiationBehavior behaviour = this.manager.getInstantiationBehavior(null); // XXX: This generates false positives without cha!
        final List <AndroidEntryPoint> entrypoits = this.manager.ENTRIES;

        for (AndroidEntryPoint ep : entrypoits) {
            final TypeName test = ep.getMethod().getDeclaringClass().getName();
            IInstantiationBehavior.InstanceBehavior behave = behaviour.getBehavior(test,
                    /* asParameterTo=  */ null, /* inCall= */ null, /* withName= */ null);
            if (behave != IInstantiationBehavior.InstanceBehavior.REUSE) {
                logger.warn("The Type {} belongs to a {} and should be marked as REUSE", test, ep.getComponent());
                pass = false;
            }
        }

        return pass;
    }

}
