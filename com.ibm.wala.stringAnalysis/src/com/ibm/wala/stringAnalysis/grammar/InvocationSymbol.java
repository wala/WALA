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
package com.ibm.wala.stringAnalysis.grammar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ISymbolCopier;
import com.ibm.wala.automaton.string.ISymbolVisitor;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class InvocationSymbol implements ISymbol {
  private List parameters;
  private ISymbol funcSymbol;
  private ISymbol recvSymbol;
  private IR ir;
  private SSAInstruction instruction;

  private InvocationSymbol() {
    parameters = new ArrayList();
    funcSymbol = null;
    instruction = null;
  }

  public InvocationSymbol(IR ir, SSAInstruction instruction, ISymbol funcSymbol, ISymbol recvSymbol, List parameters) {
    this();
    this.ir = ir;
    this.instruction = instruction;
    this.funcSymbol = funcSymbol;
    this.recvSymbol = recvSymbol;
    this.parameters.addAll(parameters);
  }

  public InvocationSymbol(IR ir, SSAInstruction instruction, ISymbol funcSymbol, ISymbol recvSymbol, ISymbol parameters[]) {
    this(ir, instruction, funcSymbol, recvSymbol, SAUtil.list(parameters));
  }

  public InvocationSymbol(IR ir, SSAInstruction instruction, String funcName, String recvName, ISymbol parameters[]) {
    this(ir, instruction, new StringSymbol(funcName), new StringSymbol(recvName), parameters);
  }

  public ISymbol getFunction() {
    return funcSymbol;
  }

  public ISymbol getReceiver() {
    return recvSymbol;
  }

  public List getParameters() {
    return parameters;
  }

  public ISymbol getParameter(int index) {
    return (ISymbol) parameters.get(index);
  }

  public SSAInstruction getInstruction() {
    return instruction;
  }

  public IR getIR() {
    return ir;
  }

  public String getName() {
    return funcSymbol.getName();
  }

  /*
    public void setName(String name) {
        funcSymbol.setName(name);
    }
   */

   public boolean matches(ISymbol symbol, IMatchContext ctx) {
     if (symbol == null) return false;
     if (!getClass().equals(symbol.getClass())) return false;

     InvocationSymbol isym = (InvocationSymbol) symbol;
     if (funcSymbol.matches(isym.funcSymbol, ctx)
         && ((recvSymbol==isym.recvSymbol) || recvSymbol.matches(isym.recvSymbol, ctx))
         && parameters.size()==isym.getParameters().size()) {
       for (int i = 0; i < parameters.size(); i++) {
         ISymbol s1 = (ISymbol) parameters.get(i);
         ISymbol s2 = (ISymbol) isym.getParameters().get(i);
         if (!s1.matches(s2,ctx)) {
           return false;
         }
       }
     }
     return true;
   }

   public boolean possiblyMatches(ISymbol symbol, IMatchContext ctx) {
     if (symbol == null) return false;
     if (!getClass().equals(symbol.getClass())) return false;

     InvocationSymbol isym = (InvocationSymbol) symbol;
     if (funcSymbol.possiblyMatches(isym.funcSymbol, ctx)
         && ((recvSymbol==isym.recvSymbol) || recvSymbol.possiblyMatches(isym.recvSymbol, ctx))
         && parameters.size()==isym.getParameters().size()) {
       for (int i = 0; i < parameters.size(); i++) {
         ISymbol s1 = (ISymbol) parameters.get(i);
         ISymbol s2 = (ISymbol) isym.getParameters().get(i);
         if (!s1.possiblyMatches(s2,ctx)) {
           return false;
         }
       }
     }
     return true;
   }

   public void traverse(ISymbolVisitor visitor) {
     visitor.onVisit(this);
     funcSymbol.traverse(visitor);
     if (recvSymbol!=null) recvSymbol.traverse(visitor);
     for (Iterator i = parameters.iterator(); i.hasNext(); ) {
       ISymbol s = (ISymbol) i.next();
       s.traverse(visitor);
     }
     visitor.onLeave(this);
   }

   public Object clone() {
     try {
       return super.clone();
     } catch (CloneNotSupportedException e) {
       throw(new RuntimeException(e));
     }
   }

   public ISymbol copy(ISymbolCopier copier) {
     InvocationSymbol s = (InvocationSymbol) copier.copy(this);
     s.funcSymbol = copier.copySymbolReference(s, funcSymbol);
     s.recvSymbol = copier.copySymbolReference(s, recvSymbol);
     s.parameters = new ArrayList(copier.copySymbolReferences(s, parameters));
     return s;
   }

   public int size() {
     return 0;
   }

   public int hashCode() {
     /*
        return funcSymbol.hashCode()
            + ((recvSymbol==null) ? 0 : recvSymbol.hashCode())
            + parameters.hashCode();
      */
     return funcSymbol.hashCode()
     + ((recvSymbol==null) ? 0 : recvSymbol.hashCode());
   }

   public boolean equals(Object obj) {
     if (obj == null) return false;
     if (!getClass().equals(obj.getClass())) return false;
     InvocationSymbol isym = (InvocationSymbol) obj;
     boolean r = funcSymbol.equals(isym.funcSymbol)
     && ((recvSymbol==isym.recvSymbol) || recvSymbol.equals(isym.recvSymbol))
     && parameters.equals(isym.parameters);
     return r;
   }

   public String toString() {
     StringBuffer paramStr = new StringBuffer();
     for (Iterator i = parameters.iterator(); i.hasNext(); ) {
       ISymbol param = (ISymbol) i.next();
       paramStr.append(param.toString());
       if (i.hasNext()) {
         paramStr.append(", ");
       }
     }
     return "&" + recvSymbol + "." + funcSymbol.toString() + "(" + paramStr + ")";
   }

}
