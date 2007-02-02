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
package com.ibm.wala.automaton.regex.string;

import java.util.List;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.parser.AmtParser;
import com.ibm.wala.automaton.string.*;

public class StringPatternSymbol implements ISymbol {
    final static public IPatternCompiler defaultCompiler = new StringPatternCompiler();
    final static private AmtParser parser = new AmtParser();
    
    IPattern pattern;
    IAutomaton compiledPattern;
    IPatternCompiler compiler;
    
    public StringPatternSymbol(IPattern pattern, IPatternCompiler compiler) {
        if (pattern == null) throw(new AssertionError("should not be null."));
        this.pattern = pattern;
        this.compiler = compiler;
        this.compiledPattern = compiler.compile(pattern);
    }
    
    public StringPatternSymbol(IPattern pattern) {
        this(pattern, defaultCompiler);
    }
    
    public StringPatternSymbol(String pattern) {
        this((IPattern) parser.parse("/" + pattern + "/").get(new Variable("_")));
    }
    
    public IAutomaton getCompiledPattern() {
        return compiledPattern;
    }
    
    public IPattern getPattern() {
        return pattern;
    }
    
    public String getName() {
        return pattern.toString();
    }

    // TODO: should improve StringPatternSymbol#matches().
    public boolean matches(ISymbol symbol, IMatchContext context) {
        if (symbol instanceof StringPatternSymbol) {
            StringPatternSymbol sps = (StringPatternSymbol) symbol;
            IContextFreeGrammar cfg = Grammars.toCFG(sps.compiledPattern);
            if (CFLReachability.containsAll(compiledPattern, cfg)) {
                context.put(this, sps);
                return true;
            }
            else {
                return false;
            }
        }
        else if (symbol instanceof StringSymbol) {
            StringSymbol strSym = (StringSymbol) symbol;
            List cs = strSym.toCharSymbols();
            if (compiledPattern.accept(cs)) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
        else if (symbol instanceof CFGSymbol) {
            CFGSymbol cfgSymbol = (CFGSymbol) symbol;
            if (CFLReachability.containsAll(compiledPattern, cfgSymbol.getGrammar())) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
        else {
            if (compiledPattern.accept(AUtil.list(new ISymbol[]{symbol}))) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
    }

    public boolean possiblyMatches(ISymbol symbol, IMatchContext context) {
        if (symbol instanceof StringPatternSymbol) {
            StringPatternSymbol sps = (StringPatternSymbol) symbol;
            IContextFreeGrammar cfg = Grammars.toCFG(sps.compiledPattern);
            if (CFLReachability.containsSome(cfg, compiledPattern)) {
                context.put(this, sps);
                return true;
            }
            else {
                return false;
            }
        }
        else if (symbol instanceof StringSymbol) {
            StringSymbol strSym = (StringSymbol) symbol;
            List cs = strSym.toCharSymbols();
            if (compiledPattern.accept(cs)) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
        else if (symbol instanceof CFGSymbol) {
            CFGSymbol cfgSymbol = (CFGSymbol) symbol;
            if (CFLReachability.containsSome(cfgSymbol.getGrammar(), compiledPattern)) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
        else {
            if (compiledPattern.accept(AUtil.list(new ISymbol[]{symbol}))) {
                context.put(this, symbol);
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    public int hashCode() {
        return pattern.hashCode() + compiledPattern.hashCode() + compiler.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (!getClass().equals(obj.getClass())) return false;
        StringPatternSymbol ps = (StringPatternSymbol) obj;
        return pattern.equals(ps.pattern)
            && compiledPattern.equals(ps.compiledPattern)
            && compiler.equals(compiler);
    }

    public void traverse(ISymbolVisitor visitor) {
        visitor.onVisit(this);
        visitor.onLeave(this);
    }

    public ISymbol copy(ISymbolCopier copier) {
        ISymbol s = copier.copy(this);
        if (s instanceof StringPatternSymbol) {
            StringPatternSymbol ps = (StringPatternSymbol) s;
            if (copier instanceof ISTSCopier) {
                ISTSCopier scopier = (ISTSCopier) copier;
                ps.compiledPattern = (IAutomaton) scopier.copy(ps.compiledPattern);
            }
        }
        return s;
    }

    public int size() {
        return 0;
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }

}
