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
package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.translator.JSAstTranslator;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class WebPageLoaderFactory extends JavaScriptLoaderFactory {

  public WebPageLoaderFactory(JavaScriptTranslatorFactory factory) {
    super(factory);
  }
  
  public WebPageLoaderFactory(JavaScriptTranslatorFactory factory, CAstRewriterFactory preprocessor) {
   super(factory, preprocessor); 
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new JavaScriptLoader( cha, translatorFactory, preprocessor ) {
      @Override
      protected TranslatorToIR initTranslator() {
        return new JSAstTranslator(this) {
          private final CAst Ast = new CAstImpl();

          private boolean isNestedWithinScriptBody(WalkContext context) {
            return isScriptBody(context) || context.getName().contains("__WINDOW_MAIN__");
          }
          
          private boolean isScriptBody(WalkContext context) {
            return context.top().getName().equals( "__WINDOW_MAIN__" );
          }
          
          @Override
          protected int doGlobalRead(CAstNode n, WalkContext context, String name) {
            int result = context.currentScope().allocateTempValue();
            if (isNestedWithinScriptBody(context) && ! "$$undefined".equals(name)  && ! "window".equals(name)) {
              
              // check if field is defined on 'window'
              int windowVal = isScriptBody(context)? super.doLocalRead(context, "this"): super.doGlobalRead(n, context, "window");
              int isDefined = context.currentScope().allocateTempValue();
              context.currentScope().getConstantValue(name);
              doIsFieldDefined(context, isDefined, windowVal, Ast.makeConstant(name));
              context.cfg().addInstruction(
                  insts.ConditionalBranchInstruction(
                      translateConditionOpcode(CAstOperator.OP_NE), 
                      null, 
                      isDefined, 
                      context.currentScope().getConstantValue(new Integer(0))));
              PreBasicBlock srcB = context.cfg().getCurrentBlock();       
              
              // field lookup of value
              context.cfg().newBlock(true);
              context.cfg().addInstruction(((JSInstructionFactory) insts).GetInstruction(result, windowVal, name));
              context.cfg().addInstruction(insts.GotoInstruction());
              PreBasicBlock trueB = context.cfg().getCurrentBlock();

              // read global
              context.cfg().newBlock(false);
              PreBasicBlock falseB = context.cfg().getCurrentBlock();
              int sr = super.doGlobalRead(n, context, name);
              context.cfg().addInstruction(((JSInstructionFactory) insts).AssignInstruction(result, sr));

              // end
              context.cfg().newBlock(true);
              
              context.cfg().addEdge(trueB, context.cfg().getCurrentBlock());
              context.cfg().addEdge(srcB, falseB);
            
              return result;
              
            } else {  
              return super.doGlobalRead(n, context, name);
            }
          }

          @Override
          protected void doLocalWrite(WalkContext context, String nm, int rval) {
            if (isScriptBody(context)) {
              int windowVal = super.doLocalRead(context, "this");
              context.currentScope().getConstantValue(nm);
              context.cfg().addInstruction(((JSInstructionFactory) insts).PutInstruction(windowVal, rval, nm));
            } 
            
            super.doLocalWrite(context, nm, rval);
          }
          
          @Override
          protected void leaveFunctionStmt(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
            super.leaveFunctionStmt(n, context, visitor);
            if (isScriptBody(context)) {
              CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
              int fnValue = context.currentScope().lookup(fn.getName()).valueNumber();
              assert fnValue > 0;
              int windowVal = super.doLocalRead(context, "this");
              context.cfg().addInstruction(((JSInstructionFactory) insts).PutInstruction(windowVal, fnValue, fn.getName()));
            }
          }
        };
      }
    };
  }
}
