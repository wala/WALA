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

import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.DefaultInstantiationBehavior;

import com.ibm.wala.ipa.cha.IClassHierarchy;

import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;

import com.ibm.wala.util.ssa.SSAValueManager;

import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;

import com.ibm.wala.classLoader.CallSiteReference;

import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.ibm.wala.util.collections.HashMapFactory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.Class;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;

/**
 *  Model configuration and Global list of entrypoints.
 *
 *  AnalysisOptions.getEntrypoints may change during an analysis. This does not.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 */
public final /* singleton */ class AndroidEntryPointManager implements Serializable {
    //private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointManager.class);
    private static final Logger logger = NOPLogger.NOP_LOGGER;

    public transient static final AndroidEntryPointManager MANAGER = new AndroidEntryPointManager();
    public transient static List<AndroidEntryPoint> ENTRIES = new ArrayList<AndroidEntryPoint>();
    private transient IInstantiationBehavior instantiation = null;

    //
    // EntryPoint stuff
    //
    /**
     *  Determines if any EntryPoint extends the specified component.
     */
    public boolean EPContainAny(AndroidComponent compo) {
        for (AndroidEntryPoint ep: ENTRIES) {
            if (ep.belongsTo(compo)) {
                return true;
            }
        }
        return false;
    }

    private AndroidEntryPointManager() {} 

    //
    //  General settings
    //

    /**
     *  Controls the instantiation of variables in the model.
     *
     *  On which occasions a new instance of a class shall be used? 
     *  This also changes the parameters to the later model.
     *
     *  @param  cha     Optional parameter given to the DefaultInstantiationBehavior if no other
     *      behavior has been set
     */
    public IInstantiationBehavior getInstantiationBehavior(IClassHierarchy cha) {
        if (this.instantiation == null) {
            this.instantiation = new DefaultInstantiationBehavior(cha);
        }
        return this.instantiation;
    }

    /**
     *  Set the value returned by {@link getInstantiationBehavior()}
     *
     *  @return the previous IInstantiationBehavior
     */
    public IInstantiationBehavior setInstantiationBehavior(IInstantiationBehavior instantiation) {
        final IInstantiationBehavior prev = this.instantiation;
        this.instantiation = instantiation;
        return prev;
    }

    private transient IProgressMonitor progressMonitor = null;
    /**
     *  Can be used to indicate the progress or to cancel operations.
     *
     *  @return a NullProgressMonitor or the one set before. 
     */
    public IProgressMonitor getProgressMonitor() {
        if (this.progressMonitor == null) {
            return new NullProgressMonitor();
        } else {
            return this.progressMonitor;
        }
    }

    /**
     *  Set the monitor returned by {@link #getProgressMonitor()}.
     */
    public IProgressMonitor setProgressMonitor(IProgressMonitor monitor) {
        IProgressMonitor prev = this.progressMonitor;
        this.progressMonitor = monitor;
        return prev;
    }

}
