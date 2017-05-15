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

package org.scandroid.prefixtransfer.modeledAllocations;

import org.scandroid.prefixtransfer.InstanceKeySite;
import org.scandroid.prefixtransfer.PrefixVariable;

public class ConstantString extends InstanceKeySite {

    final String constantValue;
    final int instanceID;

    public ConstantString(int instanceID, String constantValue)
    {
        this.constantValue = constantValue;
        this.instanceID = instanceID;
    }

    @Override
    public PrefixVariable propagate(PrefixVariable input) {
//      System.out.println("Propagating at: " + instanceID + " (" + constantValue + ")");
        PrefixVariable retVal = new PrefixVariable();
        retVal.update(instanceID, constantValue);
        retVal.include(instanceID);
        return retVal;
    }

    @Override
    public String toString() {
        return ("ConstantString(instanceID = " + instanceID + "; value = " + constantValue + ")");
    }

    @Override
    public int instanceID() {
        return instanceID;
    }

}
