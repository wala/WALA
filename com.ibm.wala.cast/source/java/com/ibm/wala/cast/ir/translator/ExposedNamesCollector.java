package com.ibm.wala.cast.ir.translator;

import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;

public class ExposedNamesCollector extends CAstVisitor {

  /**
   * names declared by each entity
   */
  private final Map<CAstEntity, Set<String>> entity2DeclaredNames = HashMapFactory.make();

  /**
   * exposed names for each entity, updated as child entities are visited
   */
  private final Map<CAstEntity, Set<String>> entity2ExposedNames = HashMapFactory.make();

  private static class EntityContext implements Context {

    private final CAstEntity top;

    EntityContext(CAstEntity top) {
      this.top = top;
    }

    public CAstEntity top() {
      return top;
    }

  }

  /**
   * run the collector on an entity
   * 
   * @param N
   *          the entity
   */
  public void run(CAstEntity N) {
    visitEntities(N, new EntityContext(N), this);
  }

  @Override
  protected Context makeCodeContext(Context context, CAstEntity n) {
    if (n.getKind() == CAstEntity.FUNCTION_ENTITY) {
      // need to handle arguments
      String[] argumentNames = n.getArgumentNames();
      for (String arg : argumentNames) {
//        System.err.println("declaration of " + arg + " in " + n);
        MapUtil.findOrCreateSet(entity2DeclaredNames, n).add(arg);
      }
    }
    return new EntityContext(n);
  }

  @Override
  protected void leaveDeclStmt(CAstNode n, Context c, CAstVisitor visitor) {
    CAstSymbol s = (CAstSymbol) n.getChild(0).getValue();
    String nm = s.name();
//    System.err.println("declaration of " + nm + " in " + c.top());
    MapUtil.findOrCreateSet(entity2DeclaredNames, c.top()).add(nm);
  }

  @Override
  protected void leaveFunctionStmt(CAstNode n, Context c, CAstVisitor visitor) {
    CAstEntity fn = (CAstEntity) n.getChild(0).getValue();
    String nm = fn.getName();
//    System.err.println("declaration of " + nm + " in " + c.top());
    MapUtil.findOrCreateSet(entity2DeclaredNames, c.top()).add(nm);
  }

  private void checkForLexicalAccess(Context c, String nm) {
    CAstEntity entity = c.top();
    final Set<String> entityNames = entity2DeclaredNames.get(entity);
    if (entityNames == null || !entityNames.contains(nm)) {
      CAstEntity declaringEntity = null;
      CAstEntity curEntity = getParent(entity);
      while (curEntity != null) {
        final Set<String> curEntityNames = entity2DeclaredNames.get(curEntity);
        if (curEntityNames != null && curEntityNames.contains(nm)) {
          declaringEntity = curEntity;
          break;
        } else {
          curEntity = getParent(curEntity);
        }
      }
      if (declaringEntity != null) {
//        System.err.println("marking " + nm + " from entity " + declaringEntity + " as exposed");
        MapUtil.findOrCreateSet(entity2ExposedNames, declaringEntity).add(nm);
      }
    }
  }

  @Override
  protected void leaveVar(CAstNode n, Context c, CAstVisitor visitor) {
    String nm = (String) n.getChild(0).getValue();
    checkForLexicalAccess(c, nm);
  }

  @Override
  protected void leaveVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, Context c, CAstVisitor visitor) {
    checkForLexicalAccess(c, (String) n.getChild(0).getValue());
  }

  @Override
  protected void leaveVarAssign(CAstNode n, CAstNode v, CAstNode a, Context c, CAstVisitor visitor) {
    checkForLexicalAccess(c, (String) n.getChild(0).getValue());
  }

  @Override
  protected boolean doVisit(CAstNode n, Context context, CAstVisitor visitor) {
    // assume unknown node types don't do anything relevant to exposed names.  
    // override if this is untrue
    return true;
  }

  
}
