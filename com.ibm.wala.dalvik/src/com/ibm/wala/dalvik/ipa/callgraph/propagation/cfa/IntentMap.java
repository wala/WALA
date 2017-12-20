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

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

/**
 *  Stores references to the WALA-Intent objects.
 *
 *  This class is only of use in conjunction with the IntentContextSelector
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 */
/*package*/ class IntentMap {
    private final Map<InstanceKey, Intent> seen = new HashMap<>();
    private final Map<Intent, Intent> immutables = new HashMap<>();

    public Intent findOrCreateImmutable(final Intent intent) {
        if (immutables.containsKey(intent)) {
            final Intent immutable = immutables.get(intent);
            assert (immutable.getAction().equals(intent.getAction()));
            return immutable;
        } else {
            final Intent immutable = intent.clone();
            immutable.setImmutable();
            immutables.put(intent, immutable);
            
            return immutable;
        }
    }

    public Intent find(final InstanceKey key) throws IndexOutOfBoundsException {
        if (key == null) {
            throw new IllegalArgumentException("InstanceKey may not be null");
        }
        if (! seen.containsKey(key)) {
            throw new IndexOutOfBoundsException("No Intent was seen for key " + key);
        }
        return seen.get(key);
    }

    public Intent create(final InstanceKey key, final String action) {
        if (key == null) {
            throw new IllegalArgumentException("InstanceKey may not be null");
        }
        if (seen.containsKey(key)) {
            throw new IndexOutOfBoundsException("There may only be one Intent for " + key);
        }
        final Intent intent = new Intent(action);
        seen.put(key, intent);
        return intent;
    }

    public Intent create(final InstanceKey key, final Atom action) {
        if (key == null) {
            throw new IllegalArgumentException("InstanceKey may not be null");
        }
        if (seen.containsKey(key)) {
            throw new IndexOutOfBoundsException("There may only be one Intent for " + key);
        }
        final Intent intent = new Intent(action);
        seen.put(key, intent);
        return intent;
    }

    public Intent create(final InstanceKey key) {
        if (key == null) {
            throw new IllegalArgumentException("InstanceKey may not be null");
        }
        if (seen.containsKey(key)) {
            throw new IndexOutOfBoundsException("There may only be one Intent for " + key);
        }
        final Intent intent = new Intent();
        seen.put(key, intent);
        return intent;
    }

    public Intent findOrCreate(final InstanceKey key) {
        if (seen.containsKey(key)) {
            return find(key);
        } else {
            return create(key);
        }
    }

    public void put(final InstanceKey key, final Intent intent) {
        seen.put(key, intent);
    }

    public boolean contains(final InstanceKey key) {
        return seen.containsKey(key);
    }

    public Intent findOrCreate(final InstanceKey key, String action) {
        final Intent intent = findOrCreate(key);
        final Atom foundAction = intent.getAction();
        if (! foundAction.equals(Atom.findOrCreateAsciiAtom(action))) {
            throw new IllegalArgumentException("Actions differ (" + action + ", " +
                    foundAction.toString() + ") for Intent " + key);
        }
        return intent;
    }

    public Intent setAction(final InstanceKey key, final String action, boolean isExplicit) {
        return setAction(key, Atom.findOrCreateAsciiAtom(action), isExplicit);
    }

    public Intent unbind(final InstanceKey key) {
        final Intent intent = find(key);
        intent.unbind();
        return intent;
    }

    public Intent setExplicit(final InstanceKey key) {
        if (contains(key)) {
            final Intent intent = find(key);
            intent.setExplicit();
            return intent;
        } else {
            throw new IllegalArgumentException("setAction: No Intent found for key " + key);
            //final Intent intent = create(key);
            //intent.setExplicit();
            //return intent;
        }
    }

    public Intent setAction(final InstanceKey key, final Atom action, boolean isExplicit) {
        if (contains(key)) {
            final Intent intent = find(key);
            if (isExplicit) {
                intent.setActionExplicit(action);
            } else {
                intent.setAction(action);
            }
            return intent;
        } else {
            
            final Intent intent = create(key, action);
            return intent;
        }
    }

    public Intent setAction(final Intent intent, final String action, boolean isExplicit) {
        for (final InstanceKey candKey : seen.keySet()) {
            if (seen.get(candKey).equals(intent)) {
                return setAction(candKey, action, isExplicit);
            }
        }

        throw new IllegalStateException("The Intent " + intent + " was not registered before!");
    }

    public Intent setAction(final InstanceKey key, final InstanceKey actionKey, boolean isExplicit) {
        if (actionKey == null) {
            
            return find(key);
        }
        final String action;
        {
            if (actionKey instanceof ConstantKey) {
                final Object actionO = ((ConstantKey<?>) actionKey).getValue();
                if (actionO instanceof String) {
                    action = StringStuff.deployment2CanonicalTypeString((String) actionO);
                } else if (actionO instanceof IClass) {
                    action = ((IClass) actionO).getName().toString();
                } else {
                    throw new IllegalArgumentException("Wrong action type: " + actionO.getClass());
                }
            } else {
                
                unbind(key);
                return null;
            }
        }

        return setAction(key, action, isExplicit);
    }
}
