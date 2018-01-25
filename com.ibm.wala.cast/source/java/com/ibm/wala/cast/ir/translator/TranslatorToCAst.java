/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ir.translator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.rewrite.CAstCloner;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter.RewriteContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.warnings.Warning;

public interface TranslatorToCAst {
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory, boolean prepend);

  public class Error extends WalaException {
    private static final long serialVersionUID = -8440950320425119751L;
    public final Set<Warning> warning;
    
    public Error(Set<Warning> message) {
      super(message.iterator().next().getMsg());
      warning = message;
    }
    
  }
  
  public CAstEntity translateToCAst() throws Error, IOException;

  public interface WalkContext<C extends WalkContext<C, T>, T> {

    WalkContext<C, T> getParent();
    
    /**
     * get a mapping from CAstNodes to the scoped entities (e.g. functions or
     * local classes) introduced by those nodes. Also maps <code>null</code> to
     * those entities not corresponding to any node (e.g nested classes)
     */
    default Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return getParent().getScopedEntities();
    }

    default CAstNode getCatchTarget() {
      return getParent().getCatchTarget();
    }

    default CAstNode getCatchTarget(String s) {
      return getParent().getCatchTarget(s);
    }

    default T top() {
      return getParent().top();
    }

    /**
     *   associate a child entity with a given CAstNode, e.g. for a function declaration
     */
    default void addScopedEntity(CAstNode newNode, CAstEntity visit) {
      getParent().addScopedEntity(newNode, visit);
    }

    /**
     * for recording control-flow relationships among the CAst nodes
     */
    default CAstControlFlowRecorder cfg() {
      return getParent().cfg();
    }

    /**
     * for recording source positions
     */
    default CAstSourcePositionRecorder pos()  {
      return getParent().pos();
    }

    /**
     * for recording types of nodes
     */
    default CAstNodeTypeMapRecorder getNodeTypeMap() {
      return getParent().getNodeTypeMap();
    }

    /**
     * for a 'continue' style goto, return the control flow target
     */
    default T getContinueFor(String label) {
      return getParent().getContinueFor(label);
    }
    
    /**
     * for a 'break' style goto, return the control flow target
     */
    default T getBreakFor(String label) {
      return getParent().getBreakFor(label);
    }

  }
  
  public class RootContext <C extends WalkContext<C, T>, T> implements WalkContext<C, T> {
    @Override
    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      assert false;
      return Collections.emptyMap();
    }

    @Override
    public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
      assert false;
    }

    @Override
    public CAstControlFlowRecorder cfg() {
      assert false;
      return null;
    }

    @Override
    public CAstSourcePositionRecorder pos() {
      assert false;
      return null;
    }

    @Override
    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      assert false;
      return null;
    }

    @Override
    public T getContinueFor(String label) {
      assert false;
      return null;
    }

    @Override
    public T getBreakFor(String label) {
      assert false;
      return null;
    }

    @Override
    public T top() {
      assert false;
      return null;
    }

    @Override
    public WalkContext<C, T> getParent() {
      assert false;
      return null;
    }
   
  }
  
  public class DelegatingContext<C extends WalkContext<C, T>, T> implements WalkContext<C, T> {
    protected final C parent;
    
    protected DelegatingContext(C parent) {
      this.parent = parent;
    }
    
    @Override
    public T top() {
      return parent.top();
    }

    @Override
    public WalkContext<C, T> getParent() {
      return parent;
    }
    
  }
  
  class BreakContext<C extends WalkContext<C, T>, T> extends DelegatingContext<C,T> {
    private final T breakTarget;
    protected final String label;

    protected BreakContext(C parent, T breakTarget, String label) {
      super(parent);
      this.breakTarget = breakTarget;
      this.label = label;
    }

    @Override
    public T getBreakFor(String l) {
      return (l == null || l.equals(label))? breakTarget: super.getBreakFor(l);
    }
  }

  public class LoopContext<C extends WalkContext<C, T>, T> extends BreakContext<C,T> {
    private final T continueTo;

    protected LoopContext(C parent, T breakTo, T continueTo, String label) {
      super(parent, breakTo, label);
      this.continueTo = continueTo;
    }

    @Override
    public T getContinueFor(String l) {
      return (l == null || l.equals(label))? continueTo: super.getContinueFor(l);
    }
  }
  
  public static class TryCatchContext<C extends WalkContext<C, T>, T> implements WalkContext<C,T> {
    private final Map<String,CAstNode> catchNode;
    private final WalkContext<C,T> parent;
    
    protected TryCatchContext(C parent, CAstNode catchNode) {
      this(parent, Collections.singletonMap(null, catchNode));
    }

    protected TryCatchContext(C parent, Map<String,CAstNode> catchNode) {
      this.parent = parent;
      this.catchNode = catchNode;
    }

    @Override
    public CAstNode getCatchTarget() { return getCatchTarget(null); }

    @Override
    public CAstNode getCatchTarget(String s) { return catchNode.get(s); }

    @Override
    public WalkContext<C, T> getParent() {
       return parent;
    }
  }
 
  public static class FunctionContext<C extends WalkContext<C, T>, T> extends DelegatingContext<C,T> {
    private final T topNode;
    private final CAstSourcePositionRecorder pos = new CAstSourcePositionRecorder();
    private final CAstControlFlowRecorder cfg = new CAstControlFlowRecorder(pos);
    private final Map<CAstNode, Collection<CAstEntity>> scopedEntities = HashMapFactory.make();

    protected FunctionContext(C parent, T s) {
      super(parent);
      this.topNode = s;
    }

    @Override
    public T top() { return topNode; }

    @Override
    public void addScopedEntity(CAstNode construct, CAstEntity e) {
      if (! scopedEntities.containsKey(construct)) {
        scopedEntities.put(construct, new HashSet<CAstEntity>(1));
      }
      scopedEntities.get(construct).add(e);
    }

    @Override
    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return scopedEntities;
    }

    @Override
    public CAstControlFlowRecorder cfg() { return cfg; }

    @Override
    public CAstSourcePositionRecorder pos() { return pos; }
  }

  public static class DoLoopTranslator {
    private final boolean replicateForDoLoops;
    
    private final CAst Ast;
    
    public DoLoopTranslator(boolean replicateForDoLoops, CAst ast) {
      this.replicateForDoLoops = replicateForDoLoops;
      Ast = ast;
    }

    public CAstNode translateDoLoop(CAstNode loopTest, CAstNode loopBody, CAstNode continueNode, CAstNode breakNode, WalkContext<?,?> wc) {      
      if (replicateForDoLoops) {
        loopBody = Ast.makeNode(CAstNode.BLOCK_STMT, loopBody, continueNode);
        
        CAstRewriter.Rewrite x = (new CAstCloner(Ast, false)).copy(loopBody, wc.cfg(), wc.pos(), wc.getNodeTypeMap(), null);
        CAstNode otherBody = x.newRoot();
        
        wc.cfg().addAll(x.newCfg());
        wc.pos().addAll(x.newPos());
        wc.getNodeTypeMap().addAll(x.newTypes());
 
        return Ast.makeNode(CAstNode.BLOCK_STMT, 
            loopBody,
            Ast.makeNode(CAstNode.LOOP, loopTest, otherBody),
            breakNode);
        
      } else {
        CAstNode header = Ast.makeNode(CAstNode.LABEL_STMT, Ast.makeConstant("_do_label"), Ast.makeNode(CAstNode.EMPTY));
        CAstNode loopGoto = Ast.makeNode(CAstNode.IFGOTO, loopTest);

        wc.cfg().map(header, header);
        wc.cfg().map(loopGoto, loopGoto);
        wc.cfg().add(loopGoto, header, Boolean.TRUE);

        return Ast.makeNode(CAstNode.BLOCK_STMT, 
            header, 
            Ast.makeNode(CAstNode.BLOCK_STMT, loopBody, continueNode), 
            loopGoto, 
            breakNode);
      }
    }
    }

}
