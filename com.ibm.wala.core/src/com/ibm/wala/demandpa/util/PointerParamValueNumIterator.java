/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SymbolTable;

/**
 * Iterates over the value numbers of the pointer parameters of
 * a method.
 * @author Manu Sridharan
 * 
 */
public class PointerParamValueNumIterator implements Iterator<Integer> {

  final TypeInference ti;

  final SymbolTable symbolTable;

  final int numParams;
  
  int paramInd;

  int nextParameter;
  
  public PointerParamValueNumIterator(CGNode node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node == null");
    }
    IR ir = node.getIR();
    ti = TypeInference.make(ir, false);
    symbolTable = ir.getSymbolTable();
    numParams = symbolTable.getNumberOfParameters();
    paramInd = 0;
    setNextParameter();
  }

  private void setNextParameter() {
    int i = paramInd;
    for ( ; i < numParams; i++) {
      int parameter = symbolTable.getParameter(i);
      TypeAbstraction t = ti.getType(parameter);
      if (t != null) {
        nextParameter = parameter;
        break;
      }      
    }
    paramInd = ++i;
  }

  /*
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return paramInd <= numParams;
  }

  /*
   * @see java.util.Iterator#next()
   */
  @Override
  public Integer next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    int ret = nextParameter;
    setNextParameter();
    return ret;
  }

  /*
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

}
