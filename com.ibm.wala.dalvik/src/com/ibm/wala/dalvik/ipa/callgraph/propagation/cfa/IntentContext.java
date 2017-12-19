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

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/**
 *  Wraps an Intent to be suitable to be a Context-Element.
 *
 *  This class takes a parent so we don't loose information by overwriting.
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-14
 */
public class IntentContext implements Context {
    /**
     *  Key into the Context that represents the Intent.
     */
    public static final ContextKey INTENT_KEY = Intent.INTENT_KEY;
    private final Intent intent;
    private final Context parent;

    public IntentContext(Intent intent) {
        this(null, intent);
    }
    
    public IntentContext(Context parent, Intent intent) {
        this.parent = parent;
        this.intent = intent;
    }

    /**
     *  Looks up a ContextKey in the Context.
     *  
     *  @return an Intent or parent-managed object.
     *  @throws IllegalArgumentException if the name is null.
     */
    @Override
    public ContextItem get (ContextKey name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (name.equals(INTENT_KEY)) {
            return intent;
        } else if(this.parent != null) {
            return this.parent.get(name);
        } else {
            return null;
        }
    }

    /**
     *  Special equality: Object may be equal to an object without associated Intent.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof IntentContext) {
            IntentContext other = (IntentContext) obj;
            if (this.intent.equals(other.intent)) {
                if (this.parent != null) {
                    return this.parent.equals(other.parent);
                } else {
                    return other.parent == null;
                }
            } else {
                return false;
            }
        } else {
            if (this.parent != null) {
                // TODO: do we really want this?
                return this.parent.equals(obj);
            } else {
                return false;
            }
        }
    }

  @Override
  public int hashCode() {
    // TODO: do we want to "clash" with the parent here?
    return 71891 * intent.hashCode();
  }

  @Override
  public String toString() {
      if (this.parent == null) {
        return "Intent: " + this.intent;
      } else {
        return "Intent: " + this.intent + ", parent: " + this.parent;
      }
  }

  public Intent getIntent() {
    return this.intent;
  }
}
