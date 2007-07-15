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
package com.ibm.wala.cast.ir.translator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstConstants;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstCloner;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstRewriter;
import com.ibm.wala.cast.tree.impl.CAstSymbolImplBase;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSALoadClassInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.Function;
import com.ibm.wala.util.MapIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

/**
 * @author Julian Dolby TODO: document me.
 */
public abstract class AstTranslator extends CAstVisitor implements ArrayOpHandler {

  protected abstract boolean useDefaultInitValues();

  protected abstract boolean treatGlobalsAsLexicallyScoped();

  protected abstract boolean useLocalValuesForLexicalVars();

  protected abstract TypeReference defaultCatchType();

  protected abstract TypeReference makeType(CAstType type);

  protected abstract void defineType(CAstEntity type, WalkContext wc);

  protected abstract void declareFunction(CAstEntity N, WalkContext context);

  protected abstract void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG cfg, SymbolTable symtab,
      boolean hasCatchBlock, TypeReference[][] caughtTypes, LexicalInformation lexicalInfo, DebuggingInformation debugInfo);

  protected abstract void defineField(CAstEntity topEntity, WalkContext context, CAstEntity n);

  protected abstract String composeEntityName(WalkContext parent, CAstEntity f);

  protected abstract void doThrow(WalkContext context, int exception);

  public abstract void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues);

  public abstract void doArrayWrite(WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval);

  protected abstract void doFieldRead(WalkContext context, int result, int receiver, CAstNode elt, CAstNode parent);

  protected abstract void doFieldWrite(WalkContext context, int receiver, CAstNode elt, CAstNode parent, int rval);

  protected abstract void doMaterializeFunction(WalkContext context, int result, int exception, CAstEntity fn);

  protected abstract void doNewObject(WalkContext context, CAstNode newNode, int result, Object type, int[] arguments);

  protected abstract void doCall(WalkContext context, CAstNode call, int result, int exception, CAstNode name, int receiver,
      int[] arguments);

  private ArrayOpHandler arrayOpHandler;

  protected boolean isExceptionLabel(Object label) {
    if (label == null) return false;
    if (label instanceof Boolean) return false;
    if (label instanceof Number) return false;
    if (label == CAstControlFlowMap.SWITCH_DEFAULT) return false;
    return true;
  }

  /**
   * If this returns true, new global declarations get created for any attempt
   * to access a non-existent variable (believe it or not, JavaScript actually
   * does this!)
   */
  protected boolean hasImplicitGlobals() {
    return false;
  }

  /**
   * If this returns true, then attempts to lookup non-existent names return
   * `null' rather than tripping an assertion. This can be used when special
   * handling is needed for built-in names. (PHP does this)
   */
  protected boolean hasSpecialUndeclaredVariables() {
    return false;
  }

  protected void handleUnspecifiedLiteralKey(WalkContext context, CAstNode objectLiteralNode, int unspecifiedLiteralIndex,
      CAstVisitor visitor) {
    Assertions.UNREACHABLE();
  }

  protected void doPrologue(WalkContext context) {
    if (useLocalValuesForLexicalVars()) {
      context.cfg().addInstruction(new AstLexicalRead(new Access[0]));
    }
  }

  protected abstract void doPrimitive(int resultVal, WalkContext context, CAstNode primitiveCall);

  protected int doLocalRead(WalkContext context, String name) {
    return context.currentScope().lookup(name).valueNumber();
  }

  protected void doLocalWrite(WalkContext context, String nm, int rval) {
    int lval = context.currentScope().lookup(nm).valueNumber();
    if (lval != rval) {
      context.cfg().addInstruction(new AssignInstruction(lval, rval));
    }
  }

  protected int doLexicallyScopedRead(WalkContext context, String name) {
    Symbol S = context.currentScope().lookup(name);
    int vn = S.valueNumber();
    CAstEntity E = S.getDefiningScope().getEntity();
    addExposedName(E, E, name, S.getDefiningScope().lookup(name).valueNumber());

    // lexically-scoped variables can be given a single vn in a method, or
    if (useLocalValuesForLexicalVars()) {
      Access A = new Access(name, getEntityName(E), vn);

      addExposedName(context.top(), E, name, vn);
      addAccess(context.top(), A);

      return vn;

      // lexically-scoped variables can be read from their scope each time
    } else {
      int result = context.currentScope().allocateTempValue();
      Access A = new Access(name, getEntityName(E), result);
      context.cfg().addInstruction(new AstLexicalRead(A));
      return result;
    }
  }

  protected void doLexicallyScopedWrite(WalkContext context, String name, int rval) {
    Symbol S = context.currentScope().lookup(name);
    CAstEntity E = S.getDefiningScope().getEntity();
    addExposedName(E, E, name, S.getDefiningScope().lookup(name).valueNumber());

    // lexically-scoped variables can be given a single vn in a method, or
    if (useLocalValuesForLexicalVars()) {
      int vn = S.valueNumber();
      Access A = new Access(name, getEntityName(E), vn);

      addExposedName(context.top(), E, name, vn);
      addAccess(context.top(), A);

      context.cfg().addInstruction(new AssignInstruction(vn, rval));
      context.cfg().addInstruction(new AstLexicalWrite(A));

      // lexically-scoped variables can be read from their scope each time
    } else {
      Access A = new Access(name, getEntityName(E), rval);
      context.cfg().addInstruction(new AstLexicalWrite(A));
    }
  }

  protected int doGlobalRead(WalkContext context, String name) {
    Symbol S = context.currentScope().lookup(name);

    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      // lexically-scoped variables can be given a single vn in a method, or
      if (useLocalValuesForLexicalVars()) {
        int vn = S.valueNumber();
        Access A = new Access(name, null, vn);

        addExposedName(context.top(), null, name, vn);
        addAccess(context.top(), A);

        return vn;

        // lexically-scoped variables can be read from their scope each time
      } else {
        int result = context.currentScope().allocateTempValue();
        Access A = new Access(name, null, result);
        context.cfg().addInstruction(new AstLexicalRead(A));
        return result;
      }

      // globals can be treated as a single static location
    } else {
      int result = context.currentScope().allocateTempValue();
      FieldReference global = makeGlobalRef(name);
      context.cfg().addInstruction(new AstGlobalRead(result, global));
      return result;
    }
  }

  protected void doGlobalWrite(WalkContext context, String name, int rval) {
    Symbol S = context.currentScope().lookup(name);

    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      // lexically-scoped variables can be given a single vn in a method, or
      if (useLocalValuesForLexicalVars()) {
        int vn = S.valueNumber();
        Access A = new Access(name, null, vn);

        addExposedName(context.top(), null, name, vn);
        addAccess(context.top(), A);

        context.cfg().addInstruction(new AssignInstruction(vn, rval));
        context.cfg().addInstruction(new AstLexicalWrite(A));

        // lexically-scoped variables can be read from their scope each time
      } else {
        Access A = new Access(name, null, rval);
        context.cfg().addInstruction(new AstLexicalWrite(A));
      }

      // globals can be treated as a single static location
    } else {
      FieldReference global = makeGlobalRef(name);
      context.cfg().addInstruction(new AstGlobalWrite(global, rval));
    }
  }

  protected void doIsFieldDefined(WalkContext context, int result, int ref, CAstNode field) {
    Assertions.UNREACHABLE();
  }

  protected FieldReference makeGlobalRef(String globalName) {
    return FieldReference.findOrCreate(TypeReference.findOrCreate(loader.getReference(), AstTypeReference.rootTypeName), Atom
        .findOrCreateUnicodeAtom("global " + globalName), TypeReference.findOrCreate(loader.getReference(),
        AstTypeReference.rootTypeName));
  }

  protected final IClassLoader loader;

  protected AstTranslator(IClassLoader loader) {
    this.loader = loader;
    this.arrayOpHandler= this;
  }

  public void setArrayOpHandler(ArrayOpHandler arrayOpHandler) {
    this.arrayOpHandler= arrayOpHandler;
  }

  private static class AstDebuggingInformation implements DebuggingInformation {
    private Position codeBodyPosition;

    private String[][] valueNumberNames;

    private Position[] instructionPositions;

    AstDebuggingInformation(Position codeBodyPosition, Position[] instructionPositions, String[] names) {
      this.codeBodyPosition = codeBodyPosition;

      this.instructionPositions = instructionPositions;

      valueNumberNames = new String[names.length][];
      for (int i = 0; i < names.length; i++) {
        if (names[i] != null) {
          valueNumberNames[i] = new String[] { names[i] };
        } else {
          valueNumberNames[i] = new String[0];
        }
      }
    }

    public Position getCodeBodyPosition() {
      return codeBodyPosition;
    }

    public Position getInstructionPosition(int instructionOffset) {
      return instructionPositions[instructionOffset];
    }

    public String[][] getSourceNamesForValues() {
      return valueNumberNames;
    }
  }

  public static final boolean DEBUG_ALL = false;

  public static final boolean DEBUG_TOP = DEBUG_ALL || false;

  public static final boolean DEBUG_CFG = DEBUG_ALL || false;

  public static final boolean DEBUG_NAMES = DEBUG_ALL || true;

  public static final boolean DEBUG_LEXICAL = DEBUG_ALL || true;

  protected final static class PreBasicBlock implements INodeWithNumber, IBasicBlock {
    private static final int NORMAL = 0;

    private static final int HANDLER = 1;

    private static final int ENTRY = 2;

    private static final int EXIT = 3;

    private int kind = NORMAL;

    private int number = -1;

    private int firstIndex = -1;

    private int lastIndex = -2;

    private final List<SSAInstruction> instructions = new ArrayList<SSAInstruction>();

    public int getNumber() {
      return getGraphNodeId();
    }

    public int getGraphNodeId() {
      return number;
    }

    public void setGraphNodeId(int number) {
      this.number = number;
    }

    public int getFirstInstructionIndex() {
      return firstIndex;
    }

    void setFirstIndex(int firstIndex) {
      this.firstIndex = firstIndex;
    }

    public int getLastInstructionIndex() {
      return lastIndex;
    }

    void setLastIndex(int lastIndex) {
      this.lastIndex = lastIndex;
    }

    void makeExitBlock() {
      kind = EXIT;
    }

    void makeEntryBlock() {
      kind = ENTRY;
    }

    void makeHandlerBlock() {
      kind = HANDLER;
    }

    public boolean isEntryBlock() {
      return kind == ENTRY;
    }

    public boolean isExitBlock() {
      return kind == EXIT;
    }

    public boolean isHandlerBlock() {
      return kind == HANDLER;
    }

    public String toString() {
      return "PreBB" + number + ":" + firstIndex + ".." + lastIndex;
    }

    List<SSAInstruction> instructions() {
      return instructions;
    }

    public boolean isCatchBlock() {
      return (lastIndex > -1) && (instructions.get(0) instanceof SSAGetCaughtExceptionInstruction);
    }

    public IMethod getMethod() {
      return null;
    }

    public Iterator<IInstruction> iterator() {
      Function<SSAInstruction, IInstruction> kludge = new Function<SSAInstruction, IInstruction>() {
        public IInstruction apply(SSAInstruction s) {
          return s;
        }
      };
      return new MapIterator<SSAInstruction, IInstruction>(instructions.iterator(), kludge);
    }
  }

  protected final class UnwindState {
    final CAstNode unwindAst;

    final WalkContext astContext;

    final CAstVisitor astVisitor;

    UnwindState(CAstNode unwindAst, WalkContext astContext, CAstVisitor astVisitor) {
      this.unwindAst = unwindAst;
      this.astContext = astContext;
      this.astVisitor = astVisitor;
    }

    public UnwindState getParent() {
      return astContext.getUnwindState();
    }

    public int hashCode() {
      return astContext.hashCode() * unwindAst.hashCode() * astVisitor.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof UnwindState) {
        if (((UnwindState) o).unwindAst != unwindAst)
          return false;
        if (((UnwindState) o).astVisitor != astVisitor)
          return false;
        if (getParent() == null) {
          return ((UnwindState) o).getParent() == null;
        } else {
          return getParent().equals(((UnwindState) o).getParent());
        }
      }

      return false;
    }

    boolean covers(UnwindState other) {
      if (equals(other))
        return true;
      if (getParent() != null)
        return getParent().covers(other);
      return false;
    }
  }

  public final class IncipientCFG extends SparseNumberedGraph<PreBasicBlock> {

    protected class Unwind {
      private final Map<PreBasicBlock, UnwindState> unwindData = new LinkedHashMap<PreBasicBlock, UnwindState>();

      private final Map<Pair<UnwindState, Pair<PreBasicBlock, Boolean>>, PreBasicBlock> code = new LinkedHashMap<Pair<UnwindState, Pair<PreBasicBlock, Boolean>>, PreBasicBlock>();

      void setUnwindState(PreBasicBlock block, UnwindState context) {
        unwindData.put(block, context);
      }

      void setUnwindState(CAstNode node, UnwindState context) {
        unwindData.put(nodeToBlock.get(node), context);
      }

      public PreBasicBlock findOrCreateCode(PreBasicBlock source, PreBasicBlock target, final boolean exception) {
        UnwindState sourceContext = unwindData.get(source);
	final CAstNode dummy = 
	  exception? (new CAstImpl()).makeNode(CAstNode.EMPTY): null;

        // no unwinding is needed, so jump to target block directly
        if (sourceContext == null)
          return target;

        WalkContext astContext = sourceContext.astContext;
        UnwindState targetContext = null;
        if (target != null)
          targetContext = unwindData.get(target);

        // in unwind context, but catch in same (or inner) unwind context
        if (targetContext != null && targetContext.covers(sourceContext))
          return target;

        Pair<UnwindState, Pair<PreBasicBlock, Boolean>> key = new Pair<UnwindState, Pair<PreBasicBlock, Boolean>>(sourceContext,
            new Pair<PreBasicBlock, Boolean>(target, exception ? Boolean.TRUE : Boolean.FALSE));

        if (code.containsKey(key)) {
          return code.get(key);

        } else {
          int e = -1;
          PreBasicBlock currentBlock = getCurrentBlock();
          if (!isDeadBlock(currentBlock)) {
            addInstruction(SSAInstructionFactory.GotoInstruction());
            newBlock(false);
          }
          PreBasicBlock startBlock = getCurrentBlock();
          if (exception) {
            setCurrentBlockAsHandler();
            e = sourceContext.astContext.currentScope().allocateTempValue();
            addInstruction(SSAInstructionFactory.GetCaughtExceptionInstruction(startBlock.getNumber(), e));
            sourceContext.astContext.setCatchType(startBlock.getNumber(), defaultCatchType());
          }
	  
          while (sourceContext != null && (targetContext == null || !targetContext.covers(sourceContext))) {
            final CAstRewriter.Rewrite ast = 
	      (new CAstCloner(new CAstImpl()) {
	        protected CAstNode flowOutTo(Map<CAstNode, CAstNode> nodeMap, 
					 CAstNode oldSource,
					 Object label,
					 CAstNode oldTarget,
					 CAstControlFlowMap orig, 
					 CAstSourcePositionMap src) 
		{
		  if (exception && !isExceptionLabel(label)) {
		    return dummy;
		  } else {
		    return oldTarget;
		  }
		}
	      }).copy(sourceContext.unwindAst,
		     sourceContext.astContext.getControlFlow(), 
		     sourceContext.astContext.getSourceMap(), 
		     sourceContext.astContext.top().getNodeTypeMap(),
		     sourceContext.astContext.top().getAllScopedEntities());
	    sourceContext.astVisitor.visit(ast.newRoot(), 
	      new DelegatingContext(sourceContext.astContext) {
                public CAstSourcePositionMap getSourceMap() {
                  return ast.newPos();
		}

		public CAstControlFlowMap getControlFlow() {
		  return ast.newCfg();
		}
	      }, 
	      sourceContext.astVisitor);

            sourceContext = sourceContext.getParent();
          }

          PreBasicBlock endBlock = getCurrentBlock();
          if (exception) {
	    addPreNode(dummy);
            doThrow(astContext, e);
          } else {
            addInstruction(SSAInstructionFactory.GotoInstruction());
          }
          newBlock(false);

          addEdge(currentBlock, getCurrentBlock());
          if (target != null) {
            addEdge(endBlock, target);

            // `null' target is idiom for branch/throw to exit
          } else {
            addDelayedEdge(endBlock, exitMarker, exception);
          }

          code.put(key, startBlock);
          return startBlock;
        }
      }
    }

    private Unwind unwind = null;

    private final List<PreBasicBlock> blocks = new ArrayList<PreBasicBlock>();

    private final Map<CAstNode, PreBasicBlock> nodeToBlock = new LinkedHashMap<CAstNode, PreBasicBlock>();

    private final Map<Object, Set<Pair<PreBasicBlock, Boolean>>> delayedEdges = new LinkedHashMap<Object, Set<Pair<PreBasicBlock, Boolean>>>();

    private final Object exitMarker = new Object();

    private final Set<PreBasicBlock> deadBlocks = new LinkedHashSet<PreBasicBlock>();

    private final Set<PreBasicBlock> normalToExit = new LinkedHashSet<PreBasicBlock>();

    private final Set<PreBasicBlock> exceptionalToExit = new LinkedHashSet<PreBasicBlock>();

    private Position[] linePositions = new Position[10];

    private boolean hasCatchBlock = false;

    private int currentInstruction = 0;

    private Position currentPosition = null;

    private PreBasicBlock currentBlock;

    public int getCurrentInstruction() {
      return currentInstruction;
    }

    public PreBasicBlock getCurrentBlock() {
      return currentBlock;
    }

    boolean hasCatchBlock() {
      return hasCatchBlock;
    }

    void noteCatchBlock() {
      hasCatchBlock = true;
    }

    void setCurrentPosition(Position pos) {
      currentPosition = pos;
    }

    Position getCurrentPosition() {
      return currentPosition;
    }

    Position[] getLinePositionMap() {
      return linePositions;
    }

    public PreBasicBlock newBlock(boolean fallThruFromPrior) {
      if (fallThruFromPrior && !currentBlock.isEntryBlock() && currentBlock.instructions().size() == 0) {
        return currentBlock;
      }

      PreBasicBlock previous = currentBlock;
      currentBlock = new PreBasicBlock();
      addNode(currentBlock);
      blocks.add(currentBlock);

      if (DEBUG_CFG)
        Trace.println("adding new block (node) " + currentBlock);
      if (fallThruFromPrior) {
        if (DEBUG_CFG)
          Trace.println("adding fall-thru edge " + previous + " --> " + currentBlock);
        addEdge(previous, currentBlock);
      } else {
        deadBlocks.add(currentBlock);
      }

      return currentBlock;
    }

    private void addDelayedEdge(PreBasicBlock src, Object dst, boolean exception) {
      Pair<PreBasicBlock, Boolean> v = new Pair<PreBasicBlock, Boolean>(src, exception ? Boolean.TRUE : Boolean.FALSE);
      if (delayedEdges.containsKey(dst))
        delayedEdges.get(dst).add(v);
      else {
        Set<Pair<PreBasicBlock, Boolean>> s = new LinkedHashSet<Pair<PreBasicBlock, Boolean>>();
        s.add(v);
        delayedEdges.put(dst, s);
      }
    }

    void makeEntryBlock(PreBasicBlock bb) {
      bb.makeEntryBlock();
    }

    void makeExitBlock(PreBasicBlock bb) {
      bb.makeExitBlock();

      for (Iterator<? extends PreBasicBlock> ps = getPredNodes(bb); ps.hasNext();)
        normalToExit.add(ps.next());

      checkForRealizedExitEdges(bb);
    }

    void setCurrentBlockAsHandler() {
      currentBlock.makeHandlerBlock();
    }

    private void checkForRealizedEdges(CAstNode n) {
      if (delayedEdges.containsKey(n)) {
        for (Iterator ss = delayedEdges.get(n).iterator(); ss.hasNext();) {
          Pair s = (Pair) ss.next();
          PreBasicBlock that = (PreBasicBlock) s.fst;
          boolean exception = ((Boolean) s.snd).booleanValue();
          if (unwind == null) {
            addEdge(that, nodeToBlock.get(n));
          } else {
            PreBasicBlock target = nodeToBlock.get(n);
            addEdge(that, unwind.findOrCreateCode(that, target, exception));
          }
        }

        delayedEdges.remove(n);
      }
    }

    private void checkForRealizedExitEdges(PreBasicBlock n) {
      if (delayedEdges.containsKey(exitMarker)) {
        for (Iterator ss = delayedEdges.get(exitMarker).iterator(); ss.hasNext();) {
          Pair s = (Pair) ss.next();
          PreBasicBlock that = (PreBasicBlock) s.fst;
          boolean exception = ((Boolean) s.snd).booleanValue();
          addEdge(that, n);
          if (exception)
            exceptionalToExit.add(that);
          else
            normalToExit.add(that);
        }

        delayedEdges.remove(exitMarker);
      }
    }

    private void setUnwindState(CAstNode node, UnwindState context) {
      if (unwind == null)
        unwind = new Unwind();
      unwind.setUnwindState(node, context);
    }

    public void addPreNode(CAstNode n) {
      addPreNode(n, null);
    }

    public void addPreNode(CAstNode n, UnwindState context) {
      if (DEBUG_CFG)
        Trace.println("adding pre-node " + n);
      nodeToBlock.put(n, currentBlock);
      deadBlocks.remove(currentBlock);
      if (context != null)
        setUnwindState(n, context);
      checkForRealizedEdges(n);
    }

    public void addPreEdge(CAstNode src, CAstNode dst, boolean exception) {
      Assertions._assert(nodeToBlock.containsKey(src));
      addPreEdge(nodeToBlock.get(src), dst, exception);
    }

    public void addPreEdge(PreBasicBlock src, CAstNode dst, boolean exception) {
      if (nodeToBlock.containsKey(dst)) {
        PreBasicBlock target = nodeToBlock.get(dst);
        if (DEBUG_CFG)
          Trace.println("adding pre-edge " + src + " --> " + dst);
        if (unwind == null) {
          addEdge(src, target);
        } else {
          addEdge(src, unwind.findOrCreateCode(src, target, exception));
        }
      } else {
        if (DEBUG_CFG)
          Trace.println("adding delayed pre-edge " + src + " --> " + dst);
        addDelayedEdge(src, dst, exception);
      }
    }

    public void addPreEdgeToExit(CAstNode src, boolean exception) {
      Assertions._assert(nodeToBlock.containsKey(src));
      addPreEdgeToExit(nodeToBlock.get(src), exception);
    }

    public void addPreEdgeToExit(PreBasicBlock src, boolean exception) {
      if (unwind != null) {
        PreBasicBlock handlers = unwind.findOrCreateCode(src, null, exception);
        if (handlers != null) {
          addEdge(src, handlers);
          return;
        }
      }

      addDelayedEdge(src, exitMarker, exception);
    }

    public void addEdge(PreBasicBlock src, PreBasicBlock dst) {
      super.addEdge(src, dst);
      deadBlocks.remove(dst);
    }

    boolean isDeadBlock(PreBasicBlock block) {
      return deadBlocks.contains(block);
    }

    public PreBasicBlock getBlock(CAstNode n) {
      return nodeToBlock.get(n);
    }

    private void noteLinePosition(int instruction) {
      if (linePositions.length < (instruction + 1)) {
        Position[] newData = new Position[instruction * 2 + 1];
        System.arraycopy(linePositions, 0, newData, 0, linePositions.length);
        linePositions = newData;
      }

      linePositions[instruction] = currentPosition;
    }

    public void addInstruction(SSAInstruction n) {
      deadBlocks.remove(currentBlock);

      int inst = currentInstruction++;

      noteLinePosition(inst);

      if (currentBlock.instructions().size() == 0) {
        currentBlock.setFirstIndex(inst);
      } else {
        Assertions._assert(!(n instanceof SSAGetCaughtExceptionInstruction));
      }

      if (DEBUG_CFG) {
        Trace.println("adding " + n + " at " + inst + " to " + currentBlock);
      }

      currentBlock.instructions().add(n);

      currentBlock.setLastIndex(inst);
    }
  }

  protected final static class AstCFG extends AbstractCFG {
    private final IInstruction[] instructions;

    private final int[] instructionToBlockMap;

    private final String functionName;

    private final SymbolTable symtab;

    AstCFG(CAstEntity n, IncipientCFG icfg, SymbolTable symtab) {
      super(null);
      List<PreBasicBlock> blocks = icfg.blocks;

      this.symtab = symtab;
      functionName = n.getName();
      instructionToBlockMap = new int[blocks.size()];

      for (int i = 0; i < blocks.size(); i++)
        instructionToBlockMap[i] = blocks.get(i).getLastInstructionIndex();

      for (int i = 0; i < blocks.size(); i++) {
        PreBasicBlock block = blocks.get(i);
        this.addNode(block);
        if (block.isCatchBlock()) {
          setCatchBlock(i);
        }

        if (DEBUG_CFG)
          Trace.println("added " + blocks.get(i) + " to final CFG as " + getNumber(blocks.get(i)));
      }
      if (DEBUG_CFG)
        Trace.println(getMaxNumber() + " blocks total");

      init();

      for (int i = 0; i < blocks.size(); i++) {
        PreBasicBlock src = blocks.get(i);
        for (Iterator j = icfg.getSuccNodes(src); j.hasNext();) {
          PreBasicBlock dst = (PreBasicBlock) j.next();
          if (isCatchBlock(dst.getNumber()) || (dst.isExitBlock() && icfg.exceptionalToExit.contains(src))) {
            if (DEBUG_CFG)
              Trace.println("exceptonal edge " + src + " -> " + dst);
            addExceptionalEdge(src, dst);
          }

          if (dst.isExitBlock() ? icfg.normalToExit.contains(src) : !isCatchBlock(dst.getNumber())) {
            if (DEBUG_CFG)
              Trace.println("normal edge " + src + " -> " + dst);
            addNormalEdge(src, dst);
          }
        }
      }

      int x = 0;
      instructions = new SSAInstruction[icfg.currentInstruction];
      for (int i = 0; i < blocks.size(); i++) {
        List<SSAInstruction> bi = blocks.get(i).instructions();
        for (int j = 0; j < bi.size(); j++) {
          instructions[x++] = bi.get(j);
        }
      }
    }

    public int hashCode() {
      return functionName.hashCode();
    }

    public boolean equals(Object o) {
      return (o instanceof AstCFG) && functionName.equals(((AstCFG) o).functionName);
    }

    public IBasicBlock getBlockForInstruction(int index) {
      for (int i = 1; i < getNumberOfNodes() - 1; i++)
        if (index <= instructionToBlockMap[i])
          return (IBasicBlock) getNode(i);

      return null;
    }

    public IInstruction[] getInstructions() {
      return instructions;
    }

    public int getProgramCounter(int index) {
      return index;
    }

    public String toString() {
      SSAInstruction[] insts = (SSAInstruction[]) getInstructions();
      StringBuffer s = new StringBuffer("CAst CFG of " + functionName);
      int params[] = symtab.getParameterValueNumbers();
      for (int i = 0; i < params.length; i++)
        s.append(" ").append(params[i]);
      s.append("\n");

      for (int i = 0; i < getNumberOfNodes(); i++) {
        PreBasicBlock bb = (PreBasicBlock) getNode(i);
        s.append(bb).append("\n");

        for (Iterator ss = getSuccNodes(bb); ss.hasNext();)
          s.append("    -->" + ss.next() + "\n");

        for (int j = bb.getFirstInstructionIndex(); j <= bb.getLastInstructionIndex(); j++)
          if (insts[j] != null)
            s.append("  " + insts[j].toString(symtab, null) + "\n");
      }

      s.append("-- END --");
      return s.toString();
    }
  }

  protected final static int TYPE_LOCAL = 1;

  protected final static int TYPE_GLOBAL = 2;

  protected final static int TYPE_SCRIPT = 3;

  protected final static int TYPE_FUNCTION = 4;

  protected final static int TYPE_TYPE = 5;

  protected class FinalCAstSymbol implements CAstSymbol {
    private final String _name;

    private FinalCAstSymbol(String _name) {
      this._name = _name;
    }

    public String name() {
      return _name;
    }

    public boolean isFinal() {
      return true;
    }

    public boolean isCaseInsensitive() {
      return false;
    }

    public boolean isInternalName() {
      return false;
    }

    public Object defaultInitValue() {
      return null;
    }
  }

  public static class InternalCAstSymbol extends CAstSymbolImplBase {
    public InternalCAstSymbol(String _name) {
      super(_name, false, false, null);
    }

    public InternalCAstSymbol(String _name, boolean _isFinal) {
      super(_name, _isFinal, false, null);
    }

    public InternalCAstSymbol(String _name, boolean _isFinal, boolean _isCaseInsensitive) {
      super(_name, _isFinal, _isCaseInsensitive, null);
    }

    public InternalCAstSymbol(String _name, boolean _isFinal, boolean _isCaseInsensitive, Object _defaultInitValue) {
      super(_name, _isFinal, _isCaseInsensitive, _defaultInitValue);
    }

    public boolean isInternalName() {
      return true;
    }
  }

  protected interface Symbol {
    int valueNumber();

    Scope getDefiningScope();

    boolean isParameter();

    Object constant();

    void setConstant(Object s);

    boolean isFinal();

    boolean isInternalName();

    Object defaultInitValue();
  }

  public interface Scope {
    int type();

    int allocateTempValue();

    int getConstantValue(Object c);

    boolean isConstant(int valueNumber);

    Object getConstantObject(int valueNumber);

    void declare(CAstSymbol s);

    void declare(CAstSymbol s, int valueNumber);

    boolean isCaseInsensitive(String name);

    boolean contains(String name);

    Symbol lookup(String name);

    Iterator<String> getAllNames();

    int size();

    boolean isGlobal(Symbol s);

    boolean isLexicallyScoped(Symbol s);

    CAstEntity getEntity();

    Scope getParent();
  }

  private static abstract class AbstractSymbol implements Symbol {
    private Object constantValue;

    private boolean isFinalValue;

    private final Scope definingScope;

    private Object defaultValue;

    AbstractSymbol(Scope definingScope, boolean isFinalValue, Object defaultValue) {
      this.definingScope = definingScope;
      this.isFinalValue = isFinalValue;
      this.defaultValue = defaultValue;
    }

    public boolean isFinal() {
      return isFinalValue;
    }

    public Object defaultInitValue() {
      return defaultValue;
    }

    public Object constant() {
      return constantValue;
    }

    public void setConstant(Object cv) {
      constantValue = cv;
    }

    public Scope getDefiningScope() {
      return definingScope;
    }
  };

  private abstract class AbstractScope implements Scope {
    private final Scope parent;

    private final Map<String, Symbol> values = new LinkedHashMap<String, Symbol>();

    private final Map<String, String> caseInsensitiveNames = new LinkedHashMap<String, String>();

    protected abstract SymbolTable getUnderlyingSymtab();

    public Scope getParent() {
      return parent;
    }

    public int size() {
      return getUnderlyingSymtab().getMaxValueNumber() + 1;
    }

    public Iterator<String> getAllNames() {
      return values.keySet().iterator();
    }

    public int allocateTempValue() {
      return getUnderlyingSymtab().newSymbol();
    }

    public int getConstantValue(Object o) {
      if (o instanceof Integer) {
        return getUnderlyingSymtab().getConstant(((Integer) o).intValue());
      } else if (o instanceof Float) {
        return getUnderlyingSymtab().getConstant(((Float) o).floatValue());
      } else if (o instanceof Double) {
        return getUnderlyingSymtab().getConstant(((Double) o).doubleValue());
      } else if (o instanceof Long) {
        return getUnderlyingSymtab().getConstant(((Long) o).longValue());
      } else if (o instanceof String) {
        return getUnderlyingSymtab().getConstant((String) o);
      } else if (o instanceof Boolean) {
        return getUnderlyingSymtab().getConstant((Boolean) o);
      } else if (o instanceof Character) {
        return getUnderlyingSymtab().getConstant(((Character) o).charValue());
      } else if (o instanceof Byte) {
        return getUnderlyingSymtab().getConstant(((Byte) o).byteValue());
      } else if (o == null) {
        return getUnderlyingSymtab().getNullConstant();
      } else if (o == CAstControlFlowMap.SWITCH_DEFAULT) {
        return getUnderlyingSymtab().getConstant("__default label");
      } else {
        Trace.println("cannot handle constant " + o);
        Assertions.UNREACHABLE();
        return -1;
      }
    }

    public boolean isConstant(int valueNumber) {
      return getUnderlyingSymtab().isConstant(valueNumber);
    }

    public Object getConstantObject(int valueNumber) {
      return getUnderlyingSymtab().getConstantValue(valueNumber);
    }

    public void declare(CAstSymbol s, int vn) {
      String nm = s.name();
      Assertions._assert(!contains(nm), nm);
      if (s.isCaseInsensitive())
        caseInsensitiveNames.put(nm.toLowerCase(), nm);
      values.put(nm, makeSymbol(s, vn));
    }

    public void declare(CAstSymbol s) {
      String nm = s.name();
      if (!contains(nm) || lookup(nm).getDefiningScope() != this) {
        if (s.isCaseInsensitive())
          caseInsensitiveNames.put(nm.toLowerCase(), nm);
        values.put(nm, makeSymbol(s));
      } else {
        Assertions._assert(!s.isFinal(), "trying to redeclare " + nm);
      }
    }

    AbstractScope(Scope parent) {
      this.parent = parent;
    }

    private final String mapName(String nm) {
      String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
      return (mappedName == null) ? nm : mappedName;
    }

    protected Symbol makeSymbol(CAstSymbol s) {
      return makeSymbol(s.name(), s.isFinal(), s.isInternalName(), s.defaultInitValue(), -1, this);
    }

    protected Symbol makeSymbol(CAstSymbol s, int vn) {
      return makeSymbol(s.name(), s.isFinal(), s.isInternalName(), s.defaultInitValue(), vn, this);
    }

    abstract protected Symbol makeSymbol(String nm, boolean isFinal, boolean isInternalName, Object defaultInitValue, int vn, Scope parent);

    public boolean isCaseInsensitive(String nm) {
      return caseInsensitiveNames.containsKey(nm.toLowerCase());
    }

    public Symbol lookup(String nm) {
      if (contains(nm)) {
        return values.get(mapName(nm));
      } else {
        Symbol scoped = parent.lookup(nm);
        if (scoped != null && getEntityScope() == this && (isGlobal(scoped) || isLexicallyScoped(scoped))) {
          values.put(nm, makeSymbol(nm, scoped.isFinal(), scoped.isInternalName(), scoped.defaultInitValue(), -1, scoped.getDefiningScope()));
          if (scoped.getDefiningScope().isCaseInsensitive(nm)) {
            caseInsensitiveNames.put(nm.toLowerCase(), nm);
          }
          return values.get(nm);
        } else {
          return scoped;
        }
      }
    }

    public boolean contains(String nm) {
      String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
      return values.containsKey(mappedName == null ? nm : mappedName);
    }

    public boolean isGlobal(Symbol s) {
      return s.getDefiningScope() == globalScope;
    }

    public abstract boolean isLexicallyScoped(Symbol s);

    protected abstract AbstractScope getEntityScope();

    public abstract CAstEntity getEntity();
  };

  private AbstractScope makeScriptScope(final CAstEntity s, Scope parent) {
    return new AbstractScope(parent) {
      SymbolTable scriptGlobalSymtab = new SymbolTable(s.getArgumentCount());

      public SymbolTable getUnderlyingSymtab() {
        return scriptGlobalSymtab;
      }

      protected AbstractScope getEntityScope() {
        return this;
      }

      public boolean isLexicallyScoped(Symbol s) {
        if (isGlobal(s))
          return false;
        else
          return ((AbstractScope) s.getDefiningScope()).getEntityScope() != this;
      }

      public CAstEntity getEntity() {
        return s;
      }

      public int type() {
        return TYPE_SCRIPT;
      }

      protected Symbol makeSymbol(final String nm, final boolean isFinal, final boolean isInternalName, final Object defaultInitValue, int vn, Scope definer) {
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            getUnderlyingSymtab().setDefaultValue(v, defaultInitValue);
          }
        }
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          public int valueNumber() {
            return v;
          }

	  public boolean isInternalName() {
	    return isInternalName;
	  }

          public boolean isParameter() {
            return false;
          }
        };
      }
    };
  }

  private AbstractScope makeFunctionScope(final CAstEntity f, Scope parent) {
    return new AbstractScope(parent) {
      private final String[] params = f.getArgumentNames();

      private final SymbolTable functionSymtab = new SymbolTable(f.getArgumentCount());

      // ctor for scope object
      {
        for (int i = 0; i < f.getArgumentCount(); i++) {
          final int yuck = i;
          declare(new CAstSymbol() {
            public String name() {
              return f.getArgumentNames()[yuck];
            }

            public boolean isFinal() {
              return false;
            }

            public boolean isCaseInsensitive() {
              return false;
            }

	    public boolean isInternalName() {
	      return false;
	    }

            public Object defaultInitValue() {
              return null;
            }

          });
        }
      }

      public SymbolTable getUnderlyingSymtab() {
        return functionSymtab;
      }

      protected AbstractScope getEntityScope() {
        return this;
      }

      public boolean isLexicallyScoped(Symbol s) {
        if (isGlobal(s))
          return false;
        else
          return ((AbstractScope) s.getDefiningScope()).getEntityScope() != this;
      }

      public CAstEntity getEntity() {
        return f;
      }

      public int type() {
        return TYPE_FUNCTION;
      }

      private int find(String n) {
        for (int i = 0; i < params.length; i++) {
          if (n.equals(params[i])) {
            return i + 1;
          }
        }

        return -1;
      }

      protected Symbol makeSymbol(final String nm, final boolean isFinal, final boolean isInternalName, final Object defaultInitValue, final int valueNumber,
          Scope definer) {
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          final int vn;

          {
            int x = find(nm);
            if (x != -1) {
              Assertions._assert(valueNumber == -1);
              vn = x;
            } else if (valueNumber != -1) {
              vn = valueNumber;
            } else {
              vn = getUnderlyingSymtab().newSymbol();
            }
            if (useDefaultInitValues() && defaultInitValue != null) {
              if (getUnderlyingSymtab().getValue(vn) == null) {
                getUnderlyingSymtab().setDefaultValue(vn, defaultInitValue);
              }
            }
          }

          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          public int valueNumber() {
            return vn;
          }

	  public boolean isInternalName() {
	    return isInternalName;
	  }

          public boolean isParameter() {
            return vn <= params.length;
          }
        };
      }
    };
  }

  private Scope makeLocalScope(CAstNode s, final Scope parent) {
    return new AbstractScope(parent) {
      public int type() {
        return TYPE_LOCAL;
      }

      public SymbolTable getUnderlyingSymtab() {
        return ((AbstractScope) parent).getUnderlyingSymtab();
      }

      protected AbstractScope getEntityScope() {
        return ((AbstractScope) parent).getEntityScope();
      }

      public boolean isLexicallyScoped(Symbol s) {
        return ((AbstractScope) getEntityScope()).isLexicallyScoped(s);
      }

      public CAstEntity getEntity() {
        return ((AbstractScope) getEntityScope()).getEntity();
      }

      protected Symbol makeSymbol(final String nm, boolean isFinal, final boolean isInternalName, final Object defaultInitValue, int vn, Scope definer) {
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            getUnderlyingSymtab().setDefaultValue(v, defaultInitValue);
          }
        }
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          public int valueNumber() {
            return v;
          }

	  public boolean isInternalName() {
	    return isInternalName;
	  }

          public boolean isParameter() {
            return false;
          }
        };
      }
    };
  }

  private Scope makeGlobalScope() {
    final Map<String, AbstractSymbol> globalSymbols = new LinkedHashMap<String, AbstractSymbol>();
    final Map<String, String> caseInsensitiveNames = new LinkedHashMap<String, String>();
    return new Scope() {
      private final String mapName(String nm) {
        String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
        return (mappedName == null) ? nm : mappedName;
      }

      public Scope getParent() {
        return null;
      }

      public boolean isGlobal(Symbol s) {
        return true;
      }

      public boolean isLexicallyScoped(Symbol s) {
        return false;
      }

      public CAstEntity getEntity() {
        return null;
      }

      public int size() {
        return globalSymbols.size();
      }

      public Iterator<String> getAllNames() {
        return globalSymbols.keySet().iterator();
      }

      public int allocateTempValue() {
        throw new UnsupportedOperationException();
      }

      public int getConstantValue(Object c) {
        throw new UnsupportedOperationException();
      }

      public boolean isConstant(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      public Object getConstantObject(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      public int type() {
        return TYPE_GLOBAL;
      }

      public boolean contains(String name) {
        return hasImplicitGlobals() || globalSymbols.containsKey(mapName(name));
      }

      public boolean isCaseInsensitive(String name) {
        return caseInsensitiveNames.containsKey(name.toLowerCase());
      }

      public Symbol lookup(final String name) {
        if (!globalSymbols.containsKey(mapName(name))) {
          if (hasImplicitGlobals()) {
            declare(new CAstSymbol() {
              public String name() {
                return name;
              }

              public boolean isFinal() {
                return false;
              }

              public boolean isCaseInsensitive() {
                return false;
              }

              public boolean isInternalName() {
                return false;
              }

              public Object defaultInitValue() {
                return null;
              }
            });
          } else if (hasSpecialUndeclaredVariables()) {
            return null;
          } else {
            throw new Error("cannot find " + name);
          }
        }

        return globalSymbols.get(mapName(name));
      }

      public void declare(CAstSymbol s, int vn) {
        Assertions._assert(vn == -1);
        declare(s);
      }

      public void declare(final CAstSymbol s) {
        final String name = s.name();
        if (s.isCaseInsensitive()) {
          caseInsensitiveNames.put(name.toLowerCase(), name);
        }
        globalSymbols.put(name, new AbstractSymbol(this, s.isFinal(), s.defaultInitValue()) {
          public String toString() {
            return name + ":" + System.identityHashCode(this);
          }

          public boolean isParameter() {
            return false;
          }

          public boolean isInternalName() {
	    return s.isInternalName();
          }

          public int valueNumber() {
            throw new UnsupportedOperationException();
          }
        });
      }
    };
  }

  protected Scope makeTypeScope(final CAstEntity type, final Scope parent) {
    final Map<String, AbstractSymbol> typeSymbols = new LinkedHashMap<String, AbstractSymbol>();
    final Map<String, String> caseInsensitiveNames = new LinkedHashMap<String, String>();
    return new Scope() {
      private final String mapName(String nm) {
        String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
        return (mappedName == null) ? nm : mappedName;
      }

      public Scope getParent() {
        return parent;
      }

      public boolean isGlobal(Symbol s) {
        return false;
      }

      public boolean isLexicallyScoped(Symbol s) {
        return false;
      }

      public CAstEntity getEntity() {
        return type;
      }

      public int size() {
        return typeSymbols.size();
      }

      public Iterator<String> getAllNames() {
        return typeSymbols.keySet().iterator();
      }

      public int allocateTempValue() {
        throw new UnsupportedOperationException();
      }

      public int getConstantValue(Object c) {
        throw new UnsupportedOperationException();
      }

      public boolean isConstant(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      public Object getConstantObject(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      public int type() {
        return TYPE_TYPE;
      }

      public boolean contains(String name) {
        return typeSymbols.containsKey(mapName(name));
      }

      public boolean isCaseInsensitive(String name) {
        return caseInsensitiveNames.containsKey(name.toLowerCase());
      }

      public Symbol lookup(String nm) {
        if (typeSymbols.containsKey(mapName(nm)))
          return typeSymbols.get(mapName(nm));
        else {
          return parent.lookup(nm);
        }
      }

      public void declare(CAstSymbol s, int vn) {
        Assertions._assert(vn == -1);
        declare(s);
      }

      public void declare(final CAstSymbol s) {
        final String name = s.name();
        Assertions._assert(!s.isFinal());
        if (s.isCaseInsensitive())
          caseInsensitiveNames.put(name.toLowerCase(), name);
        typeSymbols.put(name, new AbstractSymbol(this, s.isFinal(), s.defaultInitValue()) {
          public String toString() {
            return name + ":" + System.identityHashCode(this);
          }

          public boolean isParameter() {
            return false;
          }

          public boolean isInternalName() {
	    return s.isInternalName();
          }

          public int valueNumber() {
            throw new UnsupportedOperationException();
          }
        });
      }
    };
  }

  public interface WalkContext extends Context {

    String getName();

    String file();

    CAstSourcePositionMap getSourceMap();

    CAstControlFlowMap getControlFlow();

    Scope currentScope();

    Set<Scope> entityScopes();

    IncipientCFG cfg();

    UnwindState getUnwindState();

    void setCatchType(int blockNumber, TypeReference catchType);

    void setCatchType(CAstNode catchNode, TypeReference catchType);

    TypeReference[][] getCatchTypes();

  }

  private abstract class DelegatingContext implements WalkContext {
    private final WalkContext parent;

    DelegatingContext(WalkContext parent) {
      this.parent = parent;
    }

    public String getName() {
      return parent.getName();
    }

    public String file() {
      return parent.file();
    }

    public CAstEntity top() {
      return parent.top();
    }

    public CAstSourcePositionMap getSourceMap() {
      return parent.getSourceMap();
    }

    public CAstControlFlowMap getControlFlow() {
      return parent.getControlFlow();
    }

    public Scope currentScope() {
      return parent.currentScope();
    }

    public Set<Scope> entityScopes() {
      return parent.entityScopes();
    }

    public IncipientCFG cfg() {
      return parent.cfg();
    }

    public UnwindState getUnwindState() {
      return parent.getUnwindState();
    }

    public void setCatchType(int blockNumber, TypeReference catchType) {
      parent.setCatchType(blockNumber, catchType);
    }

    public void setCatchType(CAstNode catchNode, TypeReference catchType) {
      parent.setCatchType(catchNode, catchType);
    }

    public TypeReference[][] getCatchTypes() {
      return parent.getCatchTypes();
    }

  }

  private class FileContext extends DelegatingContext {
    private final String fUnitName;

    public FileContext(WalkContext parent, String unitName) {
      super(parent);
      fUnitName = unitName;
    }

    public String getName() {
      return fUnitName;
    }
  }

  private class UnwindContext extends DelegatingContext {
    private final UnwindState state;

    UnwindContext(CAstNode unwindNode, WalkContext parent, CAstVisitor visitor) {
      super(parent);
      this.state = new UnwindState(unwindNode, parent, visitor);
    }

    public UnwindState getUnwindState() {
      return state;
    }
  }

  private abstract class EntityContext extends DelegatingContext {
    protected final CAstEntity topNode;

    protected final String name;

    EntityContext(WalkContext parent, CAstEntity s) {
      super(parent);
      this.topNode = s;
      this.name = composeEntityName(parent, s);
      addEntityName(s, this.name);
    }

    public String getName() {
      return name;
    }

    public CAstEntity top() {
      return topNode;
    }

    public CAstSourcePositionMap getSourceMap() {
      return top().getSourceMap();
    }

  }

  private class CodeEntityContext extends EntityContext {
    private final Scope topEntityScope;

    private final Set<Scope> allEntityScopes;

    private final IncipientCFG cfg;

    private TypeReference[][] catchTypes = new TypeReference[0][];

    CodeEntityContext(WalkContext parent, Scope entityScope, CAstEntity s) {
      super(parent, s);

      this.topEntityScope = entityScope;

      this.allEntityScopes = HashSetFactory.make();
      this.allEntityScopes.add(entityScope);

      cfg = new IncipientCFG();
    }

    public CAstControlFlowMap getControlFlow() {
      return top().getControlFlow();
    }

    public IncipientCFG cfg() {
      return cfg;
    }

    public Scope currentScope() {
      return topEntityScope;
    }

    public Set<Scope> entityScopes() {
      return allEntityScopes;
    }

    public UnwindState getUnwindState() {
      return null;
    }

    public void setCatchType(CAstNode catchNode, TypeReference catchType) {
      setCatchType(cfg.getBlock(catchNode).getNumber(), catchType);
    }

    public void setCatchType(int blockNumber, TypeReference catchType) {
      if (catchTypes.length <= blockNumber) {
        TypeReference[][] data = new TypeReference[blockNumber + 1][];
        System.arraycopy(catchTypes, 0, data, 0, catchTypes.length);
        catchTypes = data;
      }

      if (catchTypes[blockNumber] == null) {
        catchTypes[blockNumber] = new TypeReference[] { catchType };
      } else {
        TypeReference[] data = catchTypes[blockNumber];

        for (int i = 0; i < data.length; i++) {
          if (data[i] == catchType) {
            return;
          }
        }

        TypeReference[] newData = new TypeReference[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[data.length] = catchType;

        catchTypes[blockNumber] = newData;
      }
    }

    public TypeReference[][] getCatchTypes() {
      return catchTypes;
    }
  }

  private final class TypeContext extends EntityContext {

    private TypeContext(WalkContext parent, CAstEntity n) {
      super(parent, n);
    }

    public CAstControlFlowMap getControlFlow() {
      Assertions.UNREACHABLE("TypeContext.getControlFlow()");
      return null;
    }

    public IncipientCFG cfg() {
      Assertions.UNREACHABLE("TypeContext.cfg()");
      return null;
    }

    public UnwindState getUnwindState() {
      Assertions.UNREACHABLE("TypeContext.getUnwindState()");
      return null;
    }
  }

  private class LocalContext extends DelegatingContext {
    private final Scope localScope;

    LocalContext(WalkContext parent, Scope localScope) {
      super(parent);
      this.localScope = localScope;
      parent.entityScopes().add(localScope);
    }

    public Scope currentScope() {
      return localScope;
    }
  }

  public static class AstLexicalInformation implements LexicalInformation {
    private final Pair[] exposedNames;

    private final int[][] instructionLexicalUses;

    private final int[] exitLexicalUses;

    private final String[] scopingParents;

    private int[] buildLexicalUseArray(Pair[] exposedNames) {
      if (exposedNames != null) {
        int[] lexicalUses = new int[exposedNames.length];
        for (int j = 0; j < exposedNames.length; j++) {
          lexicalUses[j] = ((Integer) exposedNames[j].snd).intValue();
        }

        return lexicalUses;
      } else {
        return null;
      }
    }

    private Pair[] buildLexicalNamesArray(Pair[] exposedNames) {
      if (exposedNames != null) {
        Pair[] lexicalNames = new Pair[exposedNames.length];
        for (int j = 0; j < exposedNames.length; j++) {
          lexicalNames[j] = (Pair) exposedNames[j].fst;
        }

        return lexicalNames;
      } else {
        return null;
      }
    }

    AstLexicalInformation(Scope scope, IInstruction[] instrs, Set<Pair<Pair<String, String>, Integer>> exposedNamesSet, Set accesses) {
      Pair[] EN = null;
      if (exposedNamesSet != null) {
        EN = (Pair[]) exposedNamesSet.toArray(new Pair[exposedNamesSet.size()]);
      }

      this.exposedNames = buildLexicalNamesArray(EN);

      this.exitLexicalUses = buildLexicalUseArray(EN);

      this.instructionLexicalUses = new int[instrs.length][];
      for (int i = 0; i < instrs.length; i++) {
        if (instrs[i] instanceof SSAAbstractInvokeInstruction) {
          this.instructionLexicalUses[i] = buildLexicalUseArray(EN);
        }
      }

      if (accesses != null) {
        Set<String> parents = new LinkedHashSet<String>();
        for (Iterator ACS = accesses.iterator(); ACS.hasNext();) {
          Access AC = (Access) ACS.next();
          if (AC.variableDefiner != null) {
            parents.add(AC.variableDefiner);
          }
        }
        scopingParents = parents.toArray(new String[parents.size()]);

        if (DEBUG_LEXICAL) {
          Trace.println("scoping parents of " + scope.getEntity());
          Trace.println(parents.toString());
        }

      } else {
        scopingParents = null;
      }

      if (DEBUG_NAMES) {
        Trace.println("lexical uses of " + scope.getEntity());
        for (int i = 0; i < instructionLexicalUses.length; i++) {
          if (instructionLexicalUses[i] != null) {
            Trace.println("  lexical uses of " + instrs[i]);
            for (int j = 0; j < instructionLexicalUses[i].length; j++) {
              Trace.println("    " + this.exposedNames[j].fst + ": " + instructionLexicalUses[i][j]);
            }
          }
        }
      }
    }

    public int[] getExitExposedUses() {
      return exitLexicalUses;
    }

    public int[] getExposedUses(int instructionOffset) {
      return instructionLexicalUses[instructionOffset];
    }

    public int[] getAllExposedUses() {
      List<Integer> uses = new ArrayList<Integer>();
      if (exitLexicalUses != null) {
        for (int i = 0; i < exitLexicalUses.length; i++) {
          uses.add(new Integer(exitLexicalUses[i]));
        }
      }
      if (instructionLexicalUses != null) {
        for (int i = 0; i < instructionLexicalUses.length; i++) {
          if (instructionLexicalUses[i] != null) {
            for (int j = 0; j < instructionLexicalUses[i].length; j++) {
              uses.add(new Integer(instructionLexicalUses[i][j]));
            }
          }
        }
      }

      int i = 0;
      int[] result = new int[uses.size()];
      for (Iterator x = uses.iterator(); x.hasNext();) {
        result[i++] = ((Integer) x.next()).intValue();
      }

      return result;
    }

    public Pair[] getExposedNames() {
      return exposedNames;
    }

    public String[] getScopingParents() {
      return scopingParents;
    }
  };

  private final Map<CAstNode, Integer> results = new LinkedHashMap<CAstNode, Integer>();

  protected boolean hasValue(CAstNode n) {
    return results.containsKey(n);
  }

  public final int setValue(CAstNode n, int v) {
    results.put(n, new Integer(v));
    return v;
  }

  public final int getValue(CAstNode n) {
    if (results.containsKey(n))
      return results.get(n).intValue();
    else {
      Trace.println("no value for " + n.getKind());
      return -1;
    }
  }

  private final Map<CAstEntity, String> entityNames = new LinkedHashMap<CAstEntity, String>();

  private final Map<CAstEntity, LinkedHashSet<Pair<Pair<String, String>, Integer>>> exposedNames = new LinkedHashMap<CAstEntity, LinkedHashSet<Pair<Pair<String, String>, Integer>>>();

  private final Map<CAstEntity, LinkedHashSet<Access>> accesses = new LinkedHashMap<CAstEntity, LinkedHashSet<Access>>();

  private void addEntityName(CAstEntity e, String name) {
    entityNames.put(e, name);
  }

  private void addAccess(CAstEntity e, Access access) {
    if (!accesses.containsKey(e))
      accesses.put(e, new LinkedHashSet<Access>());
    accesses.get(e).add(access);
  }

  private void addExposedName(CAstEntity entity, CAstEntity declaration, String name, int valueNumber) {
    if (!exposedNames.containsKey(entity))
      exposedNames.put(entity, new LinkedHashSet<Pair<Pair<String, String>, Integer>>());

    exposedNames.get(entity).add(
        new Pair<Pair<String, String>, Integer>(new Pair<String, String>(name, getEntityName(declaration)),
            new Integer(valueNumber)));
  }

  private String getEntityName(CAstEntity e) {
    if (e == null) {
      return null;
    } else {
      Assertions._assert(entityNames.containsKey(e));
      return "L" + entityNames.get(e);
    }
  }

  protected UnaryOpInstruction.IOperator translateUnaryOpcode(CAstNode op) {
    if (op == CAstOperator.OP_BITNOT)
      return UnaryOpInstruction.Operator.NEG;
    else if (op == CAstOperator.OP_NOT)
      return UnaryOpInstruction.Operator.NEG;
    else if (op == CAstOperator.OP_SUB)
      return AstConstants.UnaryOp.MINUS;
    else
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
    return null;

  }

  protected BinaryOpInstruction.IOperator translateBinaryOpcode(CAstNode op) {
    if (op == CAstOperator.OP_ADD)
      return BinaryOpInstruction.Operator.ADD;
    else if (op == CAstOperator.OP_DIV)
      return BinaryOpInstruction.Operator.DIV;
    else if (op == CAstOperator.OP_LSH)
      return ShiftInstruction.Operator.SHL;
    else if (op == CAstOperator.OP_MOD)
      return BinaryOpInstruction.Operator.REM;
    else if (op == CAstOperator.OP_MUL)
      return BinaryOpInstruction.Operator.MUL;
    else if (op == CAstOperator.OP_RSH)
      return ShiftInstruction.Operator.SHR;
    else if (op == CAstOperator.OP_SUB)
      return BinaryOpInstruction.Operator.SUB;
    else if (op == CAstOperator.OP_URSH)
      return ShiftInstruction.Operator.USHR;
    else if (op == CAstOperator.OP_BIT_AND)
      return BinaryOpInstruction.Operator.AND;
    else if (op == CAstOperator.OP_BIT_OR)
      return BinaryOpInstruction.Operator.OR;
    else if (op == CAstOperator.OP_BIT_XOR)
      return BinaryOpInstruction.Operator.XOR;
    else if (op == CAstOperator.OP_CONCAT)
      return AstConstants.BinaryOp.CONCAT;
    else if (op == CAstOperator.OP_EQ)
      return AstConstants.BinaryOp.EQ;
    else if (op == CAstOperator.OP_GE)
      return AstConstants.BinaryOp.GE;
    else if (op == CAstOperator.OP_GT)
      return AstConstants.BinaryOp.GT;
    else if (op == CAstOperator.OP_LE)
      return AstConstants.BinaryOp.LE;
    else if (op == CAstOperator.OP_LT)
      return AstConstants.BinaryOp.LT;
    else if (op == CAstOperator.OP_NE)
      return AstConstants.BinaryOp.NE;
    else {
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
      return null;
    }
  }

  protected ConditionalBranchInstruction.IOperator translateConditionOpcode(CAstNode op) {
    if (op == CAstOperator.OP_EQ)
      return ConditionalBranchInstruction.Operator.EQ;
    else if (op == CAstOperator.OP_GE)
      return ConditionalBranchInstruction.Operator.GE;
    else if (op == CAstOperator.OP_GT)
      return ConditionalBranchInstruction.Operator.GT;
    else if (op == CAstOperator.OP_LE)
      return ConditionalBranchInstruction.Operator.LE;
    else if (op == CAstOperator.OP_LT)
      return ConditionalBranchInstruction.Operator.LT;
    else if (op == CAstOperator.OP_NE)
      return ConditionalBranchInstruction.Operator.NE;

    else {
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
      return null;
    }
  }

  private String[] makeNameMap(CAstEntity n, Set<Scope> scopes) {
    // all scopes share the same underlying symtab, which is what
    // size really refers to.
    String[] map = new String[scopes.iterator().next().size() + 1];

    if (DEBUG_NAMES) {
      Trace.println("names array of size " + map.length);
    }

    for (Iterator<Scope> S = scopes.iterator(); S.hasNext();) {
      Scope scope = S.next();
      for (Iterator<String> I = scope.getAllNames(); I.hasNext();) {
        String nm = I.next();
        Symbol v = (Symbol) scope.lookup(nm);

	if (v.isInternalName()) {
	  continue;
	}

        // constants can flow to multiple variables
        if (scope.isConstant(v.valueNumber()))
          continue;

        Assertions._assert(map[v.valueNumber()] == null || map[v.valueNumber()].equals(nm), "value number " + v.valueNumber()
            + " mapped to multiple names in " + n.getName() + ": " + nm + " and " + map[v.valueNumber()]);

        map[v.valueNumber()] = nm;

        if (DEBUG_NAMES) {
          Trace.println("mapping name " + nm + " to " + v.valueNumber());
        }
      }
    }

    return map;
  }

  protected final CAstType getTypeForNode(WalkContext context, CAstNode node) {
    if (context.top().getNodeTypeMap() != null) {
      return context.top().getNodeTypeMap().getNodeType(node);
    } else {
      return null;
    }
  }

  private void patchLexicalAccesses(IInstruction[] instrs, Set<Access> accesses) {
    Access[] AC = accesses == null ? (Access[]) null : (Access[]) accesses.toArray(new Access[accesses.size()]);
    for (int i = 0; i < instrs.length; i++) {
      if (instrs[i] instanceof AstLexicalAccess && ((AstLexicalAccess) instrs[i]).getAccessCount() == 0) {
        if (AC != null) {
          ((AstLexicalAccess) instrs[i]).setAccesses(AC);
        } else {
          instrs[i] = null;
        }
      }
    }
  }

  protected Context makeFileContext(Context c, CAstEntity n) {
    return new FileContext((WalkContext) c, n.getName());
  }

  protected Context makeTypeContext(Context c, CAstEntity n) {
    return new TypeContext((WalkContext) c, n);
  }

  protected Context makeCodeContext(Context c, CAstEntity n) {
    WalkContext context = (WalkContext) c;
    AbstractScope scope;
    if (n.getKind() == CAstEntity.SCRIPT_ENTITY)
      scope = makeScriptScope(n, context.currentScope());
    else
      scope = makeFunctionScope(n, context.currentScope());
    return new CodeEntityContext(context, scope, n);
  }

  protected boolean enterEntity(final CAstEntity n, Context context, CAstVisitor visitor) {
    if (DEBUG_TOP)
      Trace.println("translating " + n.getName());
    return false;
  }

  protected boolean visitFileEntity(CAstEntity n, Context context, Context fileContext, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveFileEntity(CAstEntity n, Context context, Context fileContext, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitFieldEntity(CAstEntity n, Context context, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveFieldEntity(CAstEntity n, Context context, CAstVisitor visitor) {
    // Define a new field in the enclosing type, if the language we're
    // processing allows such.
    CAstEntity topEntity = context.top(); // better be a type
    Assertions._assert(topEntity.getKind() == CAstEntity.TYPE_ENTITY, "Parent of field entity is not a type???");
    defineField(topEntity, (WalkContext) context, n);
  }

  protected boolean visitTypeEntity(CAstEntity n, Context context, Context typeContext, CAstVisitor visitor) {
    defineType(n, (WalkContext) context);
    return false;
  }

  protected void leaveTypeEntity(CAstEntity n, Context context, Context typeContext, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitFunctionEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    if (n.getAST() == null) // presumably abstract
      declareFunction(n, (WalkContext) context);
    else
      initFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
    return false;
  }

  protected void leaveFunctionEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    if (n.getAST() != null) // non-abstract
      closeFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
  }

  protected boolean visitScriptEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    declareFunction(n, (WalkContext) codeContext);
    initFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
    return false;
  }

  protected void leaveScriptEntity(CAstEntity n, Context context, Context codeContext, CAstVisitor visitor) {
    closeFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
  }

  public void initFunctionEntity(final CAstEntity n, WalkContext parentContext, WalkContext functionContext) {
    // entry block
    functionContext.cfg().makeEntryBlock(functionContext.cfg().newBlock(false));
    // first real block
    functionContext.cfg().newBlock(true);
    // prologue code, if any
    doPrologue(functionContext);
  }

  public void closeFunctionEntity(final CAstEntity n, WalkContext parentContext, WalkContext functionContext) {
    // exit block
    functionContext.cfg().makeExitBlock(functionContext.cfg().newBlock(true));

    // create code entry stuff for this entity
    SymbolTable symtab = ((AbstractScope) functionContext.currentScope()).getUnderlyingSymtab();
    TypeReference[][] catchTypes = functionContext.getCatchTypes();
    AbstractCFG cfg = new AstCFG(n, functionContext.cfg(), symtab);
    Position[] line = functionContext.cfg().getLinePositionMap();
    boolean katch = functionContext.cfg().hasCatchBlock();
    String[] nms = makeNameMap(n, functionContext.entityScopes());

    /*
     * Set reachableBlocks = DFS.getReachableNodes(cfg,
     * Collections.singleton(cfg.entry()));
     * Assertions._assert(reachableBlocks.size() == cfg.getNumberOfNodes(),
     * cfg.toString());
     */

    // (put here to allow subclasses to handle stuff in scoped entities)
    // assemble lexical information
    patchLexicalAccesses(cfg.getInstructions(), accesses.get(n));
    LexicalInformation LI =
    // TODO: Ask Julian if the below change is always correct
    new AstLexicalInformation((AbstractScope) functionContext.currentScope(), cfg.getInstructions(), exposedNames.get(n), accesses
        .get(n));

    DebuggingInformation DBG = new AstDebuggingInformation(n.getPosition(), line, nms);

    // actually make code body
    defineFunction(n, parentContext, cfg, symtab, katch, catchTypes, LI, DBG);
  }

  private final Stack<Position> positions = new Stack<Position>();

  protected Context makeLocalContext(Context context, CAstNode n) {
    return new LocalContext((WalkContext) context, makeLocalScope(n, ((WalkContext) context).currentScope()));
  }

  protected Context makeUnwindContext(Context context, CAstNode n, CAstVisitor visitor) {
    return new UnwindContext(n, (WalkContext) context, visitor);
  }

  // FIXME: should it be possible to override visit() instead to do the below
  // and then call super.visit?
  private Map<CAstNode, Boolean> popPositionM = new LinkedHashMap<CAstNode, Boolean>();

  protected boolean enterNode(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    boolean popPosition = false;
    if (context.getSourceMap() != null) {
      CAstSourcePositionMap.Position p = context.getSourceMap().getPosition(n);
      if (p != null) {
        if (context.cfg().getCurrentPosition() != null) {
          positions.push(context.cfg().getCurrentPosition());
          popPosition = true;
        }

        context.cfg().setCurrentPosition(p);
      }
    }

    if (popPosition)
      popPositionM.put(n, Boolean.TRUE);
    return false;
  }

  protected void postProcessNode(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (popPositionM.get(n) != null) {
      context.cfg().setCurrentPosition(positions.pop());
    }
  }

  protected int processFunctionExpr(CAstNode n, Context c) {
    WalkContext context = (WalkContext) c;
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    declareFunction(fn, context);
    int result = context.currentScope().allocateTempValue();
    int ex = context.currentScope().allocateTempValue();
    doMaterializeFunction(context, result, ex, fn);
    return result;
  }

  protected boolean visitFunctionExpr(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveFunctionExpr(CAstNode n, Context c, CAstVisitor visitor) {
    int result = processFunctionExpr(n, c);
    setValue(n, result);
  }

  protected boolean visitFunctionStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveFunctionStmt(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = processFunctionExpr(n, c);
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    // FIXME: handle redefinitions of functions
    if (!context.currentScope().contains(fn.getName())) {
      context.currentScope().declare(new FinalCAstSymbol(fn.getName()), result);
    }
  }

  protected boolean visitLocalScope(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveLocalScope(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, getValue(n.getChild(0)));
  }

  protected boolean visitBlockExpr(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExpr(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitBlockStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveBlockStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitLoop(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    // loop test block
    context.cfg().newBlock(true);
    PreBasicBlock headerB = context.cfg().getCurrentBlock();
    visitor.visit(n.getChild(0), context, visitor);

    Assertions._assert(getValue(n.getChild(0)) != -1, "error in loop test "
        + CAstPrinter.print(n.getChild(0), context.top().getSourceMap()) + " of loop "
        + CAstPrinter.print(n, context.top().getSourceMap()));
    context.cfg().addInstruction(
        SSAInstructionFactory.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, getValue(n
            .getChild(0)), context.currentScope().getConstantValue(new Integer(0))));
    PreBasicBlock branchB = context.cfg().getCurrentBlock();

    // loop body
    context.cfg().newBlock(true);
    visitor.visit(n.getChild(1), context, visitor);
    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
      PreBasicBlock bodyB = context.cfg().getCurrentBlock();
      context.cfg().addEdge(bodyB, headerB);

      // next block
      context.cfg().newBlock(false);
    }

    PreBasicBlock nextB = context.cfg().getCurrentBlock();

    // control flow mapping;
    context.cfg().addEdge(branchB, nextB);
    return true;
  }

  // Make final to prevent overriding
  protected final void leaveLoopHeader(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveLoop(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitGetCaughtException(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveGetCaughtException(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    context.currentScope().declare(new FinalCAstSymbol(nm));
    context.cfg().addInstruction(
        SSAInstructionFactory.GetCaughtExceptionInstruction(context.cfg().getCurrentBlock().getNumber(), context.currentScope()
            .lookup(nm).valueNumber()));
  }

  protected boolean visitThis(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveThis(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, 1);
  }

  protected boolean visitSuper(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveSuper(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, 1);
  }

  protected boolean visitCall(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    return false;
  }

  protected void leaveCall(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = getValue(n);
    int exp = context.currentScope().allocateTempValue();
    int fun = getValue(n.getChild(0));
    CAstNode functionName = n.getChild(1);
    int[] args = new int[n.getChildCount() - 2];
    for (int i = 0; i < args.length; i++) {
      args[i] = getValue(n.getChild(i + 2));
    }
    doCall(context, n, result, exp, functionName, fun, args);
  }

  protected boolean visitVar(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveVar(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    assert nm != null : "cannot find var for " + CAstPrinter.print(n, context.getSourceMap());
    Symbol s = context.currentScope().lookup(nm);
    assert s != null : "cannot find symbol for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
    if (context.currentScope().isGlobal(s)) {
      setValue(n, doGlobalRead(context, nm));
    } else if (context.currentScope().isLexicallyScoped(s)) {
      setValue(n, doLexicallyScopedRead(context, nm));
    } else {
      setValue(n, doLocalRead(context, nm));
    }
  }

  protected boolean visitConstant(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveConstant(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    setValue(n, context.currentScope().getConstantValue(n.getValue()));
  }

  protected boolean visitBinaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    return false;
  }

  protected void leaveBinaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = getValue(n);
    CAstNode l = n.getChild(1);
    CAstNode r = n.getChild(2);
    Assertions._assert(getValue(r) != -1, CAstPrinter.print(n));
    Assertions._assert(getValue(l) != -1, CAstPrinter.print(n));
    context.cfg().addInstruction(
        SSAInstructionFactory.BinaryOpInstruction(translateBinaryOpcode(n.getChild(0)), result, getValue(l), getValue(r)));
  }

  protected boolean visitUnaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    return false;
  }

  protected void leaveUnaryExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = getValue(n);
    CAstNode v = n.getChild(1);
    context.cfg()
        .addInstruction(SSAInstructionFactory.UnaryOpInstruction(translateUnaryOpcode(n.getChild(0)), result, getValue(v)));
  }

  protected boolean visitArrayLength(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    return false;
  }

  protected void leaveArrayLength(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = getValue(n);
    int arrayValue = getValue(n.getChild(0));
    context.cfg().addInstruction(SSAInstructionFactory.ArrayLengthInstruction(result, arrayValue));
  }

  protected boolean visitArrayRef(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRef(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int arrayValue = getValue(n.getChild(0));
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    arrayOpHandler.doArrayRead(context, result, arrayValue, n, gatherArrayDims(n));
  }

  protected boolean visitDeclStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  // TODO: should we handle exploded declaration nodes here instead?
  protected void leaveDeclStmt(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    CAstSymbol s = (CAstSymbol) n.getChild(0).getValue();
    String nm = s.name();
    Scope scope = context.currentScope();
    if (n.getChildCount() == 2) {
      CAstNode v = n.getChild(1);
      if (scope.contains(nm) && scope.lookup(nm).getDefiningScope() == scope) {
        Assertions._assert(!s.isFinal());
        context.cfg().addInstruction(new AssignInstruction(scope.lookup(nm).valueNumber(), getValue(v)));
      } else if (v.getKind() != CAstNode.CONSTANT && v.getKind() != CAstNode.VAR && v.getKind() != CAstNode.THIS) {
        scope.declare(s, getValue(v));
      } else {
        scope.declare(s);
        context.cfg().addInstruction(new AssignInstruction(context.currentScope().lookup(nm).valueNumber(), getValue(v)));
      }
    } else {
      context.currentScope().declare(s);
    }
  }

  protected boolean visitReturn(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveReturn(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChildCount() > 0) {
      context.cfg().addInstruction(SSAInstructionFactory.ReturnInstruction(getValue(n.getChild(0)), false));
    } else {
      context.cfg().addInstruction(SSAInstructionFactory.ReturnInstruction());
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(false);
    context.cfg().addPreEdgeToExit(n, false);
  }

  protected boolean visitIfgoto(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveIfgoto(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChildCount() == 1) {
      context.cfg().addInstruction(
          SSAInstructionFactory.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_NE), null, getValue(n
              .getChild(0)), context.currentScope().getConstantValue(new Integer(0))));
    } else if (n.getChildCount() == 3) {
      context.cfg().addInstruction(
          SSAInstructionFactory.ConditionalBranchInstruction(translateConditionOpcode(n.getChild(0)), null,
              getValue(n.getChild(1)), getValue(n.getChild(2))));
    } else {
      Assertions.UNREACHABLE();
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(true);
    context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, Boolean.TRUE), false);
  }

  protected boolean visitGoto(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveGoto(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
    context.cfg().newBlock(false);
    if (context.getControlFlow().getTarget(n, null) == null) {
      Assertions._assert(context.getControlFlow().getTarget(n, null) != null, context.getControlFlow() + " does not map " + n + " (" + context.getSourceMap().getPosition(n) + ")");
    }
    context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, null), false);
  }

  protected boolean visitLabelStmt(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (!context.getControlFlow().getSourceNodes(n).isEmpty()) {
      context.cfg().newBlock(true);
      context.cfg().addPreNode(n, context.getUnwindState());
    }
    return false;
  }

  protected void leaveLabelStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected void processIf(CAstNode n, boolean isExpr, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    PreBasicBlock trueB = null, falseB = null;
    // conditional
    CAstNode l = n.getChild(0);
    visitor.visit(l, context, visitor);
    context.cfg().addInstruction(
        SSAInstructionFactory.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, getValue(l), context
            .currentScope().getConstantValue(new Integer(0))));
    PreBasicBlock srcB = context.cfg().getCurrentBlock();
    // true clause
    context.cfg().newBlock(true);
    CAstNode r = n.getChild(1);
    walkNodes(r, context);
    if (isExpr)
      context.cfg().addInstruction(new AssignInstruction(getValue(n), getValue(r)));
    if (n.getChildCount() == 3) {
      if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
        context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
        trueB = context.cfg().getCurrentBlock();

        // false clause
        context.cfg().newBlock(false);
      }

      falseB = context.cfg().getCurrentBlock();
      CAstNode f = n.getChild(2);
      visitor.visit(f, context, visitor);
      if (isExpr)
        context.cfg().addInstruction(new AssignInstruction(getValue(n), getValue(f)));
    }

    // end
    context.cfg().newBlock(true);
    if (n.getChildCount() == 3) {
      if (trueB != null)
        context.cfg().addEdge(trueB, context.cfg().getCurrentBlock());
      context.cfg().addEdge(srcB, falseB);
    } else {
      context.cfg().addEdge(srcB, context.cfg().getCurrentBlock());
    }
  }

  // Make final to prevent overriding
  protected final void leaveIfStmtCondition(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveIfStmtTrueClause(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveIfStmt(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveIfExprCondition(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveIfExprTrueClause(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveIfExpr(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitIfStmt(CAstNode n, Context c, CAstVisitor visitor) {
    processIf(n, false, c, visitor);
    return true;
  }

  protected boolean visitIfExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    processIf(n, true, c, visitor);
    return true;
  }

  protected boolean visitNew(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveNew(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;

    int result = context.currentScope().allocateTempValue();
    setValue(n, result);

    int[] arguments;
    if (n.getChildCount() <= 1) {
      arguments = null;
    } else {
      arguments = new int[n.getChildCount() - 1];
      for (int i = 1; i < n.getChildCount(); i++) {
        arguments[i - 1] = getValue(n.getChild(i));
      }
    }
    doNewObject(context, n, result, n.getChild(0).getValue(), arguments);
  }

  protected boolean visitObjectLiteral(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChild(i).getKind() == CAstNode.EMPTY) {
      handleUnspecifiedLiteralKey(context, n, i, visitor);
    }
    doFieldWrite(context, getValue(n.getChild(0)), n.getChild(i), n, getValue(n.getChild(i + 1)));
  }

  protected void leaveObjectLiteral(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, getValue(n.getChild(0)));
  }

  protected boolean visitArrayLiteral(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveArrayLiteralObject(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, getValue(n.getChild(0)));
  }

  protected void leaveArrayLiteralInitElement(CAstNode n, int i, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    arrayOpHandler.doArrayWrite(context, getValue(n.getChild(0)), n, new int[] { context.currentScope().getConstantValue(new Integer(i - 1)) },
        getValue(n.getChild(i)));
  }

  protected void leaveArrayLiteral(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitObjectRef(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);
    return false;
  }

  protected void leaveObjectRef(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = getValue(n);
    CAstNode elt = n.getChild(1);
    doFieldRead(context, result, getValue(n.getChild(0)), elt, n);
  }

  public boolean visitAssign(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  public void leaveAssign(CAstNode n, Context c, CAstVisitor visitor) {
    if (n.getKind() == CAstNode.ASSIGN) {
      setValue(n, getValue(n.getChild(1)));
    } else {
      setValue(n, getValue(n.getChild(0)));
    }
  }

  private int[] gatherArrayDims(CAstNode n) {
    int numDims = n.getChildCount() - 2;
    int[] dims = new int[numDims];
    for (int i = 0; i < numDims; i++)
      dims[i] = getValue(n.getChild(i + 2));
    return dims;
  }

  /* Prereq: a.getKind() == ASSIGN_PRE_OP || a.getKind() == ASSIGN_POST_OP */
  protected int processAssignOp(CAstNode n, CAstNode v, CAstNode a, int temp, boolean post, Context c) {
    WalkContext context = (WalkContext) c;
    int rval = getValue(v);
    CAstNode op = a.getChild(2);
    int temp2 = context.currentScope().allocateTempValue();
    context.cfg().addInstruction(SSAInstructionFactory.BinaryOpInstruction(translateBinaryOpcode(op), temp2, temp, rval));
    return temp2;
  }

  protected boolean visitArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int rval = getValue(v);
    setValue(n, rval);
    arrayOpHandler.doArrayWrite(context, getValue(n.getChild(0)), n, gatherArrayDims(n), rval);
  }

  protected boolean visitArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int temp = context.currentScope().allocateTempValue();
    int[] dims = gatherArrayDims(n);
    arrayOpHandler.doArrayRead(context, temp, getValue(n.getChild(0)), n, dims);
    int rval = processAssignOp(n, v, a, temp, !pre, c);
    setValue(n, pre ? rval : temp);
    arrayOpHandler.doArrayWrite(context, getValue(n.getChild(0)), n, dims, rval);
  }

  protected boolean visitObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int rval = getValue(v);
    setValue(n, rval);
    doFieldWrite(context, getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  protected void processObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, Context c) {
  }

  protected boolean visitObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int temp = context.currentScope().allocateTempValue();
    doFieldRead(context, temp, getValue(n.getChild(0)), n.getChild(1), n);
    int rval = processAssignOp(n, v, a, temp, !pre, c);
    setValue(n, pre ? rval : temp);
    doFieldWrite(context, getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  protected boolean visitBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    setValue(n, getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) { /* empty */
    setValue(n, getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int rval = getValue(v);
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    setValue(n, rval);
    if (context.currentScope().isGlobal(ls))
      doGlobalWrite(context, nm, rval);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, rval);
    } else {
      Assertions._assert(rval != -1, CAstPrinter.print(n, c.top().getSourceMap()));
      doLocalWrite(context, nm, rval);
    }
  }

  protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    int temp;

    if (context.currentScope().isGlobal(ls))
      temp = doGlobalRead(context, nm);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      temp = doLexicallyScopedRead(context, nm);
    } else {
      temp = doLocalRead(context, nm);
    }

    if (!pre) {
      int ret = context.currentScope().allocateTempValue();
      context.cfg().addInstruction(new AssignInstruction(ret, temp));
      setValue(n, ret);
    }

    int rval = processAssignOp(n, v, a, temp, !pre, c);

    if (pre) {
      setValue(n, rval);
    }

    if (context.currentScope().isGlobal(ls)) {
      doGlobalWrite(context, nm, rval);
    } else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, rval);
    } else {
      doLocalWrite(context, nm, rval);
    }
  }

  private boolean isSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor visitor) {
    CAstControlFlowMap ctrl = context.getControlFlow();
    Collection caseLabels = ctrl.getTargetLabels(n);
    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();

      if (x == CAstControlFlowMap.SWITCH_DEFAULT)
        continue;

      CAstNode xn = (CAstNode) x;
      if (xn.getKind() == CAstNode.CONSTANT) {
        visitor.visit(xn, context, visitor);
        if (getValue(xn) != -1) {
          if (context.currentScope().isConstant(getValue(xn))) {
            Object val = context.currentScope().getConstantObject(getValue(xn));
            if (val instanceof Number) {
              Number num = (Number) val;
              if ((double) num.intValue() == num.doubleValue()) {
                continue;
              }
            }
          }
        }
      }

      return false;
    }

    return true;
  }

  private void doSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor visitor) {
    PreBasicBlock defaultHackBlock = null;
    CAstControlFlowMap ctrl = context.getControlFlow();

    CAstNode switchValue = n.getChild(0);
    visitor.visit(switchValue, context, visitor);
    int v = getValue(switchValue);

    boolean hasExplicitDefault = ctrl.getTarget(n, CAstControlFlowMap.SWITCH_DEFAULT) != null;

    Collection caseLabels = ctrl.getTargetLabels(n);
    int cases = caseLabels.size();
    if (hasExplicitDefault)
      cases--;
    int[] casesAndLabels = new int[cases * 2];

    int defaultBlock = context.cfg().getCurrentBlock().getGraphNodeId() + 1;

    context.cfg().addInstruction(SSAInstructionFactory.SwitchInstruction(v, defaultBlock, casesAndLabels));
    context.cfg().addPreNode(n, context.getUnwindState());
    // PreBasicBlock switchB = context.cfg().getCurrentBlock();
    context.cfg().newBlock(true);

    context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
    defaultHackBlock = context.cfg().getCurrentBlock();
    context.cfg().newBlock(false);

    CAstNode switchBody = n.getChild(1);
    visitor.visit(switchBody, context, visitor);
    context.cfg().newBlock(true);

    if (!hasExplicitDefault) {
      context.cfg().addEdge(defaultHackBlock, context.cfg().getCurrentBlock());
    }

    int cn = 0;
    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();
      CAstNode target = ctrl.getTarget(n, x);
      if (x == CAstControlFlowMap.SWITCH_DEFAULT) {
        context.cfg().addEdge(defaultHackBlock, context.cfg().getBlock(target));
      } else {
        Number caseLabel = (Number) context.currentScope().getConstantObject(getValue((CAstNode) x));
        casesAndLabels[2 * cn] = caseLabel.intValue();
        casesAndLabels[2 * cn + 1] = context.cfg().getBlock(target).getGraphNodeId();
        cn++;

        context.cfg().addPreEdge(n, target, false);
      }
    }
  }

  private void doIfConvertSwitch(CAstNode n, WalkContext context, CAstVisitor visitor) {
    CAstControlFlowMap ctrl = context.getControlFlow();
    context.cfg().addPreNode(n, context.getUnwindState());

    CAstNode switchValue = n.getChild(0);
    visitor.visit(switchValue, context, visitor);
    int v = getValue(switchValue);

    Collection caseLabels = ctrl.getTargetLabels(n);
    Map<Object, PreBasicBlock> labelToBlock = new LinkedHashMap<Object, PreBasicBlock>();
    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();
      if (x != CAstControlFlowMap.SWITCH_DEFAULT) {
        walkNodes((CAstNode) x, context);
        context.cfg().addInstruction(
            SSAInstructionFactory.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, v,
                getValue((CAstNode) x)));
        labelToBlock.put(x, context.cfg().getCurrentBlock());
        context.cfg().newBlock(true);
      }
    }

    PreBasicBlock defaultGotoBlock = context.cfg().getCurrentBlock();
    context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
    context.cfg().newBlock(false);

    CAstNode switchBody = n.getChild(1);
    visitor.visit(switchBody, context, visitor);
    context.cfg().newBlock(true);

    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();
      if (x != CAstControlFlowMap.SWITCH_DEFAULT) {
        CAstNode target = ctrl.getTarget(n, x);
        context.cfg().addEdge(labelToBlock.get(x), context.cfg().getBlock(target));
      }
    }

    if (ctrl.getTarget(n, CAstControlFlowMap.SWITCH_DEFAULT) == null) {
      context.cfg().addEdge(defaultGotoBlock, context.cfg().getCurrentBlock());
    } else {
      CAstNode target = ctrl.getTarget(n, CAstControlFlowMap.SWITCH_DEFAULT);
      context.cfg().addEdge(defaultGotoBlock, context.cfg().getBlock(target));
    }
  }

  protected boolean visitSwitch(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    if (isSimpleSwitch(n, context, visitor)) {
      doSimpleSwitch(n, context, visitor);
    } else {
      doIfConvertSwitch(n, context, visitor);
    }
    return true;
  }

  // Make final to prevent overriding
  protected final void leaveSwitchValue(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveSwitch(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitThrow(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveThrow(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    doThrow(context, getValue(n.getChild(0)));

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(false);

    Collection labels = context.getControlFlow().getTargetLabels(n);
    for (Iterator iter = labels.iterator(); iter.hasNext();) {
      Object label = iter.next();
      CAstNode target = context.getControlFlow().getTarget(n, label);
      if (target == CAstControlFlowMap.EXCEPTION_TO_EXIT)
        context.cfg().addPreEdgeToExit(n, true);
      else
        context.cfg().addPreEdge(n, target, true);
    }
  }

  protected boolean visitCatch(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;

    // unreachable catch block
    if (context.getControlFlow().getSourceNodes(n).isEmpty()) {
      return true;
    }

    String id = (String) n.getChild(0).getValue();
    context.cfg().setCurrentBlockAsHandler();
    if (!context.currentScope().contains(id)) {
      context.currentScope().declare(new FinalCAstSymbol(id));
    }
    context.cfg().addInstruction(
        SSAInstructionFactory.GetCaughtExceptionInstruction(context.cfg().getCurrentBlock().getNumber(), context.currentScope()
            .lookup(id).valueNumber()));

    context.cfg().addPreNode(n, context.getUnwindState());

    CAstType caughtType = getTypeForNode(context, n);
    if (caughtType != null) {
      TypeReference caughtRef = makeType(caughtType);
      context.setCatchType(n, caughtRef);
    } else {
      context.setCatchType(n, defaultCatchType());
    }

    return false;
  }

  protected void leaveCatch(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitUnwind(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveUnwind(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitTry(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    boolean addSkipCatchGoto = false;
    visitor.visit(n.getChild(0), context, visitor);
    PreBasicBlock endOfTry = context.cfg().getCurrentBlock();

    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      addSkipCatchGoto = true;
      ;
      context.cfg().addInstruction(SSAInstructionFactory.GotoInstruction());
      context.cfg().newBlock(false);
    }

    context.cfg().noteCatchBlock();
    visitor.visit(n.getChild(1), context, visitor);

    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().newBlock(true);
    }

    if (addSkipCatchGoto) {
      PreBasicBlock afterBlock = context.cfg().getCurrentBlock();
      context.cfg().addEdge(endOfTry, afterBlock);
    }
    return true;
  }

  // Make final to prevent overriding
  protected final void leaveTryBlock(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected final void leaveTry(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
  }

  protected boolean visitEmpty(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveEmpty(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    setValue(n, context.currentScope().getConstantValue(null));
  }

  protected boolean visitPrimitive(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leavePrimitive(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    setValue(n, result);

    doPrimitive(result, context, n);
  }

  protected boolean visitVoid(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveVoid(CAstNode n, Context c, CAstVisitor visitor) {
    setValue(n, -1);
  }

  protected boolean visitAssert(CAstNode n, Context c, CAstVisitor visitor) { /* empty */
    return false;
  }

  protected void leaveAssert(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext context = (WalkContext) c;
    boolean fromSpec = true;
    int result = getValue(n.getChild(0));
    if (n.getChildCount() == 2) {
      Assertions._assert(n.getChild(1).getKind() == CAstNode.CONSTANT);
      Assertions._assert(n.getChild(1).getValue() instanceof Boolean);
      fromSpec = n.getChild(1).getValue().equals(Boolean.TRUE);
    }
    context.cfg().addInstruction(new AstAssertInstruction(result, fromSpec));
  }

  protected boolean visitEachElementGet(CAstNode n, Context c, CAstVisitor visitor) {
    return false;
  }

  protected void leaveEachElementGet(CAstNode n, Context c, CAstVisitor visitor) {
    int result = ((WalkContext) c).currentScope().allocateTempValue();
    setValue(n, result);
    ((WalkContext) c).cfg().addInstruction(new EachElementGetInstruction(result, getValue(n.getChild(0))));
  }

  protected boolean visitEachElementHasNext(CAstNode n, Context c, CAstVisitor visitor) {
    return false;
  }

  protected void leaveEachElementHasNext(CAstNode n, Context c, CAstVisitor visitor) {
    int result = ((WalkContext) c).currentScope().allocateTempValue();
    setValue(n, result);
    ((WalkContext) c).cfg().addInstruction(new EachElementHasNextInstruction(result, getValue(n.getChild(0))));
  }

  protected boolean visitTypeLiteralExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return false;
  }

  protected void leaveTypeLiteralExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext wc = (WalkContext) c;
    Assertions._assert(n.getChild(0).getKind() == CAstNode.CONSTANT);
    String typeNameStr = (String) n.getChild(0).getValue();
    TypeName typeName = TypeName.string2TypeName(typeNameStr);
    TypeReference typeRef = TypeReference.findOrCreate(loader.getReference(), typeName);

    int result = wc.currentScope().allocateTempValue();
    setValue(n, result);

    wc.cfg().addInstruction(new SSALoadClassInstruction(result, typeRef));
  }

  protected boolean visitIsDefinedExpr(CAstNode n, Context c, CAstVisitor visitor) {
    return false;
  }

  protected void leaveIsDefinedExpr(CAstNode n, Context c, CAstVisitor visitor) {
    WalkContext wc = (WalkContext) c;
    int ref = getValue(n.getChild(0));
    int result = wc.currentScope().allocateTempValue();
    setValue(n, result);
    if (n.getChildCount() == 1) {
      wc.cfg().addInstruction(new AstIsDefinedInstruction(result, ref));
    } else {
      doIsFieldDefined(wc, result, ref, n.getChild(1));
    }
  }

  protected final void walkEntities(CAstEntity N, Context c) {
    visitEntities(N, c, this);
  }

  protected final void walkNodes(CAstNode N, Context c) {
    visit(N, c, this);
  }

  public static final class DefaultContext implements WalkContext {
    private final AstTranslator t;

    private final CAstEntity N;

    private final String nm;

    public DefaultContext(AstTranslator t, CAstEntity N, String nm) {
      this.t = t;
      this.N = N;
      this.nm = nm;
    }

    public String file() {
      return nm;
    }

    public CAstEntity top() {
      return N;
    }

    public Scope currentScope() {
      return t.globalScope;
    }

    public Set<Scope> entityScopes() {
      return Collections.singleton(t.globalScope);
    }

    public CAstSourcePositionMap getSourceMap() {
      return N.getSourceMap();
    }

    public CAstControlFlowMap getControlFlow() {
      return N.getControlFlow();
    }

    public IncipientCFG cfg() {
      return null;
    }

    public UnwindState getUnwindState() {
      return null;
    }

    public String getName() {
      return null;
    }

    public void setCatchType(int blockNumber, TypeReference catchType) {
    }

    public void setCatchType(CAstNode castNode, TypeReference catchType) {
    }

    public TypeReference[][] getCatchTypes() {
      return null;
    }
  };

  public void translate(final CAstEntity N, final String nm) {
    if (DEBUG_TOP)
      Trace.println("translating " + nm);
    walkEntities(N, new DefaultContext(this, N, nm));
  }

  private final Scope globalScope = makeGlobalScope();

  protected Scope getGlobalScope() {
    return globalScope;
  }

}
