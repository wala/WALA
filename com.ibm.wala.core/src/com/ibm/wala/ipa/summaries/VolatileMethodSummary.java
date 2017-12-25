/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.ipa.summaries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 *  Instructions can be added in a non-ascending manner.
 *
 *  The later position of the instruction is determined by it's iindex.
 *  Additionally this Summary may be instructed to prune unnecessary 
 *  instructions.
 *
 *  However don't go berserk with the iindex as this will consume loads of
 *  memory.
 *
 *  You can get an ordinary MethodSummary using the {@link #getMethodSummary()}-Method.
 *
 *  It extends the MethodSummarys capabilities by the functions:
 *  * {@link #getStatementAt(int)}
 *  * {@link #reserveProgramCounters(int)}
 *  * {@link #allowReserved(boolean)}
 *
 *  @see com.ibm.wala.ssa.SSAInstructionFactory
 *  @see com.ibm.wala.ipa.callgraph.impl.FakeRootMethod
 *  @see com.ibm.wala.ipa.summaries.MethodSummary
 *
 *  @author     Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since      2013-09-08
 */
public class VolatileMethodSummary {

    private static final boolean DEBUG = false;
    private boolean allowReservedPC = false;
    private MethodSummary summary;
    private List<SSAInstruction> instructions = new ArrayList<>();
    private Map<Integer, Atom> localNames = new HashMap<>();
    private int currentProgramCounter = 0;
    private boolean locked = false;

    /**
     *  Placeholder for Reserved slots.
     */
    private static final class Reserved extends SSAInstruction {
        public Reserved () { super(SSAInstruction.NO_INDEX); }
        @Override
        public SSAInstruction copyForSSA (SSAInstructionFactory insts, int[] defs, int[] uses) {
            throw new IllegalStateException();
        }
        @Override
        public int hashCode () { return 12384; }
        @Override
        public boolean isFallThrough() { return true; }
        @Override
        public String toString (SymbolTable symbolTable) { return "Reserved Slot"; }
        @Override
        public void visit (IVisitor v) { throw new IllegalStateException(); }
    }
    private static final Reserved RESERVED = new Reserved();

    /**
     *  @param  summary a "real" summary methods get added to.
     *  @throws IllegalArgumentException if this summary is null or not empty
     */
    public VolatileMethodSummary(MethodSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("The given summary is null");
        }
        if (summary.getNumberOfStatements() > 0) {
            throw new IllegalArgumentException("The given summary is not empty");
        }
        this.summary = summary;
    }

    /**
     *  @param  programCounter  the ProgramCounter to retrieve the Instruction from
     *  @return The instruction or null if there is none
     *  @throws IllegalArgumentException if the ProgramCounter is negative
     */
    public SSAInstruction getStatementAt(int programCounter) {
        if (programCounter < 0) {
            throw new IllegalArgumentException("Program-Counter may not be negative!");
        }
        if (this.instructions.size() <= programCounter) {
            return null;
        }
        if (this.instructions.get(programCounter).equals(RESERVED)) {
            return null;
        }
        return this.instructions.get(programCounter);
    }

    /**
     *  Reserves an amount of ProgramCounters for later use.
     *
     *  This method reserves a count of ProgramCounters and thus affects the value
     *  returned by getNextProgramCounter. It also marks these ProgramCounters as
     *  reserved so you can't use them unless you explicitly allow it by 
     *  {@link #allowReserved(boolean)}.
     *
     *  @param  count   The amount of ProgramCounters to reserve ongoing from the
     *      current ProgramCounter
     *  @throws IllegalArgumentException if the count is negative (a count of zero 
     *      is however ok)
     */
    public void reserveProgramCounters(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("The count of ProgramCounters to reserve may not be negative");
        }
        for (int i=0; i<count; ++i) {
            instructions.add(RESERVED);
        }
        currentProgramCounter += count;
    }

    /**
     *  (Dis-)allows the usage of reserved ProgramCounters.
     *
     *  The setting of this function defaults to disallow upon class creation
     *
     *  @param  enable  A value of true allows the usage of all reserved ProgramCounters
     *  @return the previous value of allowReserved 
     */
    public boolean allowReserved(boolean enable) {
        boolean prev = this.allowReservedPC;
        this.allowReservedPC = enable;
        return prev;
    }

    /**
     *  Returns if the ProgramCounter is reserved.
     *
     *  @param programCounter   the ProgramCounter in question
     *  @return true if the position is reserved
     *  @throws IllegalArgumentException if the ProgramCounter is negative
     */
    public boolean isReserved(int programCounter) {
        if (programCounter < 0) {
            throw new IllegalArgumentException("The Program-Counter may not be negative");
        }

        if (instructions.size() - 1 < programCounter) return false;
        if (instructions.get(programCounter) == null) return false;
        return (instructions.get(programCounter).equals(RESERVED));
    }

    /**
     *  Returns if the ProgramCounter is writable.
     *
     *  The ProgramCounter is not writable if there is already an instruction located at that 
     *  ProgramCounter or if the reserved ProgramCounters forbid usage. Thus the answer may
     *  depend on the setting of {@link #allowReserved(boolean)}.
     *
     *  @param programCounter   the ProgramCounter in question
     *  @return true if you may write to the location
     *  @throws IllegalArgumentException if the ProgramCounter is negative
     */
    public boolean isFree(int programCounter) {
        if (programCounter < 0) {
            throw new IllegalArgumentException("The Program-Counter may not be negative");
        }

        if (instructions.size() - 1 < programCounter) return true;
        if (instructions.get(programCounter) == null) return true;
        if (instructions.get(programCounter).equals(RESERVED)) return false;
        return false;
    }

    /**
     *  Not exactly dual to {@link #isFree(int)}.
     *
     *  Returns whether an instruction is located at ProgramCounter. Thus it is a  shortcut
     *  to {@link #getStatementAt(int)} != null.
     *  <p>
     *  It is not the exact dual to {@link #isFree(int)} as it does not consider reserved
     *  ProgramCounters.
     *
     *  @param programCounter   the ProgramCounter in question
     *  @return true if there's an instruction at that program counter
     *  @throws IllegalArgumentException if the ProgramCounter is negative
    */
    public boolean isUsed(int programCounter) {
        if (programCounter < 0) {
            throw new IllegalArgumentException("The Program-Counter may not be negative");
        }

        if (instructions.size() - 1 < programCounter) return false;
        if (instructions.get(programCounter) == null) return false;
        if (instructions.get(programCounter).equals(RESERVED)) return false;
        return true;
    }

    /**
     *  Like {@link #addStatement(SSAInstruction)} but may replace an existing one.
     *
     *  @param  statement    The statement to add without care of overwriting
     *  @return true if a statement has actually been overwritten 
     *  @throws IllegalStateException if you may not write to the ProgramCounter due to
     *      the setting of {@link #allowReserved(boolean)} or {@link #getMethodSummary()} has
     *      been called and thus this summary got locked.
     *  @throws NullPointerException if statement is null
     *  @throws IllegalArgumentException if the statement has set an invalid ProgramCounter
     */
    public boolean overwriteStatement(SSAInstruction statement) {
        if (this.locked) {
            throw new IllegalStateException("Summary locked due to call to getMethodSummary().");
        }
        if (statement == null) {
            throw new NullPointerException("Statement is null!");
        }
        if (statement.iindex < 0) {
            throw new IllegalArgumentException("Statement has a negative iindex");
        }
        if ((!this.allowReservedPC) && isReserved(statement.iindex)) {
            throw new IllegalStateException("ProgramCounter " + statement.iindex + " is reserved! Use allowReserved(true).");
        }
        if (statement.iindex > this.currentProgramCounter) {
            throw new IllegalArgumentException("IIndex " + statement.iindex + " is greater than currentProgramCounter. Use getNextProgramCounter.");
        }

        boolean didOverwrite = isUsed(statement.iindex);
        while (this.instructions.size() - 1 < statement.iindex) this.instructions.add(null);
        if (DEBUG) { System.err.printf("Setting {} to {}", statement.iindex, statement); }
        this.instructions.set(statement.iindex, statement);
        return didOverwrite;
    }

    /**
     *  Generates the MethodSummary and locks class.
     *
     *  @throws IllegalStateException if you altered the referenced (by constructor) summary
     *  @return the finished MethodSummary
     */
    public MethodSummary getMethodSummary() {
        if (locked) {
            // Already generated
            return this.summary;
        }
        if (summary.getNumberOfStatements() > 0) {
            throw new IllegalStateException("Meanwhile Statements have been added to the summary given " +
                    "to the constructor. This behavior is not supported!");
        }
        this.locked = true;
        for (int i = 0; i < this.instructions.size(); ++i) {
            final SSAInstruction inst = this.instructions.get(i);
            if (inst == null) {
              if (DEBUG) { System.err.printf("No instruction at iindex {}", i); }
              this.summary.addStatement(null);
            } else if (inst == RESERVED) {
              // replace reserved slots by 'goto next' statements
              this.summary.addStatement(new SSAGotoInstruction(i, i+1));
            } else {
              if (DEBUG) { System.err.printf("Adding @{}: ", inst); }
              this.summary.addStatement(inst);
            }
        }
        
        // Let the GC free instructions..
        this.instructions = null;

        return this.summary;
    }
    // /**
    // *  Re-enable write access to VolatileMethodSummary (CAUTION...).
    // *
    // *  On a call to {@link #getMethodSummary()} the AndroidModelMethodSummary gets locked
    // *  to prevent unintended behaviour.
    // *
    // *  Through the call of this function you gain back write access. However you should
    // *  know what you are doing as the "exported" MethodSummary will not get updated. A
    // *  AndroidModelMethodSummary of course starts in unlocked state.
    // */
    //public void unlock() {
    //    this.locked = false;
    //}

    //
    // Now for the stuff you should be familiar with from MethodSummary, but with a view
    // more checks
    //

    /**
     *  Adds a statement to the MethodSummary.
     *
     *  @param  statement    The statement to be added
     *  @throws IllegalStateException if you may not write to the ProgramCounter due to
     *      the setting of {@link #allowReserved(boolean)} or {@link #getMethodSummary()} has
     *      been called and thus this summary got locked.
     *  @throws NullPointerException if statement is null
     *  @throws IllegalArgumentException if the statement has set an invalid ProgramCounter or
     *      if there is already a statement at the statements iindex. In this case you can use 
     *      {@link #overwriteStatement(SSAInstruction)}.
     */
    public void addStatement(SSAInstruction statement) {
        if (isUsed(statement.iindex)) {
            throw new IllegalArgumentException("ProgramCounter " + statement.iindex + " is in use! By " +
                    getStatementAt(statement.iindex) + " Use overwriteStatement().");
        }
     
        overwriteStatement(statement);
    }

    /**
     *  Optionally add a name for a local variable.
     */
    public void setLocalName(final int number, final String name) {
        localNames.put(number, Atom.findOrCreateAsciiAtom(name));
    }

    /**
     *  Set localNames merges with existing names.
     *
     *  If a key in merge exists the value is overwritten if not the value is 
     *  kept (it's a putAll on the internal map).
     */
    public void setLocalNames(Map<Integer, Atom> merge) {
        localNames.putAll(merge);
    }

    /**
     *  A mapping from SSA-Values to Variable-names.
     */
    public Map<Integer, Atom> getLocalNames() {
        return localNames;
    }

    /**
     *  Assigns a new Constant to a SSA-Value.
     *
     *  @throws IllegalStateException if you redefine a constant or use the number of an existent 
     *      SSA-Variable
     *  @throws IllegalArgumentException if value is null or negative
     */
    public void addConstant(java.lang.Integer vn, ConstantValue value) {

        if ((summary.getConstants() != null) && (summary.getConstants().containsKey(vn))) {
            throw new IllegalStateException("You redefined a constant at number " + vn);
        }
        if (vn <= 0) {
            throw new IllegalArgumentException("SSA-Value may not be zero or negative.");
        }
        this.summary.addConstant(vn, value);
    }

   /**
    *   Adds posion to the function.
    *
    *   This call gets passed directly to the internal MethodSummary.
    */
    public void addPoison(java.lang.String reason) {
        this.summary.addPoison(reason);
    }

    /**
    *   Retrieves a mapping from SSA-Number to a constant.
    *
    *   You can add Constants using the function {@link #addConstant(java.lang.Integer, ConstantValue)}.
    *   A call to this function gets passed directly to the internal MethodSummary.
    *
    *   @return a mapping from SSA-Number to assigned ConstantValue
    */
    public java.util.Map<java.lang.Integer,ConstantValue> getConstants() {
        return this.summary.getConstants();
    }

    /**
     *  Retrieve the Method this Summary implements.
     *
     *  You'll get a MemberReference which contains the declaring class (which should be the 
     *  FakeRootClass in most cases) and the signature of the method.
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return the implemented method as stated above
     */
    public MemberReference getMethod() {
        return this.summary.getMethod();
    }

    /**
     *  Gets you a non-reserved ProgramCounter you can write to.
     *
     *  This function returns the next ProgramCounter for which not({@link #isUsed(int)})
     *  holds. Thus it will _not_ give you a ProgramCounter which is reserved even if you enabled
     *  writing to reserved ProgramCounters using {@link #allowReserved(boolean)}! You'll have to
     *  keep track of them on your own.
     *
     * @return A non-reserved writable ProgramCounter
     */
    public int getNextProgramCounter() {
      while (isUsed(this.currentProgramCounter) || isReserved(this.currentProgramCounter)) {
        this.currentProgramCounter++;
      }
      while (this.instructions.size() < this.currentProgramCounter) this.instructions.add(null);
      return this.currentProgramCounter;
    }

    /**
     *  Get the count of parameters of the Method this Summary implements.
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return Number of parameters
     */
    public int  getNumberOfParameters() {
        return this.summary.getNumberOfParameters();
    }

    /**
     *  Gets you the TypeReference of a parameter.
     *
     *  This call gets passed directly to the internal MethodSummary after some checks.
     *
     *  @return the TypeReference of the i-th parameter.
     *  @throws IllegalArgumentException if the parameter is zero or negative
     *  @throws ArrayIndexOutOfBoundsException if the parameter is to large
     */
    public TypeReference getParameterType(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("The parater number may not be zero or negative! " + i + " given");
        }
        if (i >= this.summary.getNumberOfParameters() ) {
            throw new ArrayIndexOutOfBoundsException ("No such parameter index: " + i);
        }

        return this.summary.getParameterType(i);
    }

    /**
     *  Retrieves the poison set using {@link #addPoison(java.lang.String)}
     *
     *  @return The poison-String
     */
    public java.lang.String getPoison() {
        return this.summary.getPoison();
    }

    /**
     *  Retrieves the value of Poison-Level.
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return the poison level
     */
    public byte getPoisonLevel() {
        return this.summary.getPoisonLevel();
    }

    /**
     *  Retrieves the return-type of the Function whose body this Summary implements.
     *
     *  This call gets passed directly to the internal MethodSummary.
     */
    public TypeReference getReturnType() {
        return this.summary.getReturnType();
    }

    /**
     *  Get all statements added to the Summary.
     *
     *  This builds a copy of the internal list and may contain 'null'-values if no
     *  instruction has been placed at a particular pc.
     *
     *  @return The statements of the summary
     */
    public SSAInstruction[] getStatements() {
        SSAInstruction[] ret = new SSAInstruction[this.instructions.size()];
        ret = this.instructions.toArray(ret);
        
        // Remove Reserved
        for (int i=0; i<ret.length; ++i) {
            if (ret[i].equals(RESERVED)) {
                ret[i] = null;
            }
        }
        return ret;
    }   

    /**
     *  Returns if Poison has been added using {@link #addPoison(java.lang.String)}.
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return true if poison has been added
     */
    public boolean hasPoison() {
        return this.summary.hasPoison();
    }

    /**
     *  Returns if the implemented method is a factory.
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return true if it's a factory
     */
    public boolean isFactory() {
        return this.summary.isFactory();
    }

    /**
     *  Return if the implemented method is a native one (which it shouldn't be).
     *
     *  This call gets passed directly to the internal MethodSummary.
     *
     *  @return almost always false
     */
    public boolean  isNative() {
        return this.summary.isNative();
    }

    /**
     *  Return if the implemented method is static.
     *
     *  A static method may not access non-static (and thus instance-specific) content.
     *
     *  @return true if the method is static.
     */
    public boolean isStatic() {
        return this.summary.isStatic();
    }

    /**
     *  Set the value returned by {@link #isFactory()}
     *
     *  @throws IllegalStateException if summary was locked
     */
    public void setFactory(boolean b) {
        if (this.locked) {
            throw new IllegalStateException("Summary is locked. Unlock using unlock()");
        }
        this.summary.setFactory(b);
    }

    /**
     *  Set the value returned by {@link #getPoisonLevel()}
     *
     *  @throws IllegalStateException if summary was locked
     */
    public void setPoisonLevel(byte b) {
        if (this.locked) {
            throw new IllegalStateException("Summary is locked. Unlock using unlock()");
        }
        this.summary.setPoisonLevel(b);
    }

    /**
     *  Set the value returned by {@link #isStatic()}
     *
     *  @throws IllegalStateException if summary was locked
     */
    public void setStatic(boolean b) {
        if (this.locked) {
            throw new IllegalStateException("Summary is locked. Unlock using unlock()");
        }
        this.summary.setStatic(b);
    }

    /**
     *  Generates a String-Representation of an instance of the class.
     */
    @Override
    public java.lang.String toString() {
        return "VolatileMethodSummary of " + this.summary.toString();
    }
}
