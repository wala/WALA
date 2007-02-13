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
/**
 * translator from IR to GR.
 */
package com.ibm.wala.stringAnalysis.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.stringAnalysis.grammar.ControlledGrammars;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.IRegularlyControlledGrammar;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

public class IR2GR implements IIR2Grammar {
    IBB2Grammar bb2gr = null;

    public IR2GR(IBB2Grammar bb2gr) {
        this.bb2gr = bb2gr;
    }
    
    public GR translate(TranslationContext ctx) {
        Trace.println("invoking IR2GR#translate with a IR:");
        Trace.println(ctx.getIR());
        SSACFG cfg = ctx.getIR().getControlFlowGraph();
        IBasicBlock bb0 = cfg.getBasicBlock(0);
        
        Map<IBasicBlock,GR> map = new HashMap<IBasicBlock,GR>();
        
        Trace.println("invoking IR2GR#createTranslationMap");
        createTranslationMap(ctx, cfg, bb0, map);
        Trace.println("IR2GR#createTranslationMap returns a map: " + map);
        
        // All the variables, states and input symbols should have unique names.
        // This depends on SSA2Rule and BB2GR.
        Trace.println("invoking IR2GR#useUniqueSymbols with a map: " + map);
        useUniqueSymbols(map);
        Trace.println("IR2GR#useUniqueSymbols returns a map: " + map);
        
        Trace.println("IR2GR#translate with a map: " + map);
        translate(cfg, bb0, map);
        Trace.println("IR2GR#translate returns a map: " + map);
        
        GR gr = map.get(bb0);
        
        gr.getParameterVariables().addAll(getParameterVariables(ctx));
        gr.getReturnSymbols().addAll(getReturnSymbols(ctx));
        Trace.println("IR2GR#translate result: " + SAUtil.prettyFormat(gr.toString()));
        
        gr = bb2gr.getSSA2Rule().postTranslate(gr);
        
        return gr;
    }
    
    private void createTranslationMap(TranslationContext ctx, SSACFG cfg, IBasicBlock bb, Map map) {
        if (map.containsKey(bb)) {
            return ;
        }
        GR bgr = (GR) bb2gr.translate(bb, ctx);
        Assertions._assert(bgr != null);
        map.put(bb, bgr);
        for (Iterator i = cfg.getSuccNodes(bb); i.hasNext(); ) {
            IBasicBlock nbb = (IBasicBlock) i.next();
            createTranslationMap(ctx, cfg, nbb, map);
        }
    }
    
    private List getParameterVariables(TranslationContext ctx){
        IR ir = ctx.getIR();
        List parameters = new ArrayList();
        int vals[] = ir.getParameterValueNumbers();
        for (int i = 0; i < vals.length; i++) {
            IVariable var = (IVariable) bb2gr.getSSA2Rule().getValueSymbol(vals[i], null, ctx);
            parameters.add(var);
        }
        return parameters;
    }
    
    private Set getReturnSymbols(TranslationContext ctx) {
        IR ir = ctx.getIR();
        Set returns = new HashSet();
        for(Iterator i = ir.iterateAllInstructions(); i.hasNext(); ) {
          SSAInstruction instruction = (SSAInstruction)i.next();
          if (instruction instanceof SSAReturnInstruction) {
            int v = instruction.getUse(0);
            if (v >= 0) {
                ISymbol s = bb2gr.getSSA2Rule().getValueSymbol(v, instruction, ctx);
                returns.add(s);
            }
          }
        }
        return returns;
    }
    
    private void useUniqueSymbols(Map map) {
        Set names = new HashSet();
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            IBasicBlock bb = (IBasicBlock) i.next();
            GR gr = (GR) map.get(bb);
            Map replaceState = new HashMap();
            IRegularlyControlledGrammar g =
                ControlledGrammars.useUniqueStates(gr, names, replaceState);
            names.addAll(replaceState.values());
            Map replaceSymbol = new HashMap();
            g = ControlledGrammars.useUniqueInputSymbols(g, names, replaceSymbol);
            names.addAll(replaceSymbol.values());
            gr = new GR(gr.getIR(), gr.getParameterVariables(), gr.getReturnSymbols(),
                        g.getAutomaton(), g.getFails(), g.getRuleMap());
            map.put(bb, gr);
        }
    }
    
    private void translate(SSACFG cfg, IBasicBlock bb0, Map map) {
        int n = cfg.getNumberOfNodes();
        IBasicBlock exitBB = cfg.exit();
        GR exitGR = (GR) map.get(exitBB);
        Set finalStates = exitGR.getAutomaton().getFinalStates();
        Map ruleMap = new HashMap();
        Set fails = new HashSet();
        Set transitions = new HashSet();
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ){
            IBasicBlock bb = (IBasicBlock) i.next();
            GR gr = (GR) map.get(bb);
            fails.addAll(gr.getFails());
            ruleMap.putAll(gr.getRuleMap());
            transitions.addAll(gr.getAutomaton().getTransitions());
            for (Iterator j = cfg.getSuccNodes(bb); j.hasNext(); ) {
                IBasicBlock nbb = (IBasicBlock) j.next();
                GR ngr = (GR) map.get(nbb);
                IState succInitState = ngr.getAutomaton().getInitialState();
                for (Iterator k = gr.getAutomaton().getFinalStates().iterator(); k.hasNext(); ) {
                    IState finalState = (IState) k.next();
                    ITransition succTrans = new Transition(finalState, succInitState);
                    transitions.add(succTrans);
                }
            }
        }
        GR gr0 = (GR) map.get(bb0);
        gr0.getAutomaton().getTransitions().addAll(transitions);
        gr0.getAutomaton().getFinalStates().clear();
        gr0.getAutomaton().getFinalStates().addAll(finalStates);
        gr0.getFails().addAll(fails);
        gr0.getRuleMap().putAll(ruleMap);
    }
    
    public IBB2Grammar getBB2Grammar() {
        return bb2gr;
    }

}
