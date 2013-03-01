package com.ibm.wala.cast.ir.translator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
import com.ibm.wala.util.warnings.Warning;

public interface TranslatorToCAst {
  public <C extends RewriteContext<K>, K extends CopyKey<K>> void addRewriter(CAstRewriterFactory<C, K> factory, boolean prepend);

  public class Error extends Exception {
    public final Warning warning;
    
    public Error(Warning message) {
      super(message.getMsg());
      warning = message;
    }
    
  }
  
  public CAstEntity translateToCAst() throws Error, IOException;

  public interface WalkContext<C extends WalkContext<C, T>, T> {

    /**
     * get a mapping from CAstNodes to the scoped entities (e.g. functions or
     * local classes) introduced by those nodes. Also maps <code>null</code> to
     * those entities not corresponding to any node (e.g nested classes)
     */
    Map<CAstNode, Collection<CAstEntity>> getScopedEntities();

    /**
     *   associate a child entity with a given CAstNode, e.g. for a function declaration
     */
    void addScopedEntity(CAstNode newNode, CAstEntity visit);

    /**
     * for recording control-flow relationships among the CAst nodes
     */
    CAstControlFlowRecorder cfg();

    /**
     * for recording source positions
     */
    CAstSourcePositionRecorder pos();

    /**
     * for recording types of nodes
     */
    CAstNodeTypeMapRecorder getNodeTypeMap();

    /**
     * for a 'continue' style goto, return the control flow target
     */
    T getContinueFor(String label);

    /**
     * for a 'break' style goto, return the control flow target
     */
    T getBreakFor(String label);

  }
  
  public class RootContext <C extends WalkContext<C, T>, T> implements WalkContext<C, T> {
    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      assert false;
      return Collections.emptyMap();
    }

    public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
      assert false;
    }

    public CAstControlFlowRecorder cfg() {
      assert false;
      return null;
    }

    public CAstSourcePositionRecorder pos() {
      assert false;
      return null;
    }

    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      assert false;
      return null;
    }

    public T getContinueFor(String label) {
      assert false;
      return null;
    }

    public T getBreakFor(String label) {
      assert false;
      return null;
    }
   
  }
  
  public class DelegatingContext<C extends WalkContext<C, T>, T> implements WalkContext<C, T> {
    protected final C parent;
    
    protected DelegatingContext(C parent) {
      this.parent = parent;
    }
    
    public CAstControlFlowRecorder cfg() {
      return parent.cfg();
    }

    public CAstSourcePositionRecorder pos() {
      return parent.pos();
    }

    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      return parent.getNodeTypeMap();
    }

    public T getContinueFor(String label) {
      return parent.getContinueFor(label);
    }

    public T getBreakFor(String label) {
      return parent.getBreakFor(label);
    }

    public void addScopedEntity(CAstNode newNode, CAstEntity visit) {
      parent.addScopedEntity(newNode, visit);
    }

    public Map<CAstNode, Collection<CAstEntity>> getScopedEntities() {
      return parent.getScopedEntities();
    }
    
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
        CAstNode header = Ast.makeNode(CAstNode.EMPTY);
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
    };

}
