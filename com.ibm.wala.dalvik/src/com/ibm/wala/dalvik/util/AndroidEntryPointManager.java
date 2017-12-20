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
package com.ibm.wala.dalvik.util;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.DefaultInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.LoopAndroidModel;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.VolatileMethodSummary;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.ssa.SSAValueManager;
import com.ibm.wala.util.ssa.TypeSafeInstructionFactory;
import com.ibm.wala.util.strings.StringStuff;

/**
 *  Model configuration and Global list of entrypoints.
 *
 *  See the single settings for further description.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final /* singleton */ class AndroidEntryPointManager implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointManager.class);

    public static AndroidEntryPointManager MANAGER = new AndroidEntryPointManager();
    public static List<AndroidEntryPoint> ENTRIES = new ArrayList<>();
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
    public static boolean EPContainAny(AndroidComponent compo) {
        for (AndroidEntryPoint ep: ENTRIES) {
            if (ep.belongsTo(compo)) {
                return true;
            }
        }
        return false;
    }

    private AndroidEntryPointManager() {}

    public static void reset() {
        ENTRIES = new ArrayList<>();
        MANAGER = new AndroidEntryPointManager();
    }

    public static Set<TypeReference> getComponents() {
        if (ENTRIES.isEmpty()) {
            throw new IllegalStateException("No entrypoints loaded yet.");
        }
        
        final Set<TypeReference> ret = new HashSet<>();
        for (final AndroidEntryPoint ep : ENTRIES) {
            final TypeReference epClass = ep.getMethod().getDeclaringClass().getReference();
            if (AndroidComponent.isAndroidComponent(epClass , ep.getClassHierarchy())) {
                ret.add(epClass);
            }
        }
        return ret;
    }

    //
    //  General settings
    //

    private boolean flatComponents = false;
    /**
     *  Controlls the initialization of Components.
     *  
     *  See {@link #setDoFlatComponents(boolean)}.
     */
    public boolean doFlatComponents() {
        return flatComponents;
    }

    /**
     *  Controlls the initialization of Components.
     *
     *  If flatComponents is active an Instance of each Component of the application is generated
     *  in the AndroidModelClass. Whenever the model requires a new instance of a component this
     *  "globalone" is used.
     *
     *  This resembles the seldomly used Flat-Setting of the start of components in Android. Activating
     *  this generates a more conservative model.
     *
     *  The default is to deactivate this behavior.
     *
     *  @return previous setting
     */
    public boolean setDoFlatComponents(boolean flatComponents) {
        boolean pre = this.flatComponents;
        this.flatComponents = flatComponents;
        return pre;
    }

    /**
     *  Controls the instantiation of variables in the model.
     *
     *  See {@link #setInstantiationBehavior(IInstantiationBehavior)}.
     *
     *  @param  cha     Optional parameter given to the IInstantiationBehavior
     *  @return DefaultInstantiationBehavior if no other behavior has been set
     */
    public IInstantiationBehavior getInstantiationBehavior(IClassHierarchy cha) {
        if (this.instantiation == null) {
            this.instantiation = new DefaultInstantiationBehavior(cha);
        }
        return this.instantiation;
    }

    /**
     *  Controls the instantiation of variables in the model.
     *
     *  Controlls on which occasions a new instance to a given type shall be generated and when
     *  to reuse an existing instance.
     *
     *  This also changes the parameters to the later model.
     *
     *  The default is DefaultInstantiationBehavior.
     *
     *  See {@link #setDoFlatComponents(boolean)} for more instantiation settings that affect components
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior for more
     *      information
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

    private boolean doBootSequence = true;
    /**
     *  Whether to generate a global android environment.
     *
     *  See the {@link #setDoBootSequence} documentation.
     *
     *  @return the setting, defaults to true
     */
    public boolean getDoBootSequence() {
        return this.doBootSequence;
    }

    /**
     *  Whether to generate a global android environment.
     *
     *  Inserts some code ath the start of the model to attach some Android-context. This is mainly
     *  interesting for inter-application communication.
     *
     *  It's possible to analyze android-applications without creating these structures and save 
     *  some memory. In this case some calls to the OS (like getting the Activity-manager or so)
     *  will not be able to be resolved.
     *
     *  It is to be noted that the generated information is far from beeing complete.
     *
     *  The default is to insert the code.
     *
     *  @return the previous setting of doBootSequence
     */
    public boolean setDoBootSequence(boolean doBootSequence) {
        boolean prev = this.doBootSequence;
        this.doBootSequence = doBootSequence;
        return prev;
    }

    private Class<? extends AbstractAndroidModel> abstractAndroidModel = LoopAndroidModel.class;
    /**
     *  What special handling to insert into the model.
     *
     *  At given points in the model (called labels) special code is inserted into it (like loops).
     *  This setting controls what code is inserted there.
     *
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.structure.AbstractAndroidModel
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
                final Constructor<? extends AbstractAndroidModel> ctor = this.abstractAndroidModel.getDeclaredConstructor(
                    VolatileMethodSummary.class, TypeSafeInstructionFactory.class, SSAValueManager.class,
                    Iterable.class);
                if (ctor == null) {
                    throw new IllegalStateException("Canot find the constructor of " + this.abstractAndroidModel);
                }
                return ctor.newInstance(body, insts, paramManager, entryPoints);
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
     *  Use {@link #makeModelBehavior}
     *  to retrieve an instance of this class.
     *
     *  If no class was set it returns null, makeModelBehavior will generate a LoopAndroidModel by default.
     *
     *  @return null or the class set using setModelBehavior
     */
    public Class<? extends AbstractAndroidModel> getModelBehavior() {
        return this.abstractAndroidModel;
    }

    /**
     *  Set the class instantiated by makeModelBehavior.
     *
     *  @throws IllegalArgumentException if the abstractAndroidModel is null
     */
    public void setModelBehavior(Class<? extends AbstractAndroidModel> abstractAndroidModel) {
        if (abstractAndroidModel == null) {
            throw new IllegalArgumentException("abstractAndroidModel may not be null. Use SequentialAndroidModel " +
                    "if no special handling shall be inserted.");
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
     *  If a AndroidManifest.xml is read this getts set automaticly.
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
     *  See: {@link #setPackage(String)}
     *
     *  @return The package or null if it was indeterminable.
     *  @see    #guessPackage()
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
     *  @see    #getPackage()
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
  
    //
    //  Intent stuff
    //

    /**
     *  Overrides Intents.
     *
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent
     *  @see    com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
     */
    public final Map<Intent, Intent> overrideIntents = HashMapFactory.make();

    /**
     *  Set more information to an Intent.
     *
     *  You can call this method multiple times on the same Intent as long as you don't lower the associated
     *  information. So if you only want to change a specific value of it it is more safe to retrieve the Intent
     *  first and union it yourself before registering it.
     *
     *  @param  intent  An Intent with more or the same information as known to the system before.
     *  @throws IllegalArgumentException if you lower the information on an already registered Intent or the 
     *      information is incompatible.
     *  @see    #registerIntentForce
     */
    public void registerIntent(Intent intent) {
        if (overrideIntents.containsKey(intent)) {
            final Intent original = overrideIntents.get(intent);
            final Intent.IntentType oriType = original.getType();
            final Intent.IntentType newType = intent.getType();

            if ((newType == Intent.IntentType.UNKNOWN_TARGET) && (oriType != Intent.IntentType.UNKNOWN_TARGET)) {
                throw new IllegalArgumentException("You are lowering information on the Intent-Target of the " +
                        "Intent " + original + " from " + oriType + " to " + newType + ". Use registerIntentForce()" +
                        "If you are sure you want to do this!");
            } else if (oriType != newType) {
                throw new IllegalArgumentException("You are changing the Intents target to a contradicting one! " +
                        newType + "(new) is incompatible to " + oriType + "(before). On Intent " + intent +
                        ". Use registerIntentForce() if you are sure you want to do this!");
            }

            // TODO: Add actual target to the Intent and compare these?
            registerIntentForce(intent);
        } else {
            registerIntentForce(intent);
        }
    }

    /**
     *  Set intent possibly overwriting more specific information.
     *
     *  If you are sure that you want to override an existing registered Intent with information that is 
     *  possibly incompatible with the information originally set.
     */
    public void registerIntentForce(Intent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("The given Intent is null");
        }

        logger.info("Register Intent {}", intent);
        // Looks a bit weired but works as Intents are only matched based on their action and uri
        overrideIntents.put(intent, intent);
    }

    /**
     *  Override target of an Intent (or add an alias).
     *
     *  Use this for example to add an internal target to an implicit Intent, add an alias to an Intent, 
     *  resolve a System-name to an internal Intent, do weird stuff...
     *
     *  None of the Intents have to be registered before. However if the source is registered you may not 
     *  lower information on it.
     *
     *  Currently only one target to an Intent is supported! If you want to emulate multiple Targets you
     *  may have to add a synthetic class and register it as an Intent. If the target is not set to Internal
     *  multiple targets may implicitly emulated. See the Documentation for these targets for detail.
     *
     *  If you only intend to make an Intent known see {@link #registerIntent(Intent)}.
     *
     *  @param  from    the Intent to override
     *  @param  to      the new Intent to resolve once 'from' is seen
     *  @see    #setOverrideForce
     *  @throws IllegalArgumentException if you override an Intent with itself
     */
    public void setOverride(Intent from, Intent to) {
        if (from == null) {
            throw new IllegalArgumentException("The Intent given as 'from' is null");
        }
        if (to == null) {
            throw new IllegalArgumentException("The Intent given as 'to' is null");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("You cannot override an Intent with itself! If you want to " +
                    "alter Information on an Intent use registerIntent (you may register it multiple times).");
        }

        if (overrideIntents.containsKey(from)) {
            final Intent ori = overrideIntents.get(from);
            final Intent source;
            if (ori == from) {
                // The Intent has been registered before. Set the registered variant as source so Information
                // that may have been altered is not lost. Not that it would matter now...
                final Intent.IntentType oriType = ori.getType();
                final Intent.IntentType newType = from.getType();
                if ((newType == Intent.IntentType.UNKNOWN_TARGET) && (oriType != Intent.IntentType.UNKNOWN_TARGET)) {
                    // TODO: Test target resolvability
                    source = ori;
                } else {
                    source = from;
                }
            } else {
                source = from;
            }

            // Make sure the new target is not less specific than a known override
            final Intent original = overrideIntents.get(to);
            final Intent.IntentType oriType = original.getType();
            final Intent.IntentType newType = to.getType();

            if ((newType == Intent.IntentType.UNKNOWN_TARGET) && (oriType != Intent.IntentType.UNKNOWN_TARGET)) {
                throw new IllegalArgumentException("You are lowering information on the Intent-Target of the " +
                        "Intent " + original + " from " + oriType + " to " + newType + ". Use setOverrideForce()" +
                        "If you are sure you want to do this!");
            } else if (oriType != newType) {
                throw new IllegalArgumentException("You are changing the Intents target to a contradicting one! " +
                        newType + "(new) is incompatible to " + oriType + "(before). On Intent " + to +
                        ". Use setOverrideForce() if you are sure you want to do this!");
            }

            // TODO: Check resolvable Target is not overridden with unresolvable one

            setOverrideForce(source, to);
        } else {
            setOverrideForce(from, to);
        }
    }

    public static final Map<Intent, Intent> DEFAULT_INTENT_OVERRIDES = new HashMap<>();
    static {
        DEFAULT_INTENT_OVERRIDES.put(
                new AndroidSettingFactory.ExternalIntent("Landroid/intent/action/DIAL"),
                new AndroidSettingFactory.ExternalIntent("Landroid/intent/action/DIAL"));
    }

    /**
     *  Set multiple overrides at the same time.
     *
     *  See {@link #setOverride(Intent, Intent)}. 
     */
    public void setOverrides(Map<Intent, Intent> overrides) {
        for (final Intent from : overrides.keySet()) {
            final Intent to = overrides.get(from);
            if (from.equals(to)) {
                registerIntent(to);
            } else {
                setOverride(from, overrides.get(from));
            }
        }
    }

    /**
     *  Just throw in the override.
     *
     *  See {@link #setOverride(Intent, Intent)}.
     */
    public void setOverrideForce(Intent from, Intent to) {
        if (from == null) {
            throw new IllegalArgumentException("The Intent given as 'from' is null");
        }
        if (to == null) {
            throw new IllegalArgumentException("The Intent given as 'to' is null");
        }

        logger.info("Override Intent {} to {}", from, to);
        overrideIntents.put(from, to);
    }

    /**
     *  Get Intent with applied overrides.
     *
     *  If there are no overrides or the Intent is not registered return it as is.
     *  
     *  See {@link #setOverride(Intent, Intent)}.
     *  See {@link #registerIntent(Intent)}.
     *
     *  @param  intent  The intent to resolve
     *  @return where to resolve it to or the given intent if no information is available
     *
     *  TODO: TODO: Malicious Intent-Table could cause endless loops
     */
    public Intent getIntent(Intent intent) {
        if (overrideIntents.containsKey(intent)) {
            Intent ret = overrideIntents.get(intent);
            while (!(ret.equals(intent))) {
                // Follow the chain of overrides
                if (!overrideIntents.containsKey(intent)) {
                    logger.info("Resolved {} to {}", intent, ret);
                    return ret;
                } else {
                    logger.debug("Resolving {} hop over {}", intent, ret);
                    final Intent old = ret;
                    ret = overrideIntents.get(ret);

                    if (ret == old) { // Yes, ==
                        // This is an evil hack(tm). I should fix the Intent-Table!
                        logger.warn("Malformend Intent-Table, staying with " + ret + " for " + intent);
                        return ret;
                    }
                }
            }
            ret = overrideIntents.get(ret); // Once again to get Info set in register
            logger.info("Resolved {} to {}", intent, ret);
            return ret;
        } else {
            logger.info("No information on {} hash: {}", intent, intent.hashCode());
            for (Intent known : overrideIntents.keySet()) {
                logger.debug("Known Intents: {} hash: {}", known, known.hashCode());
            }
            return intent;
        }
    }

    /**
     *  Searches Intent specifications for the occurrence of clazz.
     *
     *  @return the intent is registered or there exists an override.
     */
    public boolean existsIntentFor(TypeName clazz) {
        for (Intent i : overrideIntents.keySet()) {
            if (i.getAction().toString().equals(clazz.toString())) { // XXX toString-Matches are shitty
                return true;
            }
        }

        for (Intent i : overrideIntents.values()) {
            if (i.getAction().toString().equals(clazz.toString())) {
                return true;
            }
        }

        return false;
    }

    private transient Map<CallSiteReference, Intent> seenIntentCalls = HashMapFactory.make();
    /**
     *  DO NOT CALL! - This is for IntentContextSelector.
     *
     *  Add information that an Intent was called to the later summary. This is for information-purpose
     *  only and does not change any behavior.
     *
     *  Intents are added as seen - without any resolved overrides.
     */
    public void addCallSeen(CallSiteReference from, Intent intent) {
        seenIntentCalls.put(from, intent);
    }

    /**
     *  Return all Sites, that start Components based on Intents.
     */
    public  Map<CallSiteReference, Intent> getSeen() {
        return seenIntentCalls; // No need to make read-only
    }

    private boolean allowIntentRerouting = true;

    /**
     *  Controll modification of an Intents target after construction.
     *
     *  After an Intent has been constructed its target may be changed using functions like
     *  setAction or setComponent. 
     *  This setting controlls the behavior of the model on occurrence of such a function:
     *
     *  If set to false the Intent will be marked as unresolvable.
     *  
     *  If set to true the first occurrence of such a function changes the target of the
     *  Intent unless:
     *
     *  * The Intent was explicit and the new action is not: The call gets ignored
     *  * The Intent was explicit and the new target is explicit: It becomes unresolvable
     *  * It's the second occurrence of such a function: It becomes unresolvable
     *  * It was resolvable: It becomes unresolvable
     *
     *  The default is to activate this behavior.
     *
     *  @param  allow   Allow rerouting as described
     *  @return previous setting
     */
    public boolean setAllowIntentRerouting(boolean allow) {
        boolean prev = allowIntentRerouting;
        allowIntentRerouting = allow;
        return prev;
    }

    /**
     *  Controll modification of an Intents target after construction.
     *
     *  See: {@link #setAllowIntentRerouting(boolean)}. 
     *
     *  @return the set behavior or true, the default
     */
    public boolean isAllowIntentRerouting() {
        return allowIntentRerouting;
    }

    /**
     *  Last 8 digits encode the date.
     */
    private final static long serialVersionUID = 8740020131212L;
}
