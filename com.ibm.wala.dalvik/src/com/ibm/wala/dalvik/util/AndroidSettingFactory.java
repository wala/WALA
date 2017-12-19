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

import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

/**
 *  Generate a Settings-Object from a String-Representation.
 *
 *  This is for use by a parser to generate the Objects to place in the AndroidEntryPointManager.
 *
 *  @see    AndroidManifestXMLReader
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-14
 */
public class AndroidSettingFactory {
    /**
     *  Add an Intent that is _shure_ to be handled internally _only_.
     *
     *  If there was an additional external handling of this intent it will be ignored!
     */
    public static class InternalIntent extends Intent {
        @Override
        public IntentType getType() {
            return Intent.IntentType.INTERNAL_TARGET;
        }
        public InternalIntent(String action) { super(action); }
        public InternalIntent(Atom action) { super(action); }
        public InternalIntent(Atom action, Atom uri) { super(action, uri); }
        @Override   // Force clash!
        public int hashCode() { return super.hashCode(); }
        @Override   // Force clash!
        public boolean equals(Object o) { return super.equals(o); }
    }

    public static class UnknownIntent extends Intent {
        @Override
        public IntentType getType() {
            return Intent.IntentType.UNKNOWN_TARGET;
        }
        public UnknownIntent(String action) { super(action); }
        public UnknownIntent(Atom action) { super(action); }
        public UnknownIntent(Atom action, Atom uri) { super(action, uri); }
        @Override   // Force clash!
        public int hashCode() { return super.hashCode(); }
        @Override   // Force clash!
        public boolean equals(Object o) { return super.equals(o); }
    }

    public static class ExternalIntent extends Intent {
        @Override
        public IntentType getType() {
            return Intent.IntentType.EXTERNAL_TARGET;
        }
        public ExternalIntent(String action) { super(action); }
        public ExternalIntent(Atom action) { super(action); }
        public ExternalIntent(Atom action, Atom uri) { super(action, uri); }
        @Override   // Force clash!
        public int hashCode() { return super.hashCode(); }
        @Override   // Force clash!
        public boolean equals(Object o) { return super.equals(o); }
    }

    public static class StandardIntent extends Intent {
        @Override
        public IntentType getType() {
            return Intent.IntentType.STANDARD_ACTION;
        }
        public StandardIntent(String action) { super(action); }
        public StandardIntent(Atom action) { super(action); }
        public StandardIntent(Atom action, Atom uri) { super(action, uri); }
        @Override   // Force clash!
        public int hashCode() { return super.hashCode(); }
        @Override   // Force clash!
        public boolean equals(Object o) { return super.equals(o); }
    }

    public static class IgnoreIntent extends Intent {
        @Override
        public IntentType getType() {
            return Intent.IntentType.IGNORE;
        }
        public IgnoreIntent(String action) { super(action); }
        public IgnoreIntent(Atom action) { super(action); }
        public IgnoreIntent(Atom action, Atom uri) { super(action, uri); }
        @Override   // Force clash!
        public int hashCode() { return super.hashCode(); }
        @Override   // Force clash!
        public boolean equals(Object o) { return super.equals(o); }
    }


    /**
     *  Make an intent.
     *
     *  @param  pack    The applications package. May be null if unknown - but this may yield an exception
     *  @param  name    The Action this intent represents
     *  @param  uri     The URI to match may be null
     *  @throws IllegalArgumentException If name was null or starts with a dot and pack is null
     *  TODO: Check Target-Types
     */
    public static Intent intent(String pack, String name, String uri) {
        if ((name == null) || (name.isEmpty())) {
            throw new IllegalArgumentException ("name may not be null or empty");
        }
        Intent.IntentType type = Intent.IntentType.UNKNOWN_TARGET;
        
        if (name.startsWith(".")) {
            if ((pack == null) || (pack.isEmpty())) {
                throw new IllegalArgumentException("The pack is needed to resolve the full name of " + name + ", but it's empty");
            }
            name = pack + name;
            type = Intent.IntentType.INTERNAL_TARGET;   // TODO Ehhh...
        } else if (!(name.contains("."))) {
            if ((pack != null) && (!pack.isEmpty())) {
                name = pack + "." + name;
            }
            type = Intent.IntentType.INTERNAL_TARGET;   // TODO Ehhh...
        } else if ((pack != null) && (name.startsWith(pack))) {
            type = Intent.IntentType.INTERNAL_TARGET;   // TODO Ehhh...
        } else if (name.startsWith("android.intent.action")) {
            type = Intent.IntentType.STANDARD_ACTION;
        } 
        
        // convert name to the L-Slash format..
        if ((name.startsWith("L") || name.contains("."))) {
            name = StringStuff.deployment2CanonicalTypeString(name);
        }

        final Atom action = Atom.findOrCreateAsciiAtom(name);
        final Intent ret;
        Atom mUri = null;
        if (uri != null) {
            mUri = Atom.findOrCreateAsciiAtom(uri);
        }
        if (type == Intent.IntentType.INTERNAL_TARGET) {
            if (uri != null) {
                ret = new InternalIntent(action, mUri);
            } else {
                ret = new InternalIntent(action);
            }
        } else if (type == Intent.IntentType.STANDARD_ACTION) {
            if (uri != null) {
                ret = new StandardIntent(action, mUri);
            } else {
                ret = new StandardIntent(action);
            }
        } else {
            if (uri != null) {
                ret = new StandardIntent(action, mUri);
            } else {
                ret = new StandardIntent(action);
            }
        }

        return ret;
    }

    //
    //  Short-Hand functions follow...
    //
    public static Intent intent(String fullyQualifiedAction, String uri) {
        if (fullyQualifiedAction.startsWith(".")) {
            String pack = AndroidEntryPointManager.MANAGER.getPackage();
            if (pack != null) {
                return intent(pack, fullyQualifiedAction, uri);
            } else {
            throw new IllegalArgumentException("The action " + fullyQualifiedAction + " is not fully qualified and the application package is unknown! Use " +
                    " intent(String pack, String name, String uri) to build the intent!");
            }
        } else {
            return intent(null, fullyQualifiedAction, uri);
        }
    }

    public static Intent intent(String fullyQualifiedAction) {
        if (fullyQualifiedAction.startsWith(".")) {
            throw new IllegalArgumentException("The action " + fullyQualifiedAction + " is not fully qualified! Use " +
                    " intent(String pack, String name, null) to build the intent!");
        }
        return intent(null, fullyQualifiedAction, null);
    }
}
