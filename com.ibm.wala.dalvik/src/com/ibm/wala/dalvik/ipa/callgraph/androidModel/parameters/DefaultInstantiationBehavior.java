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
package com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.dalvik.util.AndroidTypes;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 *  Contains some predefined behaviors.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-25
 */
public class DefaultInstantiationBehavior extends IInstantiationBehavior {

    /* package-private */ static final class BehviourValue implements Serializable {
		private static final long serialVersionUID = 190943987799306506L;
		public final InstanceBehavior behaviour;
        public final Exactness exactness;
        public final BehviourValue cacheFrom;
        public BehviourValue(final InstanceBehavior behaviour, final Exactness exactness, final BehviourValue cacheFrom) {
            this.behaviour = behaviour;
            this.exactness = exactness;
            this.cacheFrom = cacheFrom;
        }

        /**
         *  If the value can be derived using an other mapping.
         */
        public boolean isCached() {
            return this.cacheFrom != null;
        }
    }

    /* package-private */ static final class BehaviorKey<T> implements Serializable {
        private static final long serialVersionUID = -1932639921432060660L;
        // T is expected to be TypeName or Atom
        final T base;

        public BehaviorKey(T base) {
            this.base = base;
        }

        public static BehaviorKey<TypeName> mk(TypeName base) {
            return new BehaviorKey<>(base);
        }

        public static BehaviorKey<Atom> mk(Atom base) {
            return new BehaviorKey<>(base);
        }

        public static BehaviorKey<Atom> mkPackage(String pack) {
            return new BehaviorKey<>(Atom.findOrCreateAsciiAtom(pack));
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BehaviorKey) {
                BehaviorKey<?> other = (BehaviorKey<?>) o;
                return base.equals(other.base);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.base.hashCode();
        }

        @Override
        public String toString() {
            return "<BehaviorKey of " + base.toString() + ">";
        }
    }


    private final Map<BehaviorKey<?>, BehviourValue> behaviours = new HashMap<>();
    private final transient IClassHierarchy cha;

    public DefaultInstantiationBehavior(final IClassHierarchy cha) {
        this.cha = cha;

        behaviours.put(BehaviorKey.mkPackage("Ljava/lang"), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(TypeName.string2TypeName("Ljava/lang/Object")), new BehviourValue( // TypeReference.JavaLangObjectName is private
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(
                        TypeReference.findOrCreateArrayOf(TypeReference.JavaLangObject).getName()              
                    ), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.BundleName), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.ActivityName), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.ServiceName), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.BroadcastReceiverName), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.ContentProviderName), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null));
       
        // Walas method to create instances has problems with the menu-stuff
        behaviours.put(BehaviorKey.mk(AndroidTypes.MenuName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.ContextMenuName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.MenuItemName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        
        // Wala can't handle:
        behaviours.put(BehaviorKey.mk(AndroidTypes.ActionModeName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.AttributeSetName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));
        behaviours.put(BehaviorKey.mk(AndroidTypes.ActionModeCallbackName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));

        behaviours.put(BehaviorKey.mk(AndroidTypes.KeyEventName), new BehviourValue(
                    InstanceBehavior.CREATE,
                    Exactness.EXACT,
                    null));

        behaviours.put(BehaviorKey.mkPackage("Landroid/support/v4/view"), new BehviourValue(
                    InstanceBehavior.REUSE,
                    Exactness.EXACT,
                    null)); 
        /* 
        behaviours.put(Atom.findOrCreateAsciiAtom("Landroid/database"), IInstanciationBehavior.InstanceBehavior.REUSE);
        behaviours.put(Atom.findOrCreateAsciiAtom("Landroid/support/v4/app/FragmentActivity"), IInstanciationBehavior.InstanceBehavior.REUSE);
        */

    }

    /**
     *  {@inheritDoc}
     *
     *  @param  asParameterTo   not considered
     *  @param  inCall          not considered
     *  @param  withName        not considered
     */
    @Override
    public InstanceBehavior getBehavior(final TypeName type, final TypeName asParameterTo, final MethodReference inCall, final String withName) {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        final BehaviorKey<TypeName> typeK = BehaviorKey.mk(type);
        if (behaviours.containsKey(typeK)) {
            BehviourValue typeV = behaviours.get(typeK);
            while (typeV.cacheFrom != null) {
                typeV = typeV.cacheFrom;
            }
            return typeV.behaviour;
        }

        // Search based on package
        {
            final Atom pack = type.getPackage();
            if (pack != null) {
                final BehaviorKey<Atom> packK= BehaviorKey.mk(pack);
                if (behaviours.containsKey(packK)) {
                    // Add (cache) the result
                    final BehviourValue packV = behaviours.get(packK);
                    final InstanceBehavior beh = packV.behaviour;
                    behaviours.put(typeK, new BehviourValue(beh, Exactness.PACKAGE, packV));
                    return beh;
                }
            }
        }

        // Search the super-classes
        {
            if (this.cha != null) {
                IClass testClass = null;
                for (final IClassLoader loader : this.cha.getLoaders()) {
                    testClass = loader.lookupClass(type);
                    if (testClass != null) {
                        testClass = testClass.getSuperclass();
                        break;
                    }
                }
                while (testClass != null) {
                    final BehaviorKey<TypeName> testKey = BehaviorKey.mk(testClass.getName());
                    if (behaviours.containsKey(testKey)) {
                        // Add (cahce) the result
                        final BehviourValue value = behaviours.get(testKey);
                        final InstanceBehavior beh = value.behaviour;
                        behaviours.put(typeK, new BehviourValue(beh, Exactness.INHERITED, value));
                        return beh;
                    }
                    testClass = testClass.getSuperclass();
                }
            } else {
                
            }
        } // */

        // Search based on prefix
        {
            String prefix = type.toString();
            while (prefix.contains("/")) {
                prefix = prefix.substring(0, prefix.lastIndexOf("/") - 1);
                final BehaviorKey<Atom> prefixKey= BehaviorKey.mk(Atom.findOrCreateAsciiAtom(prefix));
                if (behaviours.containsKey(prefixKey)) {
                    // cache
                    final BehviourValue value = behaviours.get(prefixKey);
                    final InstanceBehavior beh = value.behaviour;
                    behaviours.put(typeK, new BehviourValue(beh, Exactness.PREFIX, value));
                    return beh;
                }
            }
        } // */

        // Fall back to default
        {
            final InstanceBehavior beh = getDafultBehavior();
            final BehviourValue packV = new BehviourValue(beh, Exactness.DEFAULT, null);
            behaviours.put(typeK, packV);
            return beh;
        }
    }

    /**
     *  {@inheritDoc}
     *
     *  The DefaultInstanciationBehavior only knows EXACT, PACKAGE, PREFIX and DEFAULT 
     */
    @Override
    public Exactness getExactness(final TypeName type, final TypeName asParameterTo, final MethodReference inCall, final String withName) {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        final BehaviorKey<TypeName> typeK = BehaviorKey.mk(type); 
        if (! behaviours.containsKey(typeK)) {
            // Use sideeffect: caches.
            getBehavior(type, asParameterTo, inCall, withName);
        }

        return behaviours.get(typeK).exactness;
    }

    /**
     *  @return InstanceBehavior.REUSE
     */
    @Override
    public InstanceBehavior getDafultBehavior() {
        return InstanceBehavior.REUSE;
    }


    /**
     *  Convert a TypeName back to an Atom.
     */
    protected static Atom type2atom(TypeName type) {
        return Atom.findOrCreateAsciiAtom(type.toString());
    }

    //
    // (De-)Serialization stuff follows
    //
    
    /**
     *  The last eight digits encode the date.
     */
    private static final long serialVersionUID = 89220020131212L;
   
    /**
     *  Including the cache may be useful to get all seen types.
     */
    public transient boolean serializationIncludesCache = true;

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        if (this.serializationIncludesCache) {
            stream.writeObject(this.behaviours);
        } else {
            final Map<BehaviorKey<?>, BehviourValue> strippedBehaviours = new HashMap<>();
            for (final BehaviorKey<?> key : this.behaviours.keySet()) {
                final BehviourValue val = this.behaviours.get(key);
                if (! val.isCached() ) {
                    strippedBehaviours.put(key, val);
                }
            }
            stream.writeObject(strippedBehaviours);
        }
    }

    /**
     *  For no apparent reason not intended to be deserialized.
     *
     *  During the implementation I thought of the DefaultInstantiationBehavior to be immutable so 
     *  hard-coded behaviors don't get mixed with loaded ones. It may be deserialized but using a
     *  LoadedInstantiationBehavior instead may be a better way (as it starts in an empty state)
     */
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream stream) 
        throws IOException, ClassNotFoundException {

        DefaultInstantiationBehavior.this.behaviours.clear();
        this.behaviours.putAll((Map<BehaviorKey<?>, BehviourValue>) stream.readObject());
    }

}
