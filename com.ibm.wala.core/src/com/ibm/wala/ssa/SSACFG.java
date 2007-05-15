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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.cfg.ShrikeCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.Function;
import com.ibm.wala.util.MapIterator;
import com.ibm.wala.util.ShrikeUtil;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A control-flow graph for ssa form.
 * 
 * @author sfink
 */

public class SSACFG implements ControlFlowGraph{

  private static final boolean DEBUG = false;

  private BasicBlock[] basicBlocks;

  protected SSAInstruction[] instructions;

  protected IMethod method;

  private WarningSet warnings;

  protected AbstractCFG cfg;
 

  /**
   * cache a ref to the exit block for efficient access
   */
  private BasicBlock exit;

  protected SSACFG(SSACFG aCFG) {
    this.warnings = aCFG.warnings;
    this.cfg = aCFG.cfg;
    this.method = aCFG.method;
    this.basicBlocks = aCFG.basicBlocks;
    this.instructions = aCFG.instructions;
  }

  /**
   * Constructor CFG.
   * 
   * @param cfg
   */
  public SSACFG(IMethod method, AbstractCFG cfg, SSAInstruction[] instructions, WarningSet warnings) {

    this.warnings = warnings;
    this.cfg = cfg;
    if (DEBUG) {
      Trace.println("Incoming CFG for " + method + ":");
      Trace.println(cfg.toString());
    }

    this.method = method;
    if (Assertions.verifyAssertions) {
      if (method.getDeclaringClass() == null) {
        Assertions._assert(method.getDeclaringClass() != null, "null declaring class for " + method);
      }
    }
    createBasicBlocks(cfg);
    if (cfg instanceof ShrikeCFG) {
      recordExceptionTypes(((ShrikeCFG) cfg).getExceptionHandlers(), method.getDeclaringClass().getClassLoader());
    }
    this.instructions = instructions;

  }

  public int hashCode() {
    return -3 * cfg.hashCode();
  }

  public boolean equals(Object o) {
    return (o instanceof SSACFG) && cfg.equals(((SSACFG) o).cfg);
  }

  /**
   * Method recordExceptionTypes.
   * 
   * @param set
   */
  private void recordExceptionTypes(Set<ExceptionHandler> set, IClassLoader loader) {
    for (Iterator<ExceptionHandler> it = set.iterator(); it.hasNext();) {
      ExceptionHandler handler = it.next();
      TypeReference t = null;
      if (handler.getCatchClass() == null) {
        // by convention, in ShrikeCT this means catch everything
        t = TypeReference.JavaLangThrowable;
      } else {
        TypeReference exceptionType = ShrikeUtil.makeTypeReference(loader.getReference(), handler.getCatchClass());
        IClass klass = null;
        klass = loader.lookupClass(exceptionType.getName(), method.getClassHierarchy());
        if (klass == null) {
          warnings.add(ExceptionLoadFailure.create(exceptionType, method));
          t = exceptionType;
        } else {
          t = klass.getReference();
        }
      }
      int instructionIndex = handler.getHandler();
      if (Assertions.verifyAssertions) {
        IBasicBlock b = getBlockForInstruction(instructionIndex);
        if (!(b instanceof ExceptionHandlerBasicBlock)) {
          Assertions._assert(b instanceof ExceptionHandlerBasicBlock, "not exception handler " + b + " index " + instructionIndex);
        }
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
    exit = basicBlocks[cfg.getNumber(cfg.exit())];
  }

  /**
   * Get the basic block an instruction belongs to. Note: the instruction2Block
   * array is filled in lazily. During initialization, the mapping is set up
   * only for the first instruction of each basic block.
   */
  public SSACFG.BasicBlock getBlockForInstruction(int instructionIndex) {
    IBasicBlock N = cfg.getBlockForInstruction(instructionIndex);
    int number = cfg.getNumber(N);
    return basicBlocks[number];
  }

  /**
   * NB: Use iterators such as IR.iterateAllInstructions() instead
   * of this method.  This will probably be deprecated someday.
   * 
   * Return the instructions. Note that the CFG is created from the Shrike CFG
   * prior to creating the SSA instructions.
   * 
   * @return an array containing the SSA instructions.
   */
  public SSAInstruction[] getInstructions() {
    return instructions;
  }
  private final Map<RefPathKey,SSAPiInstruction> piInstructions = new HashMap<RefPathKey,SSAPiInstruction>(2);

  private class RefPathKey {
    private final int n;

    private final Object src;

    private final Object path;

    RefPathKey(int n, Object src, Object path) {
      this.n = n;
      this.src = src;
      this.path = path;
    }

    public int hashCode() {
      return n * path.hashCode();
    }

    public boolean equals(Object x) {
      return (x instanceof RefPathKey) && n == ((RefPathKey) x).n && src == ((RefPathKey) x).src && path == ((RefPathKey) x).path;
    }
  }

  /**
   * A Basic Block in an SSA IR
   * 
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

    public int getNumber() {
      return number;
    }

    /**
     * Method getFirstInstructionIndex.
     */
    public int getFirstInstructionIndex() {
      IBasicBlock B = (IBasicBlock) cfg.getNode(number);
      return B.getFirstInstructionIndex();
    }

    /**
     * Is this block marked as a catch block?
     */
    public boolean isCatchBlock() {
      return cfg.getCatchBlocks().get(getNumber());
    }

    /**
     * Method getLastInstructionIndex.
     * 
     * @return int
     */
    public int getLastInstructionIndex() {
      IBasicBlock B = (IBasicBlock) cfg.getNode(number);
      return B.getLastInstructionIndex();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.ISSABasicBlock#iteratePhis()
     */
    public Iterator<? extends SSAInstruction> iteratePhis() {
      compressPhis();
      if (stackSlotPhis == null) {
        if (localPhis == null) {
          return EmptyIterator.instance();
        } else {
          return Arrays.asList(localPhis).iterator();
        }
      } else {
        if (localPhis == null) {
          return Arrays.asList(stackSlotPhis).iterator();
        } else {
          return new CompoundIterator<SSAInstruction>(Arrays.asList(stackSlotPhis).iterator(), Arrays.asList(localPhis).iterator());
        }
      }
    }

    /**
     * Method getPhiForStackSlot. This method is used during SSA construction.
     * 
     * @param slot
     * @return PhiInstruction
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
     * Method getPhiForLocal. This method is used during SSA construction.
     * 
     * @param n
     * @return PhiInstruction
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

    /**
     * Method addPhiForStackSlot.
     * 
     * @param slot
     * @param phi
     */
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

    /**
     * Method addPhiForLocal.
     * 
     * @param n
     * @param phi
     */
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
     * 
     * @param toRemove
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
          for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
              stackSlotPhis[j++] = old[i];
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
          for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
              localPhis[j++] = old[i];
            }
          }
        }
      }
    }

    SSAPiInstruction getPiForRefAndPath(int n, Object path) {
      return (SSAPiInstruction) piInstructions.get(new RefPathKey(n, this, path));
    }

    private final LinkedList<SSAPiInstruction> blockPiInstructions = new LinkedList<SSAPiInstruction>();

    void addPiForRefAndPath(int n, Object path, SSAPiInstruction pi) {
      piInstructions.put(new RefPathKey(n, this, path), pi);
      blockPiInstructions.add(pi);
    }

    public Iterator<SSAPiInstruction> iteratePis() {
      return blockPiInstructions.iterator();
    }

    /**
     * TODO: make this more efficient if needed
     */
    public List<SSAInstruction> getAllInstructions() {
      compressPhis();

      ArrayList<SSAInstruction> result = new ArrayList<SSAInstruction>();
      for (int i = getFirstInstructionIndex(); i <= getLastInstructionIndex(); i++) {
        SSAInstruction s = instructions[i];
        if (s != null) {
          result.add(s);
        }
      }
      for (Iterator<? extends SSAInstruction> it = iteratePhis(); it.hasNext();) {
        result.add(it.next());
      }
      for (Iterator<? extends SSAInstruction> it = iteratePis(); it.hasNext();) {
        result.add(it.next());
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
          for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
              stackSlotPhis[j++] = old[i];
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
          for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
              localPhis[j++] = old[i];
            }
          }
        }
      }
    }

    private int countNonNull(SSAPhiInstruction[] a) {
      int result = 0;
      for (int i = 0; i < a.length; i++) {
        if (a[i] != null) {
          result++;
        }
      }
      return result;
    }

    public Iterator<IInstruction> iterator() {
      Function<SSAInstruction,IInstruction> kludge = new Function<SSAInstruction, IInstruction>() {
        public IInstruction apply(SSAInstruction s) {
          return s;
        }
      };
      return new MapIterator<SSAInstruction, IInstruction>(getAllInstructions().iterator(),kludge);
    }

    /**
     * @return true iff this basic block has at least one phi
     */
    public boolean hasPhi() {
      return stackSlotPhis != null || localPhis != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.INodeWithNumber#getGraphNodeId()
     */
    public int getGraphNodeId() {
      return number;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.util.graph.INodeWithNumber#setGraphNodeId(int)
     */
    public void setGraphNodeId(int number) {
      // TODO Auto-generated method stub
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return "BB[SSA:" + getFirstInstructionIndex() + ".." + getLastInstructionIndex() + "]" + getNumber() + " - "
          + method.getSignature();
    }

    private SSACFG getGraph() {
      return SSACFG.this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    public IMethod getMethod() {
      return method;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return cfg.getNode(getNumber()).hashCode() * 6271;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#isExitBlock()
     */
    public boolean isExitBlock() {
      return this == SSACFG.this.exit();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.cfg.IBasicBlock#isEntryBlock()
     */
    public boolean isEntryBlock() {
      return this == SSACFG.this.entry();
    }

    public SSAInstruction getLastInstruction() {
      return instructions[getLastInstructionIndex()];
    }
  }

  /**
   * 
   * 
   */
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

    /**
     * Returns the catchInstruction.
     * 
     * @return GetCaughtExceptionInstruction
     */
    public SSAGetCaughtExceptionInstruction getCatchInstruction() {
      return catchInstruction;
    }

    /**
     * Sets the catchInstruction.
     * 
     * @param catchInstruction
     *          The catchInstruction to set
     */
    public void setCatchInstruction(SSAGetCaughtExceptionInstruction catchInstruction) {
      this.catchInstruction = catchInstruction;
    }

    /**
     * Returns the exceptionType.
     * 
     * @return TypeReference
     */
    public Iterator<TypeReference> getCaughtExceptionTypes() {
      return new Iterator<TypeReference>() {
        int next = 0;

        public boolean hasNext() {
          return next < nExceptionTypes;
        }

        public TypeReference next() {
          return exceptionTypes[next++];
        }

        public void remove() {
          Assertions.UNREACHABLE();
        }
      };
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return "BB(Handler)[SSA]" + getNumber() + " - " + method.getSignature();
    }

    /**
     * Sets the exceptionType.
     * 
     * @param exceptionType
     *          The exceptionType to set
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.CFG.BasicBlock#getAllInstructions()
     */
    public List<SSAInstruction> getAllInstructions() {
      List<SSAInstruction> result = super.getAllInstructions();
      result.add(catchInstruction);
      return result;
    }

  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer s = new StringBuffer("");
    for (int i = 0; i <= getNumber(exit()); i++) {
      BasicBlock bb = (BasicBlock) getNode(i);
      s.append("BB").append(i).append("[").append(bb.getFirstInstructionIndex()).append("..").append(bb.getLastInstructionIndex())
          .append("]\n");

      Iterator succNodes = getSuccNodes(bb);
      while (succNodes.hasNext()) {
        s.append("    -> BB").append(((BasicBlock) succNodes.next()).getNumber()).append("\n");
      }
    }
    return s.toString();
  }

  public BitVector getCatchBlocks() {
    return cfg.getCatchBlocks();
  }

  /**
   * is the given i a catch block?
   * 
   * @return true if catch block, false otherwise
   */
  public boolean isCatchBlock(int i) {
    return cfg.isCatchBlock(i);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#entry()
   */
  public SSACFG.BasicBlock entry() {
    return basicBlocks[0];
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#exit()
   */
  public SSACFG.BasicBlock exit() {
    return exit;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedGraph#getNumber(com.ibm.wala.util.graph.Node)
   */
  public int getNumber(IBasicBlock N) {
    BasicBlock b = (BasicBlock) N;
    return b.getNumber();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedGraph#getNode(int)
   */
  public IBasicBlock getNode(int number) {
    return basicBlocks[number];
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedGraph#getMaxNumber()
   */
  public int getMaxNumber() {
    return basicBlocks.length - 1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#iterateNodes()
   */
  public Iterator<IBasicBlock> iterator() {
    ArrayList<IBasicBlock> list = new ArrayList<IBasicBlock>();
    for (IBasicBlock b : basicBlocks) {
      list.add(b);
    }
    return list.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return cfg.getNumberOfNodes();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<IBasicBlock> getPredNodes(IBasicBlock N) {
    BasicBlock b = (BasicBlock) N;
    IBasicBlock n = cfg.getNode(b.getNumber());
    final Iterator i = cfg.getPredNodes(n);
    return new Iterator<IBasicBlock>() {
      public boolean hasNext() {
        return i.hasNext();
      }

      public IBasicBlock next() {
        IBasicBlock n = (IBasicBlock) i.next();
        int number = n.getNumber();
        return basicBlocks[number];
      }

      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(IBasicBlock N) {
    BasicBlock b = (BasicBlock) N;
    IBasicBlock n = cfg.getNode(b.getNumber());
    return cfg.getPredNodeCount(n);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<IBasicBlock> getSuccNodes(IBasicBlock N) {
    BasicBlock b = (BasicBlock) N;
    IBasicBlock n = cfg.getNode(b.getNumber());
    final Iterator i = cfg.getSuccNodes(n);
    return new Iterator<IBasicBlock>() {
      public boolean hasNext() {
        return i.hasNext();
      }

      public IBasicBlock next() {
        IBasicBlock n = (IBasicBlock) i.next();
        int number = n.getNumber();
        return basicBlocks[number];
      }

      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(IBasicBlock N) {
    BasicBlock b = (BasicBlock) N;
    IBasicBlock n = cfg.getNode(b.getNumber());
    return cfg.getSuccNodeCount(n);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedGraph#addNode(com.ibm.wala.util.graph.Node)
   */
  public void addNode(IBasicBlock n) {
    Assertions.UNREACHABLE("external agents shouldn't be adding nodes");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#addEdge(com.ibm.wala.util.graph.Node,
   *      com.ibm.wala.util.graph.Node)
   */
  public void addEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE("external agents shouldn't be adding edges");

  }
  
  public void removeEdge(IBasicBlock src, IBasicBlock dst) {
    Assertions.UNREACHABLE("external agents shouldn't be removing edges");

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#removeEdges(com.ibm.wala.util.graph.Node)
   */
  public void removeAllIncidentEdges(IBasicBlock node) {
    Assertions.UNREACHABLE("external agents shouldn't be removing edges");

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#removeNode(com.ibm.wala.util.graph.Node)
   */
  public void removeNodeAndEdges(IBasicBlock N) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  public void removeNode(IBasicBlock n) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getProgramCounter(int)
   */
  public int getProgramCounter(int index) {
    // delegate to incoming cfg.
    return cfg.getProgramCounter(index);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.Graph#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(IBasicBlock N) {
    if (N instanceof BasicBlock) {
      return basicBlocks[getNumber(N)] == N;
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getMethod()
   */
  public IMethod getMethod() {
    return method;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getExceptionalSuccessors(final IBasicBlock b) {
    final IBasicBlock n = (IBasicBlock) cfg.getNode(b.getNumber());
    final Iterator i = cfg.getExceptionalSuccessors(n).iterator();
    final Collection<IBasicBlock> c = new HashSet<IBasicBlock>(getSuccNodeCount(b));
    for (; i.hasNext();) {
      final IBasicBlock s = (IBasicBlock) i.next();
      c.add(basicBlocks[cfg.getNumber(s)]);
    }
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getExceptionalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getExceptionalPredecessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    IBasicBlock n = (IBasicBlock) cfg.getNode(b.getNumber());
    Function<IBasicBlock,IBasicBlock> f = new Function<IBasicBlock,IBasicBlock>() {
      public IBasicBlock apply(IBasicBlock object) {
        return basicBlocks[cfg.getNumber((IBasicBlock) object)];
      }
    };
    return new Iterator2Collection<IBasicBlock>(new MapIterator<IBasicBlock,IBasicBlock>(cfg.getExceptionalPredecessors(n).iterator(), f));
  }

  /**
   * has exceptional edge src -> dest
   */
  public boolean hasExceptionalEdge(IBasicBlock src, IBasicBlock dest) {
    if (dest.isExitBlock()) {
      int srcNum = getNumber(src);
      return cfg.getExceptionalToExit().get(srcNum);
    }
    return cfg.hasExceptionalEdge(src, dest);
  }

  /**
   * has normal edge src -> dest
   */
  public boolean hasNormalEdge(IBasicBlock src, IBasicBlock dest) {
    if (dest.isExitBlock()) {
      int srcNum = getNumber(src);
      return cfg.getNormalToExit().get(srcNum);
    }
    return cfg.hasNormalEdge(src, dest);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalSuccessors(IBasicBlock b) {
    IBasicBlock n = (IBasicBlock) cfg.getNode(b.getNumber());
    final Iterator i = cfg.getNormalSuccessors(n).iterator();
    Collection<IBasicBlock> c = new ArrayList<IBasicBlock>(getSuccNodeCount(b));
    for (; i.hasNext();) {
      IBasicBlock s = (IBasicBlock) i.next();
      c.add(basicBlocks[cfg.getNumber(s)]);
    }
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.cfg.ControlFlowGraph#getNormalSuccessors(com.ibm.wala.cfg.IBasicBlock)
   */
  public Collection<IBasicBlock> getNormalPredecessors(IBasicBlock b) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    IBasicBlock n = (IBasicBlock) cfg.getNode(b.getNumber());
    final Iterator i = cfg.getNormalPredecessors(n).iterator();
    Collection<IBasicBlock> c = new ArrayList<IBasicBlock>(getPredNodeCount(b));
    for (; i.hasNext();) {
      IBasicBlock s = (IBasicBlock) i.next();
      c.add(basicBlocks[cfg.getNumber(s)]);
    }
    return c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  public Iterator<IBasicBlock> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<IBasicBlock>(s, this);
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void removeIncomingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  /*
   * (non-Javadoc)
   * 
   */
  public void removeOutgoingEdges(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();

  }

  public boolean hasEdge(IBasicBlock src, IBasicBlock dst) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   */
  public IntSet getSuccNodeNumbers(IBasicBlock node) {
    BasicBlock b = (BasicBlock) node;
    IBasicBlock n = cfg.getNode(b.getNumber());
    return cfg.getSuccNodeNumbers(n);
  }

  public IntSet getPredNodeNumbers(IBasicBlock node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * @author sfink
   * 
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

    public String getMsg() {
      return getClass().toString() + " : " + type + " " + method;
    }

    public static ExceptionLoadFailure create(TypeReference type, IMethod method) {
      return new ExceptionLoadFailure(type, method);
    }
  }

  /**
   * @param bb
   * @return the basic block with a particular number
   */
  public IBasicBlock getBasicBlock(int bb) {
    return basicBlocks[bb];
  }

}
