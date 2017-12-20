/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.BytecodeCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.cfg.MinimalCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.shrike.ShrikeUtil;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * A control-flow graph for ssa form.
 * 
 * This implementation is uglified in the name of performance. This implementation does not directly track the graph structure, but
 * instead delegates to a prebuilt {@link ControlFlowGraph} which stores the structure. This decision from 2004 may have been
 * premature optimization, left over from a world where {@link IR}s and related structures were long-lived. In today's system, they
 * are cached and reconstituted by {@link SSACache}. Perhaps we should just extend {@link AbstractCFG} and not worry so much about
 * space.
 * 
 * As the current implementation stands, the delegate graph stores the graph structure, and this class additionally stores
 * {@link BasicBlock}s and the {@link SSAInstruction} array.
 */
public class SSACFG implements ControlFlowGraph<SSAInstruction, ISSABasicBlock>, MinimalCFG<ISSABasicBlock> {

  private static final boolean DEBUG = false;

  /**
   * The {@link ISSABasicBlock}s which live in this graph. These {@link BasicBlock}s must have the same numbers as the corresponding
   * {@link IBasicBlock}s in the delegate {@link AbstractCFG}. This array is additionally co-indexed by these numbers.
   */
  private BasicBlock[] basicBlocks;

  /**
   * The "normal" instructions which constitute the SSA form. This does not include {@link SSAPhiInstruction}s, which dwell in
   * {@link BasicBlock}s instead.
   */
  final protected SSAInstruction[] instructions;

  /**
   * The {@link IMethod} this {@link ControlFlowGraph} represents
   */
  final protected IMethod method;

  /**
   * A delegate CFG, pre-built, which stores the graph structure of this CFG.
   */
  final protected AbstractCFG<IInstruction, IBasicBlock<IInstruction>> delegate;

  /**
   * cache a ref to the exit block for efficient access
   */
  private BasicBlock exit;

  /**
   * @throws IllegalArgumentException if method is null
   */
  @SuppressWarnings("unchecked")
  public SSACFG(IMethod method, AbstractCFG cfg, SSAInstruction[] instructions) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    this.delegate = cfg;
    if (DEBUG) {
      System.err.println(("Incoming CFG for " + method + ":"));
      System.err.println(cfg.toString());
    }

    this.method = method;
    assert method.getDeclaringClass() != null : "null declaring class for " + method;
    createBasicBlocks(cfg);
    if (cfg instanceof InducedCFG) {
      addPhisFromInducedCFG((InducedCFG) cfg);
      addPisFromInducedCFG((InducedCFG) cfg);
    }
    if (cfg instanceof BytecodeCFG) {
      recordExceptionTypes(((BytecodeCFG) cfg).getExceptionHandlers(), method.getDeclaringClass().getClassLoader());
    }
    this.instructions = instructions;

  }

  /**
   * This is ugly. Clean it up someday. {@link InducedCFG}s carry around pii instructions. add these pii instructions to the
   * SSABasicBlocks
   */
  private void addPisFromInducedCFG(InducedCFG cfg) {
    for (com.ibm.wala.cfg.InducedCFG.BasicBlock ib : cfg) {
      // we rely on the invariant that basic blocks in this cfg are numbered identically as in the source
      // InducedCFG
      BasicBlock b = getBasicBlock(ib.getNumber());
      for (SSAPiInstruction pi : ib.getPis()) {
        BasicBlock path = getBasicBlock(pi.getSuccessor());
        b.addPiForRefAndPath(pi.getVal(), path, pi);
      }
    }
  }

  /**
   * This is ugly. Clean it up someday. {@link InducedCFG}s carry around phi instructions. add these phi instructions to the
   * SSABasicBlocks
   */
  private void addPhisFromInducedCFG(InducedCFG cfg) {
    for (com.ibm.wala.cfg.InducedCFG.BasicBlock ib : cfg) {
      // we rely on the invariant that basic blocks in this cfg are numbered identically as in the source
      // InducedCFG
      BasicBlock b = getBasicBlock(ib.getNumber());
      // this is really ugly. we pretend that each successively phi in the basic block defs a
      // particular 'local'. TODO: fix the API in some way so that this is unnecessary.
      // {What does Julian do for, say, Javascript?}
      int local = 0;
      for (SSAPhiInstruction phi : ib.getPhis()) {
        b.addPhiForLocal(local++, phi);
      }
    }

  }

  @Override
  public int hashCode() {
    return -3 * delegate.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof SSACFG) && delegate.equals(((SSACFG) o).delegate);
  }

  private void recordExceptionTypes(Set<ExceptionHandler> set, IClassLoader loader) {
    for (ExceptionHandler handler : set) {
      TypeReference t = null;
      if (handler.getCatchClass() == null) {
        // by convention, in ShrikeCT this means catch everything
        t = TypeReference.JavaLangThrowable;
      } else {
        TypeReference exceptionType = ShrikeUtil.makeTypeReference(loader.getReference(), handler.getCatchClass());
        IClass klass = null;
        klass = loader.lookupClass(exceptionType.getName());
        if (klass == null) {
          Warnings.add(ExceptionLoadFailure.create(exceptionType, method));
          t = exceptionType;
        } else {
          t = klass.getReference();
        }
      }
      int instructionIndex = handler.getHandler();
      IBasicBlock b = getBlockForInstruction(instructionIndex);
      if (!(b instanceof ExceptionHandlerBasicBlock)) {
        assert b instanceof ExceptionHandlerBasicBlock : "not exception handler " + b + " index " + instructionIndex;
      }
      ExceptionHandlerBasicBlock bb = (ExceptionHandlerBasicBlock) getBlockForInstruction(instructionIndex);
      bb.addCaughtExceptionType(t);
    }
  }

  private void createBasicBlocks(AbstractCFG G) {
    basicBlocks = new BasicBlock[G.getNumberOfNodes()];
    for (int i = 0; i <= G.getMaxNumber(); i++) {
      if (G.getCatchBlocks().get(i)) {
        basicBlocks[i] = new ExceptionHandlerBasicBlock(i);
      } else {
        basicBlocks[i] = new BasicBlock(i);
      }
    }
    exit = basicBlocks[delegate.getNumber(delegate.exit())];
  }

  /**
   * Get the basic block an instruction belongs to. Note: the instruction2Block array is filled in lazily. During initialization,
   * the mapping is set up only for the first instruction of each basic block.
   */
  @Override
  public SSACFG.BasicBlock getBlockForInstruction(int instructionIndex) {
    IBasicBlock<IInstruction> N = delegate.getBlockForInstruction(instructionIndex);
    int number = delegate.getNumber(N);
    return basicBlocks[number];
  }

  /**
   * NB: Use iterators such as IR.iterateAllInstructions() instead of this method. This will probably be deprecated someday.
   * 
   * Return the instructions. Note that the CFG is created from the Shrike CFG prior to creating the SSA instructions.
   * 
   * @return an array containing the SSA instructions.
   */
  @Override
  public SSAInstruction[] getInstructions() {
    return instructions;
  }

  private final Map<RefPathKey, SSAPiInstruction> piInstructions = HashMapFactory.make(2);

  private static class RefPathKey {
    private final int n;

    private final Object src;

    private final Object path;

    RefPathKey(int n, Object src, Object path) {
      this.n = n;
      this.src = src;
      this.path = path;
    }

    @Override
    public int hashCode() {
      return n * path.hashCode();
    }

    @Override
    public boolean equals(Object x) {
      return (x instanceof RefPathKey) && n == ((RefPathKey) x).n && src == ((RefPathKey) x).src && path == ((RefPathKey) x).path;
    }
  }

  /**
   * A Basic Block in an SSA IR
   */
  public class BasicBlock implements ISSABasicBlock {

    /**
     * state needed for the numbered graph.
     */
    private final int number;

    /**
     * List of PhiInstructions associated with the entry of this block.
     */
    private SSAPhiInstruction stackSlotPhis[];

    private SSAPhiInstruction localPhis[];

    private final static int initialCapacity = 10;

    public BasicBlock(int number) {
      this.number = number;
    }

    @Override
    public int getNumber() {
      return number;
    }

    /**
     * Method getFirstInstructionIndex.
     */
    @Override
    public int getFirstInstructionIndex() {
      IBasicBlock B = delegate.getNode(number);
      return B.getFirstInstructionIndex();
    }

    /**
     * Is this block marked as a catch block?
     */
    @Override
    public boolean isCatchBlock() {
      return delegate.getCatchBlocks().get(getNumber());
    }

    @Override
    public int getLastInstructionIndex() {
      IBasicBlock B = delegate.getNode(number);
      return B.getLastInstructionIndex();
    }

    /*
     * @see com.ibm.wala.ssa.ISSABasicBlock#iteratePhis()
     */
    @Override
    public Iterator<SSAPhiInstruction> iteratePhis() {
      compressPhis();
      if (stackSlotPhis == null) {
        if (localPhis == null) {
          return EmptyIterator.instance();
        } else {
          LinkedList<SSAPhiInstruction> result = new LinkedList<>();
          for (SSAPhiInstruction phi : localPhis) {
            if (phi != null) {
              result.add(phi);
            }
          }
          return result.iterator();
        }
      } else {
        // stackSlotPhis != null
        LinkedList<SSAPhiInstruction> result = new LinkedList<>();
        for (SSAPhiInstruction phi : stackSlotPhis) {
          if (phi != null) {
            result.add(phi);
          }
        }
        if (localPhis == null) {
          return result.iterator();
        } else {
          for (SSAPhiInstruction phi : localPhis) {
            if (phi != null) {
              result.add(phi);
            }
          }
          return result.iterator();
        }
      }
    }

    /**
     * This method is used during SSA construction.
     */
    public SSAPhiInstruction getPhiForStackSlot(int slot) {
      if (stackSlotPhis == null) {
        return null;
      } else {
        if (slot >= stackSlotPhis.length) {
          return null;
        } else {
          return stackSlotPhis[slot];
        }
      }
    }

    /**
     * This method is used during SSA construction.
     */
    public SSAPhiInstruction getPhiForLocal(int n) {
      if (localPhis == null) {
        return null;
      } else {
        if (n >= localPhis.length) {
          return null;
        } else {
          return localPhis[n];
        }
      }
    }

    public void addPhiForStackSlot(int slot, SSAPhiInstruction phi) {
      if (stackSlotPhis == null) {
        stackSlotPhis = new SSAPhiInstruction[initialCapacity];
      }
      if (slot >= stackSlotPhis.length) {
        SSAPhiInstruction[] temp = stackSlotPhis;
        stackSlotPhis = new SSAPhiInstruction[slot * 2];
        System.arraycopy(temp, 0, stackSlotPhis, 0, temp.length);
      }
      stackSlotPhis[slot] = phi;
    }

    public void addPhiForLocal(int n, SSAPhiInstruction phi) {
      if (localPhis == null) {
        localPhis = new SSAPhiInstruction[initialCapacity];
      }
      if (n >= localPhis.length) {
        SSAPhiInstruction[] temp = localPhis;
        localPhis = new SSAPhiInstruction[n * 2];
        System.arraycopy(temp, 0, localPhis, 0, temp.length);
      }
      localPhis[n] = phi;
    }

    /**
     * Remove any phis in the set.
     */
    public void removePhis(Set<SSAPhiInstruction> toRemove) {
      int nRemoved = 0;
      if (stackSlotPhis != null) {
        for (int i = 0; i < stackSlotPhis.length; i++) {
          if (toRemove.contains(stackSlotPhis[i])) {
            stackSlotPhis[i] = null;
            nRemoved++;
          }
        }
      }
      if (nRemoved > 0) {
        int newLength = stackSlotPhis.length - nRemoved;
        if (newLength == 0) {
          stackSlotPhis = null;
        } else {
          SSAPhiInstruction[] old = stackSlotPhis;
          stackSlotPhis = new SSAPhiInstruction[newLength];
          int j = 0;
          for (SSAPhiInstruction element : old) {
            if (element != null) {
              stackSlotPhis[j++] = element;
            }
          }
        }
      }
      nRemoved = 0;
      if (localPhis != null) {
        for (int i = 0; i < localPhis.length; i++) {
          if (toRemove.contains(localPhis[i])) {
            localPhis[i] = null;
            nRemoved++;
          }
        }
      }
      if (nRemoved > 0) {
        int newLength = localPhis.length - nRemoved;
        if (newLength == 0) {
          localPhis = null;
        } else {
          SSAPhiInstruction[] old = localPhis;
          localPhis = new SSAPhiInstruction[newLength];
          int j = 0;
          for (SSAPhiInstruction element : old) {
            if (element != null) {
              localPhis[j++] = element;
            }
          }
        }
      }
    }

    public SSAPiInstruction getPiForRefAndPath(int n, Object path) {
      return piInstructions.get(new RefPathKey(n, this, path));
    }

    private final LinkedList<SSAPiInstruction> blockPiInstructions = new LinkedList<>();

    /**
     * 
     * @param n can be the val in the pi instruction
     * @param path can be the successor block in the pi instruction
     * @param pi
     */
    public void addPiForRefAndPath(int n, Object path, SSAPiInstruction pi) {
      piInstructions.put(new RefPathKey(n, this, path), pi);
      blockPiInstructions.add(pi);
    }

    @Override
    public Iterator<SSAPiInstruction> iteratePis() {
      return blockPiInstructions.iterator();
    }

    public Iterator<SSAInstruction> iterateNormalInstructions() {
      int lookup = getFirstInstructionIndex();
      final int end = getLastInstructionIndex();
      // skip to first non-null instruction
      while (lookup <= end && instructions[lookup] == null) {
        lookup++;
      }
      final int dummy = lookup;
      return new Iterator<SSAInstruction>() {
        private int start = dummy;

        @Override
        public boolean hasNext() {
          return (start <= end);
        }

        @Override
        public SSAInstruction next() {
          SSAInstruction i = instructions[start];
          start++;
          while (start <= end && instructions[start] == null) {
            start++;
          }
          return i;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    /**
     * TODO: make this more efficient if needed
     */
    public List<SSAInstruction> getAllInstructions() {
      compressPhis();

      ArrayList<SSAInstruction> result = new ArrayList<>();
      for (SSAInstruction inst : Iterator2Iterable.make(iteratePhis())) {
        result.add(inst);
      }

      for (int i = getFirstInstructionIndex(); i <= getLastInstructionIndex(); i++) {
        SSAInstruction s = instructions[i];
        if (s != null) {
          result.add(s);
        }
      }

      for (SSAInstruction inst : Iterator2Iterable.make(iteratePis())) {
        result.add(inst);
      }
      return result;
    }

    /**
     * rewrite the phi arrays so they have no null entries.
     */
    private void compressPhis() {
      if (stackSlotPhis != null && stackSlotPhis[stackSlotPhis.length - 1] == null) {
        int size = countNonNull(stackSlotPhis);
        if (size == 0) {
          stackSlotPhis = null;
        } else {
          SSAPhiInstruction[] old = stackSlotPhis;
          stackSlotPhis = new SSAPhiInstruction[size];
          int j = 0;
          for (SSAPhiInstruction element : old) {
            if (element != null) {
              stackSlotPhis[j++] = element;
            }
          }
        }
      }
      if (localPhis != null && localPhis[localPhis.length - 1] == null) {
        int size = countNonNull(localPhis);
        if (size == 0) {
          localPhis = null;
        } else {
          SSAPhiInstruction[] old = localPhis;
          localPhis = new SSAPhiInstruction[size];
          int j = 0;
          for (SSAPhiInstruction element : old) {
            if (element != null) {
              localPhis[j++] = element;
            }
          }
        }
      }
    }

    private int countNonNull(SSAPhiInstruction[] a) {
      int result = 0;
      for (SSAPhiInstruction element : a) {
        if (element != null) {
          result++;
        }
      }
      return result;
    }

    @Override
    public Iterator<SSAInstruction> iterator() {
      return getAllInstructions().iterator();
    }

    /**
     * @return true iff this basic block has at least one phi
     */
    public boolean hasPhi() {
      return stackSlotPhis != null || localPhis != null;
    }

    /*
     * @see com.ibm.wala.util.graph.INodeWithNumber#getGraphNodeId()
     */
    @Override
    public int getGraphNodeId() {
      return number;
    }

    /*
     * @see com.ibm.wala.util.graph.INodeWithNumber#setGraphNodeId(int)
     */
    @Override
    public void setGraphNodeId(int number) {
      // TODO Auto-generated method stub
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "BB[SSA:" + getFirstInstructionIndex() + ".." + getLastInstructionIndex() + "]" + getNumber() + " - "
          + method.getSignature();
    }

    private SSACFG getGraph() {
      return SSACFG.this;
    }

    @Override
    public boolean equals(Object arg0) {
      if (arg0 instanceof BasicBlock) {
        BasicBlock b = (BasicBlock) arg0;
        if (getNumber() == b.getNumber()) {
          if (getMethod().equals(b.getMethod())) {
            return getGraph().equals(b.getGraph());
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    }

    @Override
    public IMethod getMethod() {
      return method;
    }

    @Override
    public int hashCode() {
      return delegate.getNode(getNumber()).hashCode() * 6271;
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
     */
    @Override
    public boolean isExitBlock() {
      return this == SSACFG.this.exit();
    }

    /*
     * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
     */
    @Override
    public boolean isEntryBlock() {
      return this == SSACFG.this.entry();
    }

    @Override
    public SSAInstruction getLastInstruction() {
      return instructions[getLastInstructionIndex()];
    }

    /**
     * The {@link ExceptionHandlerBasicBlock} subclass will override this.
     * 
     * @see com.ibm.wala.ssa.ISSABasicBlock#getCaughtExceptionTypes()
     */
    @Override
    public Iterator<TypeReference> getCaughtExceptionTypes() {
      return EmptyIterator.instance();
    }
  }

  public class ExceptionHandlerBasicBlock extends BasicBlock {

    /**
     * The type of the exception caught by this block.
     */
    private TypeReference[] exceptionTypes;

    private final static int initialCapacity = 3;

    private int nExceptionTypes = 0;

    /**
     * Instruction that defines the exception value this block catches
     */
    private SSAGetCaughtExceptionInstruction catchInstruction;

    public ExceptionHandlerBasicBlock(int number) {
      super(number);
    }

    public SSAGetCaughtExceptionInstruction getCatchInstruction() {
      return catchInstruction;
    }

    public void setCatchInstruction(SSAGetCaughtExceptionInstruction catchInstruction) {
      this.catchInstruction = catchInstruction;
    }

    @Override
    public Iterator<TypeReference> getCaughtExceptionTypes() {
      return new Iterator<TypeReference>() {
        int next = 0;

        @Override
        public boolean hasNext() {
          return next < nExceptionTypes;
        }

        @Override
        public TypeReference next() {
          return exceptionTypes[next++];
        }

        @Override
        public void remove() {
          Assertions.UNREACHABLE();
        }
      };
    }

    @Override
    public String toString() {
      return "BB(Handler)[SSA]" + getNumber() + " - " + method.getSignature();
    }

    public void addCaughtExceptionType(TypeReference exceptionType) {
      if (exceptionTypes == null) {
        exceptionTypes = new TypeReference[initialCapacity];
      }
      nExceptionTypes++;
      if (nExceptionTypes > exceptionTypes.length) {
        TypeReference[] temp = exceptionTypes;
        exceptionTypes = new TypeReference[nExceptionTypes * 2];
        System.arraycopy(temp, 0, exceptionTypes, 0, temp.length);
      }
      exceptionTypes[nExceptionTypes - 1] = exceptionType;
    }

    @Override
    public List<SSAInstruction> getAllInstructions() {
      List<SSAInstruction> result = super.getAllInstructions();
      if (catchInstruction != null) {
        // it's disturbing that catchInstruction can be null here. must be some
        // strange corner case
        // involving dead code. oh well .. keep on chugging until we have hard
        // evidence that this is a bug
        result.add(0, catchInstruction);
      }
      return result;
    }

  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (int i = 0; i <= getNumber(exit()); i++) {
      BasicBlock bb = getNode(i);
      s.append("BB").append(i).append("[").append(bb.getFirstInstructionIndex()).append("..").append(bb.getLastInstructionIndex())
          .append("]\n");

      Iterator<ISSABasicBlock> succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(((BasicBlock) succNodes.next()).getNumber()).append("\n");
      }
    }
    return s.toString();
  }

  @Override
  public BitVector getCatchBlocks() {
    return delegate.getCatchBlocks();
  }

  /**
   * is the given i a catch block?
   * 
   * @return true if catch block, false otherwise
   */
  public boolean isCatchBlock(int i) {
    return delegate.isCatchBlock(i);
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#entry()
   */
  @Override
  public SSACFG.BasicBlock entry() {
    return basicBlocks[0];
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#exit()
   */
  @Override
  public SSACFG.BasicBlock exit() {
    return exit;
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getNumber(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getNumber(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("N == null");
    }
    return b.getNumber();
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getNode(int)
   */
  @Override
  public BasicBlock getNode(int number) {
    return basicBlocks[number];
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getMaxNumber()
   */
  @Override
  public int getMaxNumber() {
    return basicBlocks.length - 1;
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#iterateNodes()
   */
  @Override
  public Iterator<ISSABasicBlock> iterator() {
    ArrayList<ISSABasicBlock> list = new ArrayList<>();
    for (BasicBlock b : basicBlocks) {
      list.add(b);
    }
    return list.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getNumberOfNodes()
   */
  @Override
  public int getNumberOfNodes() {
    return delegate.getNumberOfNodes();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<ISSABasicBlock> getPredNodes(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    final Iterator<IBasicBlock<IInstruction>> i = delegate.getPredNodes(n);
    return new Iterator<ISSABasicBlock>() {
      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public BasicBlock next() {
        IBasicBlock n = i.next();
        int number = n.getNumber();
        return basicBlocks[number];
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getPredNodeCount(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    return delegate.getPredNodeCount(n);
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<ISSABasicBlock> getSuccNodes(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    final Iterator<IBasicBlock<IInstruction>> i = delegate.getSuccNodes(n);
    return new Iterator<ISSABasicBlock>() {
      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public ISSABasicBlock next() {
        IBasicBlock n = i.next();
        int number = n.getNumber();
        return basicBlocks[number];
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getSuccNodeCount(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    return delegate.getSuccNodeCount(n);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#addNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void addNode(ISSABasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addEdge(ISSABasicBlock src, ISSABasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEdge(ISSABasicBlock src, ISSABasicBlock dst) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeAllIncidentEdges(ISSABasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#removeNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNodeAndEdges(ISSABasicBlock N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(ISSABasicBlock n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  @Override
  public int getProgramCounter(int index) {
    // delegate to incoming cfg.
    return delegate.getProgramCounter(index);
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(ISSABasicBlock N) {
    if (N instanceof BasicBlock) {
      return basicBlocks[getNumber(N)] == N;
    } else {
      return false;
    }
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getMethod()
   */
  @Override
  public IMethod getMethod() {
    return method;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  @Override
  public List<ISSABasicBlock> getExceptionalSuccessors(final ISSABasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    final IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    final Collection<IBasicBlock<IInstruction>> ss = delegate.getExceptionalSuccessors(n);
    final List<ISSABasicBlock> c = new ArrayList<>(getSuccNodeCount(b));
    for (final IBasicBlock<IInstruction> s : ss) {
      c.add(basicBlocks[delegate.getNumber(s)]);
    }
    return c;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  @Override
  public Collection<ISSABasicBlock> getExceptionalPredecessors(ISSABasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    Function<IBasicBlock<IInstruction>, ISSABasicBlock> f = object -> basicBlocks[delegate.getNumber(object)];
    return Iterator2Collection.toSet(new MapIterator<>(delegate
        .getExceptionalPredecessors(n).iterator(), f));
  }

  private IBasicBlock<IInstruction> getUnderlyingBlock(SSACFG.BasicBlock block) {
    return delegate.getNode(getNumber(block));
  }

  /**
   * has exceptional edge src -&gt; dest
   * 
   * @throws IllegalArgumentException if dest is null
   */
  public boolean hasExceptionalEdge(BasicBlock src, BasicBlock dest) {
    if (dest == null) {
      throw new IllegalArgumentException("dest is null");
    }
    if (dest.isExitBlock()) {
      int srcNum = getNumber(src);
      return delegate.getExceptionalToExit().get(srcNum);
    }
    return delegate.hasExceptionalEdge(getUnderlyingBlock(src), getUnderlyingBlock(dest));
  }

  /**
   * has normal edge src -&gt; dest
   * 
   * @throws IllegalArgumentException if dest is null
   */
  public boolean hasNormalEdge(BasicBlock src, BasicBlock dest) {
    if (dest == null) {
      throw new IllegalArgumentException("dest is null");
    }
    if (dest.isExitBlock()) {
      int srcNum = getNumber(src);
      return delegate.getNormalToExit().get(srcNum);
    }
    return delegate.hasNormalEdge(getUnderlyingBlock(src), getUnderlyingBlock(dest));
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  @Override
  public Collection<ISSABasicBlock> getNormalSuccessors(ISSABasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    final Collection<IBasicBlock<IInstruction>> ss = delegate.getNormalSuccessors(n);
    Collection<ISSABasicBlock> c = new ArrayList<>(getSuccNodeCount(b));
    for (IBasicBlock<IInstruction> s : ss) {
      c.add(basicBlocks[delegate.getNumber(s)]);
    }
    return c;
  }

  /*
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  @Override
  public Collection<ISSABasicBlock> getNormalPredecessors(ISSABasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    final Collection<IBasicBlock<IInstruction>> ss = delegate.getNormalPredecessors(n);
    Collection<ISSABasicBlock> c = new ArrayList<>(getPredNodeCount(b));
    for (IBasicBlock<IInstruction> s : ss) {
      c.add(basicBlocks[delegate.getNumber(s)]);
    }
    return c;
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public Iterator<ISSABasicBlock> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }

  @Override
  public void removeIncomingEdges(ISSABasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeOutgoingEdges(ISSABasicBlock node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasEdge(ISSABasicBlock src, ISSABasicBlock dst) throws UnimplementedError {
    return getSuccNodeNumbers(src).contains(getNumber(dst));
  }

  @Override
  public IntSet getSuccNodeNumbers(ISSABasicBlock b) throws IllegalArgumentException {
    if (b == null) {
      throw new IllegalArgumentException("b == null");
    }
    IBasicBlock<IInstruction> n = delegate.getNode(b.getNumber());
    return delegate.getSuccNodeNumbers(n);
  }

  @Override
  public IntSet getPredNodeNumbers(ISSABasicBlock node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * A warning for when we fail to resolve the type for a checkcast
   */
  private static class ExceptionLoadFailure extends Warning {

    final TypeReference type;

    final IMethod method;

    ExceptionLoadFailure(TypeReference type, IMethod method) {
      super(Warning.MODERATE);
      this.type = type;
      this.method = method;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + type + " " + method;
    }

    public static ExceptionLoadFailure create(TypeReference type, IMethod method) {
      return new ExceptionLoadFailure(type, method);
    }
  }

  /**
   * @return the basic block with a particular number
   */
  public BasicBlock getBasicBlock(int bb) {
    return basicBlocks[bb];
  }

}
