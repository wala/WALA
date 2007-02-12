package com.ibm.wala.stringAnalysis.js.client;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.parser.*;
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cast.js.client.*;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.js.translator.*;
import com.ibm.wala.stringAnalysis.translator.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.*;

import java.io.*;
import java.util.*;

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
