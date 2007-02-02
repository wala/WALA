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
package com.ibm.wala.stringAnalysis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

public class LocalNameTable {
    IR ir;
    HashMap localName2valueNumber;
    HashMap valueNumber2localName;
    HashMap instructionNumber;
    
    public LocalNameTable(IR ir){
        init(ir);
    }
    
    private void init(IR ir) {
      this.ir = ir;
      localName2valueNumber = new HashMap();
      valueNumber2localName = new HashMap();
      instructionNumber = new HashMap();
      IMethod m = ir.getMethod();
      SSAInstruction instructions[] = ir.getInstructions();
      for( int index = 0; index < instructions.length; index++ ){
	SSAInstruction instruction = instructions[index];
	if (instruction == null) continue;
	instructionNumber.put(instruction, new Integer(index));
	int numDefs = instruction.getNumberOfDefs();
	for (int i = 0; i < numDefs; i++) {
	  int v = instruction.getDef(i);
	  String names[] = ir.getLocalNames(index, v);
	  for (int j = 0; j < names.length; j++) {
	    String name = names[j];
	    store(v, name);
	  }
	}
	int numUses = instruction.getNumberOfUses();
	for( int i = 0; i < numUses; i++ ){
	  int v = instruction.getUse(i);
	  String names[] = ir.getLocalNames(index, v);
	  for( int j = 0; j < names.length; j++ ){
	    String name = names[j];
	    store(v, name);
	  }
	}
      }
    }
    
    public IR getIR(){
        return ir;
    }
    
    private void store(int valueNumber, String localName){
        Integer v = new Integer(valueNumber);
        List numbers = (List) localName2valueNumber.get(v);
        if( numbers == null ){
            numbers = new ArrayList();
            localName2valueNumber.put(localName, numbers);
        }
        List names = (List) valueNumber2localName.get(localName);
        if( names == null ){
            names = new ArrayList();
            valueNumber2localName.put(v, names);
        }
        numbers.add(v);
        names.add(localName);
    }
    
    public String[] getLocalNames(int valueNumber){
        List l = (List) valueNumber2localName.get(new Integer(valueNumber));
        if( l == null ){
            return new String[0];
        }
        else{
            String names[] = new String[l.size()];
            l.toArray(names);
            return names;
        }
    }
    
    public String[] getLocalNames(SSAInstruction instruction, int valueNumber){
        if( instruction == null ) {
            return getLocalNames(valueNumber);
        }
        else{
            Integer index = (Integer) instructionNumber.get(instruction);
            if( index == null ){ // in case of phi instructions
                return new String[0];
            }
            else{
                return getIR().getLocalNames(index.intValue(), valueNumber);
            }
        }
    }
    
    public String[] getLocalNames(int index, int valueNumber){
        if (index<0) {
            return getLocalNames(valueNumber);
        }
        else {
            return getIR().getLocalNames(index, valueNumber);
        }
    }
    
    public int[] getValueNumbers(String localName){
        List l = (List) localName2valueNumber.get(localName);
        if( l == null ){
            return new int[0];
        }
        int numbers[] = new int[l.size()];
        for( ListIterator i = l.listIterator(); i.hasNext(); ){
            int idx = i.nextIndex();
            Integer n = (Integer) i.next();
            numbers[idx] = n.intValue();
        }
        return numbers;
    }
}
