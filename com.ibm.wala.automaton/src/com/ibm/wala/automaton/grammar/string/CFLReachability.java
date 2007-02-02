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
package com.ibm.wala.automaton.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.string.*;

public class CFLReachability {
    static public IAutomaton lastResult;
    
    static public class AnalysisTransition extends Transition {
        public AnalysisTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols) {
            super(preState, postState, inputSymbol, outputSymbols);
        }
        public AnalysisTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol[] outputSymbols) {
            super(preState, postState, inputSymbol, outputSymbols);
        }
        
        public boolean accept(ISymbol s, IMatchContext ctx) {
            if (getInputSymbol().equals(s)) {
                ctx.put(getInputSymbol(), s);
                return true;
            }
            else {
                return false;
            }
        }
    }

    static public class ProductionRuleTransition extends CFLReachability.AnalysisTransition {
        private IProductionRule rule;
        
        public ProductionRuleTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols,
                IProductionRule rule) {
            super(preState, postState, inputSymbol, outputSymbols);
            this.rule = rule;
        }
        
        public ProductionRuleTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[],
                IProductionRule rule) {
            super(preState, postState, inputSymbol, outputSymbols);
            this.rule = rule;
        }
        
        public ProductionRuleTransition(ProductionRuleTransition transition) {
            this(transition.getPreState(), transition.getPostState(),
                    transition.getInputSymbol(), AUtil.list(transition.getOutputSymbols()),
                    transition.getProductionRule());
        }
        
        public IProductionRule getProductionRule() {
            return rule;
        }
        
        public int hashCode() {
            return super.hashCode() + rule.hashCode();
        }
        
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass().equals(obj.getClass())) {
                ProductionRuleTransition trans = (ProductionRuleTransition) obj;
                return super.equals(obj)
                    && rule.equals(trans.getProductionRule());
            }
            else{
                return false;
            }
        }
        
        public String toString() {
            return super.toString() + "#{rule:" + rule + "}";
        }
    }

    static public interface ITransitionFactory {
        AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IProductionRule rule, List assocTransitions);
    }
    
    static abstract public class AbstractTransitionFactory implements ITransitionFactory {
        abstract public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IProductionRule rule, List assocTransitions);
        
        public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IProductionRule rule, ITransition assocTransitions[]) {
            return createTransition(preState, postState, inputSymbol, AUtil.list(outputSymbols), rule, AUtil.list(assocTransitions));
        }
        public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IProductionRule rule, ITransition assocTransition) {
            return createTransition(preState, postState, inputSymbol, outputSymbols, rule, new ITransition[]{assocTransition});
        }
        public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IProductionRule rule, ITransition assocTransition1, ITransition assocTransition2) {
            return createTransition(preState, postState, inputSymbol, outputSymbols, rule, new ITransition[]{assocTransition1, assocTransition2});
        }
    }
    
    static public class SimpleTransitionFactory extends AbstractTransitionFactory {
        public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IProductionRule rule, List assocTransitions) {
            return new AnalysisTransition(preState, postState, inputSymbol, outputSymbols);
        }
    }
    
    static public class ProductionRuleTransitionFactory extends CFLReachability.AbstractTransitionFactory {
        public CFLReachability.AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IProductionRule rule, List assocTransitions) {
            return new ProductionRuleTransition(preState, postState, inputSymbol, outputSymbols, rule);
        }
    }

    static public class TraceableTransitionFactory extends AbstractTransitionFactory {
        private Set correspondings;
        private ITransitionFactory factory;
        
        public TraceableTransitionFactory(ITransitionFactory factory) {
             this.correspondings = new HashSet();
             this.factory = factory;
        }
        
        public AnalysisTransition createTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IProductionRule rule, List assocTransitions) {
            AnalysisTransition t = factory.createTransition(preState, postState, inputSymbol, outputSymbols, rule, assocTransitions);
            CorrespondingPath c = new CorrespondingPath(t, assocTransitions);
            if (!correspondings.contains(c)) {
                correspondings.add(c);
            }
            return t;
        }
        
        public Set getCorrespondingTransitions() {
            return correspondings;
        }
    }
    
    static public class CorrespondingPath {
        public ITransition derivedTransition;
        // public List<ITransition> path;
        public List pathTransitions;
        
        public CorrespondingPath(ITransition transition) {
            derivedTransition = transition;
            pathTransitions = new ArrayList();
        }
        
        public CorrespondingPath(ITransition transition, List paths) {
            this(transition);
            pathTransitions.addAll(paths);
        }
        
        public CorrespondingPath(ITransition transition, ITransition path1) {
            this(transition);
            pathTransitions.add(path1);
        }
        
        public CorrespondingPath(ITransition transition, ITransition path1, ITransition path2) {
            this(transition, path1);
            pathTransitions.add(path2);
        }
        
        public int hashCode() {
            return derivedTransition.hashCode() + pathTransitions.hashCode();
        }
        
        public boolean equals(Object obj) {
            if (!obj.getClass().equals(this.getClass())) return false;
            CorrespondingPath path = (CorrespondingPath) obj;
            return derivedTransition.equals(path.derivedTransition)
                && pathTransitions.equals(path.pathTransitions);
        }
        
        public String toString() {
            return "{derived: " + derivedTransition + ", path: " + pathTransitions + "}";
        }
    }
    
    static private boolean isSimplified(IContextFreeGrammar cfg) {
        for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            int size = rule.getRight().size();
            if (size > 2) {
                return false;
            }
        }
        return true;
    }
    
    static public IAutomaton analyze(IAutomaton automaton, IContextFreeGrammar cfg, ITransitionFactory factory){
        IAutomaton result = (IAutomaton) automaton.copy(SimpleSTSCopier.defaultCopier);
        Automatons.eliminateEpsilonTransitions(result);
        Automatons.eliminateUnreachableStates(result);

        if (!isSimplified(cfg)) {
            cfg = (IContextFreeGrammar) cfg.copy(new DeepGrammarCopier(SimpleRuleCopier.defaultCopier));
            Grammars.simplifyRules(cfg, null);
        }
        
        Set newTransitions = new HashSet();
        do {
            newTransitions.clear();
            for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
                IProductionRule rule = (IProductionRule) i.next();
                addTransitionFor(result, rule, newTransitions, factory);
            }
            result.getTransitions().addAll(newTransitions);
        } while(!newTransitions.isEmpty());
        lastResult = result;
        return result;
    }
    
    /**
     * add a transition corresponding to a specified production
     * rule to a normalized automaton.
     * @param rule      production rule
     * @return returns true if a new transition is added.
     */
    static private void addTransitionFor(IAutomaton result, IProductionRule rule, Set newTransitions, ITransitionFactory factory) {
        if (rule.isEpsilonRule()) {
            addTransitionForEpsilonRule(result, rule, newTransitions, factory);
        }
        else {
            for (Iterator i = result.getTransitions().iterator(); i.hasNext(); ) {
                ITransition trans = (ITransition) i.next();
                addTransitionFor(result, trans, rule, newTransitions, factory);
            }
        }
    }
    
    private static void addTransitionForEpsilonRule(IAutomaton result, IProductionRule rule, Set newTransitions, ITransitionFactory factory) {
        for (Iterator i = result.getStates().iterator(); i.hasNext(); ) {
            IState state = (IState) i.next();
            ITransition newTrans = factory.createTransition(state, state, rule.getLeft(), new ArrayList(), rule, new ArrayList());
            if (!result.getTransitions().contains(newTrans)) {
                newTransitions.add(newTrans);
            }
        }
    }
    
    private static void addTransitionFor(IAutomaton result, ITransition trans, IProductionRule rule, Set newTransitions, ITransitionFactory factory) {
        ISymbol right0 = rule.getRight(0);
        if (trans.accept(right0, new MatchContext())) {
            // System.err.println(trans + " accept: " + rule.getRight(0) + "(" + trans.getClass() + ")");
            if (rule.getRight().size() == 1) {
                ArrayList output = new ArrayList();
                if ((right0 instanceof IVariable) && isAlreadyAdded(trans.getPreState(), trans.getPostState(), (IVariable)right0, rule, result, newTransitions)){
                    output.add(right0);
                }
                else {
                    output.addAll(trans.transit(right0));
                }
                ArrayList assocTransitions = new ArrayList();
                assocTransitions.add(trans);
                ITransition newTrans = factory.createTransition(trans.getPreState(), trans.getPostState(), rule.getLeft(), output, rule, assocTransitions);
                if (!result.getTransitions().contains(newTrans)) {
                    //System.out.println("rule=" + rule + ", trans=" + trans + ", newTrans=" + newTrans);
                    newTransitions.add(newTrans);
                }
            }
            else { // size == 2
                for (Iterator j = result.getAcceptTransitions(trans.getPostState(), rule.getRight(1)).iterator(); j.hasNext(); ) {
                    ISymbol right1 = rule.getRight(1);
                    ITransition trans2 = (ITransition) j.next();
                    // System.err.println(trans2 + " accept: " + right1 + "(" + trans2.getClass() + ")");
                    List output = new ArrayList();
                    if ((right0 instanceof IVariable) && isAlreadyAdded(trans.getPreState(), trans.getPostState(), (IVariable)right0, rule, result, newTransitions)) {
                        output.add(right0);
                    }
                    else {
                        output.addAll(trans.transit(right0));
                    }
                    if ((right1 instanceof IVariable) && isAlreadyAdded(trans2.getPreState(), trans2.getPostState(), (IVariable)right1, rule, result, newTransitions)) {
                        output.add(right1);
                    }
                    else {
                        output.addAll(trans2.transit(right1));
                    }
                    ArrayList assocTransitions = new ArrayList();
                    assocTransitions.add(trans);
                    assocTransitions.add(trans2);
                    ITransition newTrans = factory.createTransition(trans.getPreState(), trans2.getPostState(), rule.getLeft(), output, rule, assocTransitions);
                    if (!result.getTransitions().contains(newTrans)) {
                        //System.out.println("rule=" + rule + ", trans=" + trans + ", trans2=" + trans2 + ", newTrans=" + newTrans);
                        newTransitions.add(newTrans);
                    }
                }
            }
        }
    }
    
    static private boolean isAlreadyAdded(IState preState, IState postState, IVariable v, IProductionRule rule, IAutomaton automaton, Set newTransitions) {
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if (t.getPreState().equals(preState)
                    && t.getPostState().equals(postState)
                    && t.getInputSymbol().equals(v)) {
                if (t instanceof ProductionRuleTransition) {
                    if (((ProductionRuleTransition) t).getProductionRule().equals(rule)) {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        for (Iterator i = newTransitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if (t.getPreState().equals(preState)
                    && t.getPostState().equals(postState)
                    && t.getInputSymbol().equals(v)) {
                if (t instanceof ProductionRuleTransition) {
                    if (((ProductionRuleTransition) t).getProductionRule().equals(rule)) {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
        }
        return false;
    }
    
    static public Set getCorrespondingTransitions(IAutomaton automaton, IContextFreeGrammar cfg) {
        return getCorrespondingTransitions(automaton, cfg.getRules());
    }
    
    static public Set getCorrespondingTransitions(IAutomaton automaton, IProductionRule rules[]) {
        return getCorrespondingTransitions(automaton, AUtil.set(rules));
    }
    
    static public Set getCorrespondingTransitions(IAutomaton automaton, IProductionRule rule) {
        return getCorrespondingTransitions(automaton, new IProductionRule[]{rule});
    }
    
    /**
     * TODO: use ProductionRuleTransition#getProductionRule().
     * @param automaton
     * @param rules
     * @return
     */
    static public Set getCorrespondingTransitions(IAutomaton automaton, Collection rules) {
        Map ruleMap = new HashMap();
        Set transitions = new HashSet();
        for (Iterator i = rules.iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            List l = (List) ruleMap.get(rule.getLeft());
            if (l == null) {
                l = new ArrayList();
                ruleMap.put(rule.getLeft(), l);
            }
            l.add(rule);
        }
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition trans = (ITransition) i.next();
            ISymbol isym = trans.getInputSymbol();
            if ((isym instanceof IVariable) && ruleMap.containsKey(isym)) {
                List l = (List) ruleMap.get(isym);
                for (Iterator j = l.iterator(); j.hasNext(); ) {
                    IProductionRule rule = (IProductionRule) j.next();
                    if (rule.getRight().size() == 0) {
                        if (trans.getPreState().equals(trans.getPostState())) {
                            transitions.add(new CorrespondingPath(trans));
                        }
                    }
                    else if(rule.getRight().size() == 1) {
                        ISymbol sym1 = rule.getRight(0);
                        Set paths = getCorrespondingPath(automaton, trans, sym1);
                        if (!paths.isEmpty()) {
                            transitions.addAll(paths);
                        }
                    }
                    else {
                        ISymbol sym1 = rule.getRight(0);
                        ISymbol sym2 = rule.getRight(1);
                        Set paths = getCorrespondingPath(automaton, trans, sym1, sym2);
                        if (!paths.isEmpty()) {
                            transitions.addAll(paths);
                        }
                    }
                }
            }
        }
        return transitions;
    }
    
    static private Set getCorrespondingPath(IAutomaton automaton, ITransition derived, ISymbol input) {
        return getCorrespondingPath(automaton, derived, derived.getPreState(), derived.getPostState(), input);
    }
    
    static private Set getCorrespondingPath(IAutomaton automaton, ITransition derived, ISymbol input1, ISymbol input2) {
        return getCorrespondingPath(automaton, derived, derived.getPreState(), derived.getPostState(), input1, input2);
    }
    
    static private Set getCorrespondingPath(IAutomaton automaton, ITransition derived, IState preState, IState postState, ISymbol input) {
        Set transitions = new HashSet();
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition trans = (ITransition) i.next();
            if (trans.getPreState().equals(preState)
                    && trans.getPostState().equals(postState)
                    && trans.getInputSymbol().equals(input)) {
                transitions.add(new CorrespondingPath(derived, trans));
            }
        }
        return transitions;
    }
    
    static private Set getCorrespondingPath(IAutomaton automaton, ITransition derived, IState preState, IState postState, ISymbol input1, ISymbol input2) {
        Set transitions = new HashSet();
        for (Iterator i = automaton.getTransitions(preState, input1).iterator(); i.hasNext(); ) {
            ITransition trans1 = (ITransition) i.next();
            for (Iterator j = getCorrespondingPath(automaton, derived, trans1.getPostState(), postState, input2).iterator(); j.hasNext(); ) {
                CorrespondingPath path = (CorrespondingPath) j.next();
                path.pathTransitions.add(0, trans1);
                transitions.add(path);
            }
        }
        return transitions;
    }
    
    static public boolean isReachable(IAutomaton automaton, IContextFreeGrammar cfg, Set nonterminals) {
        IAutomaton result = analyze(automaton, cfg, new SimpleTransitionFactory());
        IState initState = result.getInitialState();
        Set finalStates = result.getFinalStates();
        for (Iterator i = result.getTransitions().iterator(); i.hasNext(); ) {
            ITransition trans = (ITransition) i.next();
            if (initState.equals(trans.getPreState())
                && finalStates.contains(trans.getPostState())
                && nonterminals.contains(trans.getInputSymbol())) {
                return true;
            }
        }
        return false;
    }
    
    static public boolean isReachable(IAutomaton automaton, IContextFreeGrammar cfg, IVariable nonterminals[]) {
        return isReachable(automaton, cfg, AUtil.set(nonterminals));
    }
    
    static public boolean isReachable(IAutomaton automaton, IContextFreeGrammar cfg) {
        return isReachable(automaton, cfg, new IVariable[]{cfg.getStartSymbol()});
    }
    
    static public Set selectConnectedCorrespondingTransitions(Set correspondings, IState preState, Collection postStates, ISymbol symbol) {
        Set result = new HashSet();
        for (Iterator i = correspondings.iterator(); i.hasNext(); ) {
            CorrespondingPath path = (CorrespondingPath) i.next();
            if (path.derivedTransition.getInputSymbol().equals(symbol)
                    && path.derivedTransition.getPreState().equals(preState)
                    && postStates.contains(path.derivedTransition.getPostState())) {
                selectConnectedCorrespondingTransitions(correspondings, path.derivedTransition, result);
            }
        }
        return result;
    }
    
    static public Set selectConnectedCorrespondingTransitions(Set correspondings, IState preState, IState postStates[], ISymbol symbol) {
        return selectConnectedCorrespondingTransitions(correspondings, preState, AUtil.set(postStates), symbol);
    }
    
    static public Set selectConnectedCorrespondingTransitions(Set correspondings, CorrespondingPath path) {
        Set s = new HashSet();
        selectConnectedCorrespondingTransitions(correspondings, path, s);
        return s;
    }

    static public Set selectConnectedCorrespondingTransitions(Set correspondings, ITransition transition) {
        Set s = new HashSet();
        selectConnectedCorrespondingTransitions(correspondings, transition, s);
        return s;
    }

    static private void selectConnectedCorrespondingTransitions(Set correspondings, CorrespondingPath path, Set result) {
        if (result.contains(path)) return;
        if (!correspondings.contains(path)) return;
        result.add(path);
        for (Iterator i = path.pathTransitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            selectConnectedCorrespondingTransitions(correspondings, t, result);
        }
    }

    static private void selectConnectedCorrespondingTransitions(Set correspondings, ITransition transition, Set result) {
        for (Iterator i = correspondings.iterator(); i.hasNext(); ) {
            CorrespondingPath path = (CorrespondingPath) i.next();
            if (path.derivedTransition.equals(transition)) {
                selectConnectedCorrespondingTransitions(correspondings, path, result);
            }
        }
    }
    
    static public Set collectDerivedTransitions(Set correspondings) {
        Set transitions = new HashSet();
        for (Iterator i = correspondings.iterator(); i.hasNext(); ) {
            CorrespondingPath p = (CorrespondingPath) i.next();
            transitions.add(p.derivedTransition);
        }
        return transitions;
    }

    static public Set collectConnectedTransitions(Set correspondings) {
        Set transitions = new HashSet();
        for (Iterator i = correspondings.iterator(); i.hasNext(); ) {
            CorrespondingPath p = (CorrespondingPath) i.next();
            transitions.add(p.derivedTransition);
            transitions.addAll(p.pathTransitions);
        }
        return transitions;
    }

    static public Set selectConnectedTransitions(IAutomaton automaton, IContextFreeGrammar cfg, IState preState, Collection postStates, ISymbol startSymbol) {
        Set correspondings = getCorrespondingTransitions(automaton, cfg);
        return selectConnectedTransitions(correspondings, preState, postStates, startSymbol);
    }

    static public Set selectConnectedTransitions(IAutomaton automaton, IContextFreeGrammar cfg, IState preState, IState postStates[], ISymbol startSymbol) {
        Set correspondings = getCorrespondingTransitions(automaton, cfg);
        return selectConnectedTransitions(correspondings, preState, postStates, startSymbol);
    }
    
    static public Set selectConnectedTransitions(Set correspondings, IState preState, Collection postStates, ISymbol symbol) {
        Set s = selectConnectedCorrespondingTransitions(correspondings, preState, postStates, symbol);
        return collectConnectedTransitions(s);
    }
    
    static public Set selectConnectedTransitions(Set correspondings, IState preState, IState postStates[], ISymbol symbol) {
        Set s = selectConnectedCorrespondingTransitions(correspondings, preState, postStates, symbol);
        return collectConnectedTransitions(s);
    }

    static public Set selectConnectedTransitions(Set correspondings, ITransition transition) {
        Set s = selectConnectedCorrespondingTransitions(correspondings, transition);
        return collectConnectedTransitions(s);
    }
    
    static public boolean containsSome(IContextFreeGrammar cfg, IAutomaton automaton) {
        return isReachable(automaton, cfg);
    }
    static public boolean containsSome(IContextFreeGrammar cfg, ISymbol symbols[]) {
        Automaton automaton = Automatons.createAutomaton(symbols);
        return containsSome(cfg, automaton);
    }
    static public boolean containsSome(IContextFreeGrammar cfg, List symbols) {
        ISymbol ary[] = new ISymbol[symbols.size()];
        symbols.toArray(ary);
        return containsSome(cfg, ary);
    }

    static public boolean notContainsAll(IContextFreeGrammar cfg, IAutomaton automaton) {
        return !isReachable(automaton, cfg);
    }
    static public boolean notContainsAll(IContextFreeGrammar cfg, ISymbol symbols[]) {
        Automaton automaton = Automatons.createAutomaton(symbols);
        return notContainsAll(cfg, automaton);
    }
    static public boolean notContainsAll(IContextFreeGrammar cfg, List symbols) {
        ISymbol ary[] = new ISymbol[symbols.size()];
        symbols.toArray(ary);
        return notContainsAll(cfg, ary);
    }
    
    
    static public boolean containsAll(IAutomaton automaton, IContextFreeGrammar cfg) {
        return !isReachable(Automatons.createComplement(automaton, Grammars.collectTerminals(cfg)), cfg);
    }
    static public boolean containsAll(ISymbol symbols[], IContextFreeGrammar cfg) {
        Automaton automaton = Automatons.createAutomaton(symbols);
        return containsAll(automaton, cfg);
    }
    static public boolean containsAll(List symbols, IContextFreeGrammar cfg) {
        ISymbol ary[] = new ISymbol[symbols.size()];
        symbols.toArray(ary);
        return containsAll(ary, cfg);
    }
}
