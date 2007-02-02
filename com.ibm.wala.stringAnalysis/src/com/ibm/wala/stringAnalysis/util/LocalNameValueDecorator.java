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

import java.util.HashMap;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.ValueDecorator;

public class LocalNameValueDecorator implements ValueDecorator {
    LocalNameTable ltable;
    SSAInstruction instruction;
    HashMap cache;

    public LocalNameValueDecorator(LocalNameTable ltable, SSAInstruction instruction){
        this.ltable = ltable;
        this.instruction = instruction;
        this.cache = new HashMap();
    }
    
    public LocalNameValueDecorator(LocalNameTable ltable){
        this(ltable, null);
    }

    public String getValueString(int valueNumber) {
        String str = (String) cache.get(new Integer(valueNumber));
        if( str != null ) return str;
        SymbolTable tbl = ltable.getIR().getSymbolTable();
        str = tbl.getValueString(valueNumber);
        String names[] = null;
        if( instruction == null ){
            names = ltable.getLocalNames(valueNumber);
        }
        else{
            names = ltable.getLocalNames(instruction, valueNumber);
        }
        if( names.length > 0 ){
            str = str.concat("{");
            for( int i = 0; i < names.length; i++ ){
                str = str.concat(names[i]);
                if( i != names.length - 1 ){
                    str = str.concat(", ");
                }
            }
            str = str.concat("}");
        }
        cache.put(new Integer(valueNumber), str);
        return str;
    }
}
