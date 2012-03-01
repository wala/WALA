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
import com.ibm.wala.cast.ir.ssa.AstConstants;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.ir.ssa.SSAConversion;
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
import com.ibm.wala.cast.tree.impl.CAstCloner;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstRewriter;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.CAstSymbolImplBase;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrikeBT.ShiftInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
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
   * set to true to use new handling of lexical scoping
   */
  public static boolean NEW_LEXICAL = true;


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

  /**
   * given accesses in a method to variables defined in an enclosing lexical
   * scope, is it legal to read the variable into a local l once at the
   * beginning of the method, operate on l through the method body (rather than
   * performing separate lexical read / write operations), and write back the
   * value in l (if necessary) at the end of the method?
   */
  protected abstract boolean useLocalValuesForLexicalVars();

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
  protected abstract void defineFunction(CAstEntity N, WalkContext definingContext, AbstractCFG cfg, SymbolTable symtab,
      boolean hasCatchBlock, TypeReference[][] caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo,
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
  public abstract void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues);

  /**
   * generate IR for a CAst array write, updating context.cfg()
   */
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
  protected void handleUnspecifiedLiteralKey(WalkContext context, CAstNode objectLiteralNode, int unspecifiedLiteralIndex,
      CAstVisitor<WalkContext> visitor) {
    Assertions.UNREACHABLE();
  }

  /**
   * generate prologue code for each function body
   */
  protected void doPrologue(WalkContext context) {
    // if we are SSA converting lexical accesses, add a placeholder instruction
    // eventually (via mutation of its Access array) reads all relevant lexical
    // variables at the beginning of the method.
    if (useLocalValuesForLexicalVars()) {
      context.cfg().addInstruction(new AstLexicalRead(new Access[0]));
    } else {
      // perform a lexical write to copy the value stored in the local
      // associated with each parameter to the lexical name
      final CAstEntity entity = context.top();
      Set<String> exposedNames = entity2ExposedNames.get(entity);
      if (exposedNames != null) {
        for (String arg : entity.getArgumentNames()) {
          if (exposedNames.contains(arg)) {
            final Scope currentScope = context.currentScope();
            Symbol symbol = currentScope.lookup(arg);
            assert symbol.getDefiningScope() == currentScope;
            int argVN = symbol.valueNumber();
            Access A = new Access(arg, context.getEntityName(entity), argVN);
            context.cfg().addInstruction(new AstLexicalWrite(A));
          }
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
  protected int doLocalRead(WalkContext context, String name) {
    if (!useLocalValuesForLexicalVars()) {
      CAstEntity entity = context.top();
      Set<String> exposed = entity2ExposedNames.get(entity);
      if (exposed != null && exposed.contains(name)) {
        return doLexReadHelper(context, name);
      }
    }
    return context.currentScope().lookup(name).valueNumber();
  }

  /**
   * add an {@link AssignInstruction} to context.cfg() that copies rval to the
   * value number of local nm. Note that the caller is responsible for ensuring
   * that nm is defined in the local scope.
   */
  protected void doLocalWrite(WalkContext context, String nm, int rval) {
    if (!useLocalValuesForLexicalVars()) {
      CAstEntity entity = context.top();
      Set<String> exposed = entity2ExposedNames.get(entity);
      if (exposed != null && exposed.contains(nm)) {
        // use a lexical write
        doLexicallyScopedWrite(context, nm, rval);
        return;
      }
    }
    int lval = context.currentScope().lookup(nm).valueNumber();
    if (lval != rval) {
      context.cfg().addInstruction(new AssignInstruction(lval, rval));
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
   * @return
   */
  protected int doLexicallyScopedRead(CAstNode node, WalkContext context, final String name) {
    return doLexReadHelper(context, name);
  }

  /**
   * we only have this method to avoid having to pass a node parameter at other
   * call sites, as would be required for
   * {@link #doLexicallyScopedRead(CAstNode, WalkContext, String)}
   */
  private int doLexReadHelper(WalkContext context, final String name) {
    Symbol S = context.currentScope().lookup(name);
    Scope definingScope = S.getDefiningScope();
    CAstEntity E = definingScope.getEntity();
    // record in declaring scope that the name is exposed to a nested scope
//<<<<<<< .mine
//    Symbol S = context.currentScope().lookup(name);
//    CAstEntity E = S.getDefiningScope().getEntity();
//    addExposedName(E, E, name, S.getDefiningScope().lookup(name).valueNumber(), false, context);
//=======
    addExposedName(E, E, name, definingScope.lookup(name).valueNumber(), false, context);
//>>>>>>> .r4421

    final String entityName = context.getEntityName(E);
    if (useLocalValuesForLexicalVars()) {
      // lexically-scoped variables can be given a single vn in a method
//<<<<<<< .mine
//      Access A = new Access(name, context.getEntityName(E), vn);
//=======
//>>>>>>> .r4421

//<<<<<<< .mine
      // (context.top() is current entity)
      // record the name as exposed for the current entity, since if the name is
      // updated via a call to a nested function, SSA for the current entity may
      // need to be updated with the new definition
//      addExposedName(context.top(), E, name, vn, false, context);
//=======
      markExposedInEnclosingEntities(context, name, definingScope, E, entityName, false);
//>>>>>>> .r4421

//<<<<<<< .mine
      // record the access; later, the Accesses in the instruction
      // defining vn will be adjusted based on this information; see
      // patchLexicalAccesses()
//      addAccess(context, context.top(), A);
//=======
      return S.valueNumber();
//>>>>>>> .r4421

    } else {
      // lexically-scoped variables should be read from their scope each time
      int result = context.currentScope().allocateTempValue();
//<<<<<<< .mine
//      Access A = new Access(name, context.getEntityName(E), result);
//=======
      Access A = new Access(name, entityName, result);
//>>>>>>> .r4421
      context.cfg().addInstruction(new AstLexicalRead(A));
      markExposedInEnclosingEntities(context, name, definingScope, E, entityName, false);
      return result;
    }
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
  private void markExposedInEnclosingEntities(WalkContext context, final String name, Scope definingScope, CAstEntity E,
      final String entityName, boolean isWrite) {
    Scope curScope = context.currentScope();
    while (!curScope.equals(definingScope)) {
      final Symbol curSymbol = curScope.lookup(name);
      final int vn = curSymbol.valueNumber();
      final Access A = new Access(name, entityName, vn);
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
  protected void doLexicallyScopedWrite(WalkContext context, String name, int rval) {
    Symbol S = context.currentScope().lookup(name);
    Scope definingScope = S.getDefiningScope();
    CAstEntity E = definingScope.getEntity();
    // record in declaring scope that the name is exposed to a nested scope
    addExposedName(E, E, name, definingScope.lookup(name).valueNumber(), true, context);

    if (useLocalValuesForLexicalVars()) {
      // lexically-scoped variables can be given a single vn in a method
      
      markExposedInEnclosingEntities(context, name, definingScope, E, context.getEntityName(E), true);

      context.cfg().addInstruction(new AssignInstruction(S.valueNumber(), rval));
      // we add write instructions at every access for now
      // eventually, we may restructure the method to do a single combined write
      // before exit
      Access A = new Access(name, context.getEntityName(E), rval);
      context.cfg().addInstruction(new AstLexicalWrite(A));

    } else {
      // lexically-scoped variables must be written in their scope each time
      Access A = new Access(name, context.getEntityName(E), rval);
      context.cfg().addInstruction(new AstLexicalWrite(A));
      markExposedInEnclosingEntities(context, name, definingScope, E, context.getEntityName(E), true);
    }
  }

  /**
   * generate instructions for a read of a global
   */
  protected int doGlobalRead(CAstNode node, WalkContext context, String name) {
    Symbol S = context.currentScope().lookup(name);

    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      // lexically-scoped variables can be given a single vn in a method, or
      if (useLocalValuesForLexicalVars()) {
        int vn = S.valueNumber();
        Access A = new Access(name, null, vn);

        addExposedName(context.top(), null, name, vn, false, context);
        addAccess(context, context.top(), A);

        return vn;

        // lexically-scoped variables can be read from their scope each time
      } else {
        int result = context.currentScope().allocateTempValue();
        Access A = new Access(name, null, result);
        context.cfg().addInstruction(new AstLexicalRead(A));
        addAccess(context, context.top(), A);
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

  /**
   * generate instructions for a write of a global
   */
  protected void doGlobalWrite(WalkContext context, String name, int rval) {
    Symbol S = context.currentScope().lookup(name);

    // Global variables can be treated as lexicals defined in the CG root, or
    if (treatGlobalsAsLexicallyScoped()) {

      // lexically-scoped variables can be given a single vn in a method, or
      if (useLocalValuesForLexicalVars()) {
        int vn = S.valueNumber();
        Access A = new Access(name, null, vn);

        addExposedName(context.top(), null, name, vn, true, context);
        addAccess(context, context.top(), A);

        context.cfg().addInstruction(new AssignInstruction(vn, rval));
        context.cfg().addInstruction(new AstLexicalWrite(A));

        // lexically-scoped variables can be read from their scope each time
      } else {
        Access A = new Access(name, null, rval);
        context.cfg().addInstruction(new AstLexicalWrite(A));
        addAccess(context, context.top(), A);
      }

      // globals can be treated as a single static location
    } else {
      FieldReference global = makeGlobalRef(name);
      context.cfg().addInstruction(new AstGlobalWrite(global, rval));
    }
  }

  /**
   * generate instructions to check if ref has field, storing answer in result
   */
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
  protected final Map namedEntityResolver;

  protected final SSAInstructionFactory insts;

  protected AstTranslator(IClassLoader loader, Map namedEntityResolver, ArrayOpHandler arrayOpHandler) {
    this.loader = loader;
    this.namedEntityResolver = namedEntityResolver;
    this.arrayOpHandler = arrayOpHandler!=null? arrayOpHandler: this;
    this.insts = loader.getInstructionFactory();
  }

  protected AstTranslator(IClassLoader loader, Map namedEntityResolver) {
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

  public static final boolean DEBUG_NAMES = DEBUG_ALL || false;

  public static final boolean DEBUG_LEXICAL = DEBUG_ALL || false;

  /**
   * basic block implementation used in the CFGs constructed during the
   * IR-generating AST traversal
   */
  protected final static class PreBasicBlock implements INodeWithNumber, IBasicBlock<SSAInstruction> {
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

  /**
   * holds the control-flow graph as it is being constructed. When construction
   * is complete, information is stored in an {@link AstCFG}
   */
  public final class IncipientCFG extends SparseNumberedGraph<PreBasicBlock> {

    protected class Unwind {
      private final Map<PreBasicBlock, UnwindState> unwindData = new LinkedHashMap<PreBasicBlock, UnwindState>();

      /**
       * a cache of generated blocks
       */
      private final Map<Pair<UnwindState, Pair<PreBasicBlock, Boolean>>, PreBasicBlock> code = new LinkedHashMap<Pair<UnwindState, Pair<PreBasicBlock, Boolean>>, PreBasicBlock>();

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
            addInstruction(insts.GotoInstruction());
            newBlock(false);
          }
          PreBasicBlock startBlock = getCurrentBlock();
          if (exception) {
            setCurrentBlockAsHandler();
            e = sourceContext.astContext.currentScope().allocateTempValue();
            addInstruction(insts.GetCaughtExceptionInstruction(startBlock.getNumber(), e));
            sourceContext.astContext.setCatchType(startBlock.getNumber(), defaultCatchType());
          }

          while (sourceContext != null && (targetContext == null || !targetContext.covers(sourceContext))) {
            final CAstRewriter.Rewrite ast = (new CAstCloner(new CAstImpl()) {
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
              public CAstSourcePositionMap getSourceMap() {
                return ast.newPos();
              }

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
            addInstruction(insts.GotoInstruction());
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
      bb.makeEntryBlock();
    }

    void makeExitBlock(PreBasicBlock bb) {
      bb.makeExitBlock();

      for (Iterator<? extends PreBasicBlock> ps = getPredNodes(bb); ps.hasNext();)
        normalToExit.add(ps.next());

      // now that we have created the exit block, add the delayed edges to the
      // exit
      checkForRealizedExitEdges(bb);
    }

    void setCurrentBlockAsHandler() {
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
        for (Iterator<Pair<PreBasicBlock, Boolean>> ss = delayedEdges.get(n).iterator(); ss.hasNext();) {
          Pair<PreBasicBlock, Boolean> s = ss.next();
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
        for (Iterator<Pair<PreBasicBlock, Boolean>> ss = delayedEdges.get(exitMarker).iterator(); ss.hasNext();) {
          Pair<PreBasicBlock, Boolean> s = ss.next();
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
  }

  /**
   * data structure for the final CFG for a method, based on the information in
   * an {@link IncipientCFG}
   */
  protected final static class AstCFG extends AbstractCFG<SSAInstruction, PreBasicBlock> {
    private final SSAInstruction[] instructions;

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
          System.err.println(("added " + blocks.get(i) + " to final CFG as " + getNumber(blocks.get(i))));
      }
      if (DEBUG_CFG)
        System.err.println((getMaxNumber() + " blocks total"));

      init();

      for (int i = 0; i < blocks.size(); i++) {
        PreBasicBlock src = blocks.get(i);
        for (Iterator j = icfg.getSuccNodes(src); j.hasNext();) {
          PreBasicBlock dst = (PreBasicBlock) j.next();
          if (isCatchBlock(dst.getNumber()) || (dst.isExitBlock() && icfg.exceptionalToExit.contains(src))) {
            if (DEBUG_CFG)
              System.err.println(("exceptonal edge " + src + " -> " + dst));
            addExceptionalEdge(src, dst);
          }

          if (dst.isExitBlock() ? icfg.normalToExit.contains(src) : !isCatchBlock(dst.getNumber())) {
            if (DEBUG_CFG)
              System.err.println(("normal edge " + src + " -> " + dst));
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

    public PreBasicBlock getBlockForInstruction(int index) {
      for (int i = 1; i < getNumberOfNodes() - 1; i++)
        if (index <= instructionToBlockMap[i])
          return getNode(i);

      return null;
    }

    public SSAInstruction[] getInstructions() {
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
            s.append("  " + insts[j].toString(symtab) + "\n");
      }

      s.append("-- END --");
      return s.toString();
    }
  }

  public static enum ScopeType {
    LOCAL, GLOBAL, SCRIPT, FUNCTION, TYPE
  };

  private static final boolean DEBUG = false;

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

    public boolean isConstant(int valueNumber) {
      return getUnderlyingSymtab().isConstant(valueNumber);
    }

    public Object getConstantObject(int valueNumber) {
      return getUnderlyingSymtab().getConstantValue(valueNumber);
    }

    public void declare(CAstSymbol s, int vn) {
      String nm = s.name();
      assert !contains(nm) : nm;
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
        assert !s.isFinal() : "trying to redeclare " + nm;
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

    abstract protected Symbol makeSymbol(String nm, boolean isFinal, boolean isInternalName, Object defaultInitValue, int vn,
        Scope parent);

    public boolean isCaseInsensitive(String nm) {
      return caseInsensitiveNames.containsKey(nm.toLowerCase());
    }

    public Symbol lookup(String nm) {
      if (contains(nm)) {
        return values.get(mapName(nm));
      } else {
        Symbol scoped = parent.lookup(nm);
        if (scoped != null && getEntityScope() == this && (isGlobal(scoped) || isLexicallyScoped(scoped))) {
          values.put(nm,
              makeSymbol(nm, scoped.isFinal(), scoped.isInternalName(), scoped.defaultInitValue(), -1, scoped.getDefiningScope()));
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
      return s.getDefiningScope().type() == ScopeType.GLOBAL;
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
          return ((AbstractScope) s.getDefiningScope()).getEntity() != getEntity();
      }

      public CAstEntity getEntity() {
        return s;
      }

      public ScopeType type() {
        return ScopeType.SCRIPT;
      }

      protected Symbol makeSymbol(final String nm, final boolean isFinal, final boolean isInternalName,
          final Object defaultInitValue, int vn, Scope definer) {
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            setDefaultValue(getUnderlyingSymtab(), v, defaultInitValue);
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

      // ctor for scope object
      {
        for (int i = 0; i < getArgumentCount(f); i++) {
          final int yuck = i;
          declare(new CAstSymbol() {
            public String name() {
              return params[yuck];
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
          return ((AbstractScope) s.getDefiningScope()).getEntity() != getEntity();
      }

      public CAstEntity getEntity() {
        return f;
      }

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

      protected Symbol makeSymbol(final String nm, final boolean isFinal, final boolean isInternalName,
          final Object defaultInitValue, final int valueNumber, Scope definer) {
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
      public ScopeType type() {
        return ScopeType.LOCAL;
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

      protected Symbol makeSymbol(final String nm, boolean isFinal, final boolean isInternalName, final Object defaultInitValue,
          int vn, Scope definer) {
        final int v = vn == -1 ? getUnderlyingSymtab().newSymbol() : vn;
        if (useDefaultInitValues() && defaultInitValue != null) {
          if (getUnderlyingSymtab().getValue(v) == null) {
            setDefaultValue(getUnderlyingSymtab(), v, defaultInitValue);
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

      public ScopeType type() {
        return ScopeType.GLOBAL;
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
        assert vn == -1;
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

      public ScopeType type() {
        return ScopeType.TYPE;
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
        assert vn == -1;
        declare(s);
      }

      public void declare(final CAstSymbol s) {
        final String name = s.name();
        assert !s.isFinal();
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

  public interface WalkContext extends CAstVisitor.Context {

    ModuleEntry getModule();

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

    public Set<Access> getAccesses(CAstEntity e) {
      return parent.getAccesses(e);
    }

    public ModuleEntry getModule() {
      return parent.getModule();
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

    public void addEntityName(CAstEntity e, String name) {
      parent.addEntityName(e, name);
    }
    
    public String getEntityName(CAstEntity e) {
      return parent.getEntityName(e);
    }
    
    public boolean hasValue(CAstNode n) {
      return parent.hasValue(n);
    }

    public int setValue(CAstNode n, int v) {
      return parent.setValue(n, v);
    }

    public int getValue(CAstNode n) {
      return parent.getValue(n);
    }

    public Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet) {
      return parent.exposeNameSet(entity, writeSet);
    }

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

    Set<Pair<Pair<String, String>, Integer>> exposedReads;
    Set<Pair<Pair<String, String>, Integer>> exposedWrites;
    
    Set<Access> accesses;
    
    /**
     * maps nodes in the current function to the value number holding their value
     * or, for constants, to their constant value.
     */
    private final Map<CAstNode, Integer> results = new LinkedHashMap<CAstNode, Integer>();

    CodeEntityContext(WalkContext parent, Scope entityScope, CAstEntity s) {
      super(parent, s);

      this.topEntityScope = entityScope;

      this.allEntityScopes = HashSetFactory.make();
      this.allEntityScopes.add(entityScope);

      cfg = new IncipientCFG();
    }

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
    
    public boolean hasValue(CAstNode n) {
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

    private int[] buildLexicalUseArray(Pair<Pair<String, String>, Integer>[] exposedNames, String entityName) {
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

    private Pair<String, String>[] buildLexicalNamesArray(Pair<Pair<String, String>, Integer>[] exposedNames) {
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

    @SuppressWarnings("unchecked")
    AstLexicalInformation(String entityName, Scope scope, SSAInstruction[] instrs,
        Set<Pair<Pair<String, String>, Integer>> exposedNamesForReadSet,
        Set<Pair<Pair<String, String>, Integer>> exposedNamesForWriteSet, Set<Access> accesses) {
      this.functionLexicalName = entityName;

      Pair<Pair<String, String>, Integer>[] EN = null;
      if (exposedNamesForReadSet != null || exposedNamesForWriteSet != null) {
        Set<Pair<Pair<String, String>, Integer>> exposedNamesSet = new HashSet<Pair<Pair<String, String>, Integer>>();
        if (exposedNamesForReadSet != null) {
          exposedNamesSet.addAll(exposedNamesForReadSet);
        }
        if (exposedNamesForWriteSet != null) {
          exposedNamesSet.addAll(exposedNamesForWriteSet);
        }
        EN = exposedNamesSet.toArray(new Pair[exposedNamesSet.size()]);
      }

      if (exposedNamesForReadSet != null) {
        Set<String> readOnlyNames = new HashSet<String>();
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
        Set<String> parents = new LinkedHashSet<String>();
        for (Iterator<Access> ACS = accesses.iterator(); ACS.hasNext();) {
          Access AC = ACS.next();
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

    public int[] getExitExposedUses() {
      return exitLexicalUses;
    }

    private static final int[] NONE = new int[0];

    public int[] getExposedUses(int instructionOffset) {
      return instructionLexicalUses[instructionOffset] == null ? NONE : instructionLexicalUses[instructionOffset];
    }

    public IntSet getAllExposedUses() {
      if (allExposedUses == null) {
        allExposedUses = IntSetUtil.make();
        if (exitLexicalUses != null) {
          for (int i = 0; i < exitLexicalUses.length; i++) {
            if (exitLexicalUses[i] > 0) {
              allExposedUses.add(exitLexicalUses[i]);
            }
          }
        }
        if (instructionLexicalUses != null) {
          for (int i = 0; i < instructionLexicalUses.length; i++) {
            if (instructionLexicalUses[i] != null) {
              for (int j = 0; j < instructionLexicalUses[i].length; j++) {
                if (instructionLexicalUses[i][j] > 0) {
                  allExposedUses.add(instructionLexicalUses[i][j]);
                }
              }
            }
          }
        }
      }

      return allExposedUses;
    }

    public Pair<String, String>[] getExposedNames() {
      return exposedNames;
    }

    public String[] getScopingParents() {
      return scopingParents;
    }

    /**
     * reset cached info about value numbers that may have changed
     */
    public void handleAlteration() {
      allExposedUses = null;
    }

    public boolean isReadOnly(String name) {
      return readOnlyNames != null && readOnlyNames.contains(name);
    }

    public String getScopingName() {
      return functionLexicalName;
    }
  };
 
  /**
   * record that in entity e, the access is performed.
   * 
   * If {@link #useLocalValuesForLexicalVars()} is true, the access is performed
   * using a local variable. in
   * {@link #patchLexicalAccesses(SSAInstruction[], Set)}, this information is
   * used to update an instruction that performs all the accesses at the
   * beginning of the method and defines the locals.
   */
  private void addAccess(WalkContext context, CAstEntity e, Access access) {
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
  private void addExposedName(CAstEntity entity, CAstEntity declaration, String name, int valueNumber, boolean isWrite, WalkContext context) {
    Pair<Pair<String, String>, Integer> newVal = Pair.make(Pair.make(name, context.getEntityName(declaration)), valueNumber);
    context.exposeNameSet(entity, isWrite).add(newVal);
  }

  private void setDefaultValue(SymbolTable symtab, int vn, Object value) {
    if (value == CAstSymbol.NULL_DEFAULT_VALUE) {
      symtab.setDefaultValue(vn, null);
    } else {
      symtab.setDefaultValue(vn, value);
    }
  }

  protected IUnaryOpInstruction.IOperator translateUnaryOpcode(CAstNode op) {
    if (op == CAstOperator.OP_BITNOT)
      return AstConstants.UnaryOp.BITNOT;
    else if (op == CAstOperator.OP_NOT)
      return IUnaryOpInstruction.Operator.NEG;
    else if (op == CAstOperator.OP_SUB)
      return AstConstants.UnaryOp.MINUS;
    else if (op == CAstOperator.OP_ADD)
      return AstConstants.UnaryOp.PLUS;
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
      return AstConstants.BinaryOp.CONCAT;
    else if (op == CAstOperator.OP_EQ)
      return AstConstants.BinaryOp.EQ;
    else if (op == CAstOperator.OP_STRICT_EQ)
      return AstConstants.BinaryOp.STRICT_EQ;
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
    else if (op == CAstOperator.OP_STRICT_NE)
      return AstConstants.BinaryOp.STRICT_NE;
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
      Assertions.UNREACHABLE("cannot translate " + CAstPrinter.print(op));
      return null;
    }
  }

  private String[] makeNameMap(CAstEntity n, Set<Scope> scopes) {
    // all scopes share the same underlying symtab, which is what
    // size really refers to.
    String[] map = new String[scopes.iterator().next().size() + 1];

    if (DEBUG_NAMES) {
      System.err.println(("names array of size " + map.length));
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

        assert map[v.valueNumber()] == null || map[v.valueNumber()].equals(nm) : "value number " + v.valueNumber()
            + " mapped to multiple names in " + n.getName() + ": " + nm + " and " + map[v.valueNumber()];

        map[v.valueNumber()] = nm;

        if (DEBUG_NAMES) {
          System.err.println(("mapping name " + nm + " to " + v.valueNumber()));
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

  /**
   * find any AstLexicalAccess instructions in instrs with a zero access count,
   * and change them to perform specified accesses. If accesses is empty, null
   * out the pointers to the AstLexicalAccess instructions in the array.
   * 
   * Presumably, such empty AstLexicalAccess instructions should only exist if
   * {@link #useLocalValuesForLexicalVars()} returns true?
   */
  private void patchLexicalAccesses(SSAInstruction[] instrs, Set<Access> accesses) {
    Access[] AC = accesses == null || accesses.isEmpty() ? (Access[]) null : (Access[]) accesses.toArray(new Access[accesses.size()]);
    for (int i = 0; i < instrs.length; i++) {
      if (instrs[i] instanceof AstLexicalAccess && ((AstLexicalAccess) instrs[i]).getAccessCount() == 0) {
        // should just be AstLexicalRead for now; may add support for
        // AstLexicalWrite later
        assert instrs[i] instanceof AstLexicalRead;
        assert useLocalValuesForLexicalVars();
        if (AC != null) {
          ((AstLexicalAccess) instrs[i]).setAccesses(AC);
        } else {
          instrs[i] = null;
        }
      }
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

  protected WalkContext makeFileContext(WalkContext c, CAstEntity n) {
    return new FileContext((WalkContext) c, n.getName());
  }

  protected WalkContext makeTypeContext(WalkContext c, CAstEntity n) {
    return new TypeContext((WalkContext) c, n);
  }

  protected WalkContext makeCodeContext(WalkContext c, CAstEntity n) {
    WalkContext context = (WalkContext) c;
    AbstractScope scope;
    if (n.getKind() == CAstEntity.SCRIPT_ENTITY)
      scope = makeScriptScope(n, context.currentScope());
    else
      scope = makeFunctionScope(n, context.currentScope());
    return new CodeEntityContext(context, scope, n);
  }

  protected boolean enterEntity(final CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    if (DEBUG_TOP)
      System.err.println(("translating " + n.getName()));
    return false;
  }

  protected boolean visitFileEntity(CAstEntity n, WalkContext context, WalkContext fileContext, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveFileEntity(CAstEntity n, WalkContext context, WalkContext fileContext, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitFieldEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveFieldEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    // Define a new field in the enclosing type, if the language we're
    // processing allows such.
    CAstEntity topEntity = context.top(); // better be a type
    assert topEntity.getKind() == CAstEntity.TYPE_ENTITY : "Parent of field entity is not a type???";
    defineField(topEntity, (WalkContext) context, n);
  }

  protected boolean visitGlobalEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveGlobalEntity(CAstEntity n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    // Define a new field in the enclosing type, if the language we're
    // processing allows such.
    context.getGlobalScope().declare(new CAstSymbolImpl(n.getName()));
  }

  protected boolean visitTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) {
    return !defineType(n, (WalkContext) context);
  }

  protected void leaveTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitFunctionEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    if (n.getAST() == null) // presumably abstract
      declareFunction(n, (WalkContext) context);
    else
      initFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
    return false;
  }

  protected void leaveFunctionEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    if (n.getAST() != null) // non-abstract
      closeFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
  }

  protected boolean visitMacroEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    return true;
  }

  protected boolean visitScriptEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
    declareFunction(n, (WalkContext) codeContext);
    initFunctionEntity(n, (WalkContext) context, (WalkContext) codeContext);
    return false;
  }

  protected void leaveScriptEntity(CAstEntity n, WalkContext context, WalkContext codeContext, CAstVisitor<WalkContext> visitor) {
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
    AstCFG cfg = new AstCFG(n, functionContext.cfg(), symtab);
    Position[] line = functionContext.cfg().getLinePositionMap();
    boolean katch = functionContext.cfg().hasCatchBlock();
    boolean monitor = functionContext.cfg().hasMonitorOp();
    String[] nms = makeNameMap(n, functionContext.entityScopes());

    /*
     * Set reachableBlocks = DFS.getReachableNodes(cfg,
     * Collections.singleton(cfg.entry()));
     * Assertions._assert(reachableBlocks.size() == cfg.getNumberOfNodes(),
     * cfg.toString());
     */

    // (put here to allow subclasses to handle stuff in scoped entities)
    // assemble lexical information
    patchLexicalAccesses(cfg.getInstructions(), functionContext.getAccesses(n));
    AstLexicalInformation LI = new AstLexicalInformation(functionContext.getEntityName(n), (AbstractScope) functionContext.currentScope(), cfg.getInstructions(),
        functionContext.exposeNameSet(n, false), 
        functionContext.exposeNameSet(n, true), 
        functionContext.getAccesses(n));

    DebuggingInformation DBG = new AstDebuggingInformation(n.getPosition(), line, nms);

    // actually make code body
    defineFunction(n, parentContext, cfg, symtab, katch, catchTypes, monitor, LI, DBG);
  }

  protected WalkContext makeLocalContext(WalkContext context, CAstNode n) {
    return new LocalContext((WalkContext) context, makeLocalScope(n, ((WalkContext) context).currentScope()));
  }

  protected WalkContext makeUnwindContext(WalkContext context, CAstNode n, CAstVisitor<WalkContext> visitor) {
    // here, n represents the "finally" block of the unwind
    return new UnwindContext(n, (WalkContext) context, visitor);
  }

  private Map<CAstEntity, Set<String>> entity2ExposedNames;
  protected int processFunctionExpr(CAstNode n, WalkContext context) {
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    declareFunction(fn, context);
    int result = context.currentScope().allocateTempValue();
    int ex = context.currentScope().allocateTempValue();
    doMaterializeFunction(n, context, result, ex, fn);
    return result;
  }

  protected boolean visitFunctionExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveFunctionExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = processFunctionExpr(n, c);
    c.setValue(n, result);
  }

  protected boolean visitFunctionStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

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
      context.getGlobalScope().declare(new FinalCAstSymbol(fn.getName()));
      assignValue(n, context, cs.lookup(fn.getName()), fn.getName(), result);
    } else {
      context.currentScope().declare(new FinalCAstSymbol(fn.getName()), result);
    }
  }

  protected boolean visitLocalScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveLocalScope(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  protected boolean visitBlockExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitBlockStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveBlockStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitLoop(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    // loop test block
    context.cfg().newBlock(true);
    PreBasicBlock headerB = context.cfg().getCurrentBlock();
    visitor.visit(n.getChild(0), context, visitor);

    assert c.getValue(n.getChild(0)) != -1 : "error in loop test " + CAstPrinter.print(n.getChild(0), context.top().getSourceMap())
        + " of loop " + CAstPrinter.print(n, context.top().getSourceMap());
    context.cfg().addInstruction(
        insts.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, c.getValue(n.getChild(0)), context
            .currentScope().getConstantValue(new Integer(0))));
    PreBasicBlock branchB = context.cfg().getCurrentBlock();

    // loop body
    context.cfg().newBlock(true);
    visitor.visit(n.getChild(1), context, visitor);
    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().addInstruction(insts.GotoInstruction());
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
  protected final void leaveLoopHeader(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveLoop(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitGetCaughtException(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveGetCaughtException(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    context.currentScope().declare(new FinalCAstSymbol(nm));
    context.cfg().addInstruction(
        insts.GetCaughtExceptionInstruction(context.cfg().getCurrentBlock().getNumber(), context.currentScope().lookup(nm)
            .valueNumber()));
  }

  protected boolean visitThis(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveThis(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, 1);
  }

  protected boolean visitSuper(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveSuper(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, 1);
  }

  protected boolean visitCall(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  protected void leaveCall(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
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

  protected boolean visitVar(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveVar(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    assert nm != null : "cannot find var for " + CAstPrinter.print(n, context.getSourceMap());
    Symbol s = context.currentScope().lookup(nm);
    assert s != null : "cannot find symbol for " + nm + " at " + CAstPrinter.print(n, context.getSourceMap());
    if (context.currentScope().isGlobal(s)) {
      c.setValue(n, doGlobalRead(n, context, nm));
    } else if (context.currentScope().isLexicallyScoped(s)) {
      c.setValue(n, doLexicallyScopedRead(n, context, nm));
    } else {
      c.setValue(n, doLocalRead(context, nm));
    }
  }

  protected boolean visitConstant(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveConstant(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    c.setValue(n, context.currentScope().getConstantValue(n.getValue()));
  }

  protected boolean visitBinaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  private boolean handleBinaryOpThrow(CAstNode n, CAstNode op, WalkContext context) {
    // currently, only integer / and % throw exceptions
    boolean mayBeInteger = false;
    Collection labels = context.getControlFlow().getTargetLabels(n);
    if (!labels.isEmpty()) {
      context.cfg().addPreNode(n, context.getUnwindState());

      mayBeInteger = true;
      assert op == CAstOperator.OP_DIV || op == CAstOperator.OP_MOD : CAstPrinter.print(n);
      for (Iterator iter = labels.iterator(); iter.hasNext();) {
        Object label = iter.next();
        CAstNode target = context.getControlFlow().getTarget(n, label);
        if (target == CAstControlFlowMap.EXCEPTION_TO_EXIT)
          context.cfg().addPreEdgeToExit(n, true);
        else
          context.cfg().addPreEdge(n, target, true);
      }
    }

    return mayBeInteger;
  }

  protected void leaveBinaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = c.getValue(n);
    CAstNode l = n.getChild(1);
    CAstNode r = n.getChild(2);
    assert c.getValue(r) != -1 : CAstPrinter.print(n);
    assert c.getValue(l) != -1 : CAstPrinter.print(n);

    boolean mayBeInteger = handleBinaryOpThrow(n, n.getChild(0), context);

    context.cfg().addInstruction(
        insts.BinaryOpInstruction(translateBinaryOpcode(n.getChild(0)), false, false, result, c.getValue(l), c.getValue(r),
            mayBeInteger));

    if (mayBeInteger) {
      context.cfg().newBlock(true);
    }
  }

  protected boolean visitUnaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  protected void leaveUnaryExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = c.getValue(n);
    CAstNode v = n.getChild(1);
    context.cfg().addInstruction(insts.UnaryOpInstruction(translateUnaryOpcode(n.getChild(0)), result, c.getValue(v)));
  }

  protected boolean visitArrayLength(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  protected void leaveArrayLength(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = c.getValue(n);
    int arrayValue = c.getValue(n.getChild(0));
    context.cfg().addInstruction(insts.ArrayLengthInstruction(result, arrayValue));
  }

  protected boolean visitArrayRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int arrayValue = c.getValue(n.getChild(0));
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    arrayOpHandler.doArrayRead(context, result, arrayValue, n, gatherArrayDims(c, n));
  }

  protected boolean visitDeclStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  // TODO: should we handle exploded declaration nodes here instead?
  protected void leaveDeclStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    CAstSymbol s = (CAstSymbol) n.getChild(0).getValue();
    String nm = s.name();
    Scope scope = c.currentScope();
    if (n.getChildCount() == 2) {
      CAstNode v = n.getChild(1);
      if (scope.contains(nm) && scope.lookup(nm).getDefiningScope() == scope) {
        assert !s.isFinal();
        doLocalWrite(c, nm, c.getValue(v));
      } else if (v.getKind() != CAstNode.CONSTANT && v.getKind() != CAstNode.VAR && v.getKind() != CAstNode.THIS) {
        scope.declare(s, c.getValue(v));
      } else {
        scope.declare(s);
        doLocalWrite(c, nm, c.getValue(v));
      }
    } else {
      c.currentScope().declare(s);
    }
  }

  protected boolean visitReturn(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveReturn(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChildCount() > 0) {
      context.cfg().addInstruction(insts.ReturnInstruction(c.getValue(n.getChild(0)), false));
    } else {
      context.cfg().addInstruction(insts.ReturnInstruction());
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(false);
    context.cfg().addPreEdgeToExit(n, false);
  }

  protected boolean visitIfgoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveIfgoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChildCount() == 1) {
      context.cfg().addInstruction(
          insts.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_NE), null, c.getValue(n.getChild(0)), context
              .currentScope().getConstantValue(new Integer(0))));
    } else if (n.getChildCount() == 3) {
      context.cfg().addInstruction(
          insts.ConditionalBranchInstruction(translateConditionOpcode(n.getChild(0)), null, c.getValue(n.getChild(1)),
              c.getValue(n.getChild(2))));
    } else {
      Assertions.UNREACHABLE();
    }

    context.cfg().addPreNode(n, context.getUnwindState());
    context.cfg().newBlock(true);
    context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, Boolean.TRUE), false);
  }

  protected boolean visitGoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveGoto(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
      context.cfg().addPreNode(n, context.getUnwindState());
      context.cfg().addInstruction(insts.GotoInstruction());
      context.cfg().newBlock(false);
      if (context.getControlFlow().getTarget(n, null) == null) {
        assert context.getControlFlow().getTarget(n, null) != null : context.getControlFlow() + " does not map " + n + " ("
            + context.getSourceMap().getPosition(n) + ")";
      }
      context.cfg().addPreEdge(n, context.getControlFlow().getTarget(n, null), false);
    }
  }

  protected boolean visitLabelStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (!context.getControlFlow().getSourceNodes(n).isEmpty()) {
      context.cfg().newBlock(true);
      context.cfg().addPreNode(n, context.getUnwindState());
    }
    return false;
  }

  protected void leaveLabelStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected void processIf(CAstNode n, boolean isExpr, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    PreBasicBlock trueB = null, falseB = null;
    // conditional
    CAstNode l = n.getChild(0);
    visitor.visit(l, context, visitor);
    context.cfg().addInstruction(
        insts.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, c.getValue(l), context.currentScope()
            .getConstantValue(new Integer(0))));
    PreBasicBlock srcB = context.cfg().getCurrentBlock();
    // true clause
    context.cfg().newBlock(true);
    CAstNode r = n.getChild(1);
    visitor.visit(r, context, visitor);
    if (isExpr)
      context.cfg().addInstruction(new AssignInstruction(c.getValue(n), c.getValue(r)));
    if (n.getChildCount() == 3) {
      if (!context.cfg().isDeadBlock(context.cfg().getCurrentBlock())) {
        context.cfg().addInstruction(insts.GotoInstruction());
        trueB = context.cfg().getCurrentBlock();

        // false clause
        context.cfg().newBlock(false);
      }

      falseB = context.cfg().getCurrentBlock();
      CAstNode f = n.getChild(2);
      visitor.visit(f, context, visitor);
      if (isExpr)
        context.cfg().addInstruction(new AssignInstruction(c.getValue(n), c.getValue(f)));
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
  protected final void leaveIfStmtCondition(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveIfStmtTrueClause(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveIfStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveIfExprCondition(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveIfExprTrueClause(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveIfExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitIfStmt(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    processIf(n, false, c, visitor);
    return true;
  }

  protected boolean visitIfExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    processIf(n, true, c, visitor);
    return true;
  }

  protected boolean visitNew(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveNew(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;

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

  protected boolean visitObjectLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveObjectLiteralFieldInit(CAstNode n, int i, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (n.getChild(i).getKind() == CAstNode.EMPTY) {
      handleUnspecifiedLiteralKey(context, n, i, visitor);
    }
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(i), n, c.getValue(n.getChild(i + 1)));
  }

  protected void leaveObjectLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  protected boolean visitArrayLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveArrayLiteralObject(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(0)));
  }

  protected void leaveArrayLiteralInitElement(CAstNode n, int i, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n,
        new int[] { context.currentScope().getConstantValue(new Integer(i - 1)) }, c.getValue(n.getChild(i)));
  }

  protected void leaveArrayLiteral(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitObjectRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);
    return false;
  }

  protected void leaveObjectRef(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = c.getValue(n);
    CAstNode elt = n.getChild(1);
    doFieldRead(context, result, c.getValue(n.getChild(0)), elt, n);
  }

  public boolean visitAssign(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  public void leaveAssign(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    if (n.getKind() == CAstNode.ASSIGN) {
      c.setValue(n, c.getValue(n.getChild(1)));
    } else {
      c.setValue(n, c.getValue(n.getChild(0)));
    }
  }

  private int[] gatherArrayDims(WalkContext c, CAstNode n) {
    int numDims = n.getChildCount() - 2;
    int[] dims = new int[numDims];
    for (int i = 0; i < numDims; i++)
      dims[i] = c.getValue(n.getChild(i + 2));
    return dims;
  }

  /* Prereq: a.getKind() == ASSIGN_PRE_OP || a.getKind() == ASSIGN_POST_OP */
  protected int processAssignOp(CAstNode n, CAstNode v, CAstNode a, int temp, boolean post, WalkContext c) {
    WalkContext context = (WalkContext) c;
    int rval = c.getValue(v);
    CAstNode op = a.getChild(2);
    int temp2 = context.currentScope().allocateTempValue();

    boolean mayBeInteger = handleBinaryOpThrow(a, op, context);

    context.cfg().addInstruction(
        insts.BinaryOpInstruction(translateBinaryOpcode(op), false, false, temp2, temp, rval, mayBeInteger));

    if (mayBeInteger) {
      context.cfg().newBlock(true);
    }

    return temp2;
  }

  protected boolean visitArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int rval = c.getValue(v);
    c.setValue(n, rval);
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n, gatherArrayDims(c, n), rval);
  }

  protected boolean visitArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveArrayRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int temp = context.currentScope().allocateTempValue();
    int[] dims = gatherArrayDims(c, n);
    arrayOpHandler.doArrayRead(context, temp, c.getValue(n.getChild(0)), n, dims);
    int rval = processAssignOp(n, v, a, temp, !pre, c);
    c.setValue(n, pre ? rval : temp);
    arrayOpHandler.doArrayWrite(context, c.getValue(n.getChild(0)), n, dims, rval);
  }

  protected boolean visitObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveObjectRefAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int rval = c.getValue(v);
    c.setValue(n, rval);
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  protected void processObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, WalkContext c) {
  }

  protected boolean visitObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveObjectRefAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int temp = context.currentScope().allocateTempValue();
    doFieldRead(context, temp, c.getValue(n.getChild(0)), n.getChild(1), n);
    int rval = processAssignOp(n, v, a, temp, !pre, c);
    c.setValue(n, pre ? rval : temp);
    doFieldWrite(context, c.getValue(n.getChild(0)), n.getChild(1), n, rval);
  }

  protected boolean visitBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExprAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveBlockExprAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    c.setValue(n, c.getValue(n.getChild(n.getChildCount() - 1)));
  }

  protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  /**
   * assign rval to nm as appropriate, depending on the scope of ls
   */
  protected void assignValue(CAstNode n, WalkContext context, Symbol ls, String nm, int rval) {
    if (context.currentScope().isGlobal(ls))
      doGlobalWrite(context, nm, rval);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, rval);
    } else {
      assert rval != -1 : CAstPrinter.print(n, context.top().getSourceMap());
      doLocalWrite(context, nm, rval);
    }
  }

  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int rval = c.getValue(v);
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    c.setValue(n, rval);
    assignValue(n, context, ls, nm, rval);
  }

  protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    String nm = (String) n.getChild(0).getValue();
    Symbol ls = context.currentScope().lookup(nm);
    int temp;

    if (context.currentScope().isGlobal(ls))
      temp = doGlobalRead(n, context, nm);
    else if (context.currentScope().isLexicallyScoped(ls)) {
      temp = doLexicallyScopedRead(n, context, nm);
    } else {
      temp = doLocalRead(context, nm);
    }

    if (!pre) {
      int ret = context.currentScope().allocateTempValue();
      context.cfg().addInstruction(new AssignInstruction(ret, temp));
      c.setValue(n, ret);
    }

    int rval = processAssignOp(n, v, a, temp, !pre, c);

    if (pre) {
      c.setValue(n, rval);
    }

    if (context.currentScope().isGlobal(ls)) {
      doGlobalWrite(context, nm, rval);
    } else if (context.currentScope().isLexicallyScoped(ls)) {
      doLexicallyScopedWrite(context, nm, rval);
    } else {
      doLocalWrite(context, nm, rval);
    }
  }

  private boolean isSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    CAstControlFlowMap ctrl = context.getControlFlow();
    Collection caseLabels = ctrl.getTargetLabels(n);
    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();

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

  private void doSimpleSwitch(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
    PreBasicBlock defaultHackBlock = null;
    CAstControlFlowMap ctrl = context.getControlFlow();

    CAstNode switchValue = n.getChild(0);
    visitor.visit(switchValue, context, visitor);
    int v = context.getValue(switchValue);

    boolean hasExplicitDefault = ctrl.getTarget(n, CAstControlFlowMap.SWITCH_DEFAULT) != null;

    Collection caseLabels = ctrl.getTargetLabels(n);
    int cases = caseLabels.size();
    if (hasExplicitDefault)
      cases--;
    int[] casesAndLabels = new int[cases * 2];

    int defaultBlock = context.cfg().getCurrentBlock().getGraphNodeId() + 1;

    context.cfg().addInstruction(insts.SwitchInstruction(v, defaultBlock, casesAndLabels));
    context.cfg().addPreNode(n, context.getUnwindState());
    // PreBasicBlock switchB = context.cfg().getCurrentBlock();
    context.cfg().newBlock(true);

    context.cfg().addInstruction(insts.GotoInstruction());
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
    Map<Object, PreBasicBlock> labelToBlock = new LinkedHashMap<Object, PreBasicBlock>();
    for (Iterator kases = caseLabels.iterator(); kases.hasNext();) {
      Object x = kases.next();
      if (x != CAstControlFlowMap.SWITCH_DEFAULT) {
        visitor.visit((CAstNode) x, context, visitor);
        context.cfg().addInstruction(
            insts.ConditionalBranchInstruction(translateConditionOpcode(CAstOperator.OP_EQ), null, v, context.getValue((CAstNode) x)));
        labelToBlock.put(x, context.cfg().getCurrentBlock());
        context.cfg().newBlock(true);
      }
    }

    PreBasicBlock defaultGotoBlock = context.cfg().getCurrentBlock();
    context.cfg().addInstruction(insts.GotoInstruction());
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

  protected boolean visitSwitch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    if (isSimpleSwitch(n, context, visitor)) {
      doSimpleSwitch(n, context, visitor);
    } else {
      doIfConvertSwitch(n, context, visitor);
    }
    return true;
  }

  // Make final to prevent overriding
  protected final void leaveSwitchValue(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveSwitch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitThrow(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveThrow(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    doThrow(context, c.getValue(n.getChild(0)));

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

  protected boolean visitCatch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
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
        insts.GetCaughtExceptionInstruction(context.cfg().getCurrentBlock().getNumber(), context.currentScope().lookup(id)
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

  protected void leaveCatch(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitUnwind(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

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

  protected boolean visitTry(final CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    final WalkContext context = (WalkContext) c;
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
      context.cfg().addInstruction(insts.GotoInstruction());
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
  protected final void leaveTryBlock(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected final void leaveTry(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
  }

  protected boolean visitEmpty(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveEmpty(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    c.setValue(n, context.currentScope().getConstantValue(null));
  }

  protected boolean visitPrimitive(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leavePrimitive(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    int result = context.currentScope().allocateTempValue();
    c.setValue(n, result);

    doPrimitive(result, context, n);
  }

  protected boolean visitVoid(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveVoid(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    c.setValue(n, -1);
  }

  protected boolean visitAssert(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) { /* empty */
    return false;
  }

  protected void leaveAssert(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext context = (WalkContext) c;
    boolean fromSpec = true;
    int result = c.getValue(n.getChild(0));
    if (n.getChildCount() == 2) {
      assert n.getChild(1).getKind() == CAstNode.CONSTANT;
      assert n.getChild(1).getValue() instanceof Boolean;
      fromSpec = n.getChild(1).getValue().equals(Boolean.TRUE);
    }
    context.cfg().addInstruction(new AstAssertInstruction(result, fromSpec));
  }

  protected boolean visitEachElementGet(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  protected void leaveEachElementGet(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = ((WalkContext) c).currentScope().allocateTempValue();
    c.setValue(n, result);
    ((WalkContext) c).cfg().addInstruction(new EachElementGetInstruction(result, c.getValue(n.getChild(0))));
  }

  protected boolean visitEachElementHasNext(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  @Override
  protected void leaveEachElementHasNext(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    int result = ((WalkContext) c).currentScope().allocateTempValue();
    c.setValue(n, result);
    ((WalkContext) c).cfg().addInstruction(new EachElementHasNextInstruction(result, c.getValue(n.getChild(0))));
  }

  protected boolean visitTypeLiteralExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  protected void leaveTypeLiteralExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = (WalkContext) c;
    assert n.getChild(0).getKind() == CAstNode.CONSTANT;
    String typeNameStr = (String) n.getChild(0).getValue();
    TypeName typeName = TypeName.string2TypeName(typeNameStr);
    TypeReference typeRef = TypeReference.findOrCreate(loader.getReference(), typeName);

    int result = wc.currentScope().allocateTempValue();
    c.setValue(n, result);

    wc.cfg().addInstruction(insts.LoadMetadataInstruction(result, loader.getLanguage().getConstantType(typeRef), typeRef));
  }

  protected boolean visitIsDefinedExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  protected void leaveIsDefinedExpr(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = (WalkContext) c;
    int ref = c.getValue(n.getChild(0));
    int result = wc.currentScope().allocateTempValue();
    c.setValue(n, result);
    if (n.getChildCount() == 1) {
      wc.cfg().addInstruction(new AstIsDefinedInstruction(result, ref));
    } else {
      doIsFieldDefined(wc, result, ref, n.getChild(1));
    }
  }

  protected boolean visitEcho(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    return false;
  }

  protected void leaveEcho(CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = (WalkContext) c;

    int rvals[] = new int[n.getChildCount()];
    for (int i = 0; i < n.getChildCount(); i++) {
      rvals[i] = c.getValue(n.getChild(i));
    }

    wc.cfg().addInstruction(new AstEchoInstruction(rvals));
  }

  public CAstEntity getIncludedEntity(CAstNode n) {
    if (n.getChild(0).getKind() == CAstNode.NAMED_ENTITY_REF) {
      assert namedEntityResolver != null;
      return (CAstEntity) namedEntityResolver.get(n.getChild(0).getChild(0).getValue());
    } else {
      return (CAstEntity) n.getChild(0).getValue();
    }
  }

  protected void leaveInclude(final CAstNode n, WalkContext c, CAstVisitor<WalkContext> visitor) {
    WalkContext wc = (WalkContext) c;

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
          public CAstSourcePositionMap getSourceMap() {
            return copy.getSourceMap();
          }

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

    private final Map<CAstEntity, String> entityNames = new LinkedHashMap<CAstEntity, String>();

   public RootContext(CAstEntity N, ModuleEntry module) {
      this.N = N;
      this.module = module;
      this.globalScope = makeGlobalScope();
    }

    public ModuleEntry getModule() {
      return module;
    }

    public String file() {
      return module.getName();
    }

    public CAstEntity top() {
      return N;
    }

    public Scope currentScope() {
      return globalScope;
    }

    public Set<Scope> entityScopes() {
      return Collections.singleton(globalScope);
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
    
    public void addEntityName(CAstEntity e, String name) {
      entityNames.put(e, name);
    }

    public String getEntityName(CAstEntity e) {
      if (e == null) {
        return null;
      } else {
        assert entityNames.containsKey(e);
        return "L" + entityNames.get(e);
      }
    }

    public boolean hasValue(CAstNode n) {
      assert false;
      return false;
    }

    public int setValue(CAstNode n, int v) {
      assert false;
      return 0;
    }

    public int getValue(CAstNode n) {
      assert false;
      return -1;
    }

    public Set<Pair<Pair<String, String>, Integer>> exposeNameSet(CAstEntity entity, boolean writeSet) {
      assert false;
      return null;
    }

    public Set<Access> getAccesses(CAstEntity e) {
      assert false;
      return null;
    }

    public Scope getGlobalScope(){
      return globalScope;
    }
    
  };

  /**
   * translate module, represented by {@link CAstEntity} N
   */
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
