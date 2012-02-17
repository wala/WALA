package com.ibm.wala.cast.ir.translator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstCloner;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.impl.DelegatingEntity;
import com.ibm.wala.util.collections.Pair;

/**
 * rewrites each exposed formal parameter f (i.e., parameters that may be
 * accessed from nested functions) as follows:
 * 
 * <ol>
 * <li>rename f to f_exposed.</li>
 * <li>add a copy statement
 * 
 * <pre>
 * f = f_exposed
 * </pre>
 * 
 * to the beginning of the corresponding method.</li>
 * </ol>
 * 
 * @see ExposedNamesCollector
 * 
 */
public class ExposedParamRenamer extends CAstCloner {

  private static final boolean VERBOSE = false;

  private final Map<CAstEntity, Set<String>> entity2ExposedNames;

  private List<CAstNode> curDecls;

  protected ExposedParamRenamer(CAst Ast, Map<CAstEntity, Set<String>> entity2ExposedNames) {
    super(Ast, true);
    this.entity2ExposedNames = entity2ExposedNames;
  }

  @Override
  protected CAstNode copyNodes(CAstNode root, NonCopyingContext c, Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    if (root.getKind() == CAstNode.BLOCK_STMT && curDecls != null) {
      List<CAstNode> myDecls = curDecls;
      curDecls = null;
      CAstNode result = super.copyNodes(root, c, nodeMap);
      CAstNode[] newChildren = new CAstNode[result.getChildCount() + 1];
      newChildren[0] = Ast.makeNode(CAstNode.BLOCK_STMT, myDecls.toArray(new CAstNode[myDecls.size()]));
      for (int i = 1; i <= result.getChildCount(); i++) {
        newChildren[i] = result.getChild(i - 1);
      }
      CAstNode newNode = Ast.makeNode(CAstNode.BLOCK_STMT, newChildren);
      return newNode;
    }
    return super.copyNodes(root, c, nodeMap);
  }

  @Override
  public CAstEntity rewrite(CAstEntity root) {
    if (root.getKind() == CAstEntity.FUNCTION_ENTITY && entity2ExposedNames.containsKey(root)) {
      if (VERBOSE)
        System.err.println("function " + root);
      String[] argumentNames = root.getArgumentNames();
      final Set<String> exposedNames = entity2ExposedNames.get(root);
      assert !exposedNames.isEmpty();
      List<CAstNode> decls = new ArrayList<CAstNode>();
      List<String> newArgNames = new ArrayList<String>();
      for (String argName : argumentNames) {
        if (exposedNames.contains(argName)) {
          if (VERBOSE)
            System.err.println("argument " + argName + " exposed");
          final String newArgName = transformExposedArg(argName);
          decls.add(Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new CAstSymbolImpl(argName)),
              Ast.makeNode(CAstNode.VAR, Ast.makeConstant(newArgName))));
          newArgNames.add(newArgName);
        } else {
          newArgNames.add(argName);
        }
      }

      if (!decls.isEmpty()) {
        curDecls = decls;
        CAstEntity result = super.rewrite(root);
        if (VERBOSE)
          System.err.println(result.getAST());
        assert curDecls == null;
        assert argumentNames.length == newArgNames.size();
        final String[] newArgNameArray = newArgNames.toArray(new String[argumentNames.length]);
        return new DelegatingEntity(result) {

          @Override
          public String[] getArgumentNames() {
            return newArgNameArray;
          }

        };
      }

    }
    return super.rewrite(root);
  }

  private String transformExposedArg(String argName) {
    return argName + "__exposed";
  }

}
