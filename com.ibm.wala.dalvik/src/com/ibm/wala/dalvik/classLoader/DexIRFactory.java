/*******************************************************************************
 * Copyright (c) 2002 - 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Adam Fuchs, Avik Chaudhur, Steve Suh - Modified ShrikeIRFactory to work with Dalvik
 *******************************************************************************/

package com.ibm.wala.dalvik.classLoader;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ssa.DexSSABuilder;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.ShrikeIndirectionData;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.DeadAssignmentElimination;

public class DexIRFactory extends DefaultIRFactory {
    public final static boolean buildLocalMap = false;

	@Override
    public ControlFlowGraph makeCFG(IMethod method, Context C) throws IllegalArgumentException {
    	if (method == null) {
    	      throw new IllegalArgumentException("null method");
    	}
    	if (method instanceof DexIMethod)
    		return new DexCFG((DexIMethod)method, C);    
    	return super.makeCFG(method,C);    	    
    }

    @Override
    public IR makeIR(IMethod _method, Context C, final SSAOptions options) throws IllegalArgumentException {    	
        if (_method == null) {
            throw new IllegalArgumentException("null method");
        }
        
    	if (!(_method instanceof DexIMethod))
    		return super.makeIR(_method, C, options);
        final DexIMethod method = (DexIMethod)_method;

        //      com.ibm.wala.shrikeBT.IInstruction[] instructions = null;
        //      try {
        //        instructions = method.getInstructions();
        //      } catch (InvalidClassFileException e) {
        //        e.printStackTrace();
        //        Assertions.UNREACHABLE();
        //      }
        final DexCFG cfg = (DexCFG)makeCFG(method, C);

        // calculate the SSA registers from the given cfg

        //TODO: check this
        final SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameters());
//      final SymbolTable symbolTable = new SymbolTable(method.getNumberOfParameterRegisters());
        final SSAInstruction[] newInstrs = new SSAInstruction[method.getDexInstructions().length];

        final SSACFG newCfg = new SSACFG(method, cfg, newInstrs);

        return new IR(method, newInstrs, symbolTable, newCfg, options) {
            private final SSA2LocalMap localMap;

            private final ShrikeIndirectionData indirectionData;

            /**
             * Remove any phis that are dead assignments.
             *
             * TODO: move this elsewhere?
             */
            private void eliminateDeadPhis() {
                DeadAssignmentElimination.perform(this);
            }

            @Override
            protected String instructionPosition(int instructionIndex) {
                int bcIndex = method.getBytecodeIndex(instructionIndex);
                int lineNumber = method.getLineNumber(bcIndex);

                if (lineNumber == -1) {
                    return "";
                } else {
                    return "(line " + lineNumber + ")";
                }
            }

            @Override
            public SSA2LocalMap getLocalMap() {
                return localMap;
            }

            {
                DexSSABuilder builder = DexSSABuilder.make(method, newCfg, cfg, newInstrs, symbolTable, buildLocalMap, options.getPiNodePolicy());
                builder.build();
                if (buildLocalMap)
                    localMap = builder.getLocalMap();
                else
                    localMap = null;

                indirectionData = builder.getIndirectionData();

                eliminateDeadPhis();

                setupLocationMap();

                //System.out.println("Successfully built a Dex IR!");
                //for(SSAInstruction ssaInst:newInstrs)
                //{
                //  System.out.println("\t"+ssaInst);
                //}
            }

            @SuppressWarnings("unchecked")
            @Override
            protected ShrikeIndirectionData getIndirectionData() {
                return indirectionData;
            }
        };
    }

    @Override
    public boolean contextIsIrrelevant(IMethod method) {
    	if (method == null) {
    	      throw new IllegalArgumentException("null method");
    	}
    	if (method instanceof DexIMethod)
    		return true;
    	return super.contextIsIrrelevant(method);
    }
}
