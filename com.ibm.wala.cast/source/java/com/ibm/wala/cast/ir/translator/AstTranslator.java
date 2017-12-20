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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.ssa.SSAConversion;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstSymbolImplBase;
import com.ibm.wala.cast.tree.rewrite.CAstCloner;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;

/**
 * Common code to translate CAst to IR. Must be specialized by each language to
 * handle semantics appropriately.
 */
public abstract class AstTranslator extends CAstVisitor<AstTranslator.WalkContext> implements ArrayOpHandler, TranslatorToIR {

  /**
   * does the language care about using type-appropriate default values? For
   * Java, the answer is yes (ints should get a default value of 0, null for
   * pointers, etc.). For JavaScript, the answer is no, as any variable can hold
   * the value 'undefined'.
   */
  protected abstract boolean useDefaultInitValues();

  /**
   * can lexical reads / writes access globals?
   */
  protected abstract boolean treatGlobalsAsLexicallyScoped();

  protected boolean topLevelFunctionsInGlobalScope() {
    return true;
  }

  /**
   * for a block that catches all exceptions, what is the root exception type
   * that it can catch? E.g., for Java, java.lang.Throwable
   */
  protected abstract TypeReference defaultCatchType();

  protected abstract TypeReference makeType(CAstType type);

  /**
   * define a new (presumably nested) type. return true if type was successfully
   * defined, false otherwise
   */
  protected abstract boolean defineType(CAstEntity type, WalkContext wc);

  /**
   * declare a new function, represented by N
   */
  protected abstract void declareFunction(CAstEntity N, WalkContext context);

  /**
   * fully define a function. invoked after all the code of the function has
   * been processed
   */
  protected abstract void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG<SSAInstruction, ? extends IBasicBlock<SSAInstruction>> cfg, SymbolTable symtab,
      boolean hasCatchBlock, Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo,
      DebuggingInformation debugInfo);

  /**
   * define a new field fieldEntity within topEntity
   */
  protected abstract void defineField(CAstEntity topEntity, WalkContext context, CAstEntity fieldEntity);

  /**
   * create the language-appropriate name for f
   */
  protected abstract String composeEntityName(WalkContext parent, CAstEntity f);

  /**
   * generate IR for a CAst throw expression, updating context.cfg()
   */
  protected abstract void doThrow(WalkContext context, int exception);

  /**
   * generate IR for a CAst array read, updating context.cfg()
   */
  @Override
  public abstract void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues);

  /**
   * generate IR for a CAst array write, updating context.cfg()
   */
  @Override
  public abstract void doArrayWrite(WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval);

  /**
   * generate IR for a CAst field read, updating context.cfg()
   */
  protected abstract void doFieldRead(WalkContext context, int result, int receiver, CAstNode elt, CAstNode parent);

  /**
   * generate IR for a CAst field write, updating context.cfg()
   */
  protected abstract void doFieldWrite(WalkContext context, int receiver, CAstNode elt, CAstNode parent, int rval);

  /**
   * generate IR for a CAst function expression, updating context.cfg()
   */
  protected abstract void doMaterializeFunction(CAstNode node, WalkContext context, int result, int exception, CAstEntity fn);

  /**
   * generate IR for a CAst new expression, updating context.cfg()
   */
  protected abstract void doNewObject(WalkContext context, CAstNode newNode, int result, Object type, int[] arguments);

  /**
   * generate IR for a CAst method call expression, updating context.cfg()
   */
  protected abstract void doCall(WalkContext context, CAstNode call, int result, int exception, CAstNode name, int receiver,
      int[] arguments);

  /**
   * the most-general type for the language being translated
   */
  protected abstract CAstType topType();
  
  /**
   * the most-general exception type for the language being translated
   */
  protected abstract CAstType exceptionType();
  
  /**
   * used to generate instructions for array operations; defaults to this
   */
  private ArrayOpHandler arrayOpHandler;

  protected boolean isExceptionLabel(Object label) {
    if (label == null)
      return false;
    if (label instanceof Boolean)
      return false;
    if (label instanceof Number)
      return false;
    if (label == CAstControlFlowMap.SWITCH_DEFAULT)
      return false;
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

  /**
   * some languages let you omit initialization of certain fields when writing
   * an object literal (e.g., PHP). This method should be overridden to handle
   * such cases.
   */
  protected void handleUnspecifiedLiteralKey() {
    Assertions.UNREACHABLE();
  }

  /**
   * generate prologue code for each function body
   */
  protected void doPrologue(WalkContext context) {
    // perform a lexical write to copy the value stored in the local
    // associated with each parameter to the lexical name
    final CAstEntity entity = context.top();
    Set<String> exposedNames = entity2ExposedNames.get(entity);
    if (exposedNames != null) {
      int i = 0;
      for (String arg : entity.getArgumentNames()) {
        if (exposedNames.contains(arg)) {
          final Scope currentScope = context.currentScope();
          Symbol symbol = currentScope.lookup(arg);
          assert symbol.getDefiningScope() == currentScope;
          int argVN = symbol.valueNumber();
          CAstType type = (entity.getType() instanceof CAstType.Method)?
            (CAstType)((CAstType.Method)entity.getType()).getArgumentTypes().get(i):
            topType();
          Access A = new Access(arg, context.getEntityName(entity), makeType(type), argVN);
          context.cfg().addInstruction(new AstLexicalWrite(context.cfg().currentInstruction, A));
        }
      }
    }
  }

  /**
   * generate IR for call modeling creation of primitive value, updating
   * context.cfg()
   */
  protected abstract void doPrimitive(int resultVal, WalkContext context, CAstNode primitiveCall);

  /**
   * get the value number for a name defined locally (i.e., within the current
   * method) by looking up the name in context.currentScope(). Note that the
   * caller is responsible for ensuring that name is defined in the local scope.
   */
  protected int doLocalRead(WalkContext context, String name, TypeReference type) {
    CAstEntity entity = context.top();
    Set<String> exposed = entity2ExposedNames.get(entity);
    if (exposed != null && exposed.contains(name)) {
      return doLexReadHelper(context, name, type);
    }
    return context.currentScope().lookup(name).valueNumber();
  }

  /**
   * add an {@link AssignInstruction} to context.cfg() that copies rval to the
   * value number of local nm. Note that the caller is responsible for ensuring
   * that nm is defined in the local scope.
   */
  protected void doLocalWrite(WalkContext context, String nm, TypeReference type, int rval) {
    CAstEntity entity = context.top();
    Set<String> exposed = entity2ExposedNames.get(entity);
    if (exposed != null && exposed.contains(nm)) {
      // use a lexical write
      doLexicallyScopedWrite(context, nm, type, rval);
      return;
    }
    int lval = context.currentScope().lookup(nm).valueNumber();
    if (lval != rval) {
      context.cfg().addInstruction(new AssignInstruction(context.cfg().currentInstruction, lval, rval));
    }
  }

  /**
   * Note that the caller is responsible for ensuring that name is defined in a
   * lexical scope.
   * 
   * @param node
   *          the AST node representing the read
   * @param context
   * @param name
   */
  protected int doLexicallyScopedRead(CAstNode node, WalkContext context, final String name, TypeReference type) {
    return doLexReadHelper(context, name, type);
  }

  /**
   * @param name A variable name
   * @return is this name safe to overwrite, i.e. it's synthetic from the translator?
   */
  protected boolean ignoreName(String name) {
    return false;
  }

  /**
   * we only have this method to avoid having to pass a node parameter at other
   * call sites, as would be required for
   * {@link #doLexicallyScopedRead(CAstNode, WalkContext, String)}
   */
  private static int doLexReadHelper(WalkContext context, final String name, TypeReference type) {
    Symbol S = context.currentScope().lookup(name);
    Scope definingScope = S.getDefiningScope();
    CAstEntity E = definingScope.getEntity();
    // record in declaring scope that the name is exposed to a nested scope
    addExposedName(E, E, name, definingScope.lookup(name).valueNumber(), false, context);
    final String entityName = context.getEntityName(E);
    int result = context.currentScope().allocateTempValue();
    Access A = new Access(name, entityName, type, result);
    context.cfg().addInstruction(new AstLexicalRead(context.cfg().currentInstruction, A));
    markExposedInEnclosingEntities(context, name, definingScope, type, E, entityName, false);
    return result;
  }

  /**
   * record name as exposed for the current entity and for all enclosing
   * entities up to that of the defining scope, since if the name is updated via
   * a call to a nested function, SSA for these entities may need to be updated
   * with the new definition
   * 
   * @param context
   * @param name
   * @param definingScope
   * @param E
   * @param entityName
   * @param isWrite
   */
  private static void markExposedInEnclosingEntities(WalkContext context, final String name, Scope definingScope, TypeReference type, CAstEntity E,
      final String entityName, boolean isWrite) {
    Scope curScope = context.currentScope();
    while (!curScope.equals(definingScope)) {
      final Symbol curSymbol = curScope.lookup(name);
      final int vn = curSymbol.valueNumber();
      final Access A = new Access(name, entityName, type, vn);
      final CAstEntity entity = curScope.getEntity();
      if (entity != definingScope.getEntity()) {
        addExposedName(entity, E, name, vn, isWrite, context);
        // record the access; later, the Accesses in the instruction
        // defining vn will be adjusted based on this information; see
        // patchLexicalAccesses()
        addAccess(context, entity, A);
      }
      curScope = curScope.getParent();
    }
  }

  /**
   * Note that the caller is responsible for ensuring that name is defined in a
   * lexical scope.
   * 
   */
  protected void doLexicallyScopedWrite(WalkContext context, String name, TypeReference type, int rval) {
    Symbol S = context.currentScope().lookup(name);
    Scope definingScope = S.getDefiningScope();
    CAstEntity E = definingScope.getEntity();
    // record in declaring scope that the name is exposed to a nested scope
    addExposedName(E, E, name, definingScope.lookup(name).valueNumber(), true, context);

    // lexically-scoped variables must be written in their scope each time
    Access A = new Access(name, context.getEntityName(E), type, rval);
    context.cfg().addInstruction(new AstLexicalWrite(context.cfg().currentInstruction, A));
    markExposedInEnclosingEntities(context, name, definingScope, type, E, context.getEntityName(E), true);
  }

  /**
   * generate instructions for a read of a global
   */
  protected int doGlobalRead(@SuppressWarnings("unused") CAstNode node, WalkContext context, String name, TypeReference type) {
    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      int result = context.currentScope().allocateTempValue();
      Access A = new Access(name, null, type, result);
      context.cfg().addInstruction(new AstLexicalRead(context.cfg().currentInstruction, A));
      addAccess(context, context.top(), A);
      return result;

      // globals can be treated as a single static location
    } else {
      int result = context.currentScope().allocateTempValue();
      FieldReference global = makeGlobalRef(name);
      context.cfg().addInstruction(new AstGlobalRead(context.cfg().currentInstruction, result, global));
      return result;
    }
  }

  /**
   * generate instructions for a write of a global
   */
  protected void doGlobalWrite(WalkContext context, String name, TypeReference type, int rval) {

    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      Access A = new Access(name, null, type, rval);
      context.cfg().addInstruction(new AstLexicalWrite(context.cfg().currentInstruction, A));
      addAccess(context, context.top(), A);

      // globals can be treated as a single static location
    } else {
      FieldReference global = makeGlobalRef(name);
      context.cfg().addInstruction(new AstGlobalWrite(context.cfg().currentInstruction, global, rval));
    }
  }

  /**
   * generate instructions to check if ref has field, storing answer in result
   */
  @SuppressWarnings("unused")
  protected void doIsFieldDefined(WalkContext context, int result, int ref, CAstNode field) {
    Assertions.UNREACHABLE();
  }

  /**
   * creates a reference to a global named globalName. the declaring type and
   * type of the global are both the root type.
   */
  protected FieldReference makeGlobalRef(String globalName) {
    TypeReference rootTypeRef = TypeReference.findOrCreate(loader.getReference(), AstTypeReference.rootTypeName);
    return FieldReference.findOrCreate(rootTypeRef, Atom.findOrCreateUnicodeAtom("global " + globalName), rootTypeRef);
  }

  protected final IClassLoader loader;

  /**
   * for handling languages that let you include other source files named
   * statically (e.g., ABAP)
   */
  protected final Map<Object, CAstEntity> namedEntityResolver;

  protected final SSAInstructionFactory insts;

  protected AstTranslator(IClassLoader loader, Map<Object, CAstEntity> namedEntityResolver, ArrayOpHandler arrayOpHandler) {
    this.loader = loader;
    this.namedEntityResolver = namedEntityResolver;
    this.arrayOpHandler = arrayOpHandler!=null? arrayOpHandler: this;
    this.insts = loader.getInstructionFactory();
  }

  protected AstTranslator(IClassLoader loader, Map<Object, CAstEntity> namedEntityResolver) {
    this(loader, namedEntityResolver, null);
  }
  
  protected AstTranslator(IClassLoader loader) {
    this(loader, null);
  }

  /**
   * for keeping position information for the generated SSAInstructions and SSA
   * locals
   */
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

    @Override
    public Position getCodeBodyPosition() {
      return codeBodyPosition;
    }

    @Override
    public Position getInstructionPosition(int instructionOffset) {
      return instructionPositions[instructionOffset];
    }

    @Override
    public String[][] getSourceNamesForValues() {
      return valueNumberNames;
    }
  }

  public static final boolean DEBUG_ALL = false;

  public static final boolean DEBUG_TOP = DEBUG_ALL || false;

  public static final boolean DEBUG_CFG = DEBUG_ALL || false;

  public static final boolean DEBUG_NAMES = DEBUG_ALL || false;

  public static final boolean DEBUG_LEXICAL = DEBUG_ALL || false;

  /**
   * basic block implementation used in the CFGs constructed during the
   * IR-generating AST traversal
   */
  protected final static class PreBasicBlock implements IBasicBlock<SSAInstruction> {
    private static final int NORMAL = 0;

    private static final int HANDLER = 1;

    private static final int ENTRY = 2;

    private static final int EXIT = 3;

    private int kind = NORMAL;

    private int number = -1;

    private int firstIndex = -1;

    private int lastIndex = -2;

    private final List<SSAInstruction> instructions = new ArrayList<>();

    @Override
    public int getNumber() {
      return getGraphNodeId();
    }

    @Override
    public int getGraphNodeId() {
      return number;
    }

    @Override
    public void setGraphNodeId(int number) {
      this.number = number;
    }

    @Override
    public int getFirstInstructionIndex() {
      return firstIndex;
    }

    void setFirstIndex(int firstIndex) {
      this.firstIndex = firstIndex;
    }

    @Override
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

    @Override
    public boolean isEntryBlock() {
      return kind == ENTRY;
    }

    @Override
    public boolean isExitBlock() {
      return kind == EXIT;
    }

    public boolean isHandlerBlock() {
      return kind == HANDLER;
    }

    @Override
    public String toString() {
      return "PreBB" + number + ":" + firstIndex + ".." + lastIndex;
    }

    private List<SSAInstruction> instructions() {
      return instructions;
    }

    @Override
    public boolean isCatchBlock() {
      return (lastIndex > -1) && (instructions.get(0) instanceof SSAGetCaughtExceptionInstruction);
    }

    @Override
    public IMethod getMethod() {
      return null;
    }

    @Override
    public Iterator<SSAInstruction> iterator() {
      return instructions.iterator();
    }
  }

  protected final class UnwindState {
    final CAstNode unwindAst;

    final WalkContext astContext;

    final CAstVisitor<WalkContext> astVisitor;

    UnwindState(CAstNode unwindAst, WalkContext astContext, CAstVisitor<WalkContext> astVisitor) {
      this.unwindAst = unwindAst;
      this.astContext = astContext;
      this.astVisitor = astVisitor;
    }

    public UnwindState getParent() {
      return astContext.getUnwindState();
    }

    @Override
    public int hashCode() {
      return astContext.hashCode() * unwindAst.hashCode() * astVisitor.hashCode();
    }

    @Override
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

  /**
   * holds the control-flow graph as it is being constructed. When construction
   * is complete, information is stored in an {@link AstCFG}
   */
  public final class IncipientCFG extends SparseNumberedGraph<PreBasicBlock> {

    protected class Unwind {
      private final Map<PreBasicBlock, UnwindState> unwindData = new LinkedHashMap<>();

      /**
       * a cache of generated blocks
       */
      private final Map<Pair<UnwindState, Pair<PreBasicBlock, Boolean>>, PreBasicBlock> code = new LinkedHashMap<>();

      void setUnwindState(PreBasicBlock block, UnwindState context) {
        unwindData.put(block, context);
      }

      void setUnwindState(CAstNode node, UnwindState context) {
        unwindData.put(nodeToBlock.get(node), context);
      }

      /**
       * When adding an edge from source to target, it is possible that certain
       * exception-handling code needs to be executed before the control is
       * actually transfered to target. This method determines if this is the
       * case, and if so, it generates the exception handler blocks and adds an
       * appropriate edge to the target. It returns the basic block that should
       * be the target of the edge from source (target itself if there is no
       * exception-handling code, the initial catch block otherwise)
       */
      public PreBasicBlock findOrCreateCode(PreBasicBlock source, PreBasicBlock target, final boolean exception) {
        UnwindState sourceContext = unwindData.get(source);
        final CAstNode dummy = exception ? (new CAstImpl()).makeNode(CAstNode.EMPTY) : null;

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

        Pair<UnwindState, Pair<PreBasicBlock, Boolean>> key = Pair.make(sourceContext, Pair.make(target, exception));

        if (code.containsKey(key)) {
          return code.get(key);

        } else {
          int e = -1;
          PreBasicBlock currentBlock = getCurrentBlock();
          if (!isDeadBlock(currentBlock)) {
            addInstruction(insts.GotoInstruction(currentInstruction, -1));
            newBlock(false);
          }
          PreBasicBlock startBlock = getCurrentBlock();
          if (exception) {
            setCurrentBlockAsHandler();
            e = sourceContext.astContext.currentScope().allocateTempValue();
            addInstruction(insts.GetCaughtExceptionInstruction(currentInstruction, startBlock.getNumber(), e));
            sourceContext.astContext.setCatchType(startBlock, defaultCatchType());
          }

          while (sourceContext != null && (targetContext == null || !targetContext.covers(sourceContext))) {
            final CAstRewriter.Rewrite ast = (new CAstCloner(new CAstImpl()) {
              @Override
              protected CAstNode flowOutTo(Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap, CAstNode oldSource, Object label,
                  CAstNode oldTarget, CAstControlFlowMap orig, CAstSourcePositionMap src) {
                if (exception && !isExceptionLabel(label)) {
                  return dummy;
                } else {
                  return oldTarget;
                }
              }
            }).copy(sourceContext.unwindAst, sourceContext.astContext.getControlFlow(), sourceContext.astContext.getSourceMap(),
                sourceContext.astContext.top().getNodeTypeMap(), sourceContext.astContext.top().getAllScopedEntities());
            sourceContext.astVisitor.visit(ast.newRoot(), new DelegatingContext(sourceContext.astContext) {
              @Override
              public CAstSourcePositionMap getSourceMap() {
                return ast.newPos();
              }

              @Override
              public CAstControlFlowMap getControlFlow() {
                return ast.newCfg();
              }
            }, sourceContext.astVisitor);

            sourceContext = sourceContext.getParent();
          }

          PreBasicBlock endBlock = getCurrentBlock();
          if (exception) {
            addPreNode(dummy);
            doThrow(astContext, e);
          } else {
            addInstruction(insts.GotoInstruction(currentInstruction, -1));
          }
          newBlock(false);

          if (target != null) {
            addEdge(currentBlock, getCurrentBlock());
            addEdge(endBlock, target);

            // `null' target is idiom for branch/throw to exit
          } else {
            if (exception) addEdge(currentBlock, getCurrentBlock());
            addDelayedEdge(endBlock, exitMarker, exception);
          }

          code.put(key, startBlock);
          return startBlock;
        }
      }
    }

    private Unwind unwind = null;

    private final List<PreBasicBlock> blocks = new ArrayList<>();

    private PreBasicBlock entryBlock;
    
    private final Map<CAstNode, PreBasicBlock> nodeToBlock = new LinkedHashMap<>();

    private final Map<Object, Set<Pair<PreBasicBlock, Boolean>>> delayedEdges = new LinkedHashMap<>();

    private final Object exitMarker = new Object();

    private final Set<PreBasicBlock> deadBlocks = new LinkedHashSet<>();

    private final Set<PreBasicBlock> normalToExit = new LinkedHashSet<>();

    private final Set<PreBasicBlock> exceptionalToExit = new LinkedHashSet<>();

    private Position[] linePositions = new Position[10];

    private boolean hasCatchBlock = false;

    /**
     * does the method have any monitor operations?
     */
    private boolean hasMonitorOp = false;

    private int currentInstruction = 0;

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

    boolean hasMonitorOp() {
      return hasMonitorOp;
    }

    void noteCatchBlock() {
      hasCatchBlock = true;
    }

    Position[] getLinePositionMap() {
      return linePositions;
    }

    /**
     * create a new basic block, and set it as the current block.
     * 
     * @param fallThruFromPrior
     *          should a fall-through edge be added from the previous block
     *          (value of currentBlock at entry)? if false, the newly created
     *          block is marked as a dead block, as it has no incoming edges.
     * @return the new block
     */
    public PreBasicBlock newBlock(boolean fallThruFromPrior) {
      // optimization: if we have a fall-through from an empty block, just
      // return the empty block
      if (fallThruFromPrior && !currentBlock.isEntryBlock() && currentBlock.instructions().size() == 0) {
        return currentBlock;
      }

      PreBasicBlock previous = currentBlock;
      currentBlock = new PreBasicBlock();
      addNode(currentBlock);
      blocks.add(currentBlock);

      if (DEBUG_CFG)
        System.err.println(("adding new block (node) " + currentBlock));
      if (fallThruFromPrior) {
        if (DEBUG_CFG)
          System.err.println(("adding fall-thru edge " + previous + " --> " + currentBlock));
        addEdge(previous, currentBlock);
      } else {
        deadBlocks.add(currentBlock);
      }

      return currentBlock;
    }

    /**
     * record a delayed edge addition from src to dst. Edge will be added when
     * appropriate; see {@link #checkForRealizedEdges(CAstNode)} and
     * {@link #checkForRealizedExitEdges(PreBasicBlock)}
     */
    private void addDelayedEdge(PreBasicBlock src, Object dst, boolean exception) {
      MapUtil.findOrCreateSet(delayedEdges, dst).add(Pair.make(src, exception));
    }

    void makeEntryBlock(PreBasicBlock bb) {
      entryBlock = bb;
      bb.makeEntryBlock();
    }

    void makeExitBlock(PreBasicBlock bb) {
      bb.makeExitBlock();

      for (PreBasicBlock p : Iterator2Iterable.make(getPredNodes(bb)))
        normalToExit.add(p);

      // now that we have created the exit block, add the delayed edges to the
      // exit
      checkForRealizedExitEdges(bb);
    }

    public void setCurrentBlockAsHandler() {
      currentBlock.makeHandlerBlock();
    }

    boolean hasDelayedEdges(CAstNode n) {
      return delayedEdges.containsKey(n);
    }

    /**
     * given some n which is now mapped by nodeToBlock, add any delayed edges to
     * n's block
     */
    private void checkForRealizedEdges(CAstNode n) {
      if (delayedEdges.containsKey(n)) {
        for (Pair<PreBasicBlock, Boolean> s : delayedEdges.get(n)) {
          PreBasicBlock src = s.fst;
          boolean exception = s.snd;
          if (unwind == null) {
            addEdge(src, nodeToBlock.get(n));
          } else {
            PreBasicBlock target = nodeToBlock.get(n);
            addEdge(src, unwind.findOrCreateCode(src, target, exception));
          }
        }

        delayedEdges.remove(n);
      }
    }

    /**
     * add any delayed edges to the exit block
     */
    private void checkForRealizedExitEdges(PreBasicBlock exitBlock) {
      if (delayedEdges.containsKey(exitMarker)) {
        for (Pair<PreBasicBlock, Boolean> s : delayedEdges.get(exitMarker)) {
          PreBasicBlock src = s.fst;
          boolean exception = s.snd;
          addEdge(src, exitBlock);
          if (exception)
            exceptionalToExit.add(src);
          else
            normalToExit.add(src);
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

    /**
     * associate n with the current block, and update the current unwind state
     */
    public void addPreNode(CAstNode n, UnwindState context) {
      if (DEBUG_CFG)
        System.err.println(("adding pre-node " + n));
      nodeToBlock.put(n, currentBlock);
      deadBlocks.remove(currentBlock);
      if (context != null)
        setUnwindState(n, context);
      // now that we've associated n with a block, add associated delayed edges
      checkForRealizedEdges(n);
    }

    public void addPreEdge(CAstNode src, CAstNode dst, boolean exception) {
      assert nodeToBlock.containsKey(src);
      addPreEdge(nodeToBlock.get(src), dst, exception);
    }

    /**
     * if dst is associated with a basic block b, add an edge from src to b.
     * otherwise, record the edge addition as delayed.
     */
    public void addPreEdge(PreBasicBlock src, CAstNode dst, boolean exception) {
      if (dst == CAstControlFlowMap.EXCEPTION_TO_EXIT) {
        assert exception;
        addPreEdgeToExit(src, exception);
      } else if (nodeToBlock.containsKey(dst)) {
        PreBasicBlock target = nodeToBlock.get(dst);
        if (DEBUG_CFG)
          System.err.println(("adding pre-edge " + src + " --> " + dst));
        if (unwind == null) {
          addEdge(src, target);
        } else {
          addEdge(src, unwind.findOrCreateCode(src, target, exception));
        }
      } else {
        if (DEBUG_CFG)
          System.err.println(("adding delayed pre-edge " + src + " --> " + dst));
        addDelayedEdge(src, dst, exception);
      }
    }

    public void addPreEdgeToExit(CAstNode src, boolean exception) {
      assert nodeToBlock.containsKey(src);
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

    @Override
    public void addEdge(PreBasicBlock src, PreBasicBlock dst) {
      super.addEdge(src, dst);
      deadBlocks.remove(dst);
    }

    public boolean isDeadBlock(PreBasicBlock block) {
      return deadBlocks.contains(block);
    }

    public PreBasicBlock getBlock(CAstNode n) {
      return nodeToBlock.get(n);
    }

    /**
     * mark the current position as the position for the instruction
     */
    private void noteLinePosition(int instruction) {
      if (linePositions.length < (instruction + 1)) {
        Position[] newData = new Position[instruction * 2 + 1];
        System.arraycopy(linePositions, 0, newData, 0, linePositions.length);
        linePositions = newData;
      }

      linePositions[instruction] = getCurrentPosition();
    }

    public void addInstruction(SSAInstruction n) {
      deadBlocks.remove(currentBlock);

      int inst = currentInstruction++;

      noteLinePosition(inst);

      if (currentBlock.instructions().size() == 0) {
        currentBlock.setFirstIndex(inst);
      } else {
        for(SSAInstruction priorInst : currentBlock.instructions()) {
          assert ! (priorInst instanceof SSAGotoInstruction);
        }
        assert !(n instanceof SSAGetCaughtExceptionInstruction);
      }

      if (DEBUG_CFG) {
        System.err.println(("adding " + n + " at " + inst + " to " + currentBlock));
      }

      if (n instanceof SSAMonitorInstruction) {
        hasMonitorOp = true;
      }

      currentBlock.instructions().add(n);

      currentBlock.setLastIndex(inst);
    }
    
    @Override
    public String toString() { 
      StringBuffer sb = new StringBuffer(super.toString());
      for(PreBasicBlock b : blocks) {
        if (b.firstIndex > 0) {
          sb.append("\n" + b);
          for(int i = 0; i < b.instructions.size(); i++) {
            sb.append("\n" + b.instructions.get(i));
          }
        }
      }
      return sb.toString();
    }
  }

  /**
   * data structure for the final CFG for a method, based on the information in
   * an {@link IncipientCFG}
   */
  protected final static class AstCFG extends AbstractCFG<SSAInstruction, PreBasicBlock> {
    private final SSAInstruction[] instructions;

    private final int[] instructionToBlockMap;

    private final int[] pcMap;
    
    private final String functionName;

    private final SymbolTable symtab;

    private interface EdgeOperation {
      void act(PreBasicBlock src, PreBasicBlock dst);
    }
    
    private void transferEdges(Set<PreBasicBlock> blocks, 
                                      IncipientCFG icfg,
                                      EdgeOperation normal,
                                      EdgeOperation except) {
      for (PreBasicBlock src : blocks) {
        for (PreBasicBlock dst : Iterator2Iterable.make(icfg.getSuccNodes(src))) {
          if (isCatchBlock(dst.getNumber()) || (dst.isExitBlock() && icfg.exceptionalToExit.contains(src))) {
            except.act(src, dst);
          }

          if (dst.isExitBlock() ? icfg.normalToExit.contains(src) : !isCatchBlock(dst.getNumber())) {
            normal.act(src, dst);
          }
        }
      }
    }
    
    private static boolean checkBlockBoundaries(IncipientCFG icfg) {
      MutableIntSet boundaries = IntSetUtil.make();
      for(PreBasicBlock b : icfg) {
        if (b.getFirstInstructionIndex() >= 0) {
          if (boundaries.contains(b.getFirstInstructionIndex()))  {
            return false;
          }
          boundaries.add(b.getFirstInstructionIndex());
        }
        if (b.getLastInstructionIndex() >= 0 && b.getLastInstructionIndex() != b.getFirstInstructionIndex()) {
          if (boundaries.contains(b.getLastInstructionIndex())) {
            return false;            
          }
          boundaries.add(b.getLastInstructionIndex());
        }
      }
      return true;
    }
    
    AstCFG(CAstEntity n, IncipientCFG icfg, SymbolTable symtab, SSAInstructionFactory insts) {
      super(null);
      
      Set<PreBasicBlock> liveBlocks = DFS.getReachableNodes(icfg, Collections.singleton(icfg.entryBlock));
      List<PreBasicBlock> blocks = icfg.blocks;
      boolean hasDeadBlocks = blocks.size() > liveBlocks.size();
      
      assert checkBlockBoundaries(icfg);
      
      this.symtab = symtab;
      functionName = n.getName();
      instructionToBlockMap = new int[liveBlocks.size()];
      pcMap = hasDeadBlocks? new int[ icfg.currentInstruction ]: null;
      
      final Map<PreBasicBlock, Collection<PreBasicBlock>> normalEdges = 
        hasDeadBlocks? HashMapFactory.<PreBasicBlock,Collection<PreBasicBlock>>make() : null;
      final Map<PreBasicBlock, Collection<PreBasicBlock>> exceptionalEdges = 
        hasDeadBlocks? HashMapFactory.<PreBasicBlock,Collection<PreBasicBlock>>make() : null;
      if (hasDeadBlocks) {
        transferEdges(liveBlocks, icfg, (src, dst) -> {
          if (! normalEdges.containsKey(src)) {
            normalEdges.put(src, HashSetFactory.<PreBasicBlock>make());
          }
          normalEdges.get(src).add(dst);
        }, (src, dst) -> {
          if (! exceptionalEdges.containsKey(src)) {
            exceptionalEdges.put(src, HashSetFactory.<PreBasicBlock>make());
          }
          exceptionalEdges.get(src).add(dst);
        });
      }
      
      int instruction = 0;
      for (int i = 0, blockNumber = 0; i < blocks.size(); i++) {
        PreBasicBlock block = blocks.get(i);
        block.setGraphNodeId(-1);
        if (liveBlocks.contains(block)) {
          if (hasDeadBlocks) {
            int offset = 0;
            for(int oldPC = block.getFirstInstructionIndex(); 
                offset < block.instructions().size();
                oldPC++, offset++) {
              pcMap[instruction + offset] = oldPC;
            }
          }
          if (block.getFirstInstructionIndex() >= 0) {
            block.setFirstIndex(instruction);
            block.setLastIndex((instruction += block.instructions().size()) - 1);
          }
          instructionToBlockMap[blockNumber] = block.getLastInstructionIndex();

          this.addNode(block);
          if (block.isCatchBlock()) {
            setCatchBlock(blockNumber);
          }

          if (DEBUG_CFG) {
            System.err.println(("added " + blocks.get(i) + " to final CFG as " + getNumber(blocks.get(i))));
          } 
          
          blockNumber++;
        }
      }
      if (DEBUG_CFG)
        System.err.println((getMaxNumber() + " blocks total"));

      init();

      if (hasDeadBlocks) {
        for (int i = 0; i < blocks.size(); i++) {
          PreBasicBlock src = blocks.get(i);
          if (liveBlocks.contains(src)) {
            if (normalEdges.containsKey(src)) {
              for(PreBasicBlock succ : normalEdges.get(src)) {
                addNormalEdge(src, succ);
              }
            }
            if (exceptionalEdges.containsKey(src)) {
              for(PreBasicBlock succ : exceptionalEdges.get(src)) {
                addExceptionalEdge(src, succ);
              }
            }
          }
        }
      } else {
        transferEdges(liveBlocks, icfg, this::addNormalEdge, this::addExceptionalEdge);
      }
      
      int x = 0;
      instructions = new SSAInstruction[icfg.currentInstruction];
      for (int i = 0; i < blocks.size(); i++) {
        if (liveBlocks.contains(blocks.get(i))) {
          List<SSAInstruction> bi = blocks.get(i).instructions();
          for (int j = 0; j < bi.size(); j++) {
            SSAInstruction inst = bi.get(j);
            if (inst instanceof SSAGetCaughtExceptionInstruction) {
              SSAGetCaughtExceptionInstruction ci = (SSAGetCaughtExceptionInstruction) inst;
              if (ci.getBasicBlockNumber() != blocks.get(i).getNumber()) {
                inst = insts.GetCaughtExceptionInstruction(x, blocks.get(i).getNumber(), ci.getException());
              }
            } else if (inst instanceof SSAGotoInstruction) {
              Iterator<PreBasicBlock> succs = this.getNormalSuccessors(blocks.get(i)).iterator();
              if (succs.hasNext()) {
                PreBasicBlock target = succs.next();
                assert !succs.hasNext() : "unexpected successors for block " + blocks.get(i) + ": " + target + " and " + succs.next();
                inst = insts.GotoInstruction(x, target.firstIndex);
              } else {
                // goto to the end of the method, so the instruction is unnecessary
                inst = null;
              }
            } else if (inst instanceof SSAConditionalBranchInstruction) {
              Iterator<PreBasicBlock> succs = this.getNormalSuccessors(blocks.get(i)).iterator();
              assert succs.hasNext();
              int target;
              int t1 = succs.next().firstIndex;
              if (succs.hasNext()) {
                int t2 = succs.next().firstIndex;
                if (t1 == x+1) {
                  target = t2;
                } else {
                  target = t1;
                }
              } else {
                target = t1;
              }
              SSAConditionalBranchInstruction branch = (SSAConditionalBranchInstruction) inst;
              inst = insts.ConditionalBranchInstruction(x, branch.getOperator(), branch.getType(), branch.getUse(0), branch.getUse(1), target);
            }
            
            instructions[x++] = inst;
          }
        }
      }
    }

    @Override
    public int hashCode() {
      return functionName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof AstCFG) && functionName.equals(((AstCFG) o).functionName);
    }

    @Override
    public PreBasicBlock getBlockForInstruction(int index) {
      for (int i = 1; i < getNumberOfNodes() - 1; i++)
        if (index <= instructionToBlockMap[i])
          return getNode(i);

      return null;
    }

    @Override
    public SSAInstruction[] getInstructions() {
      return instructions;
    }

    @Override
    public int getProgramCounter(int index) {
      return pcMap == null? index: pcMap[index];
    }

    @Override
    public String toString() {
      SSAInstruction[] insts = getInstructions();
      StringBuffer s = new StringBuffer("CAst CFG of " + functionName);
      int params[] = symtab.getParameterValueNumbers();
      for (int param : params)
        s.append(" ").append(param);
      s.append("\n");

      for (int i = 0; i < getNumberOfNodes(); i++) {
        PreBasicBlock bb = getNode(i);
        s.append(bb).append("\n");

        for (PreBasicBlock pbb : Iterator2Iterable.make(getSuccNodes(bb)))
          s.append("    -->" + pbb + "\n");

        for (int j = bb.getFirstInstructionIndex(); j <= bb.getLastInstructionIndex(); j++)
          if (insts[j] != null)
            s.append("  " + insts[j].toString(symtab) + "\n");
      }

      s.append("-- END --");
      return s.toString();
    }
  }

  public static enum ScopeType {
    LOCAL, GLOBAL, SCRIPT, FUNCTION, TYPE
  }

  private static final boolean DEBUG = false;

  protected class FinalCAstSymbol implements CAstSymbol {
    private final String _name;
    private final CAstType type;
    
    public FinalCAstSymbol(String _name, CAstType type) {
      this._name = _name;
      this.type = type;
      assert _name != null;
      assert type != null;
    }

    @Override
    public CAstType type() {
      return type;
    }
    
    @Override
    public String name() {
      return _name;
    }

    @Override
    public boolean isFinal() {
      return true;
    }

    @Override
    public boolean isCaseInsensitive() {
      return false;
    }

    @Override
    public boolean isInternalName() {
      return false;
    }

    @Override
    public Object defaultInitValue() {
      return null;
    }
  }

  public static class InternalCAstSymbol extends CAstSymbolImplBase {
    public InternalCAstSymbol(String _name, CAstType type) {
      super(_name, type, false, false, null);
    }

    public InternalCAstSymbol(String _name, CAstType type, boolean _isFinal) {
      super(_name, type, _isFinal, false, null);
    }

    public InternalCAstSymbol(String _name, CAstType type, boolean _isFinal, boolean _isCaseInsensitive) {
      super(_name, type, _isFinal, _isCaseInsensitive, null);
    }

    public InternalCAstSymbol(String _name, CAstType type, boolean _isFinal, boolean _isCaseInsensitive, Object _defaultInitValue) {
      super(_name, type, _isFinal, _isCaseInsensitive, _defaultInitValue);
    }

    @Override
    public boolean isInternalName() {
      return true;
    }
  }

  /**
   * interface for name information stored in a symbol table.
   * 
   * @see Scope
   */
  protected interface Symbol {
    int valueNumber();

    Scope getDefiningScope();

    boolean isParameter();

    Object constant();

    void setConstant(Object s);

    boolean isFinal();

    boolean isInternalName();

    Object defaultInitValue();
    
    CAstType type();
  }

  /**
   * a scope in the symbol table built during AST traversal
   */
  public interface Scope {

    ScopeType type();

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

  public static abstract class AbstractSymbol implements Symbol {
    private Object constantValue;

    private boolean isFinalValue;

    private final Scope definingScope;

    private Object defaultValue;

    protected AbstractSymbol(Scope definingScope, boolean isFinalValue, Object defaultValue) {
      this.definingScope = definingScope;
      this.isFinalValue = isFinalValue;
      this.defaultValue = defaultValue;
    }

    @Override
    public boolean isFinal() {
      return isFinalValue;
    }

    @Override
    public Object defaultInitValue() {
      return defaultValue;
    }

    @Override
    public Object constant() {
      return constantValue;
    }

    @Override
    public void setConstant(Object cv) {
      constantValue = cv;
    }

    @Override
    public Scope getDefiningScope() {
      return definingScope;
    }
  }

  public abstract class AbstractScope implements Scope {
    private final Scope parent;

    private final Map<String, Symbol> values = new LinkedHashMap<>();

    private final Map<String, String> caseInsensitiveNames = new LinkedHashMap<>();

    protected abstract SymbolTable getUnderlyingSymtab();

    @Override
    public Scope getParent() {
      return parent;
    }

    @Override
    public int size() {
      return getUnderlyingSymtab().getMaxValueNumber() + 1;
    }

    @Override
    public Iterator<String> getAllNames() {
      return values.keySet().iterator();
    }

    @Override
    public int allocateTempValue() {
      return getUnderlyingSymtab().newSymbol();
    }

    @Override
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
      } else if (o instanceof Short) {
        return getUnderlyingSymtab().getConstant(((Short) o).shortValue());
      } else if (o == null) {
        return getUnderlyingSymtab().getNullConstant();
      } else if (o == CAstControlFlowMap.SWITCH_DEFAULT) {
        return getUnderlyingSymtab().getConstant("__default label");
      } else {
        System.err.println(("cannot handle constant " + o));
        Assertions.UNREACHABLE();
        return -1;
      }
    }

    @Override
    public boolean isConstant(int valueNumber) {
      return getUnderlyingSymtab().isConstant(valueNumber);
    }

    @Override
    public Object getConstantObject(int valueNumber) {
      return getUnderlyingSymtab().getConstantValue(valueNumber);
    }

    @Override
    public void declare(CAstSymbol s, int vn) {
      String nm = s.name();
      assert !contains(nm) : nm;
      if (s.isCaseInsensitive())
        caseInsensitiveNames.put(nm.toLowerCase(), nm);
      values.put(nm, makeSymbol(s, vn));
    }

    @Override
    public void declare(CAstSymbol s) {
      String nm = s.name();
      if (!contains(nm) || lookup(nm).getDefiningScope() != this) {
        if (s.isCaseInsensitive())
          caseInsensitiveNames.put(nm.toLowerCase(), nm);
        values.put(nm, makeSymbol(s));
      } else {
        assert !s.isFinal() : "trying to redeclare " + nm;
      }
    }

    protected AbstractScope(Scope parent) {
      this.parent = parent;
    }

    private final String mapName(String nm) {
      String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
      return (mappedName == null) ? nm : mappedName;
    }

    protected Symbol makeSymbol(CAstSymbol s) {
      return makeSymbol(s.name(), s.type(), s.isFinal(), s.isInternalName(), s.defaultInitValue(), -1, this);
    }

    protected Symbol makeSymbol(CAstSymbol s, int vn) {
      return makeSymbol(s.name(), s.type(), s.isFinal(), s.isInternalName(), s.defaultInitValue(), vn, this);
    }

    abstract protected Symbol makeSymbol(String nm, CAstType type, boolean isFinal, boolean isInternalName, Object defaultInitValue, int vn,
        Scope parent);

    @Override
    public boolean isCaseInsensitive(String nm) {
      return caseInsensitiveNames.containsKey(nm.toLowerCase());
    }

    @Override
    public Symbol lookup(String nm) {
      if (contains(nm)) {
        return values.get(mapName(nm));
      } else {
        Symbol scoped = parent.lookup(nm);
        if (scoped != null && getEntityScope() == this && (isGlobal(scoped) || isLexicallyScoped(scoped))) {
          values.put(nm,
              makeSymbol(nm, scoped.type(), scoped.isFinal(), scoped.isInternalName(), scoped.defaultInitValue(), -1, scoped.getDefiningScope()));
          if (scoped.getDefiningScope().isCaseInsensitive(nm)) {
            caseInsensitiveNames.put(nm.toLowerCase(), nm);
          }
          return values.get(nm);
        } else {
          return scoped;
        }
      }
    }

    @Override
    public boolean contains(String nm) {
      String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
      return values.containsKey(mappedName == null ? nm : mappedName);
    }

    @Override
    public boolean isGlobal(Symbol s) {
      return s.getDefiningScope().type() == ScopeType.GLOBAL;
    }

    @Override
    public abstract boolean isLexicallyScoped(Symbol s);

    protected abstract AbstractScope getEntityScope();

    @Override
    public abstract CAstEntity getEntity();
  }

  protected AbstractScope makeScriptScope(final CAstEntity s, Scope parent) {
    return new AbstractScope(parent) {
      SymbolTable scriptGlobalSymtab = new SymbolTable(s.getArgumentCount());

      @Override
      public SymbolTable getUnderlyingSymtab() {
        return scriptGlobalSymtab;
      }

      @Override
      protected AbstractScope getEntityScope() {
        return this;
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        if (isGlobal(s))
          return false;
        else
          return ((AbstractScope) s.getDefiningScope()).getEntity() != getEntity();
      }

      @Override
      public CAstEntity getEntity() {
        return s;
      }

      @Override
      public ScopeType type() {
        return ScopeType.SCRIPT;
      }

      @Override
      protected Symbol makeSymbol(final String nm, final CAstType type, final boolean isFinal, final boolean isInternalName,
          final Object defaultInitValue, int vn, Scope definer) {
        assert nm != null;
        assert type != null;
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            setDefaultValue(getUnderlyingSymtab(), v, defaultInitValue);
          }
        }
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          @Override
          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          @Override 
          public CAstType type() {
            return type;
          }
          @Override
          public int valueNumber() {
            return v;
          }

          @Override
          public boolean isInternalName() {
            return isInternalName;
          }

          @Override
          public boolean isParameter() {
            return false;
          }
        };
      }
    };
  }

  protected int getArgumentCount(CAstEntity f) {
    return f.getArgumentCount();
  }
  
  protected String[] getArgumentNames(CAstEntity f) {
    return f.getArgumentNames();
  }
  
  private AbstractScope makeFunctionScope(final CAstEntity f, Scope parent) {
    return new AbstractScope(parent) {
      private final String[] params = getArgumentNames(f);

      private final SymbolTable functionSymtab = new SymbolTable(getArgumentCount(f));

      @Override
      public String toString() {
        return "scope for " + f.getName();
      }
      
      // ctor for scope object
      {
        for (int i = 0; i < getArgumentCount(f); i++) {
          final int yuck = i;
          declare(new CAstSymbol() {
            @Override
            public String name() {
              return params[yuck];
            }

            @Override
            public CAstType type() {
              if (f.getType() instanceof CAstType.Method) {
                if (yuck == 0) {
                  return ((CAstType.Method)f.getType()).getDeclaringType();
                } else {
                  return ((CAstType.Method)f.getType()).getArgumentTypes().get(yuck-1);
                }
              } else if (f.getType() instanceof CAstType.Function) {
                return ((CAstType.Function)f.getType()).getArgumentTypes().get(yuck);
              } else {
                return topType();
              }
            }
            
            @Override
            public boolean isFinal() {
              return false;
            }

            @Override
            public boolean isCaseInsensitive() {
              return false;
            }

            @Override
            public boolean isInternalName() {
              return false;
            }

            @Override
            public Object defaultInitValue() {
              return null;
            }

          });
        }
      }

      @Override
      public SymbolTable getUnderlyingSymtab() {
        return functionSymtab;
      }

      @Override
      protected AbstractScope getEntityScope() {
        return this;
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        if (isGlobal(s))
          return false;
        else
          return ((AbstractScope) s.getDefiningScope()).getEntity() != getEntity();
      }

      @Override
      public CAstEntity getEntity() {
        return f;
      }

      @Override
      public ScopeType type() {
        return ScopeType.FUNCTION;
      }

      private int find(String n) {
        for (int i = 0; i < params.length; i++) {
          if (n.equals(params[i])) {
            return i + 1;
          }
        }

        return -1;
      }

      @Override
      protected Symbol makeSymbol(final String nm, final CAstType type, final boolean isFinal, final boolean isInternalName,
          final Object defaultInitValue, final int valueNumber, Scope definer) {
        assert nm != null;
        assert type != null;
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          final int vn;

          {
            int x = find(nm);
            if (x != -1) {
              assert valueNumber == -1;
              vn = x;
            } else if (valueNumber != -1) {
              vn = valueNumber;
            } else {
              vn = getUnderlyingSymtab().newSymbol();
            }
            if (useDefaultInitValues() && defaultInitValue != null) {
              if (getUnderlyingSymtab().getValue(vn) == null) {
                setDefaultValue(getUnderlyingSymtab(), vn, defaultInitValue);
              }
            }
          }

          @Override
          public CAstType type() {
            return type;
          }
          
          @Override
          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          @Override
          public int valueNumber() {
            return vn;
          }

          @Override
          public boolean isInternalName() {
            return isInternalName;
          }

          @Override
          public boolean isParameter() {
            return vn <= params.length;
          }
        };
      }
    };
  }

  private Scope makeLocalScope(final Scope parent) {
    return new AbstractScope(parent) {
      @Override
      public ScopeType type() {
        return ScopeType.LOCAL;
      }

      @Override
      public SymbolTable getUnderlyingSymtab() {
        return ((AbstractScope) parent).getUnderlyingSymtab();
      }

      @Override
      protected AbstractScope getEntityScope() {
        return ((AbstractScope) parent).getEntityScope();
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        return getEntityScope().isLexicallyScoped(s);
      }

      @Override
      public CAstEntity getEntity() {
        return getEntityScope().getEntity();
      }

      @Override
      protected Symbol makeSymbol(final String nm, final CAstType type, boolean isFinal, final boolean isInternalName, final Object defaultInitValue,
          int vn, Scope definer) {
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            setDefaultValue(getUnderlyingSymtab(), v, defaultInitValue);
          }
        }
        assert nm != null;
        assert type != null;
        return new AbstractSymbol(definer, isFinal, defaultInitValue) {
          @Override
          public String toString() {
            return nm + ":" + System.identityHashCode(this);
          }

          @Override
          public CAstType type() {
            return type;
          }
          
          @Override
          public int valueNumber() {
            return v;
          }

          @Override
          public boolean isInternalName() {
            return isInternalName;
          }

          @Override
          public boolean isParameter() {
            return false;
          }
        };
      }
    };
  }

  private Scope makeGlobalScope() {
    final Map<String, AbstractSymbol> globalSymbols = new LinkedHashMap<>();
    final Map<String, String> caseInsensitiveNames = new LinkedHashMap<>();
    return new Scope() {
      
      @Override
      public String toString() {
        return "global scope";
      }
      
      private final String mapName(String nm) {
        String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
        return (mappedName == null) ? nm : mappedName;
      }

      @Override
      public Scope getParent() {
        return null;
      }

      @Override
      public boolean isGlobal(Symbol s) {
        return true;
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        return false;
      }

      @Override
      public CAstEntity getEntity() {
        return null;
      }

      @Override
      public int size() {
        return globalSymbols.size();
      }

      @Override
      public Iterator<String> getAllNames() {
        return globalSymbols.keySet().iterator();
      }

      @Override
      public int allocateTempValue() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int getConstantValue(Object c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isConstant(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Object getConstantObject(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ScopeType type() {
        return ScopeType.GLOBAL;
      }

      @Override
      public boolean contains(String name) {
        return hasImplicitGlobals() || globalSymbols.containsKey(mapName(name));
      }

      @Override
      public boolean isCaseInsensitive(String name) {
        return caseInsensitiveNames.containsKey(name.toLowerCase());
      }

      @Override
      public Symbol lookup(final String name) {
        if (!globalSymbols.containsKey(mapName(name))) {
          if (hasImplicitGlobals()) {
            declare(new CAstSymbol() {
              @Override
              public String name() {
                return name;
              }

              @Override
              public boolean isFinal() {
                return false;
              }

              @Override
              public boolean isCaseInsensitive() {
                return false;
              }

              @Override
              public boolean isInternalName() {
                return false;
              }

              @Override
              public Object defaultInitValue() {
                return null;
              }

              @Override
              public CAstType type() {
                return topType();
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

      @Override
      public void declare(CAstSymbol s, int vn) {
        assert vn == -1;
        declare(s);
      }

      @Override
      public void declare(final CAstSymbol s) {
        final String name = s.name();
        if (s.isCaseInsensitive()) {
          caseInsensitiveNames.put(name.toLowerCase(), name);
        }
        globalSymbols.put(name, new AbstractSymbol(this, s.isFinal(), s.defaultInitValue()) {
          @Override
          public String toString() {
            return name + ":" + System.identityHashCode(this);
          }

          @Override
          public CAstType type() {
            return s.type();
          }
          
          @Override
          public boolean isParameter() {
            return false;
          }

          @Override
          public boolean isInternalName() {
            return s.isInternalName();
          }

          @Override
          public int valueNumber() {
            throw new UnsupportedOperationException();
          }
        });
      }
    };
  }

  protected Scope makeTypeScope(final CAstEntity type, final Scope parent) {
    final Map<String, AbstractSymbol> typeSymbols = new LinkedHashMap<>();
    final Map<String, String> caseInsensitiveNames = new LinkedHashMap<>();
    return new Scope() {
      private final String mapName(String nm) {
        String mappedName = caseInsensitiveNames.get(nm.toLowerCase());
        return (mappedName == null) ? nm : mappedName;
      }

      @Override
      public Scope getParent() {
        return parent;
      }

      @Override
      public boolean isGlobal(Symbol s) {
        return false;
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        return false;
      }

      @Override
      public CAstEntity getEntity() {
        return type;
      }

      @Override
      public int size() {
        return typeSymbols.size();
      }

      @Override
      public Iterator<String> getAllNames() {
        return typeSymbols.keySet().iterator();
      }

      @Override
      public int allocateTempValue() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int getConstantValue(Object c) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isConstant(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Object getConstantObject(int valueNumber) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ScopeType type() {
        return ScopeType.TYPE;
      }

      @Override
      public boolean contains(String name) {
        return typeSymbols.containsKey(mapName(name));
      }

      @Override
      public boolean isCaseInsensitive(String name) {
        return caseInsensitiveNames.containsKey(name.toLowerCase());
      }

      @Override
      public Symbol lookup(String nm) {
        if (typeSymbols.containsKey(mapName(nm)))
          return typeSymbols.get(mapName(nm));
        else {
          return parent.lookup(nm);
        }
      }

      @Override
      public void declare(CAstSymbol s, int vn) {
        assert vn == -1;
        declare(s);
      }

      @Override
      public void declare(final CAstSymbol s) {
        final String name = s.name();
        assert !s.isFinal();
        if (s.isCaseInsensitive())
          caseInsensitiveNames.put(name.toLowerCase(), name);
        typeSymbols.put(name, new AbstractSymbol(this, s.isFinal(), s.defaultInitValue()) {
          @Override
          public String toString() {
            return name + ":" + System.identityHashCode(this);
          }

          @Override
          public CAstType type() {
            return s.type();
          }
          
          @Override
          public boolean isParameter() {
            return false;
          }

          @Override
          public boolean isInternalName() {
            return s.isInternalName();
          }

          @Override
          public int valueNumber() {
            throw new UnsupportedOperationException();
          }
        });
      }
    };
  }

  public interface WalkContext extends CAstVisitor.Context {

    ModuleEntry getModule();

    String getName();

    String file();

    @Override
    CAstSourcePositionMap getSourceMap();

    CAstControlFlowMap getControlFlow();

    Scope currentScope();

    Set<Scope> entityScopes();

    IncipientCFG cfg();

    UnwindState getUnwindState();

    void setCatchType(IBasicBlock<SSAInstruction> bb, TypeReference catchType);

    void setCatchType(CAstNode catchNode, TypeReference catchType);

    Map<IBasicBlock<SSAInstruction>, TypeReference[]> getCatchTypes();

    void addEntityName(CAstEntity e, String name);
    
    String getEntityName(CAstEntity e);

    boolean hasValue(CAstNode n);

    int setValue(CAstNode n, int v);

    int getValue(CAstNode n);
    
    Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet);
    
    Set<Access> getAccesses(CAstEntity e);
    
    Scope getGlobalScope();
    
  }

  private abstract class DelegatingContext implements WalkContext {
    private final WalkContext parent;

    DelegatingContext(WalkContext parent) {
      this.parent = parent;
    }

    @Override
    public Set<Access> getAccesses(CAstEntity e) {
      return parent.getAccesses(e);
    }

    @Override
    public ModuleEntry getModule() {
      return parent.getModule();
    }

    @Override
    public String getName() {
      return parent.getName();
    }

    @Override
    public String file() {
      return parent.file();
    }

    @Override
    public CAstEntity top() {
      return parent.top();
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      return parent.getSourceMap();
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      return parent.getControlFlow();
    }

    @Override
    public Scope currentScope() {
      return parent.currentScope();
    }

    @Override
    public Set<Scope> entityScopes() {
      return parent.entityScopes();
    }

    @Override
    public IncipientCFG cfg() {
      return parent.cfg();
    }

    @Override
    public UnwindState getUnwindState() {
      return parent.getUnwindState();
    }

    @Override
    public void setCatchType(IBasicBlock<SSAInstruction> bb, TypeReference catchType) {
      parent.setCatchType(bb, catchType);
    }

    @Override
    public void setCatchType(CAstNode catchNode, TypeReference catchType) {
      parent.setCatchType(catchNode, catchType);
    }

    @Override
    public Map<IBasicBlock<SSAInstruction>, TypeReference[]> getCatchTypes() {
      return parent.getCatchTypes();
    }

    @Override
    public void addEntityName(CAstEntity e, String name) {
      parent.addEntityName(e, name);
    }
    
    @Override
    public String getEntityName(CAstEntity e) {
      return parent.getEntityName(e);
    }
    
    @Override
    public boolean hasValue(CAstNode n) {
      return parent.hasValue(n);
    }

    @Override
    public int setValue(CAstNode n, int v) {
      return parent.setValue(n, v);
    }

    @Override
    public int getValue(CAstNode n) {
      return parent.getValue(n);
    }

    @Override
    public Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet) {
      return parent.exposeNameSet(entity, writeSet);
    }

    @Override
    public Scope getGlobalScope() {
      return parent.getGlobalScope();
    }
    
  }

  private class FileContext extends DelegatingContext {
    private final String fUnitName;

    public FileContext(WalkContext parent, String unitName) {
      super(parent);
      fUnitName = unitName;
    }

    @Override
    public String getName() {
      return fUnitName;
    }
  }

  private class UnwindContext extends DelegatingContext {
    private final UnwindState state;

    UnwindContext(CAstNode unwindNode, WalkContext parent, CAstVisitor<WalkContext> visitor) {
      super(parent);
      this.state = new UnwindState(unwindNode, parent, visitor);
    }

    @Override
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

    @Override
    public String getName() {
      return name;
    }

    @Override
    public CAstEntity top() {
      return topNode;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      return top().getSourceMap();
    }

  }

  public class CodeEntityContext extends EntityContext {
    private final Scope topEntityScope;

    private final Set<Scope> allEntityScopes;

    private final IncipientCFG cfg;

    private final Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes = HashMapFactory.make();

    Set<Pair<Pair<String, String>, Integer>> exposedReads;
    Set<Pair<Pair<String, String>, Integer>> exposedWrites;
    
    Set<Access> accesses;
    
    /**
     * maps nodes in the current function to the value number holding their value
     * or, for constants, to their constant value.
     */
    private final Map<CAstNode, Integer> results = new LinkedHashMap<>();

    public CodeEntityContext(WalkContext parent, Scope entityScope, CAstEntity s) {
      super(parent, s);

      this.topEntityScope = entityScope;

      this.allEntityScopes = HashSetFactory.make();
      this.allEntityScopes.add(entityScope);

      cfg = new IncipientCFG();
    }

    @Override
    public Set<Access> getAccesses(CAstEntity e) {
      if (e == topNode) {
        if (accesses == null) {
          accesses = HashSetFactory.make();
        }
        return accesses;
      } else {
        return super.getAccesses(e);
      }
    }
    
    @Override
    public Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet) {
      if (entity == topNode) {
       if (writeSet) {
         if (exposedWrites == null) {
           exposedWrites = HashSetFactory.make();
         }
         return exposedWrites;
       } else {
         if (exposedReads == null) {
           exposedReads = HashSetFactory.make();
         }
         return exposedReads;         
       }
      } else {
        return super.exposeNameSet(entity, writeSet);
      }
    }


    @Override
    public CAstControlFlowMap getControlFlow() {
      return top().getControlFlow();
    }

    @Override
    public IncipientCFG cfg() {
      return cfg;
    }

    @Override
    public Scope currentScope() {
      return topEntityScope;
    }

    @Override
    public Set<Scope> entityScopes() {
      return allEntityScopes;
    }

    @Override
    public UnwindState getUnwindState() {
      return null;
    }

    @Override
    public void setCatchType(CAstNode catchNode, TypeReference catchType) {
      setCatchType(cfg.getBlock(catchNode), catchType);
    }

    @Override
    public void setCatchType(IBasicBlock<SSAInstruction> bb, TypeReference catchType) {
      if (! catchTypes.containsKey(bb)) {
        catchTypes.put(bb, new TypeReference[] { catchType });
      } else {
        TypeReference[] data = catchTypes.get(bb);

        for (TypeReference element : data) {
          if (element == catchType) {
            return;
          }
        }

        TypeReference[] newData = new TypeReference[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[data.length] = catchType;

        catchTypes.put(bb, newData);
      }
    }

    @Override
    public Map<IBasicBlock<SSAInstruction>, TypeReference[]> getCatchTypes() {
      return catchTypes;
    }
    
    @Override
    public boolean hasValue(CAstNode n) {
      return results.containsKey(n);
    }

    @Override
    public final int setValue(CAstNode n, int v) {
      results.put(n, new Integer(v));
      return v;
    }

    @Override
    public final int getValue(CAstNode n) {
      if (results.containsKey(n))
        return results.get(n).intValue();
      else {
        if (DEBUG) {
          System.err.println(("no value for " + n.getKind()));
        }
        return -1;
      }
    }

  }

  private final class TypeContext extends EntityContext {

    private TypeContext(WalkContext parent, CAstEntity n) {
      super(parent, n);
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      Assertions.UNREACHABLE("TypeContext.getControlFlow()");
      return null;
    }

    @Override
    public IncipientCFG cfg() {
      Assertions.UNREACHABLE("TypeContext.cfg()");
      return null;
    }

    @Override
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

    @Override
    public Scope currentScope() {
      return localScope;
    }
  }

  /**
   * lexical access information for some entity scope. used during call graph
   * construction to handle lexical accesses.
   */
  public static class AstLexicalInformation implements LexicalInformation {
    /**
     * the name of this function, as it appears in the definer portion of a
     * lexical name
     */
    private final String functionLexicalName;

    /**
     * names possibly accessed in a nested lexical scope, represented as pairs
     * (name,nameOfDefiningEntity)
     */
    private final Pair<String, String>[] exposedNames;

    /**
     * map from instruction index and exposed name (via its index in
     * {@link #exposedNames}) to the value number for the name at that
     * instruction index. This can vary at different instructions due to SSA
     * (and this information is updated during {@link SSAConversion}).
     */
    private final int[][] instructionLexicalUses;

    /**
     * maps each exposed name (via its index in {@link #exposedNames}) to its
     * value number at method exit.
     */
    private final int[] exitLexicalUses;

    /**
     * the names of the enclosing methods declaring names that are lexically
     * accessed by the entity
     */
    private final String[] scopingParents;

    /**
     * all value numbers appearing as entries in {@link #instructionLexicalUses}
     * and {@link #exitLexicalUses}, computed lazily
     */
    private MutableIntSet allExposedUses = null;

    /**
     * names of exposed variables of this method that cannot be written outside
     */
    private final Set<String> readOnlyNames;

    @SuppressWarnings("unchecked")
    public AstLexicalInformation(AstLexicalInformation original) {
      this.functionLexicalName = original.functionLexicalName;

      if (original.exposedNames != null) {
        exposedNames = new Pair[original.exposedNames.length];
        for (int i = 0; i < exposedNames.length; i++) {
          exposedNames[i] = Pair.make(original.exposedNames[i].fst, original.exposedNames[i].snd);
        }
      } else {
        exposedNames = null;
      }

      instructionLexicalUses = new int[original.instructionLexicalUses.length][];
      for (int i = 0; i < instructionLexicalUses.length; i++) {
        int[] x = original.instructionLexicalUses[i];
        if (x != null) {
          instructionLexicalUses[i] = new int[x.length];
          for (int j = 0; j < x.length; j++) {
            instructionLexicalUses[i][j] = x[j];
          }
        }
      }

      if (original.exitLexicalUses != null) {
        exitLexicalUses = new int[original.exitLexicalUses.length];
        for (int i = 0; i < exitLexicalUses.length; i++) {
          exitLexicalUses[i] = original.exitLexicalUses[i];
        }
      } else {
        exitLexicalUses = null;
      }

      if (original.scopingParents != null) {
        scopingParents = new String[original.scopingParents.length];
        for (int i = 0; i < scopingParents.length; i++) {
          scopingParents[i] = original.scopingParents[i];
        }
      } else {
        scopingParents = null;
      }

      readOnlyNames = original.readOnlyNames;
    }

    private static int[] buildLexicalUseArray(Pair<Pair<String, String>, Integer>[] exposedNames, String entityName) {
      if (exposedNames != null) {
        int[] lexicalUses = new int[exposedNames.length];
        for (int j = 0; j < exposedNames.length; j++) {
          if (entityName == null || entityName.equals(exposedNames[j].fst.snd)) {
            lexicalUses[j] = exposedNames[j].snd;
          } else {
            lexicalUses[j] = -1;
          }
        }

        return lexicalUses;
      } else {
        return null;
      }
    }

    private static Pair<String, String>[] buildLexicalNamesArray(Pair<Pair<String, String>, Integer>[] exposedNames) {
      if (exposedNames != null) {
        @SuppressWarnings("unchecked")
        Pair<String, String>[] lexicalNames = new Pair[exposedNames.length];
        for (int j = 0; j < exposedNames.length; j++) {
          lexicalNames[j] = exposedNames[j].fst;
        }

        return lexicalNames;
      } else {
        return null;
      }
    }

    AstLexicalInformation(String entityName, Scope scope, SSAInstruction[] instrs,
        Set<Pair<Pair<String, String>, Integer>> exposedNamesForReadSet,
        Set<Pair<Pair<String, String>, Integer>> exposedNamesForWriteSet, Set<Access> accesses) {
      this.functionLexicalName = entityName;

      Pair<Pair<String, String>, Integer>[] EN = null;
      if (exposedNamesForReadSet != null || exposedNamesForWriteSet != null) {
        Set<Pair<Pair<String, String>, Integer>> exposedNamesSet = new HashSet<>();
        if (exposedNamesForReadSet != null) {
          exposedNamesSet.addAll(exposedNamesForReadSet);
        }
        if (exposedNamesForWriteSet != null) {
          exposedNamesSet.addAll(exposedNamesForWriteSet);
        }
        EN = exposedNamesSet.toArray(new Pair[exposedNamesSet.size()]);
      }

      if (exposedNamesForReadSet != null) {
        Set<String> readOnlyNames = new HashSet<>();
        for (Pair<Pair<String, String>, Integer> v : exposedNamesForReadSet) {
          if (entityName != null && entityName.equals(v.fst.snd)) {
            readOnlyNames.add(v.fst.fst);
          }
        }
        if (exposedNamesForWriteSet != null) {
          for (Pair<Pair<String, String>, Integer> v : exposedNamesForWriteSet) {
            if (entityName != null && entityName.equals(v.fst.snd)) {
              readOnlyNames.remove(v.fst.fst);
            }
          }
        }
        this.readOnlyNames = readOnlyNames;
      } else {
        this.readOnlyNames = null;
      }

      this.exposedNames = buildLexicalNamesArray(EN);

      // the value numbers stored in exitLexicalUses and instructionLexicalUses
      // are identical at first; they will be updated
      // as needed during the final SSA conversion
      this.exitLexicalUses = buildLexicalUseArray(EN, entityName);

      this.instructionLexicalUses = new int[instrs.length][];
      for (int i = 0; i < instrs.length; i++) {
        if (instrs[i] instanceof SSAAbstractInvokeInstruction) {
          this.instructionLexicalUses[i] = buildLexicalUseArray(EN, null);
        }
      }

      if (accesses != null) {
        Set<String> parents = new LinkedHashSet<>();
        for (Access AC : accesses) {
          if (AC.variableDefiner != null) {
            parents.add(AC.variableDefiner);
          }
        }
        scopingParents = parents.toArray(new String[parents.size()]);

        if (DEBUG_LEXICAL) {
          System.err.println(("scoping parents of " + scope.getEntity()));
          System.err.println(parents.toString());
        }

      } else {
        scopingParents = null;
      }

      if (DEBUG_NAMES) {
        System.err.println(("lexical uses of " + scope.getEntity()));
        for (int i = 0; i < instructionLexicalUses.length; i++) {
          if (instructionLexicalUses[i] != null) {
            System.err.println(("  lexical uses of " + instrs[i]));
            for (int j = 0; j < instructionLexicalUses[i].length; j++) {
              System.err.println(("    " + this.exposedNames[j].fst + ": " + instructionLexicalUses[i][j]));
            }
          }
        }
      }
    }

    @Override
    public int[] getExitExposedUses() {
      return exitLexicalUses;
    }

    private static final int[] NONE = new int[0];

    @Override
    public int[] getExposedUses(int instructionOffset) {
      return instructionLexicalUses[instructionOffset] == null ? NONE : instructionLexicalUses[instructionOffset];
    }

    @Override
    public IntSet getAllExposedUses() {
      if (allExposedUses == null) {
        allExposedUses = IntSetUtil.make();
        if (exitLexicalUses != null) {
          for (int exitLexicalUse : exitLexicalUses) {
            if (exitLexicalUse > 0) {
              allExposedUses.add(exitLexicalUse);
            }
          }
        }
        if (instructionLexicalUses != null) {
          for (int[] instructionLexicalUse : instructionLexicalUses) {
            if (instructionLexicalUse != null) {
              for (int element : instructionLexicalUse) {
                if (element > 0) {
                  allExposedUses.add(element);
                }
              }
            }
          }
        }
      }

      return allExposedUses;
    }

    @Override
    public Pair<String, String>[] getExposedNames() {
      return exposedNames;
    }

    @Override
    public String[] getScopingParents() {
      return scopingParents;
    }

    @Override
    public boolean isReadOnly(String name) {
      return readOnlyNames != null && readOnlyNames.contains(name);
    }

    @Override
    public String getScopingName() {
      return functionLexicalName;
    }

    public static boolean hasExposedUses(CGNode caller, CallSiteReference site) {
      int uses[] = ((AstMethod) caller.getMethod()).lexicalInfo().getExposedUses(site.getProgramCounter());
      if (uses != null && uses.length > 0) {
        for (int use : uses) {
          if (use > 0) {
            return true;
          }
        }
      }
    
      return false;
    }
  }
 
  /**
   * record that in entity e, the access is performed.
   * 
   * If {@link #useLocalValuesForLexicalVars()} is true, the access is performed
   * using a local variable. in
   * {@link #patchLexicalAccesses(SSAInstruction[], Set)}, this information is
   * used to update an instruction that performs all the accesses at the
   * beginning of the method and defines the locals.
   */
  private static void addAccess(WalkContext context, CAstEntity e, Access access) {
    context.getAccesses(e).add(access);
  }

  /**
   * Record that a name assigned a value number in the scope of entity may be
   * accessed by a lexically nested scope, i.e., the name may be
   * <em>exposed</em> to lexically nested scopes. This information is needed
   * during call graph construction to properly model the data flow due to the
   * access in the nested scope.
   * 
   * @param entity
   *          an entity in whose scope name is assigned a value number
   * @param declaration
   *          the declaring entity for name (possibly an enclosing scope of
   *          entity, in the case where entity
   *          {@link #useLocalValuesForLexicalVars() accesses the name via a
   *          local})
   * @param name
   *          the accessed name
   * @param valueNumber
   *          the name's value number in the scope of entity
   */
  private static void addExposedName(CAstEntity entity, CAstEntity declaration, String name, int valueNumber, boolean isWrite, WalkContext context) {
    Pair<Pair<String, String>, Integer> newVal = Pair.make(Pair.make(name, context.getEntityName(declaration)), valueNumber);
    context.exposeNameSet(entity, isWrite).add(newVal);
  }

  public void setDefaultValue(SymbolTable symtab, int vn, Object value) {
    if (value == CAstSymbol.NULL_DEFAULT_VALUE) {
      symtab.setDefaultValue(vn, null);
    } else {
      symtab.setDefaultValue(vn, value);
    }
  }

  protected IUnaryOpInstruction.IOperator translateUnaryOpcode(CAstNode op) {
    if (op == CAstOperator.OP_BITNOT)
      return CAstUnaryOp.BITNOT;
    else if (op == CAstOperator.OP_NOT)
      return IUnaryOpInstruction.Operator.NEG;
    else if (op == CAstOperator.OP_SUB)
      return CAstUnaryOp.MINUS;
    else if (op == CAstOperator.OP_ADD)
      return CAstUnaryOp.PLUS;
    else
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
    return null;

  }

  protected IBinaryOpInstruction.IOperator translateBinaryOpcode(CAstNode op) {
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
      return CAstBinaryOp.CONCAT;
    else if (op == CAstOperator.OP_EQ)
      return CAstBinaryOp.EQ;
    else if (op == CAstOperator.OP_STRICT_EQ)
      return CAstBinaryOp.STRICT_EQ;
    else if (op == CAstOperator.OP_GE)
      return CAstBinaryOp.GE;
    else if (op == CAstOperator.OP_GT)
      return CAstBinaryOp.GT;
    else if (op == CAstOperator.OP_LE)
      return CAstBinaryOp.LE;
    else if (op == CAstOperator.OP_LT)
      return CAstBinaryOp.LT;
    else if (op == CAstOperator.OP_NE)
      return CAstBinaryOp.NE;
    else if (op == CAstOperator.OP_STRICT_NE)
      return CAstBinaryOp.STRICT_NE;
    else {
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
      return null;
    }
  }

  protected IConditionalBranchInstruction.IOperator translateConditionOpcode(CAstNode op) {
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
      assert false : "cannot translate " + CAstPrinter.print(op);
      return null;
    }
  }

  protected String[] makeNameMap(CAstEntity n, Set<Scope> scopes, @SuppressWarnings("unused") SSAInstruction[] insts) {
    // all scopes share the same underlying symtab, which is what
    // size really refers to.
    String[] map = new String[scopes.iterator().next().size() + 1];

    if (DEBUG_NAMES) {
      System.err.println(("names array of size " + map.length));
    }

    for (Scope scope : scopes) {
      for (String nm : Iterator2Iterable.make(scope.getAllNames())) {
        
        if (ignoreName(nm)) {
          continue;
        }
        
        Symbol v = scope.lookup(nm);

        if (v.isInternalName()) {
          continue;
        }

        // constants can flow to multiple variables
        if (scope.isConstant(v.valueNumber()))
          continue;

        assert map[v.valueNumber()] == null || map[v.valueNumber()].equals(nm) || ignoreName(map[v.valueNumber()]) : "value number " + v.valueNumber()
            + " mapped to multiple names in " + n.getName() + ": " + nm + " and " + map[v.valueNumber()];

        map[v.valueNumber()] = nm;

        if (DEBUG_NAMES) {
          System.err.println(("mapping name " + nm + " to " + v.valueNumber()));
        }
      }
    }

    return map;
  }

  protected final static CAstType getTypeForNode(WalkContext context, CAstNode node) {
    if (context.top().getNodeTypeMap() != null) {
      return context.top().getNodeTypeMap().getNodeType(node);
    } else {
      return null;
    }
  }

  private Position getPosition(CAstSourcePositionMap map, CAstNode n) {
    if (map.getPosition(n) != null) {
      return map.getPosition(n);
    } else {
      for (int i = 0; i < n.getChildCount(); i++) {
        Position p = getPosition(map, n.getChild(i));
        if (p != null) {
          return p;
        }
      }

      return null;
    }
  }

  @Override
  protected WalkContext makeFileContext(WalkContext c, CAstEntity n) {
    return new FileContext(c, n.getName());
  }

  @Override
  protected WalkContext makeTypeContext(WalkContext c, CAstEntity n) {
    return new TypeContext(c, n);
  }

  @Override
  protected WalkContext makeCodeContext(WalkContext context, CAstEntity n) {
    AbstractScope scope;
    if (n.getKind() == CAstEntity.SCRIPT_ENTITY)
      scope = makeScriptScope(n, context.currentScope());
    else
      scope = makeFunctionScope(n, context.currentScope());
    return new CodeEntityContext(context, scope, n);
  }

  @Override
  protected boolean enterEntity(final CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    if (DEBUG_TOP)
      System.err.println(("translating " + n.getName()));
    return false;
  }

  @Override
  protected boolean visitFileEntity(CAstEntity n, WalkContext context, WalkContext fileContext, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveFileEntity(CAstEntity n, WalkContext context, WalkContext fileContext, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitFieldEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveFieldEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    // Define a new field in the enclosing type, if the language we're
    // processing allows such.
    CAstEntity topEntity = context.top(); // better be a type
    assert topEntity.getKind() == CAstEntity.TYPE_ENTITY : "Parent of field entity is not a type???";
    defineField(topEntity, context, n);
  }

  @Override
  protected boolean visitGlobalEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveGlobalEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    // Define a new field in the enclosing type, if the language we're
    // processing allows such.
    context.getGlobalScope().declare(new CAstSymbolImpl(n.getName(), n.getType()));
  }

  @Override
  protected boolean visitTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) {
    return !defineType(n, context);
  }

  @Override
  protected void leaveTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitFunctionEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    if (n.getAST() == null) // presumably abstract
      declareFunction(n, context);
    else
      initFunctionEntity(codeContext);
    return false;
  }

  @Override
  protected void leaveFunctionEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    if (n.getAST() != null) // non-abstract
      closeFunctionEntity(n, context, codeContext);
  }

  @Override
  protected boolean visitMacroEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    return true;
  }

  @Override
  protected boolean visitScriptEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    declareFunction(n, codeContext);
    initFunctionEntity(codeContext);
    return false;
  }

  @Override
  protected void leaveScriptEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    closeFunctionEntity(n, context, codeContext);
  }

  public void initFunctionEntity(WalkContext functionContext) {
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
    Map<IBasicBlock<SSAInstruction>,TypeReference[]> catchTypes = functionContext.getCatchTypes();
    AstCFG cfg = new AstCFG(n, functionContext.cfg(), symtab, insts);
    Position[] line = functionContext.cfg().getLinePositionMap();
    boolean katch = functionContext.cfg().hasCatchBlock();
    boolean monitor = functionContext.cfg().hasMonitorOp();
    String[] nms = makeNameMap(n, functionContext.entityScopes(), cfg.getInstructions());

    /*
     * Set reachableBlocks = DFS.getReachableNodes(cfg,
     * Collections.singleton(cfg.entry()));
     * Assertions._assert(reachableBlocks.size() == cfg.getNumberOfNodes(),
     * cfg.toString());
     */

    // (put here to allow subclasses to handle stuff in scoped entities)
    // assemble lexical information
    AstLexicalInformation LI = new AstLexicalInformation(functionContext.getEntityName(n), functionContext.currentScope(), cfg.getInstructions(),
        functionContext.exposeNameSet(n, false), 
        functionContext.exposeNameSet(n, true), 
        functionContext.getAccesses(n));

    DebuggingInformation DBG = new AstDebuggingInformation(n.getPosition(), line, nms);

    // actually make code body
    defineFunction(n, parentContext, cfg, symtab, katch, catchTypes, monitor, LI, DBG);
  }

  @Override
  protected WalkContext makeLocalContext(WalkContext context, CAstNode n) {
    return new LocalContext(context, makeLocalScope(context.currentScope()));
  }

  
  @Override
  protected WalkContext makeSpecialParentContext(final WalkContext context, CAstNode n) {
    final String specialName = (String) n.getChild(0).getValue();
    
    return new LocalContext(context, new AbstractScope(context.currentScope()) {
      private Scope parent = null;
      
      private Scope parent() {
        if (parent == null) {
            parent = ((AbstractScope)context.currentScope()).getEntityScope().getParent();
        }
        return parent;
      }
      
      @Override
      public ScopeType type() {
        return ScopeType.LOCAL;
      }

      private Scope scopeFor(String name) {
        if (name.equals(specialName)) {
          return parent();
        } else {
          return context.currentScope();
        }       
      }
      
      @Override
      public boolean contains(String name) {
        return scopeFor(name).contains(name);
      }

      @Override
      public Symbol lookup(String name) {
        return scopeFor(name).lookup(name);
      }

      @Override
      protected SymbolTable getUnderlyingSymtab() {
        return ((AbstractScope)context.currentScope()).getUnderlyingSymtab();
      }

      @Override
      protected Symbol makeSymbol(String nm, CAstType type, boolean isFinal, boolean isInternalName, Object defaultInitValue,
          int vn, Scope parent) {
          return ((AbstractScope)context.currentScope()).makeSymbol(nm, type, isFinal, isInternalName, defaultInitValue, vn, parent);
      }

      @Override
      protected AbstractScope getEntityScope() {
        return ((AbstractScope)context.currentScope()).getEntityScope();
      }

      @Override
      public boolean isLexicallyScoped(Symbol s) {
        return context.currentScope().isLexicallyScoped(s);
      }

      @Override
      public CAstEntity getEntity() {
        return context.top();
      }
      
    });
  }

  @Override
  protected WalkContext makeUnwindContext(WalkContext context, CAstNode n, CAstVisitor<WalkContext> visitor) {
    // here, n represents the "finally" block of the unwind
    return new UnwindContext(n, context, visitor);
  }

  protected Map<CAstEntity, Set<String>> entity2ExposedNames;
  protected int processFunctionExpr(CAstNode n, WalkContext context) {
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    declareFunction(fn, context);
    int result = context.currentScope().allocateTempValue();
    int ex = context.currentScope().allocateTempValue();
    doMaterializeFunction(n, context, result, ex, fn);
    return result;
  }

  @Override
  protected boolean visitFunctionExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveFunctionExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = processFunctionExpr(n, c);
    c.setValue(n, result);
  }

  @Override
  protected boolean visitFunctionStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveFunctionStmt(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    int result = processFunctionExpr(n, context);
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    // FIXME: handle redefinitions of functions
    Scope cs = context.currentScope();
    if (cs.contains(fn.getName()) && !cs.isLexicallyScoped(cs.lookup(fn.getName())) && !cs.isGlobal(cs.lookup(fn.getName()))) {
      // if we already have a local with the function's name, write the function
      // value to that local
      assignValue(n, context, cs.lookup(fn.getName()), fn.getName(), result);
    } else if (topLevelFunctionsInGlobalScope() && context.top().getKind() == CAstEntity.SCRIPT_ENTITY) {
      context.getGlobalScope().declare(new FinalCAstSymbol(fn.getName(), fn.getType()));
      assignValue(n, context, cs.lookup(fn.getName()), fn.getName(), result);
    } else {
      context.currentScope().declare(new FinalCAstSymbol(fn.getName(), fn.getType()), result);
    }
  }

  @Override
  protected boolean visitLocalScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected boolean visitSpecialParentScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveLocalScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  @Override
  protected void leaveSpecialParentScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(1)));
  }

  @Override
  protected boolean visitBlockExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveBlockExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  @Override
  protected boolean visitBlockStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveBlockStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitLoop(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    // loop test block
    context.cfg().newBlock(true);
    PreBasicBlock headerB = context.cfg().getCurrentBlock();
    visitor.visit(n.getChild(0), context, visitor);

    assert c.getValue(n.getChild(0)) != -1 : "error in loop test " + CAstPrinter.print(n.getChild(0), context.top().getSourceMap())
        + " of loop " + CAstPrinter.print(n, context.top().getSourceMap());
    context.cfg().addInstruction(
        insts.ConditionalBranchInstruction(context.cfg().currentInstruction, translateConditionOpcode(CAstOperator.OP_EQ), null, c.getValue(n.getChild(0)), context
            .currentScope().getConstantValue(new Integer(0)), -1));
    PreBasicBlock branchB = context.cfg().getCurrentBlock();

    // loop body
    context.cfg().newBlock(true);
    visitor.visit(n.getChild(1), context, visitor);
    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));
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
  @Override
  protected final void leaveLoopHeader(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveLoop(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitGetCaughtException(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveGetCaughtException(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    String nm = (String) n.getChild(0).getValue();
    context.currentScope().declare(new FinalCAstSymbol(nm, exceptionType()));
    context.cfg().addInstruction(
        insts.GetCaughtExceptionInstruction(context.cfg().currentInstruction, context.cfg().getCurrentBlock().getNumber(), context.currentScope().lookup(nm)
            .valueNumber()));
  }

  @Override
  protected boolean visitThis(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveThis(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, 1);
  }

  @Override
  protected boolean visitSuper(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveSuper(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, 1);
  }

  @Override
  protected boolean visitCall(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  @Override
  protected void leaveCall(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = c.getValue(n);
    int exp = context.currentScope().allocateTempValue();
    int fun = c.getValue(n.getChild(0));
    CAstNode functionName = n.getChild(1);
    int[] args = new int[n.getChildCount() - 2];
    for (int i = 0; i < args.length; i++) {
      args[i] = c.getValue(n.getChild(i + 2));
    }
    doCall(context, n, result, exp, functionName, fun, args);
  }

  @Override
  protected boolean visitVar(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveVar(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    String nm = (String) n.getChild(0).getValue();
    assert nm != null : "cannot find var for " + CAstPrinter.print(n, context.getSourceMap());
    Symbol s = context.currentScope().lookup(nm);
    assert s != null : "cannot find symbol for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
    assert s.type() != null : "no type for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
    TypeReference type = makeType(s.type());
    if (context.currentScope().isGlobal(s)) {
      c.setValue(n, doGlobalRead(n, context, nm, type));
    } else if (context.currentScope().isLexicallyScoped(s)) {
      c.setValue(n, doLexicallyScopedRead(n, context, nm, type));
    } else {
      c.setValue(n, doLocalRead(context, nm, type));
    }
  }

  @Override
  protected boolean visitConstant(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveConstant(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    c.setValue(n, context.currentScope().getConstantValue(n.getValue()));
  }

  @Override
  protected boolean visitBinaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  private static boolean handleBinaryOpThrow(CAstNode n, CAstNode op, WalkContext context) {
    // currently, only integer / and % throw exceptions
    boolean mayBeInteger = false;
    Collection<Object> labels = context.getControlFlow().getTargetLabels(n);
    if (!labels.isEmpty()) {
      context.cfg().addPreNode(n, context.getUnwindState());

      mayBeInteger = true;
      assert op == CAstOperator.OP_DIV || op == CAstOperator.OP_MOD : CAstPrinter.print(n);
      for (Object label : labels) {
        CAstNode target = context.getControlFlow().getTarget(n, label);
        if (target == CAstControlFlowMap.EXCEPTION_TO_EXIT)
          context.cfg().addPreEdgeToExit(n, true);
        else
          context.cfg().addPreEdge(n, target, true);
      }
    }

    return mayBeInteger;
  }

  @Override
  protected void leaveBinaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = c.getValue(n);
    CAstNode l = n.getChild(1);
    CAstNode r = n.getChild(2);
    assert c.getValue(r) != -1 : CAstPrinter.print(n);
    assert c.getValue(l) != -1 : CAstPrinter.print(n);

    boolean mayBeInteger = handleBinaryOpThrow(n, n.getChild(0), context);

    context.cfg().addInstruction(
        insts.BinaryOpInstruction(context.cfg().currentInstruction, translateBinaryOpcode(n.getChild(0)), false, false, result, c.getValue(l), c.getValue(r),
            mayBeInteger));

    if (mayBeInteger) {
      context.cfg().newBlock(true);
    }
  }

  @Override
  protected boolean visitUnaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  @Override
  protected void leaveUnaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = c.getValue(n);
    CAstNode v = n.getChild(1);
    context.cfg().addInstruction(insts.UnaryOpInstruction(context.cfg().currentInstruction, translateUnaryOpcode(n.getChild(0)), result, c.getValue(v)));
  }

  @Override
  protected boolean visitArrayLength(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  @Override
  protected void leaveArrayLength(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = c.getValue(n);
    int arrayValue = c.getValue(n.getChild(0));
    context.cfg().addInstruction(insts.ArrayLengthInstruction(context.cfg().currentInstruction, result, arrayValue));
  }

  @Override
  protected boolean visitArrayRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveArrayRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int arrayValue = c.getValue(n.getChild(0));
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    arrayOpHandler.doArrayRead(context, result, arrayValue, n, gatherArrayDims(c, n));
  }

  @Override
  protected boolean visitDeclStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  // TODO: should we handle exploded declaration nodes here instead?
  @Override
  protected void leaveDeclStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    CAstSymbol s = (CAstSymbol) n.getChild(0).getValue();
    String nm = s.name();
    CAstType t = s.type();
    Scope scope = c.currentScope();
    if (n.getChildCount() == 2) {
      CAstNode v = n.getChild(1);
      if (scope.contains(nm) && scope.lookup(nm).getDefiningScope() == scope) {
        assert !s.isFinal();
        doLocalWrite(c, nm, makeType(t), c.getValue(v));
      } else if (v.getKind() != CAstNode.CONSTANT && v.getKind() != CAstNode.VAR && v.getKind() != CAstNode.THIS) {
        scope.declare(s, c.getValue(v));
      } else {
        scope.declare(s);
        doLocalWrite(c, nm, makeType(t), c.getValue(v));
      }
    } else {
      c.currentScope().declare(s);
    }
  }

  @Override
  protected boolean visitReturn(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveReturn(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    if (n.getChildCount() > 0) {
      context.cfg().addInstruction(insts.ReturnInstruction(context.cfg().currentInstruction, c.getValue(n.getChild(0)), false));
    } else {
      context.cfg().addInstruction(insts.ReturnInstruction(context.cfg().currentInstruction));
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(false);
    context.cfg().addPreEdgeToExit(n, false);
  }

  @Override
  protected boolean visitIfgoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveIfgoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    if (n.getChildCount() == 1) {
      context.cfg().addInstruction(
          insts.ConditionalBranchInstruction(context.cfg().currentInstruction, translateConditionOpcode(CAstOperator.OP_NE), null, c.getValue(n.getChild(0)), context
              .currentScope().getConstantValue(new Integer(0)), -1));
    } else if (n.getChildCount() == 3) {
      context.cfg().addInstruction(
          insts.ConditionalBranchInstruction(context.cfg().currentInstruction, translateConditionOpcode(n.getChild(0)), null, c.getValue(n.getChild(1)),
              c.getValue(n.getChild(2)), -1));
    } else {
      Assertions.UNREACHABLE();
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, Boolean.TRUE), false);

    context.cfg().newBlock(true);
  }

  @Override
  protected boolean visitGoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveGoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().addPreNode(n, context.getUnwindState());
           context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, null), false);
      context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));            
      if (context.getControlFlow().getTarget(n, null) == null) {
        assert context.getControlFlow().getTarget(n, null) != null : context.getControlFlow() + " does not map " + n + " ("
            + context.getSourceMap().getPosition(n) + ")";
      }
      context.cfg().newBlock(false);
    }
  }

  @Override
  protected boolean visitLabelStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {    
    WalkContext context = c;
    if (!context.getControlFlow().getSourceNodes(n).isEmpty()) {
      context.cfg().newBlock(true);
      context.cfg().addPreNode(n, context.getUnwindState());
    }
    return false;
  }

  @Override
  protected void leaveLabelStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected void processIf(CAstNode n, boolean isExpr, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    PreBasicBlock trueB = null, falseB = null;
    // conditional
    CAstNode l = n.getChild(0);
    visitor.visit(l, context, visitor);
    context.cfg().addInstruction(
        insts.ConditionalBranchInstruction(context.cfg().currentInstruction, translateConditionOpcode(CAstOperator.OP_EQ), null, c.getValue(l), context.currentScope()
            .getConstantValue(new Integer(0)), -1));
    PreBasicBlock srcB = context.cfg().getCurrentBlock();
    // true clause
    context.cfg().newBlock(true);
    CAstNode r = n.getChild(1);
    visitor.visit(r, context, visitor);
    if (isExpr)
      context.cfg().addInstruction(new AssignInstruction(context.cfg().currentInstruction, c.getValue(n), c.getValue(r)));
    if (n.getChildCount() == 3) {
      if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
        context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));
        trueB = context.cfg().getCurrentBlock();

        // false clause
        context.cfg().newBlock(false);
      }

      falseB = context.cfg().getCurrentBlock();
      CAstNode f = n.getChild(2);
      context.cfg().deadBlocks.remove(falseB);
      visitor.visit(f, context, visitor);
      if (isExpr)
        context.cfg().addInstruction(new AssignInstruction(context.cfg().currentInstruction, c.getValue(n), c.getValue(f)));
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
  @Override
  protected final void leaveIfStmtCondition(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveIfStmtTrueClause(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveIfStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveIfExprCondition(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveIfExprTrueClause(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveIfExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitIfStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    processIf(n, false, c, visitor);
    return true;
  }

  @Override
  protected boolean visitIfExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    processIf(n, true, c, visitor);
    return true;
  }

  @Override
  protected boolean visitNew(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveNew(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;

    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);

    int[] arguments;
    if (n.getChildCount() <= 1) {
      arguments = null;
    } else {
      arguments = new int[n.getChildCount() - 1];
      for (int i = 1; i < n.getChildCount(); i++) {
        arguments[i - 1] = c.getValue(n.getChild(i));
      }
    }
    doNewObject(context, n, result, n.getChild(0).getValue(), arguments);
  }

  @Override
  protected boolean visitObjectLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    if (n.getChild(i).getKind() == CAstNode.EMPTY) {
      handleUnspecifiedLiteralKey();
    }
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(i), n, c.getValue(n.getChild(i + 1)));
  }

  @Override
  protected void leaveObjectLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  @Override
  protected boolean visitArrayLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveArrayLiteralObject(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  @Override
  protected void leaveArrayLiteralInitElement(CAstNode n, int i, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n,
        new int[] { context.currentScope().getConstantValue(new Integer(i - 1)) }, c.getValue(n.getChild(i)));
  }

  @Override
  protected void leaveArrayLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitObjectRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  @Override
  protected void leaveObjectRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = c.getValue(n);
    CAstNode elt = n.getChild(1);
    doFieldRead(context, result, c.getValue(n.getChild(0)), elt, n);
  }

  @Override
  public boolean visitAssign(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  public void leaveAssign(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    if (n.getKind() == CAstNode.ASSIGN) {
      c.setValue(n, c.getValue(n.getChild(1)));
    } else {
      c.setValue(n, c.getValue(n.getChild(0)));
    }
  }

  private static int[] gatherArrayDims(WalkContext c, CAstNode n) {
    int numDims = n.getChildCount() - 2;
    int[] dims = new int[numDims];
    for (int i = 0; i < numDims; i++)
      dims[i] = c.getValue(n.getChild(i + 2));
    return dims;
  }

  /* Prereq: a.getKind() == ASSIGN_PRE_OP || a.getKind() == ASSIGN_POST_OP */
  protected int processAssignOp(CAstNode v, CAstNode a, int temp, WalkContext c) {
    WalkContext context = c;
    int rval = c.getValue(v);
    CAstNode op = a.getChild(2);
    int temp2 = context.currentScope().allocateTempValue();

    boolean mayBeInteger = handleBinaryOpThrow(a, op, context);

    context.cfg().addInstruction(
        insts.BinaryOpInstruction(context.cfg().currentInstruction, translateBinaryOpcode(op), false, false, temp2, temp, rval, mayBeInteger));

    if (mayBeInteger) {
      context.cfg().newBlock(true);
    }

    return temp2;
  }

  @Override
  protected boolean visitArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int rval = c.getValue(v);
    c.setValue(n, rval);
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n, gatherArrayDims(c, n), rval);
  }

  @Override
  protected boolean visitArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int temp = context.currentScope().allocateTempValue();
    int[] dims = gatherArrayDims(c, n);
    arrayOpHandler.doArrayRead(context, temp, c.getValue(n.getChild(0)), n, dims);
    int rval = processAssignOp(v, a, temp, c);
    c.setValue(n, pre ? rval : temp);
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n, dims, rval);
  }

  @Override
  protected boolean visitObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int rval = c.getValue(v);
    c.setValue(n, rval);
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  @Override
  protected boolean visitObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int temp = context.currentScope().allocateTempValue();
    doFieldRead(context, temp, c.getValue(n.getChild(0)), n.getChild(1), n);
    int rval = processAssignOp(v, a, temp, c);
    c.setValue(n, pre ? rval : temp);
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  @Override
  protected boolean visitBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  @Override
  protected boolean visitBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  @Override
  protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  /**
   * assign rval to nm as appropriate, depending on the scope of ls
   */
  protected void assignValue(CAstNode n, WalkContext context, Symbol ls, String nm, int rval) {
    if (context.currentScope().isGlobal(ls))
      doGlobalWrite(context, nm, makeType(ls.type()), rval);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, makeType(ls.type()), rval);
    } else {
      assert rval != -1 : CAstPrinter.print(n, context.top().getSourceMap());
      doLocalWrite(context, nm, makeType(ls.type()), rval);
    }
  }

  @Override
  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int rval = c.getValue(v);
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    c.setValue(n, rval);
    assignValue(n, context, ls, nm, rval);
  }

  @Override
  protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    TypeReference type = makeType(ls.type());
    int temp;

    if (context.currentScope().isGlobal(ls))
      temp = doGlobalRead(n, context, nm, type);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      temp = doLexicallyScopedRead(n, context, nm, type);
    } else {
      temp = doLocalRead(context, nm, type);
    }

    if (!pre) {
      int ret = context.currentScope().allocateTempValue();
      context.cfg().addInstruction(new AssignInstruction(context.cfg().currentInstruction, ret, temp));
      c.setValue(n, ret);
    }

    int rval = processAssignOp(v, a, temp, c);

    if (pre) {
      c.setValue(n, rval);
    }

    if (context.currentScope().isGlobal(ls)) {
      doGlobalWrite(context, nm, type, rval);
    } else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, type, rval);
    } else {
      doLocalWrite(context, nm, type, rval);
    }
  }

  private static boolean isSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    CAstControlFlowMap ctrl = context.getControlFlow();
    Collection<Object> caseLabels = ctrl.getTargetLabels(n);
    for (Object x : caseLabels) {
      if (x == CAstControlFlowMap.SWITCH_DEFAULT)
        continue;

      CAstNode xn = (CAstNode) x;
      if (xn.getKind() == CAstNode.CONSTANT) {
        visitor.visit(xn, context, visitor);
        if (context.getValue(xn) != -1) {
          if (context.currentScope().isConstant(context.getValue(xn))) {
            Object val = context.currentScope().getConstantObject(context.getValue(xn));
            if (val instanceof Number) {
              Number num = (Number) val;
              if (num.intValue() == num.doubleValue()) {
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

  private void doSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    PreBasicBlock defaultHackBlock = null;
    CAstControlFlowMap ctrl = context.getControlFlow();

    CAstNode switchValue = n.getChild(0);
    visitor.visit(switchValue, context, visitor);
    int v = context.getValue(switchValue);

    boolean hasExplicitDefault = ctrl.getTarget(n, CAstControlFlowMap.SWITCH_DEFAULT) != null;

    Collection<Object> caseLabels = ctrl.getTargetLabels(n);
    int cases = caseLabels.size();
    if (hasExplicitDefault)
      cases--;
    int[] casesAndLabels = new int[cases * 2];

    int defaultBlock = context.cfg().getCurrentBlock().getGraphNodeId() + 1;

    context.cfg().addInstruction(insts.SwitchInstruction(context.cfg().currentInstruction, v, defaultBlock, casesAndLabels));
    context.cfg().addPreNode(n, context.getUnwindState());
    // PreBasicBlock switchB = context.cfg().getCurrentBlock();
    context.cfg().newBlock(true);

    context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));
    defaultHackBlock = context.cfg().getCurrentBlock();
    context.cfg().newBlock(false);

    CAstNode switchBody = n.getChild(1);
    visitor.visit(switchBody, context, visitor);
    context.cfg().newBlock(true);

    if (!hasExplicitDefault) {
      context.cfg().addEdge(defaultHackBlock, context.cfg().getCurrentBlock());
    }

    int cn = 0;
    for (Object x : caseLabels) {
      CAstNode target = ctrl.getTarget(n, x);
      if (x == CAstControlFlowMap.SWITCH_DEFAULT) {
        context.cfg().addEdge(defaultHackBlock, context.cfg().getBlock(target));
      } else {
        Number caseLabel = (Number) context.currentScope().getConstantObject(context.getValue((CAstNode) x));
        casesAndLabels[2 * cn] = caseLabel.intValue();
        casesAndLabels[2 * cn + 1] = context.cfg().getBlock(target).getGraphNodeId();
        cn++;

        context.cfg().addPreEdge(n, target, false);
      }
    }
  }

  private void doIfConvertSwitch(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    CAstControlFlowMap ctrl = context.getControlFlow();
    context.cfg().addPreNode(n, context.getUnwindState());

    CAstNode switchValue = n.getChild(0);
    visitor.visit(switchValue, context, visitor);
    int v = context.getValue(switchValue);

    Collection<Object> caseLabels = ctrl.getTargetLabels(n);
    Map<Object, PreBasicBlock> labelToBlock = new LinkedHashMap<>();
    for (Object x : caseLabels) {
      if (x != CAstControlFlowMap.SWITCH_DEFAULT) {
        visitor.visit((CAstNode) x, context, visitor);
        context.cfg().addInstruction(
            insts.ConditionalBranchInstruction(context.cfg().currentInstruction, translateConditionOpcode(CAstOperator.OP_EQ), null, v, context.getValue((CAstNode) x), -1));
        labelToBlock.put(x, context.cfg().getCurrentBlock());
        context.cfg().newBlock(true);
      }
    }

    PreBasicBlock defaultGotoBlock = context.cfg().getCurrentBlock();
    context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));
    context.cfg().newBlock(false);

    CAstNode switchBody = n.getChild(1);
    visitor.visit(switchBody, context, visitor);
    context.cfg().newBlock(true);

    for (Object x : caseLabels) {
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

  @Override
  protected boolean visitSwitch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    if (isSimpleSwitch(n, context, visitor)) {
      doSimpleSwitch(n, context, visitor);
    } else {
      doIfConvertSwitch(n, context, visitor);
    }
    return true;
  }

  // Make final to prevent overriding
  @Override
  protected final void leaveSwitchValue(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveSwitch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitThrow(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveThrow(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    doThrow(context, c.getValue(n.getChild(0)));

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(false);

    Collection<Object> labels = context.getControlFlow().getTargetLabels(n);
    for (Object label : labels) {
      CAstNode target = context.getControlFlow().getTarget(n, label);
      if (target == CAstControlFlowMap.EXCEPTION_TO_EXIT)
        context.cfg().addPreEdgeToExit(n, true);
      else
        context.cfg().addPreEdge(n, target, true);
    }
  }

  @Override
  protected boolean visitCatch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;

    // unreachable catch block
    if (context.getControlFlow().getSourceNodes(n).isEmpty()) {
      return true;
    }

    String id = (String) n.getChild(0).getValue();
    context.cfg().setCurrentBlockAsHandler();
    if (!context.currentScope().contains(id)) {
      context.currentScope().declare(new FinalCAstSymbol(id, exceptionType()));
    }
    context.cfg().addInstruction(
        insts.GetCaughtExceptionInstruction(context.cfg().currentInstruction, context.cfg().getCurrentBlock().getNumber(), context.currentScope().lookup(id)
            .valueNumber()));

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

  @Override
  protected void leaveCatch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitUnwind(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveUnwind(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  private boolean hasIncomingEdges(CAstNode n, WalkContext context) {
    if (context.cfg().hasDelayedEdges(n)) {
      return true;
    } else {
      for (int i = 0; i < n.getChildCount(); i++) {
        if (hasIncomingEdges(n.getChild(i), context)) {
          return true;
        }
      }

      return false;
    }
  }

  @Override
  protected boolean visitTry(final CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    final WalkContext context = c;
    boolean addSkipCatchGoto = false;
    visitor.visit(n.getChild(0), context, visitor);
    PreBasicBlock endOfTry = context.cfg().getCurrentBlock();

    if (!hasIncomingEdges(n.getChild(1), context)) {
      if (loader instanceof CAstAbstractLoader) {
        ((CAstAbstractLoader) loader).addMessage(context.getModule(), new Warning(Warning.MILD) {
          @Override
          public String getMsg() {
            return "Dead catch block at " + getPosition(context.getSourceMap(), n.getChild(1));
          }
        });
      }
      return true;
    }

    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      addSkipCatchGoto = true;
      context.cfg().addInstruction(insts.GotoInstruction(context.cfg().currentInstruction, -1));
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
  @Override
  protected final void leaveTryBlock(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected final void leaveTry(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  @Override
  protected boolean visitEmpty(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveEmpty(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    c.setValue(n, context.currentScope().getConstantValue(null));
  }

  @Override
  protected boolean visitPrimitive(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leavePrimitive(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);

    doPrimitive(result, context, n);
  }

  @Override
  protected boolean visitVoid(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveVoid(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, -1);
  }

  @Override
  protected boolean visitAssert(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  @Override
  protected void leaveAssert(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = c;
    boolean fromSpec = true;
    int result = c.getValue(n.getChild(0));
    if (n.getChildCount() == 2) {
      assert n.getChild(1).getKind() == CAstNode.CONSTANT;
      assert n.getChild(1).getValue() instanceof Boolean;
      fromSpec = n.getChild(1).getValue().equals(Boolean.TRUE);
    }
    context.cfg().addInstruction(new AstAssertInstruction(context.cfg().currentInstruction, result, fromSpec));
  }

  @Override
  protected boolean visitEachElementGet(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveEachElementGet(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = c.currentScope().allocateTempValue();
    c.setValue(n, result);
    c.cfg().addInstruction(new EachElementGetInstruction(c.cfg().currentInstruction, result, c.getValue(n.getChild(0)), c.getValue(n.getChild(1))));
  }

  @Override
  protected boolean visitEachElementHasNext(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveEachElementHasNext(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = c.currentScope().allocateTempValue();
    c.setValue(n, result);
    c.cfg().addInstruction(new EachElementHasNextInstruction(c.cfg().currentInstruction, result, c.getValue(n.getChild(0)), c.getValue(n.getChild(1))));
  }

  @Override
  protected boolean visitTypeLiteralExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveTypeLiteralExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = c;
    assert n.getChild(0).getKind() == CAstNode.CONSTANT;
    String typeNameStr = (String) n.getChild(0).getValue();
    TypeName typeName = TypeName.string2TypeName(typeNameStr);
    TypeReference typeRef = TypeReference.findOrCreate(loader.getReference(), typeName);

    int result = wc.currentScope().allocateTempValue();
    c.setValue(n, result);

    wc.cfg().addInstruction(insts.LoadMetadataInstruction(wc.cfg().currentInstruction, result, loader.getLanguage().getConstantType(typeRef), typeRef));
  }

  @Override
  protected boolean visitIsDefinedExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveIsDefinedExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = c;
    int ref = c.getValue(n.getChild(0));
    int result = wc.currentScope().allocateTempValue();
    c.setValue(n, result);
    if (n.getChildCount() == 1) {
      wc.cfg().addInstruction(new AstIsDefinedInstruction(wc.cfg().currentInstruction, result, ref));
    } else {
      doIsFieldDefined(wc, result, ref, n.getChild(1));
    }
  }

  @Override
  protected boolean visitEcho(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveEcho(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = c;

    int rvals[] = new int[n.getChildCount()];
    for (int i = 0; i < n.getChildCount(); i++) {
      rvals[i] = c.getValue(n.getChild(i));
    }

    wc.cfg().addInstruction(new AstEchoInstruction(wc.cfg().currentInstruction, rvals));
  }

  public CAstEntity getIncludedEntity(CAstNode n) {
    if (n.getChild(0).getKind() == CAstNode.NAMED_ENTITY_REF) {
      assert namedEntityResolver != null;
      return namedEntityResolver.get(n.getChild(0).getChild(0).getValue());
    } else {
      return (CAstEntity) n.getChild(0).getValue();
    }
  }

  @Override
  protected void leaveInclude(final CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = c;

    CAstEntity included = getIncludedEntity(n);

    if (included == null) {
      System.err.println(("cannot find include for " + CAstPrinter.print(n)));
      System.err.println(("from:\n" + namedEntityResolver));
    } else {
      final boolean isMacroExpansion = (included.getKind() == CAstEntity.MACRO_ENTITY);

      System.err.println("found " + included.getName() + " for " + CAstPrinter.print(n));

      final CAstEntity copy = (new CAstCloner(new CAstImpl(), true) {

        private CAstNode copyIncludeExpr(CAstNode expr) {
          if (expr.getValue() != null) {
            return Ast.makeConstant(expr.getValue());
          } else if (expr instanceof CAstOperator) {
            return expr;
          } else {
            CAstNode nc[] = new CAstNode[expr.getChildCount()];

            for (int i = 0; i < expr.getChildCount(); i++) {
              nc[i] = copyIncludeExpr(expr.getChild(i));
            }

            return Ast.makeNode(expr.getKind(), nc);
          }
        }

        @Override
        protected CAstNode copyNodes(CAstNode root, final CAstControlFlowMap cfg, NonCopyingContext c, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
          if (isMacroExpansion && root.getKind() == CAstNode.MACRO_VAR) {
            int arg = ((Number) root.getChild(0).getValue()).intValue();
            CAstNode expr = copyIncludeExpr(n.getChild(arg));
            nodeMap.put(Pair.make(root, c.key()), expr);
            return expr;
          } else {
            return super.copyNodesHackForEclipse(root, cfg, c, nodeMap);
          }
        }
      }).rewrite(included);

      if (copy.getAST() == null) {
        System.err.println((copy.getName() + " has no AST"));

      } else {
        visit(copy.getAST(), new DelegatingContext(wc) {
          @Override
          public CAstSourcePositionMap getSourceMap() {
            return copy.getSourceMap();
          }

          @Override
          public CAstControlFlowMap getControlFlow() {
            return copy.getControlFlow();
          }
        }, visitor);

        visitor.visitScopedEntities(copy, copy.getAllScopedEntities(), wc, visitor);
      }
    }
  }

  protected final void walkEntities(CAstEntity N, WalkContext c) {
    visitEntities(N, c, this);
  }

  public final class RootContext implements WalkContext {
    private final Scope globalScope;
    
    private final CAstEntity N;

    private final ModuleEntry module;

    private final Map<CAstEntity, String> entityNames = new LinkedHashMap<>();

   public RootContext(CAstEntity N, ModuleEntry module) {
      this.N = N;
      this.module = module;
      this.globalScope = makeGlobalScope();
    }

    @Override
    public ModuleEntry getModule() {
      return module;
    }

    @Override
    public String file() {
      return module.getName();
    }

    @Override
    public CAstEntity top() {
      return N;
    }

    @Override
    public Scope currentScope() {
      return globalScope;
    }

    @Override
    public Set<Scope> entityScopes() {
      return Collections.singleton(globalScope);
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      return N.getSourceMap();
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      return N.getControlFlow();
    }

    @Override
    public IncipientCFG cfg() {
      return null;
    }

    @Override
    public UnwindState getUnwindState() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void setCatchType(IBasicBlock<SSAInstruction> bb, TypeReference catchType) {
    }

    @Override
    public void setCatchType(CAstNode castNode, TypeReference catchType) {
    }

    @Override
    public Map<IBasicBlock<SSAInstruction>, TypeReference[]> getCatchTypes() {
      return null;
    }
    
    @Override
    public void addEntityName(CAstEntity e, String name) {
      entityNames.put(e, name);
    }

    @Override
    public String getEntityName(CAstEntity e) {
      if (e == null) {
        return null;
      } else {
        assert entityNames.containsKey(e);
        return "L" + entityNames.get(e);
      }
    }

    @Override
    public boolean hasValue(CAstNode n) {
      assert false;
      return false;
    }

    @Override
    public int setValue(CAstNode n, int v) {
      assert false;
      return 0;
    }

    @Override
    public int getValue(CAstNode n) {
      assert false;
      return -1;
    }

    @Override
    public Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet) {
      assert false;
      return null;
    }

    @Override
    public Set<Access> getAccesses(CAstEntity e) {
      assert false;
      return null;
    }

    @Override
    public Scope getGlobalScope(){
      return globalScope;
    }
    
  }

  /**
   * translate module, represented by {@link CAstEntity} N
   */
  @Override
  public void translate(final CAstEntity N, final ModuleEntry module) {
    if (DEBUG_TOP)
      System.err.println(("translating " + module.getName()));
    // this.inlinedSourceMap = inlinedSourceMap;
    final ExposedNamesCollector exposedNamesCollector = new ExposedNamesCollector();
    exposedNamesCollector.run(N);
    entity2ExposedNames = exposedNamesCollector.getEntity2ExposedNames();
    // CAstEntity rewrite = (new ExposedParamRenamer(new CAstImpl(),
    // entity2ExposedNames)).rewrite(N);
    walkEntities(N, new RootContext(N, module));
  }

  public void translate(final CAstEntity N, final WalkContext context) {
    final ExposedNamesCollector exposedNamesCollector = new ExposedNamesCollector();
    exposedNamesCollector.run(N);
    entity2ExposedNames = exposedNamesCollector.getEntity2ExposedNames();
    walkEntities(N, context);
  }
 
}
