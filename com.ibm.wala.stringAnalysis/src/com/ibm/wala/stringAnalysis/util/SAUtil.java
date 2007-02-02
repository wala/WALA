/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.stringAnalysis.util;

import java.util.*;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.grammar.CDVariable;

public class SAUtil extends AUtil {
    static public class Domo extends com.ibm.wala.cast.ipa.callgraph.Util {
        static public IR getIR(CallGraph cg, CGNode node) {
            //IMethod m = node.getMethod();
            //Context c = node.getContext();
            IR ir = cg.getInterpreter(node).getIR(node, null);
            return ir;
        }
        
        // Set<CGNode> findIRs(CallGraph cg, String signature) {
        static public Set findCGNodes(CallGraph cg, String signature) {
            Set s = new HashSet();
            for (Iterator i = cg.iterateNodes(); i.hasNext(); ) {
                CGNode node = (CGNode) i.next();
                String sig = node.getMethod().getSignature();
                if (signature.equals(sig)) {
                    s.add(node);
                }
            }
            return s;
        }
        
        static public int findPC(IR ir, SSAInstruction instruction) {
            SSAInstruction instructions[] = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++ ) {
                if (instruction == instructions[i]) {
                    return i;
                }
            }
            return -1;
        }
        
        static public int[] findValueNumbers(CallGraph cg, CGNode node, String varName) {
            LocalNameTable ltable = new LocalNameTable(getIR(cg, node));
            return ltable.getValueNumbers(varName);
        }
        
        static public int findValueNumber(CallGraph cg, CGNode node, String varName, int line) {
            IR ir = getIR(cg, node);
            LocalNameTable ltable = new LocalNameTable(ir);
            int vals[] = ltable.getValueNumbers(varName);
            SSAInstruction instructions[] = ir.getInstructions();
            for (int i = 0; i < instructions.length; i++) {
                int l = ir.getMethod().getLineNumber(i);
                if (l != line) continue;
                for (int j = 0; j < vals.length; j++) {
                    String names[] = ir.getLocalNames(i, vals[j]);
                    if (names == null) continue;
                    for (int k = 0; k < names.length; k++) {
                        if (names[k].equals(varName)) {
                            return vals[j];
                        }
                    }
                }
            }
            return -1;
        }

        static public int[] findValueNumbers(CallGraph cg, String signature, String varName) {
            Set nodes = findCGNodes(cg, signature);
            Set vSet = new HashSet();
            for (Iterator i = nodes.iterator(); i.hasNext(); ) {
                CGNode node = (CGNode) i.next();
                int vals[] = findValueNumbers(cg, node, varName);
                for (int j = 0; j < vals.length; j++) {
                    vSet.add(new Integer(vals[j]));
                }
            }
            int v[] = new int[vSet.size()];
            int idx = 0;
            for (Iterator i = vSet.iterator(); i.hasNext(); ) {
                Integer ival = (Integer) i.next();
                v[idx] = ival.intValue();
                idx ++;
            }
            return v;
        }

        static public int[] findValueNumbers(CallGraph cg, String signature, String varName, int line) {
            Set nodes = findCGNodes(cg, signature);
            Set vSet = new HashSet();
            for (Iterator i = nodes.iterator(); i.hasNext(); ) {
                CGNode node = (CGNode) i.next();
                int val = findValueNumber(cg, node, varName, line);
                vSet.add(new Integer(val));
            }
            int v[] = new int[vSet.size()];
            int idx = 0;
            for (Iterator i = vSet.iterator(); i.hasNext(); ) {
                Integer ival = (Integer) i.next();
                v[idx] = ival.intValue();
                idx ++;
            }
            return v;
        }
    }
    
    static public CDVariable[] createCDVariables(String varName, CallGraph cg, CGNode node) {
        int v[] = Domo.findValueNumbers(cg, node, varName);
        CDVariable vars[] = new CDVariable[v.length];
        for (int i = 0; i < v.length; i++) {
            vars[i] = new CDVariable(varName, v[i], cg, node, null);
        }
        return vars;
    }
    
    static public CDVariable createCDVariable(String varName, CallGraph cg, CGNode node, int lineNumber) {
        int v = Domo.findValueNumber(cg, node, varName, lineNumber);
        CDVariable var = new CDVariable(varName, v, cg, node, null);
        return var;
    }
    
    static public CDVariable[] createCDVariables(String varName, CallGraph cg, String signature) {
        Set s = new HashSet();
        for (Iterator i = Domo.findCGNodes(cg, signature).iterator(); i.hasNext(); ) {
            CGNode node = (CGNode) i.next();
            CDVariable vars[] = createCDVariables(varName, cg, node);
            s.addAll(set(vars));
        }
        CDVariable result[] = new CDVariable[s.size()];
        s.toArray(result);
        return result;
    }
    
    static public CDVariable[] createCDVariables(String varName, CallGraph cg, String signature, int lineNumber) {
        Set s = new HashSet();
        for (Iterator i = Domo.findCGNodes(cg, signature).iterator(); i.hasNext(); ) {
            CGNode node = (CGNode) i.next();
            CDVariable var = createCDVariable(varName, cg, node, lineNumber);
            s.add(var);
        }
        CDVariable result[] = new CDVariable[s.size()];
        s.toArray(result);
        return result;
    }
}
