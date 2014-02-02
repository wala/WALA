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
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopAndroidModel;

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
    private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointManager.class);

    public static final AndroidEntryPointManager MANAGER = new AndroidEntryPointManager();
    public static List<AndroidEntryPoint> ENTRIES = new ArrayList<AndroidEntryPoint>();
    /**
     * This is TRANSIENT!
     */
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

    private Class abstractAndroidModel = LoopAndroidModel.class;
    /**
     *  What special handling to insert into the model.
     *
     *  At given points in the model (called labels) special code is inserted into it (like loops).
     *  This setting controls what code is inserted there.
     *
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.SequentialAndroidModel
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopAndroidModel
     *  @return An object that handles "events" that occur while generating the model.
     *  @throws IllegalStateException if initialization fails
     */
    public AbstractAndroidModel makeModelBehavior(VolatileMethodSummary body, TypeSafeInstructionFactory insts,
            SSAValueManager paramManager, Iterable<? extends Entrypoint> entryPoints) {
        if (abstractAndroidModel == null) {
            return new LoopAndroidModel(body, insts, paramManager, entryPoints);
        } else {
            try {
                final Constructor<AbstractAndroidModel> ctor = this.abstractAndroidModel.getDeclaredConstructor(
                    VolatileMethodSummary.class, TypeSafeInstructionFactory.class, SSAValueManager.class,
                    Iterable.class);
                if (ctor == null) {
                    throw new IllegalStateException("Canot find the constructor of " + this.abstractAndroidModel);
                }
                return (AbstractAndroidModel) ctor.newInstance(body, insts, paramManager, entryPoints);
            } catch (java.lang.InstantiationException e) {
                throw new IllegalStateException(e);
            } catch (java.lang.IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new IllegalStateException(e);
            } catch (java.lang.NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     *  The behavior set using setModelBehavior(Class).
     *
     *  Use {@link makeModelBehavior(VolatileMethodSummary, JavaInstructionFactory, AndroidModelParameterManager, Iterable<? extends Entrypoint>} 
     *  to retrieve an instance of this class.
     *
     *  If no class was set it returns null, makeModelBehavior will generate a LoopAndroidModel by default.
     *
     *  @return null or the class set using setModelBehavior
     */
    public Class getModelBehavior() {
        return this.abstractAndroidModel;
    }

    /**
     *  Set the class instantiated by makeModelBehavior.
     *
     *  @throws IllgealArgumentException if the abstractAndroidModel does not subclass AbstractAndroidModel
     */
    public void setModelBehavior(Class abstractAndroidModel) {
        if (abstractAndroidModel == null) {
            throw new IllegalArgumentException("abstractAndroidModel may not be null. Use SequentialAndroidModel " +
                    "if no special handling shall be inserted.");
        }
        if (! AbstractAndroidModel.class.isAssignableFrom(abstractAndroidModel)) {
            throw new IllegalArgumentException("The given argument abstractAndroidModel does not subclass " +
                    "AbtractAndroidModel");
        }
        this.abstractAndroidModel = abstractAndroidModel;
    }

    //
    //  Propertys of the analyzed app
    //
    private transient String pack = null;
   
    /**
     *  Set the package of the analyzed application.
     *
     *  Setting the package of the application is completely optional. However if you do it it helps
     *  determining whether an Intent has an internal target.
     *
     *  @param  pack    The package of the analyzed application
     *  @throws IllegalArgumentException if the package has already been set and the value of the
     *      packages differ. Or if the given package is null.
     */
    public void setPackage(String pack) {
        if (pack == null) {
            throw new IllegalArgumentException("Setting the package to null is disallowed.");
        }
        if ((! pack.startsWith("L") || pack.contains("."))) {
            pack = StringStuff.deployment2CanonicalTypeString(pack);
        }
        if (this.pack == null) {
            logger.info("Setting the package to {}", pack);
            this.pack = pack;
        } else if (!(this.pack.equals(pack))) {
            throw new IllegalArgumentException("The already set package " + this.pack + " and " + pack +
                    " differ. You can only set pack once.");
        }
    }

    /**
     *  Return the package of the analyzed app.
     *
     *  This only returns a value other than null if the package has explicitly been set using 
     *  setPackage (which is for example called when reading in the Manifest).
     *
     *  If you didn't read the manifest you can still try and retrieve the package name using
     *  guessPackage().
     *
     *  @return The package or null if it was indeterminable.
     *  @see    guessPacakge()
     */
    public String getPackage() {
        if (this.pack == null) {
            logger.warn("Returning null as package");
            return null;
        } else {
            return this.pack;
        }
    }

    /**
     *  Get the package of the analyzed app.
     *
     *  If the package has been set using setPackage() return this value. Else try and determine 
     *  the package based on the first entrypoint.
     *
     *  @return The package or null if it was indeterminable.
     *  @see    getPackage()
     */
    public String guessPackage() {
        if (this.pack != null) {
            return this.pack;
        } else {
            if (ENTRIES.isEmpty()) {
                logger.error("guessPackage() called when no entrypoints had been set");
                return null;
            }
            final String first = ENTRIES.get(0).getMethod().getReference().getDeclaringClass().getName().getPackage().toString();
            // TODO: Iterate all?
            return first;
        }
    }
  
    /**
     *  Last 8 digits encode the date.
     */
    private final static long serialVersionUID = 8740020131212L;
}
