/******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import static com.ibm.wala.cast.tree.CAstNode.ASSIGN;
import static com.ibm.wala.cast.tree.CAstNode.BINARY_EXPR;
import static com.ibm.wala.cast.tree.CAstNode.BLOCK_EXPR;
import static com.ibm.wala.cast.tree.CAstNode.BLOCK_STMT;
import static com.ibm.wala.cast.tree.CAstNode.CALL;
import static com.ibm.wala.cast.tree.CAstNode.CONSTANT;
import static com.ibm.wala.cast.tree.CAstNode.EMPTY;
import static com.ibm.wala.cast.tree.CAstNode.FUNCTION_EXPR;
import static com.ibm.wala.cast.tree.CAstNode.FUNCTION_STMT;
import static com.ibm.wala.cast.tree.CAstNode.GOTO;
import static com.ibm.wala.cast.tree.CAstNode.IF_STMT;
import static com.ibm.wala.cast.tree.CAstNode.LOCAL_SCOPE;
import static com.ibm.wala.cast.tree.CAstNode.OBJECT_LITERAL;
import static com.ibm.wala.cast.tree.CAstNode.OBJECT_REF;
import static com.ibm.wala.cast.tree.CAstNode.OPERATOR;
import static com.ibm.wala.cast.tree.CAstNode.RETURN;
import static com.ibm.wala.cast.tree.CAstNode.TRY;
import static com.ibm.wala.cast.tree.CAstNode.VAR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A CAst rewriter for extracting bits of code into one-shot closures. What to extract is determined 
 * by an {@link ExtractionPolicy}.
 * 
 * <p>For instance, a {@link ForInBodyExtractionPolicy} extracts the body of every for-in loop in
 * the program, whereas a {@link CorrelatedPairExtractionPolicy} extracts pieces of code containing
 * correlated property reads and writes of the same property.</p>
 * 
 * <p>As an example, consider the following function:</p>
 * <pre>
 *   function extend(dest, src) {
 *     for(var p in src)
 *       dest[p] = src[p];
 *   }
 * </pre>
 * <p>Under both {@link ForInBodyExtractionPolicy} and {@link CorrelatedPairExtractionPolicy}, this 
 * should be transformed into</p>
 * <pre>
 *   function extend(dest, src) {
 *     for(var p in src)
 *       (function _forin_body_0(p) {
 *         dest[p] = src[p];
 *       })(p);
 *   }
 * </pre>
 * 
 * <p>There are four issues to be considered here.</p>
 * <ul>
 * <li><b>References to <code>this</code></b>: 
 *   <p>If the code to extract contains references to <code>this</code>, these references have to be 
 *   rewritten; otherwise they would refer to the global object in the transformed code.</p>
 *   <p>We do this by giving the extracted function an extra parameter <code>thi$</code>, and rewriting
 *   <code>this</code> to <code>thi$</code> within the extracted code.</p>
 *   <p>For instance,</p>
 *   <pre>
 *   Object.prototype.extend = function(src) {
 *     for(var p in src)
 *       this[p] = src[p];
 *   }
 *   </pre>
 *   <p>becomes</p>
 *   <pre>
 *   Object.prototype.extend = function(src) {
 *     for(var p in src)
 *       (function _forin_body_0(p, thi$) {
 *         thi$[p] = src[p];
 *       })(p, this);
 *   }
 *   </pre>
 * </li>
 * <li><b>Local variable declarations</b>:
 *   <p>Local variable declarations inside the extracted code have to be hoisted to the enclosing function;
 *   otherwise they would become local variables of the extracted function instead.</p>
 *   <p>This is already taken care of by the translation from Rhino's AST to CAst.</p>
 * </li>
 * <li><b><code>break</code>,<code>continue</code>,<code>return</code></b>:
 *   <p>A <code>break</code> or <code>continue</code> statement within the extracted loop body that refers
 *   to the loop itself or an enclosing loop would become invalid in the transformed code. A <code>return</code>
 *   statement would no longer return from the enclosing function, but instead from the extracted function.</p>
 *   <p>We transform all three statements into <code>return</code> statements returning an object literal with a
 *   property <code>type</code> indicating whether this is a 'goto' (i.e., <code>break</code> or <code>return</code>)
 *   or a 'return'. In the former case, the 'target' property contains an integer identifying the jump target; in
 *   the latter case, the 'value' property contains the value to return.</p>
 *   <p>The return value of the extracted function is then examined to determine whether it completed normally
 *   (i.e., returned <code>undefined</code>), or whether it returned an object indicating special control flow.</p>
 *   <p>For example, consider this code from MooTools:</p>
 *   <pre>
 *   for(var style in Element.ShortStyles) {
 *     if(property != style)
 *       continue;
 *     for(var s in Element.ShortStyles[style])
 *       result.push(this.getStyle(s));
 *     return result.join(' ');
 *   }
 *   </pre>
 *   <p>Under {@link ForInBodyExtractionPolicy}, this is transformed into</p>
 *   <pre>
 *   for(var style in Element.ShortStyles) {
 *     var s;
 *     re$ = (function _forin_body_0(style, thi$) {
 *       if(property != style)
 *         return { type: 'goto', target: 1 };
 *       for(s in Element.ShortStyles[style]) {
 *         (function _forin_body_2(s) {
 *           result.push(thi$.getStyle(s));
 *         })(s);
 *       }
 *       return { type: 'return', value: result.join(' ') };
 *     })(style, this);
 *     if(re$) {
 *       if(re$.type == 'return')
 *         return re$.value;
 *       if(re$.type == 'goto') {
 *         if(re$.target == 1)
 *           continue;
 *       }
 *     }
 *   }
 *   </pre>
 *   <p>Note that at the CAst level, <code>break</code> and <code>continue</code> are represented as <code>goto</code>
 *   statements, which simplifies the translation somewhat. The numerical encoding of jump targets does not matter
 *   as long as the extracted function and the fixup code agree on which number represents which label.</p>
 * </li>
 * <li><b>Assignment to loop variable</b>:
 *   <p>The loop body may assign to the loop variable. If the variable is referenced after the loop, this assignment
 *   needs to be propagated back to the enclosing function in the extracted code.</p>
 *   <p><b>TODO:</b> This is not handled at the moment.</p> 
 * </li>
 * </ul>
 * 
 * <p>Finally, note that exceptions do not need to be handled specially.</p>
 * 
 * @author mschaefer
 *
 */
public class ClosureExtractor extends CAstRewriterExt {
  private LinkedList<ExtractionPolicy> policies = new LinkedList<ExtractionPolicy>();
  private final ExtractionPolicyFactory policyFactory;
  
  // names for extracted functions are built from this string with a number appended
  private static final String EXTRACTED_FUN_BASENAME = "_forin_body_";

  private NodeLabeller labeller = new NodeLabeller();

  public ClosureExtractor(CAst Ast, ExtractionPolicyFactory policyFactory) {
    super(Ast, true, new RootPos());
    this.policyFactory = policyFactory;
  }
  
  @Override
  protected void enterEntity(CAstEntity entity) {
    policies.push(policyFactory.createPolicy(entity));
  }
  
  @Override
  protected void leaveEntity(CAstEntity entity) {
    policies.pop();
  }

  @Override
  protected CAstNode copyNodes(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    switch(root.getKind()) {
    case OPERATOR:
      return root;
    case CONSTANT:
      return copyConstant(root, cfg, context, nodeMap);
    case BLOCK_STMT:
      return copyBlock(root, cfg, context, nodeMap);
    case RETURN: 
      return copyReturn(root, cfg, context, nodeMap);
    case VAR: 
      return copyVar(root, cfg, context, nodeMap);
    case GOTO:
      return copyGoto(root, cfg, context, nodeMap);
    default:
      return copyNode(root, cfg, context, nodeMap);
    }
  }

  /* Constants are not affected by the rewriting, they are just copied. */
  private CAstNode copyConstant(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstNode newNode = Ast.makeConstant(root.getValue());
    nodeMap.put(Pair.make(root, context.key()), newNode);
    return newNode;
  }

  /* Ask the policy whether it wants anything extracted from this block; otherwise the node is simply copied. */
  private CAstNode copyBlock(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    List<ExtractionRegion> regions = policies.getFirst().extract(root);
    if(regions == null) {
      return copyNode(root, cfg, context, nodeMap);
    } else {
      ArrayList<CAstNode> copied_children = new ArrayList<CAstNode>();
      int next_child = 0;
      // code in between regions is handled by invoking copyNodes, the regions themselves by extractRegion
      for(ExtractionRegion region : regions) {
        for(;next_child<region.getStart();++next_child)
          copied_children.add(copyNodes(root.getChild(next_child), cfg, new ChildPos(root, next_child, context), nodeMap));
        for(CAstNode stmt : extractRegion(root, cfg, new ExtractionPos(root, region, context), nodeMap))
          copied_children.add(stmt);
        next_child = region.getEnd();
      }
      for(;next_child<root.getChildCount();++next_child)
        copied_children.add(copyNodes(root.getChild(next_child), cfg, new ChildPos(root, next_child, context), nodeMap));
      CAstNode newNode = Ast.makeNode(root.getKind(), copied_children.toArray(new CAstNode[0]));
      nodeMap.put(Pair.make(root, context.key()), newNode);
      return newNode;
    }
  }

  /*
   * Normal variables are just copied, but 'this' references need to be rewritten if we are inside an extracted
   * function body.
   */
  private CAstNode copyVar(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    /*
     * If this node is a "this" reference, the outermost enclosing extracted function needs to pass in
     * the value of "this" as a parameter.
     * 
     * NB: This has to be done by the _outermost_ function. If it were done by an inner function instead,
     *     the outermost one may not pass in the value of "this" at all, so the inner one would
     *     get the wrong value.
     */
    if(root.getChild(0).getValue().equals("this")) {
      ExtractionPos epos = ExtractionPos.getOutermostEnclosingExtractionPos(context);
      if(epos != null) {
        epos.addThis();
        CAstNode newNode = makeVarRef(epos.getThisParmName());
        addExnFlow(newNode, JavaScriptTypes.ReferenceError, getCurrentEntity(), context);
        nodeMap.put(Pair.make(root, context.key()), newNode);
        return newNode;
      } else {
        return copyNode(root, cfg, context, nodeMap);
      }
    } else {
      return copyNode(root, cfg, context, nodeMap);
    }
  }

  /*
   * 'break' and 'continue' statements are both encoded as GOTO. If they refer to a target outside the innermost
   * enclosing extracted function body, they are rewritten into a 'return' statement.
   */
  private CAstNode copyGoto(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstNode target = getCurrentEntity().getControlFlow().getTarget(root, null);
    ExtractionPos epos = ExtractionPos.getEnclosingExtractionPos(context);
    if(epos != null && !NodePos.inSubtree(target, epos.getParent())) {
      epos.addGotoTarget(root.getChildCount() > 0 ? (String)root.getChild(0).getValue(): null, target);
      int label = labeller.addNode(target);
      // return { type: 'goto', target: <label> }
      CAstNode newNode = 
          Ast.makeNode(RETURN,
              Ast.makeNode(OBJECT_LITERAL,
                  addExnFlow(Ast.makeNode(CALL,
                      addExnFlow(makeVarRef("Object"), JavaScriptTypes.ReferenceError, getCurrentEntity(), context),
                      Ast.makeConstant("ctor")), null, getCurrentEntity(), context),
                      Ast.makeConstant("type"),
                      Ast.makeConstant("goto"),
                      Ast.makeConstant("target"),
                      Ast.makeConstant(((double)label)+"")));
      // remove outgoing cfg edges of the old node
      deleteFlow(root, getCurrentEntity());
      nodeMap.put(Pair.make(root, context.key()), newNode);
      return newNode;
    } else {
      return copyNode(root, cfg, context, nodeMap);
    }
  }

  /* 'return' statements inside an extracted function body need to be encoded in a similar fashion. */
  private CAstNode copyReturn(CAstNode root, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    ExtractionPos epos = ExtractionPos.getEnclosingExtractionPos(context);

    if(epos == null)
      return copyNode(root, cfg, context, nodeMap);

    // add a return to every enclosing extracted function body
    do {
      epos.addReturn();
      epos = ExtractionPos.getEnclosingExtractionPos(epos.getParentPos());
    } while(epos != null);

    // emit appropriate 'return' statement
    if(root.getChildCount() > 0) {
      // return { type: 'return', value: <retval> }
      CAstNode retval = copyNodes(root.getChild(0), cfg, new ChildPos(root, 0, context), nodeMap);
      CAstNode newNode = 
          Ast.makeNode(RETURN,
              Ast.makeNode(OBJECT_LITERAL,
                  addExnFlow(Ast.makeNode(CALL,
                      addExnFlow(makeVarRef("Object"), JavaScriptTypes.ReferenceError, getCurrentEntity(), context),
                      Ast.makeConstant("ctor")), null, getCurrentEntity(), context),
                      Ast.makeConstant("type"),
                      Ast.makeConstant("return"),
                      Ast.makeConstant("value"),
                      retval));
      nodeMap.put(Pair.make(root, context.key()), newNode);
      return newNode;
    } else {
      // return { type: 'return' }
      CAstNode newNode = 
          Ast.makeNode(RETURN,
              Ast.makeNode(OBJECT_LITERAL,
                  addExnFlow(Ast.makeNode(CALL,
                      addExnFlow(makeVarRef("Object"), JavaScriptTypes.ReferenceError, getCurrentEntity(), context),
                      Ast.makeConstant("ctor")), null, getCurrentEntity(), context),
                      Ast.makeConstant("type"),
                      Ast.makeConstant("return")));
      nodeMap.put(Pair.make(root, context.key()), newNode);
      return newNode;
    }
  }

  /* Recursively copy child nodes. */
  private CAstNode copyNode(CAstNode node, CAstControlFlowMap cfg, NodePos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstNode children[] = new CAstNode[node.getChildCount()];
    for (int i = 0; i < children.length; i++) {
      children[i] = copyNodes(node.getChild(i), cfg, new ChildPos(node, i, context), nodeMap);
    }
    CAstNode newNode = Ast.makeNode(node.getKind(), children);
    nodeMap.put(Pair.make(node, context.key()), newNode);

    // if this node has a control flow successor beyond the innermost enclosing extracted function loop, we need to reroute
    ExtractionPos epos = ExtractionPos.getEnclosingExtractionPos(context);
    if(!isFlowDeleted(newNode, getCurrentEntity()) && epos != null) {
      // CAstControlFlowMap cfg = getCurrentEntity().getControlFlow();
      Collection<Object> labels = cfg.getTargetLabels(node);
      boolean invalidateCFlow = false;
      for(Object label : labels) {
        CAstNode target = cfg.getTarget(node, label);
        if(target != CAstControlFlowMap.EXCEPTION_TO_EXIT && !epos.contains(target)) {
          invalidateCFlow = true;
          break;
        }
      }
      if(invalidateCFlow) {
        deleteFlow(node, getCurrentEntity());
        for(Object label : labels) {
          CAstNode target = cfg.getTarget(node, label);
          if(epos.contains(target))
            addFlow(node, label, target, cfg);
          else
            addFlow(node, label, CAstControlFlowMap.EXCEPTION_TO_EXIT, cfg);
        }
      }
    }
    return newNode;
  }

  private int anonymous_counter = 0;
  
  private List<CAstNode> extractRegion(CAstNode root, CAstControlFlowMap cfg, ExtractionPos context, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstEntity entity = getCurrentEntity();

    // whether we are extracting a single statement that is itself a block
    boolean extractingBlock = context.getStart() + 1 == context.getEnd() && root.getChild(context.getStart()).getKind() == BLOCK_STMT;
    
    // whether we are extracting the body of a local scope
    boolean extractingLocalScope = false;
    
    // whether we are extracting an empty loop body
    boolean extractingEmpty = false;

    String name = EXTRACTED_FUN_BASENAME + (anonymous_counter++);

    // Create a new entity for the extracted function.
    ExtractedFunction new_entity = new ExtractedFunction(name, context);
    context.setExtractedEntity(new_entity);

    // rewrite the code to be extracted

    /*
     * First, we need to massage the code a little bit, prepending an assignment of the form '<name> = <name>'
     * and appending a RETURN statement if it may complete normally (i.e., if execution may 'fall off'
     * the end). Additionally, if the extraction starts inside a nested BLOCK_EXPR, we flatten it out into a list
     * of statements.
     * 
     * The whole thing is then wrapped into a block.
     */
    ArrayList<CAstNode> prologue = new ArrayList<CAstNode>();
    ArrayList<CAstNode> fun_body_stmts = new ArrayList<CAstNode>();

    // if we are extracting a block, unwrap it
    if(extractingBlock) {
      CAstNode block = root.getChild(context.getStart());
      for(int i=0;i<block.getChildCount();++i)
        fun_body_stmts.add(block.getChild(i));
    } else {
      if(context.getRegion() instanceof TwoLevelExtractionRegion) {
        CAstNode start = root.getChild(context.getStart());
        TwoLevelExtractionRegion tler = (TwoLevelExtractionRegion)context.getRegion();
        if(tler.getEndInner() != -1)
          throw new UnimplementedError("Two-level extraction not fully implemented.");
        int i;
        if(start.getKind() == CAstNode.BLOCK_EXPR) {
          for(i=0;i<tler.getStartInner();++i)
            prologue.add(copyNodes(start.getChild(i), cfg, context, nodeMap));
          for(;i<start.getChildCount();++i)
            fun_body_stmts.add(start.getChild(i));
          for(i=context.getStart()+1;i<context.getEnd();++i)
            fun_body_stmts.add(root.getChild(i));
        } else if(start.getKind() == CAstNode.LOCAL_SCOPE) {
          if(tler.getStartInner() != 0 || tler.getEnd() != tler.getStart() + 1)
            throw new UnimplementedError("Unsupported two-level extraction");
          fun_body_stmts.add(start.getChild(0));
          extractingLocalScope = true;
        } else {
          throw new UnimplementedError("Unsupported two-level.");
        }
      } else {
        if(context.getEnd() > context.getStart()+1) {
          CAstNode[] stmts = new CAstNode[context.getEnd()-context.getStart()];
          for(int i=context.getStart();i<context.getEnd();++i)
            stmts[i-context.getStart()] = root.getChild(i);
          fun_body_stmts.add(Ast.makeNode(root.getKind(), stmts));
        } else {
          CAstNode node_to_extract = root.getChild(context.getStart());
          if(node_to_extract.getKind() == CAstNode.EMPTY)
            extractingEmpty = true;
          fun_body_stmts.add(node_to_extract);
        }
      }
    }
    CAstNode fun_body = Ast.makeNode(BLOCK_STMT, fun_body_stmts.toArray(new CAstNode[0]));

    /*
     * Now we rewrite the body and construct a Rewrite object.
     */
    final Map<Pair<CAstNode, NoKey>, CAstNode> nodes = HashMapFactory.make();
    final CAstNode newRoot = copyNodes(fun_body, cfg, context, nodes);
    final CAstSourcePositionMap theSource = copySource(nodes, entity.getSourceMap());
    final CAstControlFlowMap theCfg = copyFlow(nodes, entity.getControlFlow(), theSource);
    final CAstNodeTypeMap theTypes = copyTypes(nodes, entity.getNodeTypeMap());
    final Map<CAstNode, Collection<CAstEntity>> theChildren = HashMapFactory.make();
    for(int i=context.getStart();i<context.getEnd();++i)
      theChildren.putAll(copyChildren(root.getChild(i), nodes, entity.getAllScopedEntities()));

    Rewrite rw = new Rewrite() {
      public CAstNode newRoot() { return newRoot; }
      public CAstControlFlowMap newCfg() { return theCfg; }
      public CAstSourcePositionMap newPos() { return theSource; }
      public CAstNodeTypeMap newTypes() { return theTypes; }
      public Map<CAstNode, Collection<CAstEntity>> newChildren() { return theChildren; }
    };
    new_entity.setRewrite(rw);

    /* Now we construct a call to the extracted function.
     * 
     * If the body never referenced 'this', the function call will be of the form
     * 
     *   <extracted_fun_expr>("do", <global_object>, <parm1>, ... , <parmn>)
     *   
     * The "do" argument is a dummy argument required by the CAst encoding for JavaScript.
     * 
     * If, on the other hand, there are references to 'this', these will have been rewritten into references
     * to an additional parameter 'thi$', so the call will be of the form
     * 
     *   <extracted_fun_expr>("do", <global_object>, <parm1>, ..., <parmn>, this)
     *   
     * if we are in a function, and
     * 
     *   <extracted_fun_expr>("do", <global_object>, <parm1>, ..., <parmn>, <global_object>)
     *   
     * at the top-level.
     * 
     * In any case, we also add a CFG edge for a ReferenceError exception on the <parmi> arguments, and for
     * a null pointer exception on the call; these are infeasible, but we add them anyway to get the same AST 
     * as with a manual extraction.
     */
    List<CAstNode> args = new ArrayList<CAstNode>();
    CAstNode funExpr = Ast.makeNode(FUNCTION_EXPR, Ast.makeConstant(new_entity));
    args.add(funExpr);
    context.setCallSite(funExpr);
    ExtractionPos outer = ExtractionPos.getEnclosingExtractionPos(context.getParentPos());
    if(outer == null) {
      addEntity(funExpr, new_entity);
    } else {
      outer.addNestedPos(context);
    }

    args.add(Ast.makeConstant("do"));
    args.add(addNode(makeVarRef("__WALA__int3rnal__global"), entity.getControlFlow()));
    for(String parmName : context.getParameters())
      args.add(addExnFlow(makeVarRef(parmName), JavaScriptTypes.ReferenceError, entity, context));
    if(context.containsThis())
      args.add(inFunction() ? Ast.makeNode(VAR, Ast.makeConstant("this")) : Ast.makeConstant(null));
    CAstNode call = Ast.makeNode(CALL, args.toArray(new CAstNode[0])); 
    addExnFlow(call, null, entity, context);

    // if the extracted code contains jumps, we need to insert some fix-up code
    List<CAstNode> stmts = new ArrayList<CAstNode>(prologue);
    if(context.containsJump()) {
      CAstNode decl = Ast.makeNode(ASSIGN,
          addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
          call);

      CAstNode fixup = null;
      if(context.containsGoto())
        fixup = createGotoFixup(context, entity);
      if(context.containsReturn()) {
        if(context.isOutermost()) {
          CAstNode return_fixup = createReturnFixup(context, entity);
          if(fixup != null)
            fixup = Ast.makeNode(BLOCK_EXPR, return_fixup, fixup);
          else
            fixup = return_fixup;
        } else {
          fixup = Ast.makeNode(RETURN, addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context));
        }
      }

      // if this is a nested for-in loop, we need to pass on unhandled jumps
      if(!context.isOutermost() && (context.containsReturn() || context.containsOuterGoto()))
        fixup = Ast.makeNode(RETURN, addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context));

      // if(re$) <check>;
      fixup = Ast.makeNode(IF_STMT, 
          addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
          Ast.makeNode(LOCAL_SCOPE, fixup == null ? Ast.makeNode(EMPTY) : fixup));

      stmts.add(Ast.makeNode(BLOCK_EXPR, decl, fixup));
    } else {
      stmts.add(call);
    }

    if(extractingBlock) {
      // put the call and the fixup code together
      CAstNode newNode = Ast.makeNode(BLOCK_STMT, stmts.toArray(new CAstNode[0]));
      nodeMap.put(Pair.make(root, context.key()), newNode);
      deleteFlow(root, getCurrentEntity());
      stmts = Collections.singletonList(newNode);
    }
    
    if(extractingLocalScope || extractingEmpty) {
      CAstNode newNode = Ast.makeNode(LOCAL_SCOPE, stmts.toArray(new CAstNode[0]));
      stmts = Collections.singletonList(newNode);
    }
    
    return stmts;
  }

  private CAstNode createReturnFixup(ExtractionPos context, CAstEntity entity) {
    return Ast.makeNode(IF_STMT,
        Ast.makeNode(BINARY_EXPR,
            CAstOperator.OP_EQ,
            addExnFlow(Ast.makeNode(OBJECT_REF,
                addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
                Ast.makeConstant("type")), JavaScriptTypes.TypeError, entity, context),
            Ast.makeConstant("return")),
        Ast.makeNode(RETURN,
            addExnFlow(Ast.makeNode(OBJECT_REF,
                addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
                Ast.makeConstant("value")), JavaScriptTypes.TypeError, entity, context)));
  }

  private CAstNode createGotoFixup(ExtractionPos context, CAstEntity entity) {
    CAstNode fixup = null;

    // add fixup code for every goto in the extracted code
    for(Pair<String, CAstNode> goto_target : context.getGotoTargets()) {
      // if(re$.target == <goto_target>) goto <goto_target>; else <fixup>
      CAstNode cond = Ast.makeNode(BINARY_EXPR,
          CAstOperator.OP_EQ,
          addExnFlow(Ast.makeNode(OBJECT_REF,
              addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
              Ast.makeConstant("target")), JavaScriptTypes.TypeError, entity, context),
          Ast.makeConstant((double)labeller.getLabel(goto_target.snd)+""));
      CAstNode then_branch;
      if(goto_target.fst != null)
        then_branch = Ast.makeNode(GOTO, Ast.makeConstant(goto_target.fst));
      else
        then_branch = Ast.makeNode(GOTO);
      addFlow(then_branch, null, goto_target.snd, entity.getControlFlow());
      if(fixup != null)
        fixup = Ast.makeNode(IF_STMT, cond, then_branch, fixup);
      else
        fixup = Ast.makeNode(IF_STMT, cond, then_branch);
    }

    // add check whether re$ is actually a 'goto'
    return Ast.makeNode(IF_STMT,
        Ast.makeNode(BINARY_EXPR,
            CAstOperator.OP_EQ,
            addExnFlow(Ast.makeNode(OBJECT_REF,
                addExnFlow(makeVarRef("re$"), JavaScriptTypes.ReferenceError, entity, context),
                Ast.makeConstant("type")), JavaScriptTypes.TypeError, entity, context),
            Ast.makeConstant("goto")),
        Ast.makeNode(LOCAL_SCOPE, fixup));
  }

  // helper functions for adding exceptional CFG edges
  private CAstNode addExnFlow(CAstNode node, Object label, CAstEntity entity, NodePos pos) {
    return addExnFlow(node, label, entity.getControlFlow(), pos);
  }

  private CAstNode addExnFlow(CAstNode node, Object label, CAstControlFlowMap flow, NodePos pos) {
    CAstNode target = getThrowTarget(pos);
    return addFlow(node, label, target, flow);
  }

  // determine the innermost enclosing throw target at position pos
  private CAstNode getThrowTarget(NodePos pos) {
    return pos.accept(new PosSwitch<CAstNode>() {
      @Override 
      public CAstNode caseRootPos(RootPos pos) { 
        return CAstControlFlowMap.EXCEPTION_TO_EXIT; 
      }

      @Override
      public CAstNode caseChildPos(ChildPos pos) {
        int kind = pos.getParent().getKind();
        if(kind == TRY && pos.getIndex() == 0)
          return pos.getParent().getChild(1);
        if(kind == FUNCTION_EXPR || kind == FUNCTION_STMT)
          return CAstControlFlowMap.EXCEPTION_TO_EXIT;
        return getThrowTarget(pos.getParentPos());
      }

      @Override
      public CAstNode caseForInLoopBodyPos(ExtractionPos pos) {
        return getThrowTarget(pos.getParentPos());
      }
    });
  }

  // helper function for creating VAR nodes
  private CAstNode makeVarRef(String name) {
    return Ast.makeNode(VAR, Ast.makeConstant(name));
  }

  // determine whether we are inside a function
  private boolean inFunction() {
    for(CAstEntity e : getEnclosingEntities())
      if(e.getKind() == CAstEntity.FUNCTION_ENTITY)
        return true;
    return false;
  }
}
