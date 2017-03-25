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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;

public class BlockSearch {

    private ArrayList<ISSABasicBlock> blockQueue = new ArrayList<>();
    private int location = 0;

    private final SSACFG cfg;

    BlockSearch(IR ir)
    {
        cfg = ir.getControlFlowGraph();
    }

    public ISSABasicBlock searchFromBlock(ISSABasicBlock b, Set<ISSABasicBlock> targets)
    {
        blockQueue.clear();
        location = 0;
        Iterator<ISSABasicBlock> startNodes = cfg.getPredNodes(b);
        while(startNodes.hasNext())
        {
            blockQueue.add(startNodes.next());
        }

        ISSABasicBlock candidate = null;
        while(location < blockQueue.size())
        {
            ISSABasicBlock current = blockQueue.get(location);
            location++;
            // TODO: inspect current for function calls or other instructions that could confuse the analysis
            if(targets.contains(current))
            {
                if(candidate == null)
                {
                    candidate = current;
                }
                else if(candidate != current)
                {
                    return null;
                }
                continue;
            }
            else
            {
                Iterator<ISSABasicBlock> predNodes = cfg.getPredNodes(current);
                while(predNodes.hasNext())
                {
                    blockQueue.add(predNodes.next());
                }
            }
        }
        return candidate;
    }
}
