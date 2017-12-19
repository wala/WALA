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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.util.androidEntryPoints.ActivityEP;
import com.ibm.wala.dalvik.util.androidEntryPoints.ApplicationEP;
import com.ibm.wala.dalvik.util.androidEntryPoints.LoaderCB;
import com.ibm.wala.dalvik.util.androidEntryPoints.LocationEP;
import com.ibm.wala.dalvik.util.androidEntryPoints.ProviderEP;
import com.ibm.wala.dalvik.util.androidEntryPoints.ServiceEP;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.SetOfClasses;

/**
 *  Searches an Android application for its EntryPoints.
 *
 *  Iterates the ClassHierarchy matching its elements to a set of hardcoded entrypoint-specifications.
 *  Then optionally uses heuristics to select further entrypoints.
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
public final class AndroidEntryPointLocator {
    private static final Logger logger = LoggerFactory.getLogger(AndroidEntryPointLocator.class);
    private final IProgressMonitor mon;

    /**
     *  Used to control the search mechanisms of AndroidEntryPointLocator.
     */
    public static enum LocatorFlags {
        /**
         *  If this flag is not set only functions of actual Android-Components are returned.
         *
         *  If it's enabled additional call-backs are handled as an entrypoint.
         */
        INCLUDE_CALLBACKS,
        /**
         *  Additionally select all functions that override a function of an AndroidComponent.
         */
        EP_HEURISTIC,
        /**
         *  Additionally select all functions that override a function of the Android stubs.
         */
        CB_HEURISTIC,
        /**
         *  Add the constructor of detected components to the entrypoints.
         */
        WITH_CTOR,
        /**
         *  If a class does not override a method of a component, add a call to the method in the
         *  super-class to the entriepoints.
         */
        WITH_SUPER,
        /**
         *  This will pull in components defined in the Android-API. 
         *  In most cases this is not wanted.
         */
        WITH_ANDROID
    }

    private final static List<AndroidPossibleEntryPoint> possibleEntryPoints = new ArrayList<>();
    protected final Set<LocatorFlags> flags;

    public AndroidEntryPointLocator(final Set<LocatorFlags> flags) {
        if (flags == null) {
            this.flags = EnumSet.noneOf(LocatorFlags.class);
        } else {
            this.flags = flags;
        }
        this.mon = AndroidEntryPointManager.MANAGER.getProgressMonitor();

        populatePossibleEntryPoints();
    }

    /**
     *  Searches a ClassHierarchy for EntryPoints by their method-signature (optionally with heuristics).
     *
     *  Matches the hardcoded signatures against the methods in cha. 
     *  Uses heuristics depending on the LocatorFlags given to the constructor .
     *
     *  @param  cha The ClassHierarchy to be searched
     *  @return partially sorted list of applicable EntryPoints
     */
    public List<AndroidEntryPoint> getEntryPoints(final IClassHierarchy cha) {
        if (cha == null) {
            throw new IllegalArgumentException("I need a ClassHierarchy to search");
        }

        Set<AndroidEntryPoint> entryPoints = new HashSet<>();

        mon.beginTask("Locating Entrypoints", IProgressMonitor.UNKNOWN);
        int dummy = 0;  // for the progress monitor
        for (IClass cls : cha) {
            mon.worked(dummy++);
            if (cls.getName().toString().contains("MainActivity")) {
            	System.err.println("got here");
            }
            if (isExcluded(cls)) continue;
            if (!cls.isInterface() && 
            	!cls.isAbstract() && 
            	!( cls.getClassLoader().getName().equals(AnalysisScope.PRIMORDIAL) ||
            	   cls.getClassLoader().getName().equals(AnalysisScope.EXTENSION)
            	 )) {
nextMethod:
                for (final IMethod m : cls.getDeclaredMethods()) {
                    if (cls.getName().toString().contains("MainActivity")) {
                    	System.err.println("got here: " + m);
                    }
                	// If there is a Method signature in the possible entry points use thatone
                    for (AndroidPossibleEntryPoint e: possibleEntryPoints) {
                        if (e.name.equals(m.getName().toString()) ) {
                            if (this.flags.contains(LocatorFlags.WITH_ANDROID)) {
                                entryPoints.add(new AndroidEntryPoint(e, m, cha));
                            } else if (! isAPIComponent(m)) {
                                entryPoints.add(new AndroidEntryPoint(e, m, cha));
                            }
                            continue nextMethod;
                        }
                    }

                } // for IMethod m
            } 
        } // for IClass : cha

        if (this.flags.contains(LocatorFlags.EP_HEURISTIC) || this.flags.contains(LocatorFlags.CB_HEURISTIC)) {
            final Set<TypeReference> bases = new HashSet<>();

            if (this.flags.contains(LocatorFlags.EP_HEURISTIC)) {
                // Add bases for EP-Heuristic

                if (this.flags.contains(LocatorFlags.INCLUDE_CALLBACKS)) {
                    for (final AndroidComponent compo : AndroidComponent.values()) {
                        if (compo == AndroidComponent.UNKNOWN) continue;
                        if (compo.toReference() == null) {
                            logger.error("Null-Reference for " + compo);
                        } else {
                            bases.add(compo.toReference());
                        }
                    }
                } else {
                    // Restrict the set
                    bases.add(AndroidTypes.Application);
                    bases.add(AndroidTypes.Activity);
                    /** TODO: TODO: add Fragments in getEntryPoints */
                    //bases.add(AndroidTypes.Fragment);
                    bases.add(AndroidTypes.Service);
                    bases.add(AndroidTypes.ContentProvider);
                    bases.add(AndroidTypes.BroadcastReceiver);
                }

                heuristicScan(bases, entryPoints, cha);       
            }
            if (this.flags.contains(LocatorFlags.CB_HEURISTIC)) {
                heuristicAnyAndroid(entryPoints, cha);
            }
        }


        List<AndroidEntryPoint> ret = new ArrayList<>(entryPoints);
        Collections.sort(ret, new AndroidEntryPoint.ExecutionOrderComperator());
        mon.done();
        return ret;
    }

    /**
     *  Select all methods that override a method in base.
     *
     *  If the heuristic selects a method it is added to the eps-argument.
     *
     *  @param  bases   classes to search
     *  @param  eps     The set of detected entrypoints to add to
     *  @param  cha     The ClassHierarchy to search
     */
    private void heuristicScan(Collection<? extends TypeReference> bases, Set<? super AndroidEntryPoint> eps, IClassHierarchy cha) {
        for (final TypeReference base : bases) {
            final IClass baseClass = cha.lookupClass(base);
            this.mon.subTask("Heuristic scan in " + base);
            final Collection<IClass> candids;
            try {
                candids = cha.computeSubClasses(base);
            } catch (IllegalArgumentException e) {  // Pretty agan :(
                logger.error(e.getMessage());
                continue;
            }
            for (final IClass candid : candids) {
                if (isExcluded(candid)) continue;
                if ((! this.flags.contains(LocatorFlags.WITH_ANDROID) ) && (isAPIComponent(candid))) {   
                    // Don't consider internal overrides
                    continue;
                }
                final Collection<IMethod> methods = candid.getDeclaredMethods();
                for (final IMethod method : methods) {


                    if ((method.isInit() || method.isClinit()) && (! this.flags.contains(LocatorFlags.WITH_CTOR))) {
                        logger.debug("Skipping constructor of {}", method); 
                        continue;
                    }
                    if (baseClass.getMethod(method.getSelector()) != null) {
                        final AndroidEntryPoint ep = makeEntryPointForHeuristic(method, cha);
                       
                        if (! eps.contains(ep)) {  // Just to be sure that a previous element stays as-is
                            if (eps.add(ep)) {
                                logger.debug("Heuristic 1: selecting {} for base {}", method, base);
                            }
                        }
                    }
                }
            }
        }
    }

//    private boolean isInnerClass(final TypeReference test) {
//        return test.getName().toString().contains("$"); // PRETTY!
//    }

    private static AndroidEntryPoint makeEntryPointForHeuristic(final IMethod method, final IClassHierarchy cha) {
        AndroidComponent compo;
        { // Guess component
            compo = AndroidComponent.from(method, cha);
            if (compo == AndroidComponent.UNKNOWN) {

            }
        }
        final AndroidEntryPoint ep = new AndroidEntryPoint(selectPositionForHeuristic(), method, cha, compo);

        return ep;
    }

    /**
     *  Select all methods that override or implement any method from the andoidPackage.
     *
     *  Like heuristicScan but with an other restriction: instead of methods overriding methods in base 
     *  select methods whose super-class starts with "Landroid".
     *
     *  @param  eps     The set of detected entrypoints to add to
     */
    private void heuristicAnyAndroid(Set<AndroidEntryPoint> eps, IClassHierarchy cha) {
        final IClassLoader appLoader = cha.getLoader(ClassLoaderReference.Application);
        final Iterator<IClass> appIt = appLoader.iterateAllClasses();

        for (IClass appClass = ((appIt.hasNext())?appIt.next():null); appIt.hasNext(); appClass = appIt.next()) {
            IClass androidClass = appClass; //.getSuperclass(); Override on new
            { // Only for android-classes
                boolean isAndroidClass = false;
                while (androidClass != null) {
                    if (isAPIComponent(androidClass)) {
                        isAndroidClass = true;
                        break;
                    }
                    logger.trace("Heuristic: \t {} is {}", appClass.getName().toString(), androidClass.getName().toString()); 
                    for (IClass iface : appClass.getAllImplementedInterfaces ()) {
                        logger.trace("Heuristic: \t implements {}", iface.getName().toString()); 
                        if (isAPIComponent(iface)) {
                            isAndroidClass = true;
                            break;
                        }
                    }
                    if (isAndroidClass) break;
                    androidClass = androidClass.getSuperclass();
                }
                if (! isAndroidClass) {
                    logger.trace("Heuristic: Skipping non andoid {}", appClass.getName().toString()); 
                    continue; // continue appClass;
                }
            }

            logger.debug("Heuristic: Scanning methods of {}", appClass.getName().toString());
            { // Overridden methods
                if (isAPIComponent(appClass)) continue;
                if (isExcluded(appClass)) continue;
                final Collection<IMethod> methods = appClass.getDeclaredMethods();
                for (final IMethod method : methods) {
                    if ((method.isInit() || method.isClinit()) && (! this.flags.contains(LocatorFlags.WITH_CTOR))) {
                        logger.debug("Skipping constructor of {}", method); 
                        continue;
                    }
                    assert (method.getSelector() != null): "Method has no selector: " + method;
                    assert (androidClass != null): "androidClass is null";
                    if (androidClass.getMethod(method.getSelector()) != null) {
                        final AndroidEntryPoint ep = makeEntryPointForHeuristic(method, cha);

                        if (! eps.contains(ep)) {  // Just to be sure that a previous element stays as-is
                        if (eps.add(ep)) {
                            logger.debug("Heuristic 2a: selecting {}", method);
                        }} else {
                            logger.debug("Heuristic 2a: already selected {}", method);
                        }
                    }
                }
            }

            { // Implemented interfaces
                final Collection<IClass> iFaces = appClass.getAllImplementedInterfaces();
                for (final IClass iFace : iFaces) {
                    if (isAPIComponent(iFace)) {
                        logger.debug("Skipping iFace: {}", iFace);
                        continue;
                    }
                    if (isExcluded(iFace)) continue;
                    logger.debug("Searching Interface {}", iFace);
                    final Collection<IMethod> ifMethods = iFace.getDeclaredMethods();
                    for (final IMethod ifMethod : ifMethods) {
                        final IMethod method = appClass.getMethod(ifMethod.getSelector());
                        if (method != null && method.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
                            // The function is overridden
                            final AndroidEntryPoint ep = new AndroidEntryPoint(selectPositionForHeuristic(), method, cha);

                            if (! eps.contains(ep)) {  // Just to be sure that a previous element stays as-is
                            if (eps.add(ep)) {
                                logger.debug("Heuristic 2b: selecting {}", method);
                            }}
                        } else if (method != null) {
                            // The function is taken from the super-class
                            if (this.flags.contains(LocatorFlags.WITH_SUPER)) {
                                final AndroidEntryPoint ep = makeEntryPointForHeuristic(method, cha);

                                if ((eps.contains(ep)) && (! method.isStatic())) {
                                    // eps.get(ep) ... suuuuuper!
                                    for (final AndroidEntryPoint eps_ep : eps) {
                                        if (eps_ep.equals(ep)) {
                                            final TypeReference[] oldTypes = eps_ep.getParameterTypes(0);
                                            final TypeReference[] newTypes = new TypeReference[oldTypes.length + 1];
                                            System.arraycopy(oldTypes, 0, newTypes, 0, oldTypes.length);
                                            newTypes[oldTypes.length] = appClass.getReference();
                                            eps_ep.setParameterTypes(0, newTypes);
                                            logger.debug("New This-Types for {} are {}", method.getSelector(), Arrays.toString(newTypes));
                                        }
                                    }
                                } else {
                                    if (! method.isStatic()) {
                                        ep.setParameterTypes(0, new TypeReference[]{appClass.getReference()});
                                    }
                                    eps.add(ep);
                                    logger.debug("Heuristic 2b: selecting from super {}", method);
                                }
                            } else {
                                logger.debug("Heuristic 2b: Skipping {}", method);
                            }
                        }
                    }
                }
            } // Of 'Implemented interfaces'
        }
    }

    private static boolean isAPIComponent(final IMethod method) {
        return isAPIComponent(method.getDeclaringClass());
    }

    private static boolean isAPIComponent(final IClass cls) {
        ClassLoaderReference clr = cls.getClassLoader().getReference();
		if (! (clr.equals(ClassLoaderReference.Primordial) || clr.equals(ClassLoaderReference.Extension))) {
            if (cls.getName().toString().startsWith("Landroid/")) {
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    private static boolean isExcluded(final IClass cls) {
    	final SetOfClasses set = cls.getClassHierarchy().getScope().getExclusions();
    	if (set == null) {
    		return false; // exclusions null ==> no exclusions ==> no class is excluded
    	} else {
    		final String clsName = cls.getReference().getName().toString().substring(1);
    		return set.contains(clsName);
    	}
    }

    /**
     *  The position to place entrypoints selected by a heuristic at.
     *
     *  Currently all methods are placed at ExecutionOrder.MULTIPLE_TIMES_IN_LOOP.
     */
    private static ExecutionOrder selectPositionForHeuristic() {
        return ExecutionOrder.MULTIPLE_TIMES_IN_LOOP;
    }

    /**
     *  A definition of an Entrypoint functions o the App are matched against.
     *
     *  To locate the Entrypoints of the analyzed Application all their Methods are matched
     *  against a set of hardcoded definitions. This set consists of AndroidPossibleEntryPoints.
     *  An AndroidPossibleEntryPoint is rather useless as you can build an actual AndroidEntryPoint
     *  without having a AndroidPossibleEntryPoint.
     *
     *  To extend the set of known definitions have a look at the classes ActivityEP, ServiceEP, ...
     */
    public static class AndroidPossibleEntryPoint implements AndroidEntryPoint.IExecutionOrder  {
//        private final AndroidComponent cls;
        private final String name;
        public final AndroidEntryPoint.ExecutionOrder order;

        public AndroidPossibleEntryPoint(String n, ExecutionOrder o) {
//            cls = c; 
            name = n; 
            order = o; 
        }
 
        public AndroidPossibleEntryPoint(String n, AndroidPossibleEntryPoint o) {
//            cls = c; 
            name = n; 
            order = o.order; 
        }
       
        @Override public int getOrderValue() { return order.getOrderValue(); }
        @Override public int compareTo(AndroidEntryPoint.IExecutionOrder o) { return order.compareTo(o); }
        @Override public AndroidEntryPoint.ExecutionOrder getSection() { return order.getSection(); }

        public static class ExecutionOrderComperator implements Comparator<AndroidPossibleEntryPoint> {
            @Override public int compare(AndroidPossibleEntryPoint a, AndroidPossibleEntryPoint b) {
                return a.order.compareTo(b.order);
            }
        }
    }

    /**
     *  Read in the hardcoded entrypoint-specifications.
     *
     *  Builds a list of possible EntryPoints in a typical Android application and produces information on
     *  the order in which they should be modeled.
     */
    private void populatePossibleEntryPoints() {
        // Populate the list of possible EntryPoints
    	if (possibleEntryPoints.size() > 0) {
    		// already populated
    		return;
    	}
        ApplicationEP.populate(possibleEntryPoints);
		ActivityEP.populate(possibleEntryPoints);
		ServiceEP.populate(possibleEntryPoints);
		ProviderEP.populate(possibleEntryPoints);
		
        if (this.flags.contains(LocatorFlags.INCLUDE_CALLBACKS)) {
            LocationEP.populate(possibleEntryPoints);
            LoaderCB.populate(possibleEntryPoints);
        }
            
        Collections.sort(possibleEntryPoints, new AndroidPossibleEntryPoint.ExecutionOrderComperator());
    }

    public static void debugDumpEntryPoints(List<AndroidPossibleEntryPoint> eps) {
        int dfa = 0;
        int indent = 0;

        for (AndroidPossibleEntryPoint ep: eps) {
            if (dfa == 0) {
                System.out.println("AT_FIRST:");
                indent = 1;
                dfa++;
            }
            if ((dfa == 1) && (ep.getOrderValue() >= ExecutionOrder.BEFORE_LOOP.getOrderValue())) {
                System.out.println("BEFORE_LOOP:");
                dfa++;
            }
            if ((dfa == 2) && (ep.getOrderValue() >= ExecutionOrder.START_OF_LOOP.getOrderValue())) {
                System.out.println("START_OF_LOOP:");
                indent = 2;
                dfa++;
            }
            if ((dfa == 3) && (ep.getOrderValue() >= ExecutionOrder.MIDDLE_OF_LOOP.getOrderValue())) {
                System.out.println("MIDDLE_OF_LOOP:");
                dfa++;
            }
            if ((dfa == 4) && (ep.getOrderValue() >= ExecutionOrder.MULTIPLE_TIMES_IN_LOOP.getOrderValue())) {
                System.out.println("MULTIPLE_TIMES_IN_LOOP:");
                indent = 3;
                dfa++;
            }
            if ((dfa == 5) && (ep.getOrderValue() >= ExecutionOrder.END_OF_LOOP.getOrderValue())) {
                System.out.println("END_OF_LOOP:");
                indent = 2;
                dfa++;
            }
            if ((dfa == 6) && (ep.getOrderValue() >= ExecutionOrder.AFTER_LOOP.getOrderValue())) {
                System.out.println("AFTER_LOOP:");
                indent = 1;
                dfa++;
            }
            if ((dfa == 7) && (ep.getOrderValue() >= ExecutionOrder.AT_LAST.getOrderValue())) {
                System.out.println("AT_LAST:");
                indent = 1;
                dfa++;
            }

            for (int i=0; i<indent; i++) System.out.print("\t");
            System.out.println(ep.name + " metric: " + ep.getOrderValue());
        }
    }
}
