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

import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

/**
 * Determines the target of an Android-Intent.
 *
 * An Intent is generated at each Constructor-Call of an Android-Intent. Information to that Constructor
 * is added as target. 
 *
 * If you want to change the target don't change this Object! Instead place an override using the 
 * AndroidEntryPointManager so no information is lost.
 *
 * Upon the creation of an Intent it's target-type almost always points to UNKNOWN. Again change this
 * using an override.
 *
 * This class contains functions that determine the target-type on the Intent itself. They are intended
 * as fallback only.
 *
 * CAUTION: If you inherit from this class - keep hashCodes and equals clashing!
 *
 * @see     com.ibm.wala.dalvik.util.AndroidEntryPointManager
 * @see     com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector
 * @see     com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextInterpreter
 *
 * @author  Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 * @since   2013-10-12
 */
public class Intent implements ContextItem, Comparable<Intent> {
    /**
     *  Key into the Context that represents the Intent.
     */
    public static final ContextKey INTENT_KEY = new ContextKey() {};
    public enum IntentType {
        /** The target could not be Identified for sure */
        UNKNOWN_TARGET,     
        /** The Class the Intent Action refers to belongs to this App */
        INTERNAL_TARGET,   
        /** The Class the Intent Action refers to belongs to an external App */
        EXTERNAL_TARGET,    
        /** The Action is a Constant defined on the Android-Reference Manual */
        STANDARD_ACTION,    
        SYSTEM_SERVICE,
        /** So External and maybe internal */
        BROADCAST,          
        /** Do not handle intent */
        IGNORE
    }

    private enum Explicit {
        UNSET,
        IMPLICIT,
        EXPLICIT,
        /** An other target was set for an explicit Intent */
        MULTI
    }

    public static final Atom UNBOUND = Atom.findOrCreateAsciiAtom("Unbound");

    private Atom action;
    public Atom uri;
    private IntentType type;
    private Explicit explicit = Explicit.UNSET;
    private AndroidComponent targetCompontent;  // Activity, Service, ...
    private boolean immutable = false;

    public Intent() {
        this.action = null;
    }
    public Intent(String action) {
        this(Atom.findOrCreateAsciiAtom(action));
    }
    public Intent(Atom action) {
        this(action, null);
    }
    public Intent(Atom action, Atom uri) {
        this.action = action;
        this.uri = uri;
        this.type = null;   // Delay computation upon it's need
        this.targetCompontent = null;
    }
    public Intent(TypeName action, Atom uri) {
         this(Atom.findOrCreateAsciiAtom(action.toString()), uri);
    }
    public Intent(TypeName action) {
        this(action, null);
    }

    public void setExplicit() {
        switch (explicit) {
            case UNSET:
                // TODO: This is dangerous?
                explicit = Explicit.EXPLICIT;
                break;
            case EXPLICIT:
                unbind();
                break;
            default:
            	throw new UnsupportedOperationException(String.format("unexpected explicitness setting %s", explicit));
        }
    }

    public boolean isExplicit() {
        return explicit == Explicit.EXPLICIT;
    }

    public void setImmutable() {
        this.immutable = true;
    }

    @Override
    public Intent clone() {
        final Intent clone = new Intent();
        clone.action = this.action; // OK here?
        clone.uri = this.uri;
        clone.type = this.type;
        clone.explicit = this.explicit;
        clone.targetCompontent = this.targetCompontent;
        return clone;
    }


    /**
     *  Set the explicit target of the intent.
     *
     *  If setAction is called multible times on an Intent it becomes UNBOUND.
     */
    public void setActionExplicit(Atom action) {
        if (action == null) {
            throw new IllegalArgumentException("Action may not be null!");
        }

        if (this.action == null) {
            assert (this.explicit == Explicit.UNSET) : "No Action but Intent is not UNSET - is " + this.explicit;
            assert (! immutable) : "Intent was marked immutable - can't change it.";
            this.action = action;
            this.explicit = Explicit.EXPLICIT;
        } else if (isExplicit() && (! this.action.equals(action))) {
            // We already have the explicit target. Ignore the change.
            
            unbind();
        } else if (! isExplicit() ) {
            
            assert (! immutable) : "Intent was marked immutable - can't change it.";
            this.action = action;
            this.explicit = Explicit.EXPLICIT;
            // TODO: Set type?
        } else {
            // Set to same values - OK
        }
    }


    public void unbind() {
        assert (! immutable) : "Intent was marked immutable - can't change it.";
        this.action = UNBOUND;
        this.type = IntentType.UNKNOWN_TARGET;
        this.explicit = Explicit.MULTI;             // XXX shoulb we do this?
    }

    /**
     *  Set the target of the intent.
     *
     *  If setAction is called multible times on an Intent it becomes UNBOUND.
     */
    public void setAction(Atom action) {
        if (this.action == null) {
            assert (! immutable) : "Intent was marked immutable - can't change it.";
            this.action = action;
        } else if (isExplicit()) {
            // We already have the explicit target. Ignore the change.
        } else if (! action.equals(this.action)) {
            unbind();
        }
    }

    public Atom getAction() {
        if (this.action == null)  {
            assert (! isExplicit()) : "Beeing explicit implies having an action!";
            return UNBOUND; 
        }
        return this.action;
    }

    public IntentType getType() {
        if (this.type != null) {
            return this.type;
        } else {
            if (isSystemService(this)) {
                this.type = IntentType.SYSTEM_SERVICE;
            } else if (isStandardAction(this)) {
                this.type = IntentType.STANDARD_ACTION;
            } else if (isInternal(this)) {
                this.type = IntentType.INTERNAL_TARGET;
            } else if (isExternal(this)) {
                this.type = IntentType.EXTERNAL_TARGET;
            } else {
                this.type = IntentType.UNKNOWN_TARGET;
            }
        }
        return this.type;
    }

    /**
     *  Return the type of Component associated with this Intent.
     *
     *  May return null (especially on an UNKNOWN target). The IntentContextInterpreter uses the
     *  IntentStarters.StartInfo to determine the Target. However it is nicer to set the Component
     *  here.
     *
     *  TODO: Set the Component somewhere
     */
    public AndroidComponent getComponent() {
        return this.targetCompontent;
    }

    private static boolean isSystemService(Intent intent) {  
        assert (intent.action != null);
        return (intent.action.getVal(0) != 'L') && (intent.action.rIndex((byte) '/') < 0) && (intent.action.rIndex((byte) '.') < 0);
    }

    /**
     *  Fallback: tries to determine on the Intent itself if it's internal.
     *
     *  Use {@link #isInternal(boolean)} instead.
     *
     *  Recomputes if the Intent is internal.
     *  TODO: 
     *  @param intent 
     *  TODO: Implement it ;)
     *  TODO: What to return if it does not, but Summary-Information is available?
     *  TODO: We should read in the Manifest.xml rather than relying on the packet name!
     */
    private static boolean isInternal(Intent intent) {  // XXX: This may loop forever!
        /*final Intent override = AndroidEntryPointManager.MANAGER.getIntent(intent);

        logger.warn("Intent.isInternal(Intent) is an unsafe fallback!");

        if (override.getType() != IntentType.UNKNOWN_TARGET) {
            return override.isInternal(true);   // The isInternal defined later not this one!
        }*/

        return false;
    }

    /**
     *  Fallback: tries to determine on the Intent itself if it's external.
     *
     *  Use {@link #isExternal(boolean)} instead.
     */
    private static boolean isExternal(Intent intent) {  // XXX: This may loop forever!
        /*final Intent override = AndroidEntryPointManager.MANAGER.getIntent(intent);

        logger.warn("Intent.isExternal(Intent) is an unsafe fallback!");

        if (override.getType() != IntentType.UNKNOWN_TARGET) {
            return override.isExternal(true);   // The isExternal defined later not this one!
        }*/

        if ((intent.action == null ) || (intent.action.equals(UNBOUND))) {
            return false; // Is Unknown
        }

        String pack = AndroidEntryPointManager.MANAGER.guessPackage();
        
       
        if (pack == null) {
            // Unknown so not selected as external
            return false;
        }
        return (! (intent.action.toString().startsWith("L" + pack) || intent.action.toString().startsWith(pack)));
    }

    /**
     *  Fallback: tries to determine on the Intent itself if it's a standard action.
     */
    private static boolean isStandardAction(Intent intent) {    //TODO: This may loop forever!
        /*final Intent override = AndroidEntryPointManager.MANAGER.getIntent(intent);

        logger.warn("Intent.isStandardAction(Intent) is an unsafe fallback!");

        if (override.getType() != IntentType.UNKNOWN_TARGET) {
            return override.isStandard(true);
        }*/
        if ((intent.action == null ) || (intent.action.equals(UNBOUND))) {
            return false; // Is Unknown
        }

        // TODO: Make this static or so
        final Atom andoidIntentAction = Atom.findOrCreateAsciiAtom("Landroid/intent/action");
        return intent.action.startsWith(andoidIntentAction);
    }

    /**
     *  Is the Intents target internally resolvable.
     *
     *  @return if the Intent is associated to a class in the analyzed application.
     *  @param  strict   if false return unknown target as internal
     */
    public boolean isInternal(boolean strict) {
        IntentType type = getType();    // Asserts type is computed
        
        return ((type == IntentType.INTERNAL_TARGET) || ((! strict) && (type == IntentType.UNKNOWN_TARGET)));
    }

    /**
     *  Has the target to be resolved by an external App.
     *
     *  The Intent is not associated to a class in this application or it's a Standard
     *  action defined in the Android Reference Manual.
     *
     * @param   strict    if false return unknown target as external
     */
    public boolean isExternal(boolean strict) {
        IntentType type = getType();    // Asserts type is computed
        
        return ((type == IntentType.EXTERNAL_TARGET) || (type == IntentType.STANDARD_ACTION) 
                || ((! strict) && (type == IntentType.UNKNOWN_TARGET)));
    }

    /**
     *  Is the Intent one of the System-Defined ones.
     *
     *  It's a Standard action defined in the Android Reference Manual. Implies isExternal.
     *
     *  @param  strict    if false return unknown target as standard
     */
    public boolean isStandard(boolean strict) {
        IntentType type = getType();    // Asserts type is computed
        
        return ((type == IntentType.STANDARD_ACTION) || ((! strict) && (type == IntentType.UNKNOWN_TARGET)));
    }


    @Override
    public String toString() {
        StringBuffer ret;
        if ((this.action == null) || (this.action.equals(UNBOUND))) {
            return "Unbound Intent";
        } else if (getType() == IntentType.SYSTEM_SERVICE) {
            ret = new StringBuffer("SystemService(");
        } else {
            ret = new StringBuffer("Intent(");
        }
        ret.append(this.action.toString());
        if (uri != null) {
            ret.append(", ");
            ret.append(this.uri.toString());
        }
        ret.append(") of type ");
        ret.append(getType());
        return ret.toString();
    }

    /**
     *  CLASHES: Does not consider intent-type. 
     *
     *  This clash is however intended: This aids in resolving the override of an Intent.
     *  The AndroidEntryPointManager generates new Intent Objects. Instead of searching all
     *  overrides we get it for free. 
     */
    @Override
    public int hashCode() {
        // DO NOT USE TYPE!
        if (this.uri != null) {
            return getAction().hashCode() * this.uri.hashCode();
        } else {
            return getAction().hashCode();
        }
    }

    /**
     *  Does not consider the associated URI.
     */
    public boolean equalAction(Intent other) {
        return getAction().equals(other.getAction());
    }

    /**
     *  Intents are equal to Intents with other type.
     *
     *  This clash is however intended: This aids in resolving the override of an Intent.
     *  The AndroidEntryPointManager generates new Intent Objects. Instead of searching all
     *  overrides we get it for free. 
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Intent) {
            Intent other = (Intent) o;

            // DO NOT USE TYPE!
            if (this.uri != null) {
                return ( (this.uri.equals(other.uri)) && equalAction(other) ); // && (this.explicit == other.explicit));
            } else {
                return ( (other.uri == null) && equalAction(other) ); // && (this.explicit == other.explicit)) ;
            }
        } else {
            System.err.println("WARNING: Can't compare Intent to " + o.getClass());
            return false;
        }
    }

    public Intent resolve() {
        return AndroidEntryPointManager.MANAGER.getIntent(this);
    }

    @Override
    public int compareTo(Intent other) {
        return getAction().toString().compareTo(other.getAction().toString());
    }
}
