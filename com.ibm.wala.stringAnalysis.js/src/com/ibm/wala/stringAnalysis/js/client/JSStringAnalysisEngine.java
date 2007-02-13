package com.ibm.wala.stringAnalysis.js.client;

import com.ibm.wala.automaton.grammar.string.CFLReachability;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.automaton.parser.AmtParser;
import com.ibm.wala.automaton.regex.string.IPattern;
import com.ibm.wala.automaton.regex.string.IPatternCompiler;
import com.ibm.wala.automaton.regex.string.StringPatternCompiler;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.cast.js.client.JavaScriptAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.LexicalVariable;
import com.ibm.wala.stringAnalysis.js.translator.JSFunctionNameResolver;
import com.ibm.wala.stringAnalysis.js.translator.JSSSA2Rule;
import com.ibm.wala.stringAnalysis.js.translator.JSTranslatorRepository;
import com.ibm.wala.stringAnalysis.translator.BB2GR;
import com.ibm.wala.stringAnalysis.translator.CG2GR;
import com.ibm.wala.stringAnalysis.translator.FunctionNameCalleeResolver;
import com.ibm.wala.stringAnalysis.translator.GR2CFG;
import com.ibm.wala.stringAnalysis.translator.IR2GR;
import com.ibm.wala.stringAnalysis.translator.ISSA2Rule;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Trace;

public class JSStringAnalysisEngine extends JavaScriptAnalysisEngine {

  private final AmtParser parser = new AmtParser();

  private final IPatternCompiler patternCompiler = new StringPatternCompiler();

  protected GR2CFG gr2cfg;
  
  protected GR gr;

  private CG2GR setupStringAnalysis() {
    ISSA2Rule ssa2rule = new JSSSA2Rule();
    BB2GR bb2gr = new BB2GR(ssa2rule);
    IR2GR ir2gr = new IR2GR(bb2gr);
    return 
      new CG2GR(
        ir2gr, 
	new FunctionNameCalleeResolver(new JSFunctionNameResolver()));
  }

  public CallGraph buildDefaultCallGraph() {
    PropagationCallGraphBuilder cgb = 
      (PropagationCallGraphBuilder)defaultCallGraphBuilder();

    CallGraph CG = cgb.makeCallGraph(options);
    
    CG2GR cg2gr = setupStringAnalysis();
    ISimplify g = cg2gr.translate(cgb);
    gr = (GR) g;

    gr2cfg = new GR2CFG(new JSTranslatorRepository());
    
    return CG;
  }

  private IContextFreeGrammar verifyCFG(IVariable v) {
    IContextFreeGrammar cfg = gr2cfg.solve(gr, v);
    Trace.println("-- context-free grammar for " + v + ": ");
    Trace.println(SAUtil.prettyFormat(cfg));
    Trace.println("-- context-free grammar for " + v + ": ");
    Grammars.normalize(cfg, null);
    Trace.println(SAUtil.prettyFormat(cfg));
    return cfg;
  }
    
  private IAutomaton pattern(String patStr) {
    IPattern pat = (IPattern) 
      parser.parse("/" + patStr + "/").get(new Variable("_"));
    if (pat == null) {
      return new Automaton(                
        new State("s1"),
	new State[]{},
	new Transition[]{}
      );
    }
    IAutomaton a = patternCompiler.compile(pat);
    return a;
  }

  private IContextFreeGrammar getCFG(IVariable v) {
    return verifyCFG(v);
  }
	
  public IContextFreeGrammar getCFG(String variableName) {
    return verifyCFG(new LexicalVariable(variableName));
  }

  public boolean containsAll(String variableName, String pattern) {
    IContextFreeGrammar cfg = getCFG(variableName);
    return CFLReachability.containsAll(pattern(pattern), cfg);
  }

  public boolean containsSome(String variableName, String pattern) {
    IContextFreeGrammar cfg = getCFG(variableName);
    return CFLReachability.containsSome(cfg, pattern(pattern));
  }
}
