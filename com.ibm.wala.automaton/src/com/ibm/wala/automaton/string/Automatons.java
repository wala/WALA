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
package com.ibm.wala.automaton.string;

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.tree.CompositeState;

public class Automatons {
    static public String prefixState = "s";
    static public String prefixInputSymbol = "i";
    
    static public String createUniqueStateName(IAutomaton automaton) {
        final Set states = new HashSet();
        automaton.traverseStates(new IStateVisitor(){
            public void onVisit(IState state) {
                states.add(state.getName());
            }
        });
        return AUtil.createUniqueName(prefixState, states);
    }

    static public String createUniqueInputSymbolName(IAutomaton automaton) {
        final Set symbols = new HashSet();
        automaton.traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                ISymbol sym = transition.getInputSymbol();
                if (sym != null) {
                    symbols.add(sym.getName());
                }
            }
        });
        return AUtil.createUniqueName(prefixInputSymbol, symbols);
    }
    
    static public IState createUniqueState(IAutomaton automaton) {
        return new State(createUniqueStateName(automaton));
    }
    
    static public ISymbol createUniqueInputSymbol(IAutomaton automaton) {
        return new Symbol(createUniqueInputSymbolName(automaton));
    }
    
    static public Set collectStateNames(Set states) {
        return new HashSet(AUtil.collect(states, new AUtil.IElementMapper(){
            public Object map(Object obj) {
                return ((IState)obj).getName();
            }
        }));
    }
    
    static public Set collectStateNames(IAutomaton automaton) {
        return collectStateNames(automaton.getStates());
    }
    
    static public Set collectInputSymbols(IAutomaton automaton) {
        final Set symbols = new HashSet();
        automaton.traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                if (!transition.isEpsilonTransition()) {
                    ISymbol s = transition.getInputSymbol();
                    symbols.add(s);
                }
            }
        });
        return symbols;
    }
    
    static public Set collectInputSymbolNames(Set symbols) {
        return new HashSet(AUtil.collect(symbols, new AUtil.IElementMapper(){
            public Object map(Object obj) {
                return ((ISymbol)obj).getName();
            }
        }));
    }
    
    static public Set collectInputSymbolNames(IAutomaton automaton) {
        return collectInputSymbolNames(collectInputSymbols(automaton));
    }
    
    static public IAutomaton useUniqueStates(IAutomaton target, IAutomaton base, Map stateMap) {
        Set baseNames = collectStateNames(base);
        return useUniqueStates(target, baseNames, stateMap);
    }

    static public IAutomaton useUniqueStates(IAutomaton target, Set baseNames, Map stateMap) {
        final Map map = (stateMap==null) ? new HashMap() : stateMap;
        Set states = target.getStates();
        for (Iterator i = states.iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            String n = AUtil.createUniqueName(prefixState, baseNames);
            IState u = new State(n);
            map.put(s, u);
        }
        final Set newTransitions = new HashSet();
        target.traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                IState preState = (IState) map.get(transition.getPreState());
                IState postState = (IState) map.get(transition.getPostState());
                ITransition newTransition = transition.copy(SimpleTransitionCopier.defaultCopier);
                newTransition.setPreState(preState);
                newTransition.setPostState(postState);
                newTransitions.add(newTransition);
            }
        });
        IState initState = (IState) map.get(target.getInitialState());
        Set finalStates = new HashSet(AUtil.collect(target.getFinalStates(), new AUtil.IElementMapper(){
            public Object map(Object obj) {
                IState s = (IState) map.get(obj);
                return s;
            }
        }));
        // TODO: should use Automaton#copy
        IAutomaton result = new Automaton(initState, finalStates, newTransitions);
        return result;
    }
    
    static public IAutomaton useUniqueInputSymbols(IAutomaton target, IAutomaton base, Map symMap) {
        Set baseNames = collectInputSymbolNames(base);
        return useUniqueInputSymbols(target, baseNames, symMap);
    }
    
    static public IAutomaton useUniqueInputSymbols(IAutomaton target, Set baseNames, Map symMap) {
        final Map map = (symMap==null) ? new HashMap() : symMap;
        Set symbols = collectInputSymbols(target);
        for (Iterator i = symbols.iterator(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            String n = AUtil.createUniqueName("i", baseNames);
            ISymbol u = new Symbol(n);
            map.put(s, u);
        }
        final Set newTransitions = new HashSet();
        target.traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                if (transition.getInputSymbol() instanceof IVariable) {
                    newTransitions.add(transition);
                }
                else {
                    ISymbol isym = (ISymbol) map.get(transition.getInputSymbol());
                    ITransition newTransition = transition.copy(SimpleTransitionCopier.defaultCopier);
                    newTransition.setInputSymbol(isym);
                    newTransitions.add(newTransition);
                }
            }
        });
        IState initState = target.getInitialState();
        Set finalStates = target.getFinalStates();
        // TODO: should use Automaton#copy
        IAutomaton result = new Automaton(initState, finalStates, newTransitions);
        return result;
    }
    
    static public IAutomaton createConcatenation(IAutomaton automaton1, IAutomaton automaton2, Map stateMap) {
        automaton2 = useUniqueStates(automaton2, automaton1, stateMap);
        
        IAutomaton result = (IAutomaton) automaton1.copy(SimpleSTSCopier.defaultCopier);
        createSimpleConcatenation(result, automaton2);
        return result;
    }
    
    static public IAutomaton createConcatenation(IAutomaton automaton1, IAutomaton automaton2) {
        return createConcatenation(automaton1, automaton2, new HashMap());
    }
    
    static public void createSimpleConcatenation(IAutomaton automaton1, IAutomaton automaton2) {
        IState initState2 = automaton2.getInitialState();
        Set finalStates1 = automaton1.getFinalStates();
        for (Iterator i = finalStates1.iterator(); i.hasNext(); ) {
            IState finalState1 = (IState) i.next();
            if (finalState1.equals(initState2)) {
                continue ;
            }
            ITransition newTransition = new Transition(finalState1, initState2);
            automaton1.getTransitions().add(newTransition);
        }
        automaton1.getTransitions().addAll(automaton2.getTransitions());
        automaton1.getFinalStates().clear();
        automaton1.getFinalStates().addAll(automaton2.getFinalStates());
    }
    
    static public IAutomaton createUnion(IAutomaton automaton1, IAutomaton automaton2, Map stateMap) {
        automaton2 = useUniqueStates(automaton2, automaton1, stateMap);
        
        IAutomaton result = (IAutomaton) automaton1.copy(SimpleSTSCopier.defaultCopier);
        createSimpleUnion(result, automaton2);
        return result;
    }
    
    static public void createSimpleUnion(IAutomaton automaton1, IAutomaton automaton2) {
        automaton1.getFinalStates().addAll(automaton2.getFinalStates());
        automaton1.getTransitions().addAll(automaton2.getTransitions());
        List stateNames = AUtil.collect(automaton1.getStates(), new AUtil.IElementMapper(){
            public Object map(Object obj) {
                IState state = (IState) obj;
                return state.getName();
            }}
        );
        IState initState = new State(AUtil.createUniqueName("s", stateNames));
        IState initState1 = automaton1.getInitialState();
        IState initState2 = automaton2.getInitialState();
        ITransition transition1 = new Transition(initState, initState1);
        ITransition transition2 = new Transition(initState, initState2);
        automaton1.setInitialState(initState);
        automaton1.getTransitions().add(transition1);
        automaton1.getTransitions().add(transition2);
    }
    
    static public IAutomaton createUnion(IAutomaton automaton1, IAutomaton automaton2) {
        return createUnion(automaton1, automaton2, new HashMap());
    }
    
    static public void completeAutomaton(IAutomaton automaton, IState failState, IVariable v, List outputSymbols, FilteredTransition.IFilter filter) {
        // eliminateEpsilonTransitions(automaton);
        Set stateNames = collectStateNames(automaton);
        stateNames.add(failState.getName());
        eliminateNonDeterministics(automaton, stateNames);
        Set states = automaton.getStates();
        Set newTransitions = new HashSet();
        for (Iterator i = states.iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            Set nextTransitions = automaton.getTransitions(s);
            ITransition complement = new ComplementTransition(s, failState, v, outputSymbols, filter, nextTransitions);
            newTransitions.add(complement);
        }
        automaton.getTransitions().addAll(newTransitions);
    }
    
    static public IAutomaton createComplement(IAutomaton automaton, IState failState, IVariable v, FilteredTransition.IFilter filter) {
        IAutomaton ca = (IAutomaton) automaton.copy(SimpleSTSCopier.defaultCopier);
        completeAutomaton(ca, failState, v, AUtil.list(new ISymbol[]{}), filter);
        
        ITransition t = new FilteredTransition(failState, failState, v, new ISymbol[]{}, filter);
        Set finalStates = new HashSet(ca.getStates());
        finalStates.add(failState);
        finalStates.removeAll(ca.getFinalStates());
        ca.getTransitions().add(t);
        ca.getFinalStates().clear();
        ca.getFinalStates().addAll(finalStates);

        return ca;
    }

    static public IAutomaton createComplement(IAutomaton automaton, Set symbols) {
        return createComplement(expand(automaton, symbols));
    }

    static public IAutomaton createComplement(IAutomaton automaton, Set symbols, IState failState) {
        return createComplement(expand(automaton, symbols), failState);
    }

    static public IAutomaton createComplement(IAutomaton automaton, IState failState) {
        return createComplement(automaton, failState, new Variable("_"), null);
    }
    
    static public IAutomaton createComplement(IAutomaton automaton) {
        return createComplement(automaton, createUniqueState(automaton));
    }
    
    static public IAutomaton createIntersection(IAutomaton automaton1, IAutomaton automaton2) {
      automaton1 = (IAutomaton) automaton1.copy(SimpleSTSCopier.defaultCopier);
      automaton2 = (IAutomaton) automaton2.copy(SimpleSTSCopier.defaultCopier);
      Automatons.eliminateEpsilonTransitions(automaton1);
      Automatons.eliminateEpsilonTransitions(automaton2);
        final Set transitions = new HashSet();
        final DMap map = new DMap(new DMap.Factory(){
            private Set names = new HashSet();
            public Object create(Object key) {
                String name = AUtil.createUniqueName("s", names);
                return new State(name);
            }
        });
        for (Iterator i = automaton1.getTransitions().iterator(); i.hasNext(); ) {
            ITransition t1 = (ITransition) i.next();
            for (Iterator j = automaton2.getTransitions().iterator(); j.hasNext(); ) {
                ITransition t2 = (ITransition) j.next();
                IMatchContext ctx = new MatchContext();
                ISymbol input1 = t1.getInputSymbol();
                ISymbol input2 = t2.getInputSymbol();
                if (input1 instanceof IVariable || input2 instanceof IVariable) {
                    CompositeState pre = new CompositeState("s", new IState[]{t1.getPreState(), t2.getPreState()});
                    CompositeState post = new CompositeState("s", new IState[]{t1.getPostState(), t2.getPostState()});

                    IntersectionTransition.IFilter filter1 = null;
                    IntersectionTransition.IFilter filter2 = null;
                    if (t1 instanceof FilteredTransition) {
                        filter1 = ((FilteredTransition)t1).getFilter();
                    }
                    if (t2 instanceof FilteredTransition) {
                        filter2 = ((FilteredTransition)t2).getFilter();
                    }
                    Transition ta = new IntersectionTransition((IState)map.get(pre), (IState)map.get(post), t1.getInputSymbol(), t1.getOutputSymbols(), filter1, new ITransition[]{t1, t2});
                    transitions.add(ta);
                    Transition tb = new IntersectionTransition((IState)map.get(pre), (IState)map.get(post), t2.getInputSymbol(), t2.getOutputSymbols(), filter2, new ITransition[]{t1, t2});
                    transitions.add(tb);
                }
                else if (input1.matches(input2, ctx)) {
                    CompositeState pre = new CompositeState("s", new IState[]{t1.getPreState(), t2.getPreState()});
                    CompositeState post = new CompositeState("s", new IState[]{t1.getPostState(), t2.getPostState()});
                    if (t1.hasOutputSymbols() || t2.hasOutputSymbols()) {
                        Transition ta = new Transition((IState)map.get(pre), (IState)map.get(post), t2.getInputSymbol(), t1.getOutputSymbols());
                        transitions.add(ta);
                        Transition tb = new Transition((IState)map.get(pre), (IState)map.get(post), t2.getInputSymbol(), t1.getOutputSymbols());
                        transitions.add(tb);
                    }
                    else {
                        Transition t = new Transition((IState)map.get(pre), (IState)map.get(post), t2.getInputSymbol());
                        transitions.add(t);
                    }
                }
                else if (input2.matches(input1, ctx)) {
                    CompositeState pre = new CompositeState("s", new IState[]{t1.getPreState(), t2.getPreState()});
                    CompositeState post = new CompositeState("s", new IState[]{t1.getPostState(), t2.getPostState()});
                    if (t1.hasOutputSymbols() || t2.hasOutputSymbols()) {
                        Transition ta = new Transition((IState)map.get(pre), (IState)map.get(post), t1.getInputSymbol(), t1.getOutputSymbols());
                        transitions.add(ta);
                        Transition tb = new Transition((IState)map.get(pre), (IState)map.get(post), t1.getInputSymbol(), t1.getOutputSymbols());
                        transitions.add(tb);
                    }
                    else {
                        Transition t = new Transition((IState)map.get(pre), (IState)map.get(post), t1.getInputSymbol());
                        transitions.add(t);
                    }
                }
            }
        }
        
        final IState initState = (IState) map.get(new CompositeState("s", new IState[]{automaton1.getInitialState(), automaton2.getInitialState()}));
        final Set finalStates = new HashSet();
        for (Iterator i = automaton1.getFinalStates().iterator(); i.hasNext(); ) {
            IState f1 = (IState) i.next();
            for (Iterator j = automaton2.getFinalStates().iterator(); j.hasNext(); ) {
                IState f2 = (IState) j.next();
                CompositeState cs = new CompositeState("s", new IState[]{f1, f2});
                finalStates.add((IState)map.get(cs));
            }
        }

        final IAutomaton origAutomaton1 = automaton1;
        IAutomaton a = (IAutomaton) automaton1.copy(new SimpleSTSCopier(){
            public Collection copyTransitions(Collection c) {
                return transitions;
            }
            
            public IState copy(IState s) {
                if (s == origAutomaton1.getInitialState()) {
                    return initState;
                }
                else {
                    return super.copy(s);
                }
            }
            
            public Collection copyStates(Collection states) {
                if (states == origAutomaton1.getFinalStates()) {
                    return finalStates;
                }
                else {
                    return super.copyStates(states);
                }
            }
        });
        return a;
    }
    
    public static IAutomaton createSubtraction(IAutomaton a1, IAutomaton a2) {
        return createIntersection(a1, createComplement(a2));
    }
    
    public static IPattern toPattern(IAutomaton automaton) {
        Set equations = createEquations(automaton);
        return solvePatternEquations(equations, new Variable(automaton.getInitialState().getName()));
    }
    
    /**
     * solve equations
     * @param equations
     * @return
     */
    public static IPattern solvePatternEquations(Set equations, IVariable variable) {
        // TODO: implement equation solver
        throw(new AssertionError("equation solver is not implemented yet"));
    }
    
    private static Set createEquations(IAutomaton automaton) {
        final Set equations = new HashSet();
        automaton.traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                IVariable lhs = new Variable(transition.getPreState().getName());
                IPattern rhs = null;
                if (!transition.isEpsilonTransition()) {
                    SymbolPattern rhs1 = new SymbolPattern(transition.getInputSymbol());
                    VariableReferencePattern rhs2 = new VariableReferencePattern(new Variable(transition.getPostState().getName()));
                    rhs = new ConcatenationPattern(rhs1, rhs2);
                }
                else {
                    rhs = new VariableReferencePattern(new Variable(transition.getPostState().getName()));
                }
                VariableBindingPattern equation = new VariableBindingPattern(lhs, rhs);
                equations.add(equation);
            }
        });
        return equations;
    }
    
    public static Automaton createAutomaton(List symbols) {
        ISymbol syms[] = new ISymbol[symbols.size()];
        symbols.toArray(syms);
        return createAutomaton(syms);
    }

    public static Automaton createAutomaton(ISymbol[] symbols) {
        Set transitions = new HashSet();
        Set finalStates = new HashSet();
        int i = 0;
        State initState = new State("s" + i);
        State preState = initState; 
        while (i < symbols.length) {
            State postState = new State("s" + i+1);
            Transition trans = new Transition(preState, postState, symbols[i]);
            transitions.add(trans);
            preState = postState;
            i++;
        }
        finalStates.add(preState);
        Automaton automaton = new Automaton(initState, finalStates, transitions);
        return automaton;
    }
    
    static public void eliminateUnreachableStates(IAutomaton automaton) {
        Set reachableStates = collectReachableStates(automaton);
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if (!(reachableStates.contains(t.getPreState())
                    && reachableStates.contains(t.getPostState()))) {
                i.remove();
            }
        }
        for (Iterator i = automaton.getFinalStates().iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            if (!reachableStates.contains(s)) {
                i.remove();
            }
        }
    }
    
    static public Set collectReachableStates(IAutomaton automaton) {
        Set s = new HashSet();
        collectReachableStates(automaton, automaton.getInitialState(), s);
        return s;
    }
    
    static public void collectReachableStates(IAutomaton automaton, IState s, Set states) {
        if (states.contains(s)) {
            return ;
        }
        states.add(s);
        for (Iterator i = automaton.getTransitions(s).iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            collectReachableStates(automaton, t.getPostState(), states);
        }
    }
    
    static private void copyFinalStatesForEliminateEpsilonTransitions(IAutomaton automaton) {
        int n = 0;
        do {
            n = 0;
            for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
                ITransition t = (ITransition) i.next();
                if (t.isEpsilonTransition() && automaton.getFinalStates().contains(t.getPostState())) {
                    if (t.hasOutputSymbols()) {
                        // TODO: should support this case.
                        throw(new AssertionError("unsupported operation"));
                    }
                    IState s = t.getPreState();
                    if (!automaton.getFinalStates().contains(s)) {
                        automaton.getFinalStates().add(s);
                        n ++;
                    }
                }
            }
        } while (n != 0);
    }
    
    static public void eliminateEpsilonTransitions(IAutomaton automaton) {
        copyFinalStatesForEliminateEpsilonTransitions(automaton);
        Set epsTransitions = new HashSet();
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if (t.isEpsilonTransition()) {
                if (t.getPreState().equals(t.getPostState())) {
                    i.remove();
                }
                else {
                    epsTransitions.add(t);
                }
            }
        }
        for (Iterator i = epsTransitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            Set newTransitions = getNonEpsilonTransitions(automaton, t);
            automaton.getTransitions().addAll(newTransitions);
        }
        automaton.getTransitions().removeAll(epsTransitions);
    }
    
    static public void eliminateEpsilonTransition(IAutomaton automaton, ITransition t) {
        Set newTransitions = getNonEpsilonTransitions(automaton, t);
        if (automaton.getFinalStates().contains(t.getPostState())) {
            automaton.getFinalStates().add(t.getPreState());
        }
        automaton.getTransitions().remove(t);
        automaton.getTransitions().addAll(newTransitions);
    }

    static private Set getNonEpsilonTransitions(IAutomaton automaton, ITransition transition) {
        return getNonEpsilonTransitions(automaton, transition, transition.getPreState(), new ArrayList(), new HashSet());
    }
    
    static private Set getNonEpsilonTransitions(IAutomaton automaton, ITransition transition, IState state, List outputs, Set history) {
        if (history.contains(transition)) {
            return new HashSet();
        }
        history.add(transition);
        Set result = new HashSet();
        if (transition.isEpsilonTransition()) {
            List l = new ArrayList(outputs);
            l.addAll(AUtil.list(transition.getOutputSymbols()));
            Set nextTransitions = automaton.getTransitions(transition.getPostState());
            for (Iterator i = nextTransitions.iterator(); i.hasNext(); ) {
                ITransition t = (ITransition) i.next();
                result.addAll(getNonEpsilonTransitions(automaton, t, state, l, history));
            }
        }
        else {
            ITransition newTransition = (ITransition) transition.clone();
            newTransition.setPreState(state);
            newTransition.prependOutputSymbols(outputs);
            result.add(newTransition);
        }
        return result;
    }
    
    static public void eliminateNonDeterministics(IAutomaton automaton) {
        eliminateNonDeterministics(automaton, null);
    }
    
    static public void eliminateNonDeterministics(IAutomaton automaton, Set stateNames) {
        if (stateNames == null) {
            stateNames = Automatons.collectStateNames(automaton);
        }
        eliminateEpsilonTransitions(automaton);
        for (Iterator i = automaton.getStates().iterator(); i.hasNext(); ) {
            IState state = (IState) i.next();
            eliminateNonDeterministics(automaton, state, stateNames);
        }
        eliminateUnreachableStates(automaton);
    }
    
    static private void eliminateNonDeterministics(IAutomaton automaton, IState state, Set stateNames) {
        Set transitions = automaton.getTransitions(state);
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if (t.getInputSymbol() instanceof IVariable) {
                throw(new RuntimeException("can't handle the variable at the transition: " + t.getInputSymbol()));
            }
            if (t.hasOutputSymbols()) {
              List<ISymbol> outs = new ArrayList<ISymbol>();
              for (Iterator<ISymbol> o = t.getOutputSymbols(); o.hasNext(); ) {
                ISymbol out = o.next();
                outs.add(out);
              }
              throw(new RuntimeException("can't handle the output symbols at the transition: " + outs));
            }
        }
        Set inputs = new HashSet(AUtil.collect(transitions, new AUtil.IElementMapper(){
            public Object map(Object obj) {
                return ((ITransition) obj).getInputSymbol();
            }
        }));
        for (Iterator i = inputs.iterator(); i.hasNext(); ) {
            ISymbol input = (ISymbol) i.next();
            eliminateNonDeterministics(automaton, state, input, stateNames);
        }
    }
    
    static private void eliminateNonDeterministics(IAutomaton automaton, IState state, ISymbol inputSymbol, Set stateNames) {
        final Set removeTransitions = new HashSet();
        final Set newTransitions = new HashSet();
        Set sameTransitions = automaton.getTransitions(state, inputSymbol);
        /*
        AUtil.select(sameTransitions, new AUtil.IElementSelector(){
            public boolean selected(Object obj) {
                ITransition t = (ITransition) obj;
                return AUtil.list(t.getOutputSymbols()).equals(outputSymbols);
            }
        });
        */
        if (sameTransitions.size()<=1) return;
            
        String newStateName = AUtil.createUniqueName("s", stateNames);
        IState newState = new State(newStateName);
        ITransition newTransition = new Transition(state, newState, inputSymbol);
        removeTransitions.addAll(sameTransitions);
        newTransitions.add(newTransition);
        for (Iterator j = sameTransitions.iterator(); j.hasNext(); ) {
            ITransition jt = (ITransition) j.next();
            IState postState = jt.getPostState();
            if (automaton.getFinalStates().contains(postState)) {
                automaton.getFinalStates().add(newState);
            }
            if (postState.equals(state)) {
                ITransition t = (ITransition) jt.clone();
                t.setPreState(newState);
                t.setPostState(newState);
                newTransitions.add(t);
            }
            else {
                for (Iterator k = automaton.getTransitions(postState).iterator(); k.hasNext(); ) {
                    ITransition kt = (ITransition) k.next();
                    ITransition newPostTransition = (ITransition) kt.clone();
                    kt.setPreState(newState);
                    newTransitions.add(newPostTransition);
                }
            }
        }
        HashSet ts = new HashSet(automaton.getTransitions());
        ts.addAll(newTransitions);
        ts.removeAll(removeTransitions);
        automaton.getTransitions().clear();
        automaton.getTransitions().addAll(ts);

        for (Iterator i = newTransitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            eliminateNonDeterministics(automaton, newState, t.getInputSymbol(), stateNames);
        }
    }
    
    /**
     * replace all the transitions of the automaton with the Transition object.
     * @param automaton
     * @param allSymbols
     */
    static public IAutomaton expand(IAutomaton automaton, Set allSymbols) {
        automaton = (IAutomaton) automaton.copy(SimpleSTSCopier.defaultCopier);
        IMatchContext ctx = new MatchContext();
        Set transitions = new HashSet();
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            final ITransition t = (ITransition) i.next();
            for (Iterator j = allSymbols.iterator(); j.hasNext(); ) {
                ISymbol input = (ISymbol) j.next();
                if (t.isEpsilonTransition()) {
                    transitions.add(t);
                }
                else if (t.accept(input, ctx)) {
                    final List outputs = t.transit(input);
                    Transition tt = new Transition(t.getPreState(), t.getPostState(), input, outputs);
                    transitions.add(tt);
                }
            }
        }
        automaton.getTransitions().clear();
        automaton.getTransitions().addAll(transitions);
        return automaton;
    }
    
    public interface IGraphvizLabelGenerator {
      public String getLabel(ITransition t);
    }
    
    static public class SimpleGraphvizLabelGenerator implements IGraphvizLabelGenerator {
      public String getLabel(ITransition t) {
        return t.getInputSymbol() + "/" + AUtil.list(t.getOutputSymbols());
      }
    }
    
    static public String toGraphviz(IAutomaton automaton) {
      return toGraphviz(automaton, new SimpleGraphvizLabelGenerator());
    }

    static public String toGraphviz(IAutomaton automaton, IGraphvizLabelGenerator labelGenerator) {
        StringBuffer buff = new StringBuffer();
        buff.append("digraph G {");
        buff.append(AUtil.lineSeparator);
        buff.append("  \"\" [fillcolor=black, shape=point]");
        buff.append(AUtil.lineSeparator);
        buff.append("  \"\" -> \"" + automaton.getInitialState() + "\";");
        buff.append(AUtil.lineSeparator);
        for (Iterator i = automaton.getFinalStates().iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            buff.append("  \"" + s + "\" [shape=doublecircle];");
            buff.append(AUtil.lineSeparator);
        }
        List lines = new ArrayList();
        for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            lines.add("  \"" + t.getPreState() + "\" -> \"" + t.getPostState() + "\" [label=\"" + labelGenerator.getLabel(t).replaceAll("\"", "\\\"") + "\"]" + ";");
        }
        Collections.sort(lines);
        for (Iterator i = lines.iterator(); i.hasNext(); ) {
            buff.append(i.next());
            buff.append(AUtil.lineSeparator);
        }
        buff.append("}");
        return buff.toString();
    }
}
