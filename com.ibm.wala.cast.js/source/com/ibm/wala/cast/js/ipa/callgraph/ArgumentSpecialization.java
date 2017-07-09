/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.translator.JSAstTranslator;
import com.ibm.wala.cast.js.translator.JSConstantFoldingRewriter;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.Retranslatable;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextItem.Value;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;

public class ArgumentSpecialization {

  public static class ArgumentSpecializationContextIntepreter extends AstContextInsensitiveSSAContextInterpreter {

    public ArgumentSpecializationContextIntepreter(AnalysisOptions options, IAnalysisCacheView cache) {
      super(options, cache);
    }

    @Override
    public IR getIR(CGNode node) {
      if (node.getMethod() instanceof Retranslatable) {
        return getAnalysisCache().getIR(node.getMethod(), node.getContext());
      } else {
        return super.getIR(node);
      }
    }
    
    @Override
    public DefUse getDU(CGNode node) {
      if (node.getMethod() instanceof Retranslatable) {
        return getAnalysisCache().getDefUse(getIR(node));
      } else {
        return super.getDU(node);
      }
    }
  }

  public static class ArgumentCountContext implements Context {
    private final Context base;
    private final int argumentCount;

    public static ContextKey ARGUMENT_COUNT = new ContextKey() {
      @Override
      public String toString() {
        return "argument count key";
      }
    };
    
    @Override
    public int hashCode() {
      return base.hashCode() + (argumentCount * 4073);
    }
    
    @Override
    public boolean equals(Object o) {
      return 
        o.getClass() == this.getClass() && 
        base.equals(((ArgumentCountContext)o).base) &&
        argumentCount == ((ArgumentCountContext)o).argumentCount;
    }
    
    public ArgumentCountContext(int argumentCount, Context base) {
      this.argumentCount = argumentCount;
      this.base = base;
    }
    
    @Override
    public ContextItem get(ContextKey name) {
      return (name == ARGUMENT_COUNT)? ContextItem.Value.make(argumentCount): base.get(name);
    }

    @Override
    public String toString() {
      return base.toString() + "(nargs:" + argumentCount + ")";
    }
  }

  public static class ArgumentCountContextSelector implements ContextSelector, ContextKey {
    private final ContextSelector base;
    
    public ArgumentCountContextSelector(ContextSelector base) {
      this.base = base;
    }

    @Override
    public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
      Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
      if (caller.getMethod() instanceof Retranslatable) {
        int v = -1;
        for (SSAAbstractInvokeInstruction x : caller.getIR().getCalls(site)) {
          if (v == -1) {
            v = x.getNumberOfParameters();
          } else {
            if (v != x.getNumberOfParameters()) {
              return baseContext; 
            }
          }
        }
        
        return new ArgumentCountContext(v, baseContext);
      } else {
        return baseContext;
      }
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
      return base.getRelevantParameters(caller, site);
    }

  }

  public static class ArgumentCountIRFactory extends AstIRFactory.AstDefaultIRFactory<IMethod> {
    private static final CAstPattern directAccessPattern = CAstPattern.parse("|(ARRAY_REF(VAR(\"arguments\"),<value>*)||OBJECT_REF(VAR(\"arguments\"),<value>*))|");

    private static final CAstPattern destructuredAccessPattern = CAstPattern.parse("BLOCK_EXPR(ASSIGN(VAR(/[$][$]destructure[$]rcvr[0-9]+/),VAR(\"arguments\")),ASSIGN(VAR(<name>/[$][$]destructure[$]elt[0-9]+/),<value>*))");

    private static final CAstPattern destructuredCallPattern = CAstPattern.parse("CALL(VAR(<name>/[$][$]destructure[$]elt[0-9]+/),\"dispatch\",VAR(<thisptr>/[$][$]destructure[$]rcvr[0-9]+/),<args>**)");
    
    private final SSAOptions defaultOptions;
    
    public ArgumentCountIRFactory(SSAOptions defaultOptions) {
      this.defaultOptions = defaultOptions;
    }

    @Override
    public boolean contextIsIrrelevant(IMethod method) {
      return method instanceof Retranslatable? false: super.contextIsIrrelevant(method);
    }

    @Override
    public IR makeIR(final IMethod method, Context context, SSAOptions options) {
      if (method instanceof Retranslatable) {
        @SuppressWarnings("unchecked")
        final Value<Integer> v = (Value<Integer>) context.get(ArgumentCountContext.ARGUMENT_COUNT);
        final Retranslatable m = (Retranslatable)method;
        if (v != null) {
          final JavaScriptLoader myloader = (JavaScriptLoader) method.getDeclaringClass().getClassLoader();
                    
          class FixedArgumentsRewriter extends CAstBasicRewriter {
            private final CAstEntity e;
            private final Map<String, CAstNode> argRefs = HashMapFactory.make();
 
            public FixedArgumentsRewriter(CAst Ast) {
              super(Ast, false);
              this.e = m.getEntity();
              for(Segments s : CAstPattern.findAll(destructuredAccessPattern, m.getEntity())) {
                argRefs.put(s.getSingle("name").getValue().toString(), s.getSingle("value"));
              }
            }

            private CAstNode handleArgumentRef(CAstNode n) {
              Object x = n.getValue();
              if (x != null) {
                if (x instanceof Number && ((Number)x).intValue() < v.getValue()-2) {
                  int arg = ((Number)x).intValue() + 2;
                  if (arg < e.getArgumentCount()) {
                    return Ast.makeNode(CAstNode.VAR, Ast.makeConstant(e.getArgumentNames()[arg]));
                  } else {
                    return Ast.makeNode(CAstNode.VAR, Ast.makeConstant("$arg" + arg));                    
                  }
                } else if (x instanceof String && "length".equals(x)) {
                  return Ast.makeConstant(v.getValue());
                }
              }
              
              return null;
            }
            
            @Override
            protected CAstNode copyNodes(CAstNode root, 
                CAstControlFlowMap cfg, 
                NonCopyingContext context,
                Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap)
            {
              CAstNode result = null;
              Segments s;

              if ((s = CAstPattern.match(directAccessPattern, root)) != null) {
                result = handleArgumentRef(s.getSingle("value"));

              } else if ((s = CAstPattern.match(destructuredCallPattern, root)) != null) {
                if (argRefs.containsKey(s.getSingle("name").getValue().toString())) {
                 List<CAstNode> x = new ArrayList<>();
                 CAstNode ref = handleArgumentRef(argRefs.get(s.getSingle("name").getValue().toString()));
                 if (ref != null) {
                   x.add(ref);
                   x.add(Ast.makeConstant("do"));
                   x.add(Ast.makeNode(CAstNode.VAR, Ast.makeConstant("arguments")));
                   for (CAstNode c : s.getMultiple("args")) {
                     x.add(copyNodes(c, cfg, context, nodeMap));
                   }
                   result = Ast.makeNode(CAstNode.CALL, x.toArray(new CAstNode[ x.size() ]));
                 }
                }
               
              } else if (root.getKind() == CAstNode.CONSTANT) {
                result = Ast.makeConstant(root.getValue());

              } else if (root.getKind() == CAstNode.OPERATOR) {
                result = root;
              } 
              
              if (result == null) {
                CAstNode children[] = new CAstNode[root.getChildCount()];
                for (int i = 0; i < children.length; i++) {
                  children[i] = copyNodes(root.getChild(i), cfg, context, nodeMap);
                }
                for(Object label: cfg.getTargetLabels(root)) {
                  if (label instanceof CAstNode) {
                    copyNodes((CAstNode)label, cfg, context, nodeMap);
                  }
                }
                CAstNode copy = Ast.makeNode(root.getKind(), children);
                result = copy;
              }

              nodeMap.put(Pair.make(root, context.key()), result);
              return result;
            }
            
          }

          final FixedArgumentsRewriter args = new FixedArgumentsRewriter(new CAstImpl());
          final JSConstantFoldingRewriter fold = new JSConstantFoldingRewriter(new CAstImpl());

          class ArgumentativeTranslator extends JSAstTranslator {
            
            public ArgumentativeTranslator(JavaScriptLoader loader) {
              super(loader);
            }

            private CAstEntity codeBodyEntity;
            private IMethod specializedCode;
            

            @Override
            protected int getArgumentCount(CAstEntity f) {
              return Math.max(super.getArgumentCount(f), v.getValue());
            }

            @Override
            protected String[] getArgumentNames(CAstEntity f) {
              if (super.getArgumentCount(f) >= v.getValue()) {
                return super.getArgumentNames(f);
              } else {
                String[] argNames = new String[ v.getValue() ];
                System.arraycopy(super.getArgumentNames(f), 0, argNames, 0, super.getArgumentCount(f));
                for(int i = super.getArgumentCount(f); i < argNames.length; i++) {
                  argNames[i] = "$arg" + i;
                }
                
                return argNames;
              }
            }

            @Override
            protected String composeEntityName(WalkContext parent, CAstEntity f) {
              if (f == codeBodyEntity) {
                return super.composeEntityName(parent, f) + "_" + v.getValue().intValue();                
              } else {
                return super.composeEntityName(parent, f);
              }
            }

            @Override
            protected void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG<SSAInstruction, ? extends IBasicBlock<SSAInstruction>> cfg, SymbolTable symtab,
                boolean hasCatchBlock, Map<IBasicBlock<SSAInstruction>,TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation LI,
                DebuggingInformation debugInfo) {
              if (N == codeBodyEntity) {
                specializedCode = myloader.makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, LI, debugInfo, method.getDeclaringClass());
              } else {
                super.defineFunction(N, definingContext, cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, LI, debugInfo);
              }
            }

            @Override
            public void translate(CAstEntity N, WalkContext context) {
              if (N == m.getEntity()) {
                codeBodyEntity = fold.rewrite(args.rewrite(N));
                super.translate(codeBodyEntity, context);
              } else {
                super.translate(N, context);                
              }
            }
            
          }
          ArgumentativeTranslator a = new ArgumentativeTranslator(myloader);
          m.retranslate(a);
          return super.makeIR(a.specializedCode, context, options);
        }
      }
      
      return super.makeIR(method, context, options);
    }

    @Override
    public ControlFlowGraph makeCFG(IMethod method, Context context) {
      return makeIR(method, context, defaultOptions).getControlFlowGraph();
    }
  }
}
