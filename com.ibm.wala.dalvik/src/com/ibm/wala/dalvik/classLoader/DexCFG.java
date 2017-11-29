/*******************************************************************************
 * Copyright (c) 2002 - 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Steve Suh <suhsteve@gmail.com> - Modified to handle dalvik instructions
 *******************************************************************************/

package com.ibm.wala.dalvik.classLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.BytecodeCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.BytecodeLanguage;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.dalvik.dex.instructions.Instruction;
import com.ibm.wala.dalvik.dex.instructions.Invoke;
import com.ibm.wala.dalvik.dex.instructions.Return;
import com.ibm.wala.dalvik.dex.instructions.Throw;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.NodeWithNumber;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

public class DexCFG extends AbstractCFG<Instruction, DexCFG.BasicBlock> implements BytecodeCFG {
    private static final boolean DEBUG = false;

    private int[] instruction2Block;

    private final DexIMethod dexMethod;
    private final Context context;

    private static int totalEdges = 0;

    /**
     * Cache this here for efficiency
     */
    private final int hashBase;

    /**
     * Set of Shrike {@link ExceptionHandler} objects that cover this method.
     */
    final private Set<ExceptionHandler> exceptionHandlers = HashSetFactory.make(10);

    protected DexCFG(DexIMethod method, Context context) throws IllegalArgumentException {
        super(method);
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }
        this.dexMethod = method;
        this.context = context;
        this.hashBase = method.hashCode() * 9967;
        makeBasicBlocks();
        init();
        computeI2BMapping();
        computeEdges();
    }

    public DexIMethod getDexMethod() {
        return dexMethod;
    }

    public static int getTotalEdges() {
        return totalEdges;
    }

    @Override
    public int hashCode() {
        return 9511 * getMethod().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        //return (o instanceof DexCFG) && getMethod().equals(((DexCFG) o).getMethod());
        return o instanceof DexCFG && ((DexCFG)o).dexMethod.equals(dexMethod) && ((DexCFG)o).context.equals(context);

    }

    @Override
    public Instruction[] getInstructions() {
        return dexMethod.getDexInstructions();

    }

    /**
     * Compute a mapping from instruction to basic block. Also, compute the blocks that end with a 'normal' return.
     */
    private void computeI2BMapping() {
        instruction2Block = new int[getInstructions().length];
        for (BasicBlock b : this) {
            for (int j = b.getFirstInstructionIndex(); j <= b.getLastInstructionIndex(); j++) {
                instruction2Block[j] = getNumber(b);
            }
        }
    }

    /**
     * Compute outgoing edges in the control flow graph.
     */
    private void computeEdges() {
        for (BasicBlock b : this) {
            if (b.equals(exit())) {
                continue;
            } else if (b.equals(entry())) {
                BasicBlock bb0 = getBlockForInstruction(0);
                assert bb0 != null;
                addNormalEdge(b, bb0);
            } else {
                b.computeOutgoingEdges();
            }
        }
    }

    private void makeBasicBlocks() {
        ExceptionHandler[][] handlers;
        handlers = dexMethod.getHandlers();
        boolean[] r = new boolean[getInstructions().length];
        boolean[] catchers = new boolean[getInstructions().length];
        // we initially start with both the entry and exit block.
        @SuppressWarnings("unused")
		int blockCount = 2;

        // Compute r so r[i] == true iff instruction i begins a basic block.
        // While doing so count the number of blocks.
        r[0] = true;
        Instruction[] instructions = getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            int[] targets = instructions[i].getBranchTargets();

            // if there are any targets, then break the basic block here.
            // also break the basic block after a return
            if (targets.length > 0 || !instructions[i].isFallThrough()) {
                if (i + 1 < instructions.length && !r[i + 1]) {
                    r[i + 1] = true;
                    blockCount++;
                }
            }

            for (int j = 0; j < targets.length; j++) {
                if (!r[targets[j]]) {
                    r[targets[j]] = true;
                    blockCount++;
                }
            }
            if (instructions[i].isPEI()) {
                ExceptionHandler[] hs = handlers[i];
                // break the basic block here.
                if (i + 1 < instructions.length && !r[i + 1]) {
                    r[i + 1] = true;
                    blockCount++;
                }
                if (hs != null && hs.length > 0) {
                    for (int j = 0; j < hs.length; j++) {
                        exceptionHandlers.add(hs[j]);
                        if (!r[hs[j].getHandler()]) {
                            // we have not discovered the catch block yet.
                            // form a new basic block
                            r[hs[j].getHandler()] = true;
                            blockCount++;
                        }
                        catchers[hs[j].getHandler()] = true;
                    }
                }
            }
        }

        BasicBlock entry = new BasicBlock(-1);
        addNode(entry);

        int j = 1;
        for (int i = 0; i < r.length; i++) {
            if (r[i]) {
                BasicBlock b = new BasicBlock(i);
                addNode(b);
                if (catchers[i]) {
                    setCatchBlock(j);
                }
                j++;
            }
        }

        BasicBlock exit = new BasicBlock(-1);
        addNode(exit);
    }

    /**
     * Return an instruction's basic block in the CFG given the index of the instruction in the CFG's instruction array.
     */
    @Override
    public BasicBlock getBlockForInstruction(int index) {
        return getNode(instruction2Block[index]);
    }

    public final class BasicBlock extends NodeWithNumber implements IBasicBlock<Instruction> {

        /**
         * The number of the ShrikeBT instruction that begins this block.
         */
        final private int startIndex;

        public BasicBlock(int startIndex) {
            this.startIndex = startIndex;
        }

        @Override
        public boolean isCatchBlock() {
            return DexCFG.this.isCatchBlock(getNumber());
        }

        private void computeOutgoingEdges() {
            if (DEBUG) {
                System.err.println("Block " + this + ": computeOutgoingEdges()");
            }

            Instruction last = getInstructions()[getLastInstructionIndex()];
            int[] targets = last.getBranchTargets();
            for (int target : targets) {
                BasicBlock b = getBlockForInstruction(target);
                addNormalEdgeTo(b);
            }
            addExceptionalEdges(last);
            if (last.isFallThrough()) {
                BasicBlock next = getNode(getNumber() + 1);
                addNormalEdgeTo(next);
            }
            if (last instanceof Return) {
                // link each return instruction to the exit block.
                BasicBlock exit = exit();
                addNormalEdgeTo(exit);
            }
        }

        /**
         * Add any exceptional edges generated by the last instruction in a basic block.
         *
         * @param last the last instruction in a basic block.
         */
        protected void addExceptionalEdges(Instruction last) {
            IClassHierarchy cha = getMethod().getClassHierarchy();
            if (last.isPEI()) {
                Collection<TypeReference> exceptionTypes = null;
                boolean goToAllHandlers = false;

                ExceptionHandler[] hs = getExceptionHandlers();
                if (last instanceof Throw) {
                    // this class does not have the type information needed
                    // to determine what the athrow throws. So, add an
                    // edge to all reachable handlers. Better information can
                    // be obtained later with SSA type propagation.
                    // TODO: consider pruning to only the exception types that
                    // this method either catches or allocates, since these are
                    // the only types that can flow to an athrow.
                    goToAllHandlers = true;
                } else {
                    if (hs != null && hs.length > 0) {
                        IClassLoader loader = getMethod().getDeclaringClass().getClassLoader();
                        BytecodeLanguage l = (BytecodeLanguage) loader.getLanguage();
                        //exceptionTypes = l.getImplicitExceptionTypes(last);
                        exceptionTypes = getImplicitExceptionTypes(last);
                        if (last instanceof Invoke) {
                            Invoke call = (Invoke) last;
                            exceptionTypes = HashSetFactory.make(exceptionTypes);
                            MethodReference target = MethodReference.findOrCreate(l, loader.getReference(), call.clazzName, call
                                    .methodName, call.descriptor);
                            try {
                                exceptionTypes.addAll(l.inferInvokeExceptions(target, cha));
                            } catch (InvalidClassFileException e) {
                                e.printStackTrace();
                                Assertions.UNREACHABLE();
                            }
                            IMethod mTarget = cha.resolveMethod(target);
                            if (mTarget == null) {
                              goToAllHandlers = true;
                            }
                        }
                    }
                }

                if (hs != null && hs.length > 0) {
                    // found a handler for this PEI

                    // create a mutable copy
                    if (!goToAllHandlers) {
                        exceptionTypes = HashSetFactory.make(exceptionTypes);
                    }

                    for (ExceptionHandler element : hs) {
                        if (DEBUG) {
                            System.err.println(" handler " + element);
                        }
                        BasicBlock b = getBlockForInstruction(element.getHandler());
                        if (DEBUG) {
                            System.err.println(" target " + b);
                        }
                        if (goToAllHandlers) {
                            // add an edge to the catch block.
                            if (DEBUG) {
                                System.err.println(" gotoAllHandlers " + b);
                            }
                            addExceptionalEdgeTo(b);
                        } else {
                            TypeReference caughtException = null;
                            if (element.getCatchClass() != null) {
                                ClassLoaderReference loader = DexCFG.this.getMethod().getDeclaringClass().getReference().getClassLoader();
                                caughtException = ShrikeUtil.makeTypeReference(loader, element.getCatchClass());
                                if (DEBUG) {
                                    System.err.println(" caughtException " + caughtException);
                                }
                                IClass caughtClass = cha.lookupClass(caughtException);
                                if (caughtClass == null) {
                                    // conservatively add the edge, and raise a warning
                                    addExceptionalEdgeTo(b);
                                    Warnings.add(FailedExceptionResolutionWarning.create(caughtException));
                                    // null out caughtException, to avoid attempting to process it
                                    caughtException = null;
                                }
                            } else {
                                if (DEBUG) {
                                    System.err.println(" catchClass() == null");
                                }
                                // hs[j].getCatchClass() == null.
                                // this means that the handler catches all exceptions.
                                // add the edge and null out all types
                                if (!exceptionTypes.isEmpty()) {
                                    addExceptionalEdgeTo(b);
                                    exceptionTypes.clear();
                                    assert caughtException == null;
                                }
                            }
                            if (caughtException != null) {
                                IClass caughtClass = cha.lookupClass(caughtException);
                                // the set "caught" should be the set of exceptions that MUST
                                // have been caught by the handlers in scope
                                ArrayList<TypeReference> caught = new ArrayList<>(exceptionTypes.size());
                                // check if we should add an edge to the catch block.
                                for (TypeReference t : exceptionTypes) {
                                    if (t != null) {
                                        IClass klass = cha.lookupClass(t);
                                        if (klass == null) {
                                            Warnings.add(FailedExceptionResolutionWarning.create(caughtException));
                                            // conservatively add an edge
                                            addExceptionalEdgeTo(b);
                                        } else {
                                            boolean subtype1 = cha.isSubclassOf(klass, caughtClass);
                                            if (subtype1 || cha.isSubclassOf(caughtClass, klass)) {
                                                // add the edge and null out the type from the array
                                                addExceptionalEdgeTo(b);
                                                if (subtype1) {
                                                    caught.add(t);
                                                }
                                            }
                                        }
                                    }
                                }
                                exceptionTypes.removeAll(caught);
                            }
                        }
                    }
                    // if needed, add an edge to the exit block.
                    if (exceptionTypes == null || !exceptionTypes.isEmpty()) {
                        BasicBlock exit = exit();
                        addExceptionalEdgeTo(exit);
                    }
                } else {
                    // found no handler for this PEI ... link to the exit block.
                    BasicBlock exit = exit();
                    addExceptionalEdgeTo(exit);
                }
            }
        }

        /**
         * @param pei a potentially-excepting instruction
         * @return the exception types that pei may throw, independent of the class hierarchy. null if none.
         *
         *         Notes
         *         <ul>
         *         <li>this method will <em>NOT</em> return the exception type explicitly thrown by an athrow
         *         <li>this method will <em>NOT</em> return the exception types that a called method may throw
         *         <li>this method ignores OutOfMemoryError
         *         <li>this method ignores linkage errors
         *         <li>this method ignores IllegalMonitorState exceptions
         *         </ul>
         *
         * @throws IllegalArgumentException if pei is null
         */
        public Collection<TypeReference> getImplicitExceptionTypes(Instruction pei) {
            if (pei == null) {
                throw new IllegalArgumentException("pei is null");
            }
            switch (pei.getOpcode()) {
            //TODO: Make sure all the important cases and exceptions are covered.
            case AGET:
            case AGET_WIDE:
            case AGET_OBJECT:
            case AGET_BOOLEAN:
            case AGET_BYTE:
            case AGET_CHAR:
            case AGET_SHORT:
                //      case OP_iaload:
                //      case OP_laload:
                //      case OP_faload:
                //      case OP_daload:
                //      case OP_aaload:
                //      case OP_baload:
                //      case OP_caload:
                //      case OP_saload:
            case APUT:
            case APUT_WIDE:
                //      case APUT_OBJECT:
            case APUT_BOOLEAN:
            case APUT_BYTE:
            case APUT_CHAR:
            case APUT_SHORT:
                //      case OP_iastore:
                //      case OP_lastore:
                //      case OP_fastore:
                //      case OP_dastore:
                //      case OP_bastore:
                //      case OP_castore:
                //      case OP_sastore:
                return JavaLanguage.getArrayAccessExceptions();
            case APUT_OBJECT:
                //case OP_aastore:
                return JavaLanguage.getAaStoreExceptions();
            case IGET:
            case IGET_WIDE:
            case IGET_OBJECT:
            case IGET_BOOLEAN:
            case IGET_BYTE:
            case IGET_CHAR:
            case IGET_SHORT:
                //      case OP_getfield:
            case IPUT:
            case IPUT_WIDE:
            case IPUT_OBJECT:
            case IPUT_BOOLEAN:
            case IPUT_BYTE:
            case IPUT_CHAR:
            case IPUT_SHORT:
                //      case OP_putfield:

                //Shrike imp does not include the static invoke calls, so likewise will do the same
            case INVOKE_VIRTUAL:
            case INVOKE_SUPER:
            case INVOKE_DIRECT:
                //case INVOKE_STATIC:
            case INVOKE_INTERFACE:
            case INVOKE_VIRTUAL_RANGE:
            case INVOKE_SUPER_RANGE:
            case INVOKE_DIRECT_RANGE:
                //case INVOKE_STATIC_RANGE:
            case INVOKE_INTERFACE_RANGE:
                //      case OP_invokevirtual:
                //      case OP_invokespecial:
                //      case OP_invokeinterface:
                 return JavaLanguage.getNullPointerException();
            case DIV_INT:
            case DIV_INT_2ADDR:
            case DIV_INT_LIT16:
            case DIV_INT_LIT8:
            case REM_INT:
            case REM_INT_2ADDR:
            case REM_INT_LIT16:
            case REM_INT_LIT8:
            case DIV_LONG:
            case DIV_LONG_2ADDR:
            case REM_LONG:
            case REM_LONG_2ADDR:
                //      case OP_idiv:
                //      case OP_irem:
                //      case OP_ldiv:
                //      case OP_lrem:
                return JavaLanguage.getArithmeticException();
            case NEW_INSTANCE:
                //case OP_new:
                return JavaLanguage.getNewScalarExceptions();
            case NEW_ARRAY:
            case FILLED_NEW_ARRAY:
            case FILLED_NEW_ARRAY_RANGE:
                //      case OP_newarray:
                //      case OP_anewarray:
                //      case OP_multianewarray:
                return JavaLanguage.getNewArrayExceptions();
            case ARRAY_LENGTH:
                //      case OP_arraylength:
                return JavaLanguage.getNullPointerException();
            case THROW:
                //      case OP_athrow:
                // N.B: the caller must handle the explicitly-thrown exception
                return JavaLanguage.getNullPointerException();
            case CHECK_CAST:
                //      case OP_checkcast:
                return JavaLanguage.getClassCastException();
            case MONITOR_ENTER:
            case MONITOR_EXIT:
                //      case OP_monitorenter:
                //      case OP_monitorexit:
                // we're currently ignoring MonitorStateExceptions, since J2EE stuff
                // should be
                // logically single-threaded
                return JavaLanguage.getNullPointerException();

                //I Don't think dalvik has to worry about this?
                //      case OP_ldc_w:
                //        if (((ConstantInstruction) pei).getType().equals(TYPE_Class))
                //          return JavaLanguage.getClassNotFoundException();
                //        else
                //          return null;

            case SGET:
            case SGET_BOOLEAN:
            case SGET_BYTE:
            case SGET_CHAR:
            case SGET_OBJECT:
            case SGET_SHORT:
            case SGET_WIDE:
            case SPUT:
            case SPUT_BOOLEAN:
            case SPUT_BYTE:
            case SPUT_CHAR:
            case SPUT_OBJECT:
            case SPUT_SHORT:
            case SPUT_WIDE:
                //      case OP_getstatic:
                //      case OP_putstatic:
                return JavaLanguage.getExceptionInInitializerError();
            default:
                return Collections.emptySet();
            }
        }

        private ExceptionHandler[] getExceptionHandlers() {
            ExceptionHandler[][] handlers;
            handlers = dexMethod.getHandlers();
            ExceptionHandler[] hs = handlers[getLastInstructionIndex()];
            return hs;
        }

        private void addNormalEdgeTo(BasicBlock b) {
            totalEdges++;
            addNormalEdge(this, b);
        }

        private void addExceptionalEdgeTo(BasicBlock b) {
            totalEdges++;
            addExceptionalEdge(this, b);
        }

        @Override
        public int getLastInstructionIndex() {
            if (this == entry() || this == exit()) {
                // these are the special end blocks
                return -2;
            }
            if (getNumber() == (getMaxNumber() - 1)) {
                // this is the last non-exit block
                return getInstructions().length - 1;
            } else {
                BasicBlock next = getNode(getNumber() + 1);
                return next.getFirstInstructionIndex() - 1;
            }
        }

        @Override
        public int getFirstInstructionIndex() {
            return startIndex;
        }

        @Override
        public String toString() {
            return "BB[Dex]" + getNumber() + " - " + dexMethod.getDeclaringClass().getReference().getName() + "." + dexMethod.getName();
        }

        /*
         * @see com.ibm.wala.cfg.BasicBlock#isExitBlock()
         */
        @Override
        public boolean isExitBlock() {
            return this == DexCFG.this.exit();
        }

        /*
         * @see com.ibm.wala.cfg.BasicBlock#isEntryBlock()
         */
        @Override
        public boolean isEntryBlock() {
            return this == DexCFG.this.entry();
        }

        /*
         * @see com.ibm.wala.cfg.BasicBlock#getMethod()
         */
        @Override
        public IMethod getMethod() {
            return DexCFG.this.getMethod();
        }

        @Override
        public int hashCode() {
            return hashBase + getNumber();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof BasicBlock) && ((BasicBlock) o).getMethod().equals(getMethod())
                    && ((BasicBlock) o).getNumber() == getNumber();
        }

        /*
         * @see com.ibm.wala.cfg.BasicBlock#getNumber()
         */
        @Override
        public int getNumber() {
            return getGraphNodeId();
        }

        @Override
        public Iterator<Instruction> iterator() {
            return new ArrayIterator<>(getInstructions(), getFirstInstructionIndex(), getLastInstructionIndex());
        }
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("");
        BitVector catches = this.getCatchBlocks();
        for (BasicBlock bb : this) {
            s.append("BB").append(getNumber(bb));
            if (catches.contains(bb.getNumber())) {
            	s.append("<Handler>");
            }
            s.append("\n");
            for (int j = bb.getFirstInstructionIndex(); j <= bb.getLastInstructionIndex(); j++) {
                s.append("  ").append(j).append("  ").append(getInstructions()[j]).append("\n");
            }

            Iterator<BasicBlock> succNodes = getSuccNodes(bb);
            while (succNodes.hasNext()) {
                s.append("    -> BB").append(getNumber(succNodes.next())).append("\n");
            }
        }
        return s.toString();
    }

    public int getMaxStackHeight() {
        return dexMethod.getMaxStackHeight();
    }

    public int getMaxLocals() {
        return dexMethod.getMaxLocals();
    }

    @Override
    public Set<ExceptionHandler> getExceptionHandlers() {
        return exceptionHandlers;
    }

    /*
     * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
     */
    @Override
    public int getProgramCounter(int index) {
        return dexMethod.getAddressFromIndex(index);
        //    return dexMethod.getInstructionFromIndex(index).pc;
    }

    /**
     * A warning when we fail to resolve the type of an exception
     */
    private static class FailedExceptionResolutionWarning extends Warning {

        final TypeReference T;

        FailedExceptionResolutionWarning(TypeReference T) {
            super(Warning.MODERATE);
            this.T = T;
        }

        @Override
        public String getMsg() {
            return getClass().toString() + " : " + T;
        }

        public static FailedExceptionResolutionWarning create(TypeReference T) {
            return new FailedExceptionResolutionWarning(T);
        }
    }
}

