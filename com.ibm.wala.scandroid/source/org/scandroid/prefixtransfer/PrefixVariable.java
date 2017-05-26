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
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.prefixtransfer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.ibm.wala.fixpoint.AbstractVariable;


public class PrefixVariable extends AbstractVariable<PrefixVariable>{

    // map instance keys to their prefixes
    public HashMap<Integer, String> knownPrefixes = new HashMap<>();
    public HashSet<Integer> fullPrefixKnown = new HashSet<>();

    // TODO: keep track of completely known strings (not just known by prefix)
    //  HashMap<Integer, String> knownStrings = new HashMap<Integer,String>();

    @Override
    public void copyState(PrefixVariable v) {
        knownPrefixes.clear();
        knownPrefixes.putAll(v.knownPrefixes);
        fullPrefixKnown.clear();
        fullPrefixKnown.addAll(v.fullPrefixKnown);
    }

    public static String intersect(String one, String two)
    {
        int i = 0;
        while(i < one.length() && i < two.length())
        {
            if(one.charAt(i) != two.charAt(i))
                break;
            i++;
        }
        return one.substring(0, i);
    }

    public String getPrefix(int instance)
    {
        return knownPrefixes.get(instance);
    }

    public boolean updateAll(PrefixVariable other)
    {
        boolean changed = false;
        for(Entry<Integer,String> e:other.knownPrefixes.entrySet())
        {
            changed = update(e.getKey(),e.getValue()) || changed;
        }
        for (Integer i: other.fullPrefixKnown) {
            changed = include(i) || changed;
        }
        return changed;
    }

    public boolean include(Integer i)
    {
        if (fullPrefixKnown.contains(i)) return false;
        fullPrefixKnown.add(i);
        return true;
    }

    // set an instance key to have a known prefix
    public boolean update(Integer instance, String prefix)
    {
        String prevPrefix = knownPrefixes.get(instance);

        if(prevPrefix == null)
        {
            knownPrefixes.put(instance, prefix);
            return true;
        }
        else
        {
            String newPrefix = intersect(prevPrefix,prefix);
            if(newPrefix.equals(prevPrefix))
                return false;
            knownPrefixes.put(instance, newPrefix);
            return true;
        }
    }

    @Override
    public String toString() {
        return knownPrefixes.toString();
    }

    // set an instance key to be a particular constant
//  public boolean setConstant(Integer instance, String prefix)
//  {
//      return false;
//  }
}
