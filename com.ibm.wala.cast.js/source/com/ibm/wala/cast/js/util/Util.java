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
package com.ibm.wala.cast.js.util;

import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class Util {

  public static IntSet getArgumentsArrayVns(IR ir, final DefUse du) {
    int originalArgsVn = getArgumentsArrayVn(ir);
    final MutableIntSet result = IntSetUtil.make();
    if (originalArgsVn == -1) {
      return result;
    }
    
    result.add(originalArgsVn);
    int size; 
    do {
      size = result.size();
      result.foreach(vn -> {
        for(SSAInstruction inst : Iterator2Iterable.make(du.getUses(vn))) {
          if (inst instanceof PrototypeLookup || inst instanceof SSAPhiInstruction) {
            result.add(inst.getDef());
          }
        }
      });
    } while (size != result.size());
    
    return result;
  }
  
  public static int getArgumentsArrayVn(IR ir) {
    for(int i = 0; i < ir.getInstructions().length; i++) {
      SSAInstruction inst = ir.getInstructions()[i];
      if (inst != null) {
        for(int v = 0; v < inst.getNumberOfUses(); v++) {
          String[] names = ir.getLocalNames(i, inst.getUse(v));
          if (names != null && names.length == 1 && "arguments".equals(names[0])) {
            return inst.getUse(v);
          }
        }
      }
    }
    
    return -1;
  }
  

}
