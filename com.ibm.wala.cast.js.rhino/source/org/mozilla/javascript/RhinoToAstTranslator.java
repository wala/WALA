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
package org.mozilla.javascript;

import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.tools.ToolErrorReporter;

import com.ibm.wala.cast.js.html.MappedSourceModule;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.LineNumberPosition;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

public class RhinoToAstTranslator {

  /**
   * a dummy name to use for standard function calls, only used to distinguish
   * them from constructor calls
   */
  public static final String STANDARD_CALL_FN_NAME = "do";

  /**
   * name used for constructor calls, used to distinguish them from standard
   * function calls
   */
  public static final String CTOR_CALL_FN_NAME = "ctor";

  private final boolean DEBUG = true;

  /**
   * shared interface for all objects storing contextual information during the
   * Rhino AST traversal
   * 
   */
  private interface WalkContext {

    /**
     * get the name of the enclosing script
     */
    String script();

    /**
     * get the enclosing Rhino {@link ScriptOrFnNode}
     */
    ScriptOrFnNode top();

    /**
     * Add a scoped entity to this context. The only scoped entities for
     * JavaScript are functions. (Why not variables? --MS) For a function
     * expression, construct will be the corresponding
     * {@link CAstNode#FUNCTION_EXPR}. For a function statement, construct is
     * <code>null</code>.
     */
    void addScopedEntity(CAstNode construct, CAstEntity e);

    /**
     * get a mapping from CAstNodes to the scoped entities (functions for
     * JavaScript) introduced by those nodes. Also maps <code>null</code> to
     * those entities not corresponding to any node
     */
    Map<CAstNode, Collection<CAstEntity>> getScopedEntities();

    /**
     * is the current node within an expression?
     */
    boolean expressionContext();

    /**
     * for recording control-flow relationships among the CAst nodes
     */
    CAstControlFlowRecorder cfg();

    /**
     * for recording source positions
     */
    CAstSourcePositionRecorder pos();

    /**
     * get the current control-flow target if an exception is thrown, or
     * <code>null</code> if unknown
     */
    CAstNode getCatchTarget();

    /**
     * @see BaseCollectingContext
     */
    CAstNode getBaseVarIfRelevant(Node node);

    /**
     * @see BaseCollectingContext
     */
    boolean foundBase(Node node);

    /**
     * @see BaseCollectingContext
     */
    void updateBase(Node from, Node to);

    /**
     * @see CatchBlockContext
     */
    String getCatchVar();

    /**
     * @see CatchBlockContext
     */
    void setCatchVar(String name);

    /**
     * @see LoopContext
     */
    String createForInVar(Node initExpr);

    /**
     * @see LoopContext
     */
    String getForInInitVar();

    /**
     * Add a name declaration to this context. For variables or constants, n
     * should be a {@link CAstNode#DECL_STMT}, and the initialization of the
     * variable (if any) may occur in a separate assignment. For functions, n
     * should be a {@link CAstNode#FUNCTION_STMT}, including the function body.
     */
    void addNameDecl(CAstNode n);
  }

  /**
   * default implementation of WalkContext; methods do nothing / return null
   * 
   */
  private static class RootContext implements WalkContext {

    public String script() {
      return null;
    }

    public ScriptOrFnNode top() {
      return null;
    }

    public void addScopedEntity(CAstNode construct, CAstEntity e) {
    }

    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return null;
    }

    public boolean expressionContext() {
      return false;
    }

    public CAstControlFlowRecorder cfg() {
      return null;
    }

    public CAstSourcePositionRecorder pos() {
      return null;
    }

    public CAstNode getCatchTarget() {
      return null;
    }

    public CAstNode getBaseVarIfRelevant(Node node) {
      return null;
    }

    public boolean foundBase(Node node) {
      return false;
    }

    public void updateBase(Node from, Node to) {
    }

    public String getCatchVar() {
      return null;
    }

    public void setCatchVar(String name) {
    }

    public String createForInVar(Node initExpr) {
      return null;
    }

    public String getForInInitVar() {
      return null;
    }

    public void addNameDecl(CAstNode n) {
    }
  }

  /**
   * WalkContext that delegates all behavior to a parent
   */
  private static abstract class DelegatingContext implements WalkContext {
    private final WalkContext parent;

    DelegatingContext(WalkContext parent) {
      this.parent = parent;
    }

    public String script() {
      return parent.script();
    }

    public ScriptOrFnNode top() {
      return parent.top();
    }

    public void addScopedEntity(CAstNode construct, CAstEntity e) {
      parent.addScopedEntity(construct, e);
    }

    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return parent.getScopedEntities();
    }

    public boolean expressionContext() {
      return parent.expressionContext();
    }

    public CAstControlFlowRecorder cfg() {
      return parent.cfg();
    }

    public CAstSourcePositionRecorder pos() {
      return parent.pos();
    }

    public CAstNode getCatchTarget() {
      return parent.getCatchTarget();
    }

    public CAstNode getBaseVarIfRelevant(Node node) {
      return parent.getBaseVarIfRelevant(node);
    }

    public boolean foundBase(Node node) {
      return parent.foundBase(node);
    }

    public void updateBase(Node from, Node to) {
      parent.updateBase(from, to);
    }

    public String getCatchVar() {
      return parent.getCatchVar();
    }

    public void setCatchVar(String name) {
      parent.setCatchVar(name);
    }

    public String createForInVar(Node initExpr) {
      return parent.createForInVar(initExpr);
    }

    public String getForInInitVar() {
      return parent.getForInInitVar();
    }

    public void addNameDecl(CAstNode n) {
      parent.addNameDecl(n);
    }
  }

  /**
   * context used for function / script declarations
   */
  private static class FunctionContext extends DelegatingContext {
    private final ScriptOrFnNode topNode;

    private final Map<CAstNode, Collection<CAstEntity>> scopedEntities = HashMapFactory.make();

    private final List<CAstNode> nameDecls = new ArrayList<CAstNode>();

    private final CAstSourcePositionRecorder pos = new CAstSourcePositionRecorder();

    private final CAstControlFlowRecorder cfg = new CAstControlFlowRecorder(pos);

    FunctionContext(WalkContext parent, ScriptOrFnNode s) {
      super(parent);
      this.topNode = s;
    }

    public ScriptOrFnNode top() {
      return topNode;
    }

    public void addScopedEntity(CAstNode construct, CAstEntity e) {
      if (!scopedEntities.containsKey(construct)) {
        HashSet<CAstEntity> s = HashSetFactory.make();
        scopedEntities.put(construct, s);
      }

      scopedEntities.get(construct).add(e);
    }

    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return scopedEntities;
    }

    public boolean expressionContext() {
      return false;
    }

    public CAstControlFlowRecorder cfg() {
      return cfg;
    }

    public CAstSourcePositionRecorder pos() {
      return pos;
    }

    // TODO do we really need to override this method?  --MS
    public String createForInVar(Node initExpr) {
      return null;
    }

    // TODO do we really need to override this method?  --MS
    public String getForInInitVar() {
      return null;
    }

    public void addNameDecl(CAstNode n) {
      assert n.getKind() == CAstNode.FUNCTION_STMT || n.getKind() == CAstNode.DECL_STMT;
      nameDecls.add(n);
    }

    public CAstNode getCatchTarget() {
      return CAstControlFlowMap.EXCEPTION_TO_EXIT;
    }
  }

  /**
   * context used for top-level script declarations
   */
  private static class ScriptContext extends FunctionContext {
    private final String script;

    ScriptContext(WalkContext parent, ScriptOrFnNode s, String script) {
      super(parent, s);
      this.script = script;
    }

    public String script() {
      return script;
    }
  }

  private static class ExpressionContext extends DelegatingContext {

    ExpressionContext(WalkContext parent) {
      super(parent);
    }

    public boolean expressionContext() {
      return true;
    }

  }

  private static class CatchBlockContext extends DelegatingContext {
    private String catchVarName;

    CatchBlockContext(WalkContext parent) {
      super(parent);
    }

    public String getCatchVar() {
      return catchVarName;
    }

    public void setCatchVar(String name) {
      catchVarName = name;
    }
  }

  private static class TryBlockContext extends DelegatingContext {
    private final CAstNode catchNode;

    TryBlockContext(WalkContext parent, CAstNode catchNode) {
      super(parent);
      this.catchNode = catchNode;
    }

    public CAstNode getCatchTarget() {
      return catchNode;
    }

  }

  /**
   * Used to determine the value to be passed as the 'this' argument for a
   * function call. This is needed since in JavaScript, you can write a call
   * e(...) where e is some arbitrary expression, and in the case where e is a
   * property access like e'.f, we must discover that the value of expression e'
   * is passed as the 'this' parameter.
   * 
   * The general strategy is to store the value of the expression passed as the
   * 'this' parameter in baseVar, and then to use baseVar as the actual argument
   * sub-node for the CAst call node
   */
  private static class BaseCollectingContext extends DelegatingContext {
    /**
     * node for which we actually care about what the base pointer is. this
     * helps to handle cases like x.y.f(), where we would like to store x.y in
     * baseVar, but not x when we recurse.
     */
    private Node baseFor;

    /**
     * the variable to be used to store the value of the expression passed as
     * the 'this' parameter
     */
    private final CAstNode baseVar;

    /**
     * have we discovered a value to be passed as the 'this' parameter?
     */
    private boolean foundBase = false;

    BaseCollectingContext(WalkContext parent, Node initialBaseFor, CAstNode baseVar) {
      super(parent);
      baseFor = initialBaseFor;
      this.baseVar = baseVar;
    }

    /**
     * if node is one that we care about, return baseVar, and as a side effect
     * set foundBase to true. Otherwise, return <code>null</code>.
     */
    public CAstNode getBaseVarIfRelevant(Node node) {
      if (baseFor.equals(node)) {
        foundBase = true;
        return baseVar;
      } else {
        return null;
      }
    }

    public boolean foundBase(Node node) {
      return foundBase;
    }

    /**
     * if we currently care about the base pointer of from, switch to searching
     * for the base pointer of to. Used for cases like comma expressions: if we
     * have (x,y.f)(), we want to assign y to baseVar
     */
    public void updateBase(Node from, Node to) {
      if (baseFor.equals(from))
        baseFor = to;
    }
  }

  /**
   * Used to model for-in loops. Given a loop "for (x in e) {...}", we generate
   * a new variable forInVar that should hold the value of e. Then, the
   * navigation of e is modeled via {@link CAstNode#EACH_ELEMENT_GET} and
   * {@link CAstNode#EACH_ELEMENT_HAS_NEXT} nodes.
   */
  private static class LoopContext extends DelegatingContext {
    /**
     * for generating fresh loop vars
     */
    private static int counter = 0;

    /**
     * the variable holding the value being navigated by the loop
     */
    private String forInVar = null;

    /**
     * the expression evaluating to the value being navigated
     */
    private Node forInInitExpr = null;

    private LoopContext(WalkContext parent) {
      super(parent);
    }

    /**
     * create a fresh for-in loop variable that should be initialized to
     * initExpr, and return it
     */
    public String createForInVar(Node initExpr) {
      assert this.forInVar == null;
      this.forInVar = "_forin_tmp" + counter++;
      this.forInInitExpr = initExpr;
      return forInVar;
    }

    public String getForInInitVar() {
      assert forInVar != null;
      return forInVar;
    }

  }

  private CAstNode translateOpcode(int nodeType) {
    switch (nodeType) {
    case Token.ADD:
      return CAstOperator.OP_ADD;
    case Token.DIV:
      return CAstOperator.OP_DIV;
    case Token.LSH:
      return CAstOperator.OP_LSH;
    case Token.MOD:
      return CAstOperator.OP_MOD;
    case Token.MUL:
      return CAstOperator.OP_MUL;
    case Token.RSH:
      return CAstOperator.OP_RSH;
    case Token.SUB:
      return CAstOperator.OP_SUB;
    case Token.URSH:
      return CAstOperator.OP_URSH;
    case Token.BITAND:
      return CAstOperator.OP_BIT_AND;
    case Token.BITOR:
      return CAstOperator.OP_BIT_OR;
    case Token.BITXOR:
      return CAstOperator.OP_BIT_XOR;

    case Token.EQ:
      return CAstOperator.OP_EQ;
    case Token.SHEQ:
      return CAstOperator.OP_STRICT_EQ;
    case Token.IFEQ:
      return CAstOperator.OP_EQ;
    case Token.GE:
      return CAstOperator.OP_GE;
    case Token.GT:
      return CAstOperator.OP_GT;
    case Token.LE:
      return CAstOperator.OP_LE;
    case Token.LT:
      return CAstOperator.OP_LT;
    case Token.NE:
      return CAstOperator.OP_NE;
    case Token.SHNE:
      return CAstOperator.OP_STRICT_NE;
    case Token.IFNE:
      return CAstOperator.OP_NE;

    case Token.BITNOT:
      return CAstOperator.OP_BITNOT;
    case Token.NOT:
      return CAstOperator.OP_NOT;

    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  private CAstNode makeBuiltinNew(String typeName) {
    return Ast.makeNode(CAstNode.NEW, Ast.makeConstant(typeName));
  }

  private CAstNode handleNew(WalkContext context, String globalName, Node arguments) {
    return handleNew(context, readName(context, globalName), arguments);
  }

  private CAstNode handleNew(WalkContext context, CAstNode value, Node arguments) {
    return makeCtorCall(value, arguments, context);
  }

  private boolean isPrologueScript(WalkContext context) {
    return JavaScriptLoader.bootstrapFileNames.contains(context.script());
  }

  /**
   * is n a call to "primitive" within our synthetic modeling code?
   */
  private boolean isPrimitiveCall(WalkContext context, Node n) {
    return isPrologueScript(context) && n.getType() == Token.CALL && n.getFirstChild().getType() == Token.NAME
        && n.getFirstChild().getString().equals("primitive");
  }

  private boolean isPrimitiveCreation(WalkContext context, Node n) {
    return isPrologueScript(context) && n.getType() == Token.NEW && n.getFirstChild().getType() == Token.NAME
        && n.getFirstChild().getString().equals("Primitives");
  }

  private CAstNode makeCall(CAstNode fun, CAstNode thisptr, Node firstChild, WalkContext context) {
    return makeCall(fun, thisptr, firstChild, context, STANDARD_CALL_FN_NAME);
  }

  private CAstNode makeCtorCall(CAstNode thisptr, Node firstChild, WalkContext context) {
    return makeCall(thisptr, null, firstChild, context, CTOR_CALL_FN_NAME);
  }

  private CAstNode makeCall(CAstNode fun, CAstNode thisptr, Node firstChild, WalkContext context, String callee) {
    int children = countSiblingsStartingFrom(firstChild);

    // children of CAst CALL node are the expression that evaluates to the
    // function, followed by a name (either STANDARD_CALL_FN_NAME or
    // CTOR_CALL_FN_NAME), followed by the actual
    // parameters
    int nargs = (thisptr == null) ? children + 2 : children + 3;
    int i = 0;
    CAstNode arguments[] = new CAstNode[nargs];
    arguments[i++] = fun;
    assert callee.equals(STANDARD_CALL_FN_NAME) || callee.equals(CTOR_CALL_FN_NAME);
    arguments[i++] = Ast.makeConstant(callee);
    if (thisptr != null)
      arguments[i++] = thisptr;
    for (Node arg = firstChild; arg != null; arg = arg.getNext())
      arguments[i++] = walkNodes(arg, context);

    CAstNode call = Ast.makeNode(CAstNode.CALL, arguments);

    context.cfg().map(call, call);
    if (context.getCatchTarget() != null) {
      context.cfg().add(call, context.getCatchTarget(), null);
    }

    return call;
  }

  /**
   * count the number of successor siblings of n, including n
   */
  private int countSiblingsStartingFrom(Node n) {
    int siblings = 0;
    for (Node c = n; c != null; c = c.getNext(), siblings++)
      ;
    return siblings;
  }

  /**
   * Used to represent a script or function in the CAst; see walkEntity().
   * 
   */
  private class ScriptOrFnEntity implements CAstEntity {
    private final String[] arguments;

    private final String name;

    private final int kind;

    private final Map<CAstNode, Collection<CAstEntity>> subs;

    private final CAstNode ast;

    private final CAstControlFlowMap map;

    private final CAstSourcePositionMap pos;

    ScriptOrFnEntity(ScriptOrFnNode n, Map<CAstNode, Collection<CAstEntity>> subs, CAstNode ast, CAstControlFlowMap map,
        CAstSourcePositionMap pos) {
      if (n instanceof FunctionNode) {
        String x = ((FunctionNode) n).getFunctionName();
        if (x == null || "".equals(x)) {
          name = scriptName + "_anonymous_" + anonymousCounter++;
        } else {
          name = x;
        }
      } else {
        name = n.getSourceName();
      }

      if (n instanceof FunctionNode) {
        FunctionNode f = (FunctionNode) n;
        f.flattenSymbolTable(false);
        int i = 0;
        arguments = new String[f.getParamCount() + 2];
        arguments[i++] = name;
        arguments[i++] = "this";
        for (int j = 0; j < f.getParamCount(); j++) {
          arguments[i++] = f.getParamOrVarName(j);
        }
      } else {
        arguments = new String[0];
      }
      kind = (n instanceof FunctionNode) ? CAstEntity.FUNCTION_ENTITY : CAstEntity.SCRIPT_ENTITY;
      this.subs = subs;
      this.ast = ast;
      this.map = map;
      this.pos = pos;
    }

    public String toString() {
      return "<JS function " + getName() + ">";
    }

    public String getName() {
      return name;
    }

    public String getSignature() {
      Assertions.UNREACHABLE();
      return null;
    }

    public int getKind() {
      return kind;
    }

    public String[] getArgumentNames() {
      return arguments;
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return arguments.length;
    }

    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
      return Collections.unmodifiableMap(subs);
    }

    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
      if (subs.containsKey(construct))
        return subs.get(construct).iterator();
      else
        return EmptyIterator.instance();
    }

    public CAstNode getAST() {
      return ast;
    }

    public CAstControlFlowMap getControlFlow() {
      return map;
    }

    public CAstSourcePositionMap getSourceMap() {
      return pos;
    }

    public CAstSourcePositionMap.Position getPosition() {
      return null;
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      return null;
    }

    public Collection<CAstQualifier> getQualifiers() {
      Assertions.UNREACHABLE("JuliansUnnamedCAstEntity$2.getQualifiers()");
      return null;
    }

    public CAstType getType() {
      Assertions.UNREACHABLE("JuliansUnnamedCAstEntity$2.getType()");
      return null;
    }
  }

  private CAstEntity walkEntity(final ScriptOrFnNode n, WalkContext context) {
    final FunctionContext child = (n instanceof FunctionNode) ? new FunctionContext(context, n) : new ScriptContext(context, n,
        n.getSourceName());

    CAstNode[] stmts = gatherChildren(n, child);

    // add variable / constant / function declarations, if any
    if (!child.nameDecls.isEmpty()) {
      // new first statement will be a block declaring all names.
      CAstNode[] newStmts = new CAstNode[stmts.length + 1];

      newStmts[0] = Ast.makeNode(CAstNode.BLOCK_STMT, child.nameDecls.toArray(new CAstNode[child.nameDecls.size()]));

      System.arraycopy(stmts, 0, newStmts, 1, stmts.length);

      stmts = newStmts;
    }

    final CAstNode ast = Ast.makeNode(CAstNode.BLOCK_STMT, stmts);
    final CAstControlFlowMap map = child.cfg();
    final CAstSourcePositionMap pos = child.pos();

    // not sure if we need this copy --MS
    final Map<CAstNode, Collection<CAstEntity>> subs = HashMapFactory.make(child.getScopedEntities());

    return new ScriptOrFnEntity(n, subs, ast, map, pos);
  }

  private CAstNode[] gatherSiblings(Node n, WalkContext context) {
    int cnt = countSiblingsStartingFrom(n);

    CAstNode[] result = new CAstNode[cnt];
    for (int i = 0; i < result.length; i++, n = n.getNext()) {
      result[i] = walkNodes(n, context);
    }

    return result;
  }

  private CAstNode[] gatherChildren(Node n, WalkContext context, int skip) {
    Node c = n.getFirstChild();
    while (skip-- > 0)
      c = c.getNext();
    return gatherSiblings(c, context);
  }

  private CAstNode[] gatherChildren(Node n, WalkContext context) {
    return gatherSiblings(n.getFirstChild(), context);
  }

  private CAstNode walkNodes(final Node n, WalkContext context) {
    return noteSourcePosition(context, walkNodesInternal(n, context), n);
  }

  private Position makePosition(Node n) {
    URL url = sourceModule.getURL();
    int line = n.getLineno();
    if (sourceModule instanceof MappedSourceModule) {
      Position loc = ((MappedSourceModule) sourceModule).getMapping().getAssociatedFileAndLine(line);
      if (loc != null) {
        return loc;
      }
    }
    return new LineNumberPosition(url, url, line);
  }

  private CAstNode noteSourcePosition(WalkContext context, CAstNode n, Node p) {
    if (p.getLineno() != -1 && context.pos().getPosition(n) == null) {
      context.pos().setPosition(n, makePosition(p));
    }
    return n;
  }

  private CAstNode readName(WalkContext context, String name) {
    CAstNode cn = Ast.makeNode(CAstNode.VAR, Ast.makeConstant(name));
    context.cfg().map(cn, cn);
    CAstNode target = context.getCatchTarget();
    if (target != null) {
      context.cfg().add(cn, target, JavaScriptTypes.ReferenceError);
    } else {
      context.cfg().add(cn, CAstControlFlowMap.EXCEPTION_TO_EXIT, JavaScriptTypes.ReferenceError);
    }
    return cn;
  }

  private CAstNode walkNodesInternal(final Node n, WalkContext context) {
    final int NT = n.getType();
    switch (NT) {

    case Token.FUNCTION: {
      int fnIndex = n.getExistingIntProp(Node.FUNCTION_PROP);
      FunctionNode fn = context.top().getFunctionNode(fnIndex);

      CAstEntity fne = walkEntity(fn, context);

      if (context.expressionContext()) {
        CAstNode fun = Ast.makeNode(CAstNode.FUNCTION_EXPR, Ast.makeConstant(fne));

        context.addScopedEntity(fun, fne);

        return fun;

      } else {
        context.addNameDecl(Ast.makeNode(CAstNode.FUNCTION_STMT, Ast.makeConstant(fne)));

        context.addScopedEntity(null, fne);

        return Ast.makeNode(CAstNode.EMPTY);
      }
    }

    case Token.CATCH_SCOPE: {
      Node catchVarNode = n.getFirstChild();
      String catchVarName = catchVarNode.getString();
      assert catchVarName != null;
      context.setCatchVar(catchVarName);
      return Ast.makeNode(CAstNode.EMPTY);
    }

    case Token.LOCAL_BLOCK: {
      return Ast.makeNode(CAstNode.BLOCK_EXPR, gatherChildren(n, context));
    }

    case Token.TRY: {
      Node catchNode = ((Node.Jump) n).target;
      Node finallyNode = ((Node.Jump) n).getFinally();

      ArrayList<Node> tryList = new ArrayList<Node>();
      ArrayList<Node> catchList = new ArrayList<Node>();
      ArrayList<Node> finallyList = new ArrayList<Node>();
      ArrayList<Node> current = tryList;
      Node c;
      for (c = n.getFirstChild(); c.getNext() != null; c = c.getNext()) {
        if (c == catchNode) {
          current = catchList;
        } else if (c == finallyNode) {
          current = finallyList;
        }

        if (c.getType() == Token.GOTO &&
        // ((Node.Jump)c).target == lastChildNode &&
            (c.getNext() == catchNode || c.getNext() == finallyNode)) {
          continue;
        }

        current.add(c);
      }

      CAstNode finallyBlock = null;
      if (finallyNode != null) {
        int i = 0;
        CAstNode[] finallyAsts = new CAstNode[finallyList.size()];
        for (Iterator<Node> fns = finallyList.iterator(); fns.hasNext();) {
          finallyAsts[i++] = walkNodes(fns.next(), context);
        }
        finallyBlock = Ast.makeNode(CAstNode.BLOCK_STMT, finallyAsts);
      }

      if (catchNode != null) {

        int i = 0;
        WalkContext catchChild = new CatchBlockContext(context);
        CAstNode[] catchAsts = new CAstNode[catchList.size()];
        for (Iterator<Node> cns = catchList.iterator(); cns.hasNext();) {
          catchAsts[i++] = walkNodes(cns.next(), catchChild);
        }
        CAstNode catchBlock = Ast.makeNode(CAstNode.CATCH, Ast.makeConstant(catchChild.getCatchVar()),
            Ast.makeNode(CAstNode.BLOCK_STMT, catchAsts));
        context.cfg().map(catchBlock, catchBlock);

        i = 0;
        WalkContext tryChild = new TryBlockContext(context, catchBlock);
        CAstNode[] tryAsts = new CAstNode[tryList.size()];
        for (Iterator<Node> tns = tryList.iterator(); tns.hasNext();) {
          tryAsts[i++] = walkNodes(tns.next(), tryChild);
        }
        CAstNode tryBlock = Ast.makeNode(CAstNode.BLOCK_STMT, tryAsts);

        if (finallyBlock != null) {
          return Ast.makeNode(CAstNode.BLOCK_STMT,
              Ast.makeNode(CAstNode.UNWIND, Ast.makeNode(CAstNode.TRY, tryBlock, catchBlock), finallyBlock), walkNodes(c, context));
        } else {
          return Ast.makeNode(CAstNode.BLOCK_STMT, Ast.makeNode(CAstNode.TRY, tryBlock, catchBlock), walkNodes(c, context));
        }

      } else {
        int i = 0;
        CAstNode[] tryAsts = new CAstNode[tryList.size()];
        for (Iterator<Node> tns = tryList.iterator(); tns.hasNext();) {
          tryAsts[i++] = walkNodes(tns.next(), context);
        }
        CAstNode tryBlock = Ast.makeNode(CAstNode.BLOCK_STMT, tryAsts);

        return Ast.makeNode(
            CAstNode.BLOCK_STMT,
            Ast.makeNode(CAstNode.UNWIND, Ast.makeNode(CAstNode.BLOCK_STMT, tryBlock),
                Ast.makeNode(CAstNode.BLOCK_STMT, finallyBlock)), walkNodes(c, context));
      }
    }

    case Token.JSR: {
      return Ast.makeNode(CAstNode.EMPTY);
      /*
       * Node jsrTarget = ((Node.Jump)n).target; Node finallyNode =
       * jsrTarget.getNext(); return walkNodes(finallyNode, context);
       */
    }

    case Token.COMMA: {
      int count = countSiblingsStartingFrom(n.getFirstChild());

      CAstNode[] cs = new CAstNode[count];
      int i = 0;
      for (Node c = n.getFirstChild(); c != null; i++, c = c.getNext()) {
        if (c.getNext() == null) {
          // for the final sub-expression of the comma, if we care about the
          // base pointer
          // of n, we care about the base pointer of c
          context.updateBase(n, c);
        }
        cs[i] = walkNodes(c, context);
      }

      return Ast.makeNode(CAstNode.BLOCK_EXPR, cs);
    }

      /*
       * case Token.ENTERWITH: { return
       * Ast.makeNode(JavaScriptCAstNode.ENTER_WITH,
       * walkNodes(n.getFirstChild(), context)); }
       * 
       * case Token.LEAVEWITH: { return
       * Ast.makeNode(JavaScriptCAstNode.EXIT_WITH, Ast.makeConstant(null)); }
       */
    case Token.ENTERWITH:
    case Token.LEAVEWITH: {
      return Ast.makeNode(CAstNode.EMPTY);
    }

    case Token.LOOP: {
      LoopContext child = new LoopContext(context);
      CAstNode[] nodes = gatherChildren(n, child);

      if (child.forInInitExpr != null) {
        String nm = child.forInVar;
        return Ast.makeNode(CAstNode.BLOCK_STMT, Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl(nm, true)),
            walkNodes(child.forInInitExpr, context)), nodes);
      } else {
        return Ast.makeNode(CAstNode.BLOCK_STMT, nodes);
      }
    }

    case Token.WITH:
    case Token.FINALLY:
    case Token.BLOCK:
    case Token.LABEL: {
      Node c1 = n.getFirstChild();
      if (c1 != null && c1.getType() == Token.SWITCH) {
        Node switchValue = c1.getFirstChild();

        CAstNode defaultLabel = Ast.makeNode(CAstNode.LABEL_STMT, Ast.makeNode(CAstNode.EMPTY));
        context.cfg().map(defaultLabel, defaultLabel);

        List<CAstNode> labelCasts = new ArrayList<CAstNode>();
        for (Node kase = switchValue.getNext(); kase != null; kase = kase.getNext()) {
          assert kase.getType() == Token.CASE;
          Node caseLbl = kase.getFirstChild();
          Node target = ((Node.Jump) kase).target;
          CAstNode labelCast = walkNodes(caseLbl, context);
          labelCasts.add(labelCast);
          context.cfg().add(c1, target, labelCast);
        }
        CAstNode[] children = new CAstNode[labelCasts.size() + 1];
        int i = 0;
        children[i++] = Ast.makeNode(CAstNode.BLOCK_STMT, defaultLabel, gatherChildren(n, context, 1));

        // Note that we are placing the labels as children in the AST
        // even if they are not used, because we want them copied when AST is
        // re-written.
        for (CAstNode labelCast : labelCasts) {
          children[i++] = labelCast;
        }

        context.cfg().add(c1, defaultLabel, CAstControlFlowMap.SWITCH_DEFAULT);
        CAstNode switchAst = Ast.makeNode(CAstNode.SWITCH, walkNodes(switchValue, context), children);

        noteSourcePosition(context, switchAst, c1);
        context.cfg().map(c1, switchAst);
        return switchAst;
      }

      else {
        return Ast.makeNode(CAstNode.BLOCK_STMT, gatherChildren(n, context));
      }
    }

    case Token.EXPR_VOID:
    case Token.EXPR_RESULT: {
      WalkContext child = new ExpressionContext(context);
      Node expr = n.getFirstChild();

      if (NT == Token.EXPR_RESULT) {
        // EXPR_RESULT node is just a wrapper, so if we care about base pointer
        // of n, we
        // care about child of n
        child.updateBase(n, expr);
      }

      return walkNodes(expr, child);
    }

    case Token.POS: {
      return Ast.makeNode(CAstNode.UNARY_EXPR, translateOpcode(Token.ADD), walkNodes(n.getFirstChild(), context));
    }

    case Token.CALL: {
      if (!isPrimitiveCall(context, n)) {
        CAstNode base = Ast.makeNode(CAstNode.VAR, Ast.makeConstant("base"));
        Node callee = n.getFirstChild();
        WalkContext child = new BaseCollectingContext(context, callee, base);
        CAstNode fun = walkNodes(callee, child);

        // the first actual parameter appearing within the parentheses of the
        // call (i.e., possibly excluding the 'this' parameter)
        Node firstParamInParens = callee.getNext();
        if (child.foundBase(callee))
          return Ast.makeNode(
              CAstNode.LOCAL_SCOPE,
              Ast.makeNode(CAstNode.BLOCK_EXPR,
                  Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl("base")), Ast.makeConstant(null)),
                  makeCall(fun, base, firstParamInParens, context)));
        else
          return makeCall(fun, Ast.makeConstant(null), firstParamInParens, context);
      } else {
        return Ast.makeNode(CAstNode.PRIMITIVE, gatherChildren(n, context, 1));
      }
    }

    case Token.BINDNAME:
    case Token.NAME: {
      return readName(context, n.getString());
    }

    case Token.THIS: {
      return Ast.makeNode(CAstNode.VAR, Ast.makeConstant("this"));
    }

    case Token.THISFN: {
      return Ast.makeNode(CAstNode.VAR, Ast.makeConstant(((FunctionNode) context.top()).getFunctionName()));
    }

    case Token.STRING: {
      return Ast.makeConstant(n.getString());
    }

    case Token.NUMBER: {
      return Ast.makeConstant(n.getDouble());
    }

    case Token.FALSE: {
      return Ast.makeConstant(false);
    }

    case Token.TRUE: {
      return Ast.makeConstant(true);
    }

    case Token.NULL:
    case Token.VOID: {
      return Ast.makeConstant(null);
    }

    case Token.ADD:
    case Token.DIV:
    case Token.LSH:
    case Token.MOD:
    case Token.MUL:
    case Token.RSH:
    case Token.SUB:
    case Token.URSH:
    case Token.BITAND:
    case Token.BITOR:
    case Token.BITXOR:
    case Token.EQ:
    case Token.SHEQ:
    case Token.GE:
    case Token.GT:
    case Token.LE:
    case Token.LT:
    case Token.SHNE:
    case Token.NE: {
      Node l = n.getFirstChild();
      Node r = l.getNext();
      return Ast.makeNode(CAstNode.BINARY_EXPR, translateOpcode(NT), walkNodes(l, context), walkNodes(r, context));
    }

    case Token.NEG: {
      return Ast.makeNode(CAstNode.UNARY_EXPR, translateOpcode(Token.SUB), walkNodes(n.getFirstChild(), context));
    }

    case Token.BITNOT:
    case Token.NOT: {
      return Ast.makeNode(CAstNode.UNARY_EXPR, translateOpcode(NT), walkNodes(n.getFirstChild(), context));
    }

    case Token.VAR:
    case Token.CONST: {
      List<CAstNode> result = new ArrayList<CAstNode>();
      Node nm = n.getFirstChild();
      while (nm != null) {
        context.addNameDecl(Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl(nm.getString())),
            readName(context, "$$undefined")));

        if (nm.getFirstChild() != null) {
          WalkContext child = new ExpressionContext(context);

          result.add(Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(nm.getString())),
              walkNodes(nm.getFirstChild(), child)));

        }

        nm = nm.getNext();
      }

      if (result.size() > 0) {
        return Ast.makeNode(CAstNode.BLOCK_EXPR, result.toArray(new CAstNode[result.size()]));
      } else {
        return Ast.makeNode(CAstNode.EMPTY);
      }
    }

    case Token.REGEXP: {
      int regexIdx = n.getIntProp(Node.REGEXP_PROP, -1);

      assert regexIdx != -1 : "while converting: " + context.top().toStringTree(context.top()) + "\nlooking at bad regex:\n "
          + n.toStringTree(context.top());

      String flags = context.top().getRegexpFlags(regexIdx);
      Node flagsNode = Node.newString(flags);

      String str = context.top().getRegexpString(regexIdx);
      Node strNode = Node.newString(str);

      strNode.addChildToFront(flagsNode);

      return handleNew(context, "RegExp", strNode);
    }

    case Token.ENUM_INIT_KEYS: {
      context.createForInVar(n.getFirstChild());
      return Ast.makeNode(CAstNode.EMPTY);
    }

    case Token.ENUM_ID: {
      return Ast.makeNode(CAstNode.EACH_ELEMENT_GET, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(context.getForInInitVar())));
    }

    case Token.ENUM_NEXT: {
      return Ast.makeNode(CAstNode.EACH_ELEMENT_HAS_NEXT, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(context.getForInInitVar())));
    }

    case Token.RETURN: {
      Node val = n.getFirstChild();
      if (val != null) {
        WalkContext child = new ExpressionContext(context);
        return Ast.makeNode(CAstNode.RETURN, walkNodes(val, child));
      } else {
        return Ast.makeNode(CAstNode.RETURN);
      }
    }

    case Token.SETNAME: {
      Node nm = n.getFirstChild();
      return Ast.makeNode(CAstNode.ASSIGN, walkNodes(nm, context), walkNodes(nm.getNext(), context));
    }

    case Token.IFNE:
    case Token.IFEQ: {
      context.cfg().add(n, ((Node.Jump) n).target, Boolean.TRUE);
      WalkContext child = new ExpressionContext(context);
      CAstNode gotoAst = Ast.makeNode(CAstNode.IFGOTO, translateOpcode(NT), walkNodes(n.getFirstChild(), child),
          Ast.makeConstant(1));

      context.cfg().map(n, gotoAst);
      return gotoAst;
    }

    case Token.GOTO: {
      context.cfg().add(n, ((Node.Jump) n).target, null);
      CAstNode gotoAst = Ast.makeNode(CAstNode.GOTO, Ast.makeConstant(((Node.Jump) n).target.labelId()));

      context.cfg().map(n, gotoAst);
      return gotoAst;
    }

    case Token.BREAK: {
      context.cfg().add(n, ((Node.Jump) n).getJumpStatement().target, null);
      CAstNode gotoAst = Ast.makeNode(CAstNode.GOTO, Ast.makeConstant(((Node.Jump) n).getJumpStatement().target.labelId()));

      context.cfg().map(n, gotoAst);
      return gotoAst;
    }

    case Token.CONTINUE: {
      context.cfg().add(n, ((Node.Jump) n).getJumpStatement().getContinue(), null);
      CAstNode gotoAst = Ast.makeNode(CAstNode.GOTO, Ast.makeConstant(((Node.Jump) n).getJumpStatement().getContinue().labelId()));

      context.cfg().map(n, gotoAst);
      return gotoAst;
    }

    case Token.TARGET: {
      CAstNode result = Ast.makeNode(CAstNode.LABEL_STMT, Ast.makeConstant(n.labelId()), Ast.makeNode(CAstNode.EMPTY));

      context.cfg().map(n, result);
      return result;
    }

    case Token.OR: {
      Node l = n.getFirstChild();
      Node r = l.getNext();
      return Ast.makeNode(CAstNode.IF_EXPR, walkNodes(l, context), Ast.makeConstant(true), walkNodes(r, context));
    }

    case Token.AND: {
      Node l = n.getFirstChild();
      Node r = l.getNext();
      return Ast.makeNode(CAstNode.IF_EXPR, walkNodes(l, context), walkNodes(r, context), Ast.makeConstant(false));
    }

    case Token.HOOK: {
      Node cond = n.getFirstChild();
      Node thenBranch = cond.getNext();
      Node elseBranch = thenBranch.getNext();
      return Ast.makeNode(CAstNode.IF_EXPR, walkNodes(cond, context), walkNodes(thenBranch, context),
          walkNodes(elseBranch, context));

    }

    case Token.INC:
    case Token.DEC: {
      int flags = n.getIntProp(Node.INCRDECR_PROP, -1);
      CAstNode op = ((flags & Node.DECR_FLAG) != 0) ? CAstOperator.OP_SUB : CAstOperator.OP_ADD;

      Node l = n.getFirstChild();
      CAstNode last = walkNodes(l, context);

      return Ast.makeNode((((flags & Node.POST_FLAG) != 0) ? CAstNode.ASSIGN_POST_OP : CAstNode.ASSIGN_PRE_OP), last,
          Ast.makeConstant(1), op);
    }

    case Token.NEW: {
      if (isPrimitiveCreation(context, n)) {
        return makeBuiltinNew(n.getFirstChild().getString());
      } else {
        Node receiver = n.getFirstChild();
        return handleNew(context, walkNodes(receiver, context), receiver.getNext());
      }
    }

    case Token.ARRAYLIT: {
      int count = 0;
      for (Node x = n.getFirstChild(); x != null; count++, x = x.getNext())
        ;

      int i = 0;
      CAstNode[] args = new CAstNode[2 * count + 1];
      args[i++] = (isPrologueScript(context)) ? makeBuiltinNew("Array") : handleNew(context, "Array", null);

      int[] skips = (int[]) n.getProp(Node.SKIP_INDEXES_PROP);
      int skip = 0;
      int idx = 0;
      Node elt = n.getFirstChild();
      while (elt != null) {
        if (skips != null && skip < skips.length && skips[skip] == idx) {
          skip++;
          idx++;
          continue;
        }

        args[i++] = Ast.makeConstant(idx++);
        args[i++] = walkNodes(elt, context);

        elt = elt.getNext();
      }

      return Ast.makeNode(CAstNode.OBJECT_LITERAL, args);
    }

    case Token.OBJECTLIT: {
      Object[] propertyList = (Object[]) n.getProp(Node.OBJECT_IDS_PROP);
      CAstNode[] args = new CAstNode[propertyList.length * 2 + 1];
      int i = 0;
      args[i++] = ((isPrologueScript(context)) ? makeBuiltinNew("Object") : handleNew(context, "Object", null));

      Node val = n.getFirstChild();
      int nameIdx = 0;
      for (; nameIdx < propertyList.length; nameIdx++, val = val.getNext()) {
        args[i++] = Ast.makeConstant(propertyList[nameIdx]);
        args[i++] = walkNodes(val, context);
      }

      return Ast.makeNode(CAstNode.OBJECT_LITERAL, args);
    }

    case Token.GETPROP:
    case Token.GETELEM: {
      Node receiver = n.getFirstChild();
      Node element = receiver.getNext();

      CAstNode rcvr = walkNodes(receiver, context);
      CAstNode baseVar = context.getBaseVarIfRelevant(n);

      CAstNode elt = walkNodes(element, context);

      CAstNode get, result;
      if (baseVar != null) {
        result = Ast.makeNode(CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.ASSIGN, baseVar, rcvr),
            get = Ast.makeNode(CAstNode.OBJECT_REF, baseVar, elt));
      } else {
        result = get = Ast.makeNode(CAstNode.OBJECT_REF, rcvr, elt);
      }

      if (context.getCatchTarget() != null) {
        context.cfg().map(get, get);
        context.cfg().add(get, context.getCatchTarget(), JavaScriptTypes.TypeError);
      }

      return result;
    }
    
    case Token.GET_REF: {
      // read of __proto__
      // first and only child c1 is of type Token.REF_SPECIAL whose NAME_PROP property should be "__proto__".  
      // c1 has a single child, the base pointer for the reference
      Node child1 = n.getFirstChild();
      assert child1.getType() == Token.REF_SPECIAL;
      assert child1.getProp(Node.NAME_PROP).equals("__proto__");
      Node receiver = child1.getFirstChild();     
      assert child1.getNext() == null;
      CAstNode rcvr = walkNodes(receiver, context);
      final CAstNode result = Ast.makeNode(CAstNode.OBJECT_REF, rcvr, Ast.makeConstant("__proto__"));
      
      if (context.getCatchTarget() != null) {
        context.cfg().map(result, result);
        context.cfg().add(result, context.getCatchTarget(), JavaScriptTypes.TypeError);
      }
      
      return result;
    }

    case Token.SETPROP:
    case Token.SETELEM: {
      Node receiver = n.getFirstChild();
      Node elt = receiver.getNext();
      Node val = elt.getNext();

      CAstNode rcvr = walkNodes(receiver, context);

      return Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, rcvr, walkNodes(elt, context)),
          walkNodes(val, context));
    }
    
    case Token.SET_REF: {
      // first child c1 is of type Token.REF_SPECIAL whose NAME_PROP property should be "__proto__".  
      // c1 has a single child, the base pointer for the reference
      // second child c2 is RHS of assignment
      Node child1 = n.getFirstChild();
      assert child1.getType() == Token.REF_SPECIAL;
      assert child1.getProp(Node.NAME_PROP).equals("__proto__");
      Node receiver = child1.getFirstChild();
      Node val = child1.getNext();

      CAstNode rcvr = walkNodes(receiver, context);

      return Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, rcvr, Ast.makeConstant("__proto__")),
          walkNodes(val, context));
    }
      

    case Token.DELPROP: {
      Node receiver = n.getFirstChild();
      Node element = receiver.getNext();

      CAstNode rcvr = walkNodes(receiver, context);
      CAstNode baseVar = context.getBaseVarIfRelevant(n);

      CAstNode elt = walkNodes(element, context);

      if (baseVar != null) {
        return Ast.makeNode(CAstNode.BLOCK_EXPR, Ast.makeNode(CAstNode.ASSIGN, baseVar, rcvr),
            Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, baseVar, elt), Ast.makeConstant(null)));
      } else {
        return Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, rcvr, elt), Ast.makeConstant(null));
      }
    }

    case Token.TYPEOFNAME: {
      return Ast.makeNode(CAstNode.TYPE_OF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(n.getString())));
    }

    case Token.TYPEOF: {
      return Ast.makeNode(CAstNode.TYPE_OF, walkNodes(n.getFirstChild(), context));
    }

    case Token.SETPROP_OP:
    case Token.SETELEM_OP: {
      Node receiver = n.getFirstChild();
      Node elt = receiver.getNext();
      Node op = elt.getNext();

      CAstNode rcvr = walkNodes(receiver, context);

      return Ast.makeNode(CAstNode.ASSIGN_POST_OP, Ast.makeNode(CAstNode.OBJECT_REF, rcvr, walkNodes(elt, context)),
          walkNodes(op.getFirstChild().getNext(), context), translateOpcode(op.getType()));
    }

    case Token.THROW: {
      CAstNode catchNode = context.getCatchTarget();
      if (catchNode != null)
        context.cfg().add(n, context.getCatchTarget(), null);
      else
        context.cfg().add(n, CAstControlFlowMap.EXCEPTION_TO_EXIT, null);

      CAstNode throwAst = Ast.makeNode(CAstNode.THROW, walkNodes(n.getFirstChild(), context));

      context.cfg().map(n, throwAst);
      return throwAst;
    }

    case Token.EMPTY: {
      return Ast.makeConstant(null);
    }

    case Token.INSTANCEOF: {
      Node value = n.getFirstChild();
      Node type = value.getNext();
      return Ast.makeNode(CAstNode.INSTANCEOF, walkNodes(value, context), walkNodes(type, context));
    }

    case Token.IN: {
      Node value = n.getFirstChild();
      Node property = value.getNext();
      return Ast.makeNode(CAstNode.IS_DEFINED_EXPR, walkNodes(value, context), walkNodes(property, context));
    }

    default: {
      System.err.println("while converting: " + context.top().toStringTree(context.top()) + "\nlooking at unhandled:\n "
          + n.toStringTree(context.top()) + "\n(of type " + NT + ") (of class " + n.getClass() + ")" + " at " + n.getLineno());

      Assertions.UNREACHABLE();
      return null;
    }
    }
  }

  /**
   * parse the JavaScript code using Rhino, and then translate the resulting AST
   * to CAst
   */
  public CAstEntity translate() throws java.io.IOException {
    ToolErrorReporter reporter = new ToolErrorReporter(true);
    CompilerEnvirons compilerEnv = new CompilerEnvirons();
    compilerEnv.setErrorReporter(reporter);
    compilerEnv.setReservedKeywordAsIdentifier(true);

    if (DEBUG)
      System.err.println(("translating " + scriptName + " with Rhino"));

    Parser P = new Parser(compilerEnv, compilerEnv.getErrorReporter());

    sourceReader = sourceModule.getInputReader();

    ScriptOrFnNode top = P.parse(sourceReader, scriptName, 1);

    return walkEntity(top, new RootContext());
  }

  private final CAst Ast;

  private final String scriptName;

  private final SourceModule sourceModule;

  private Reader sourceReader;

  private int anonymousCounter = 0;

  public RhinoToAstTranslator(CAst Ast, SourceModule M, String scriptName) {
    this.Ast = Ast;
    this.scriptName = scriptName;
    this.sourceModule = M;
  }
}
