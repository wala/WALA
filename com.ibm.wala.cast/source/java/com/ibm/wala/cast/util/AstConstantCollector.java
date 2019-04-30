package com.ibm.wala.cast.util;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AstConstantCollector {

  public static final CAstPattern simplePreUpdatePattern =
      CAstPattern.parse("ASSIGN_PRE_OP(VAR(<name>CONSTANT()),**)");

  public static final CAstPattern simplePostUpdatePattern =
      CAstPattern.parse("ASSIGN_POST_OP(VAR(<name>CONSTANT()),**)");

  public static final CAstPattern simpleGlobalPattern =
      CAstPattern.parse("GLOBAL_DECL(@(VAR(<name>CONSTANT()))@)");

  public static final CAstPattern simpleValuePattern =
      CAstPattern.parse("ASSIGN(VAR(<name>CONSTANT()),<value>*)");

  public static Map<String, Object> collectConstants(
      CAstEntity function, Map<String, Object> values, Set<String> bad) {
    if (function.getAST() != null) {
      for (Segments s : CAstPattern.findAll(simplePreUpdatePattern, function)) {
        bad.add((String) s.getSingle("name").getValue());
      }
      for (Segments s : CAstPattern.findAll(simpleGlobalPattern, function)) {
        s.getMultiple("name")
            .iterator()
            .forEachRemaining((name) -> bad.add((String) name.getValue()));
      }
      for (Segments s : CAstPattern.findAll(simplePostUpdatePattern, function)) {
        bad.add((String) s.getSingle("name").getValue());
      }
      for (Segments s : CAstPattern.findAll(simpleValuePattern, function)) {
        String var = (String) s.getSingle("name").getValue();
        if (s.getSingle("value").getKind() != CAstNode.CONSTANT) {
          bad.add(var);
        } else {
          Object val = s.getSingle("value").getValue();
          if (!bad.contains(var)) {
            if (values.containsKey(var)) {
              if (val == null ? values.get(var) != null : !val.equals(values.get(var))) {
                values.remove(var);
                bad.add(var);
              }
            } else {
              values.put(var, val);
            }
          }
        }
      }
    }

    for (Collection<CAstEntity> ces : function.getAllScopedEntities().values()) {
      for (CAstEntity ce : ces) {
        collectConstants(ce, values, bad);
      }
    }

    bad.forEach(values::remove);

    for (Collection<CAstEntity> ces : function.getAllScopedEntities().values()) {
      for (CAstEntity ce : ces) {
        for (String s : ce.getArgumentNames()) {
          values.remove(s);
        }
      }
    }

    return values;
  }

  public static Map<String, Object> collectConstants(CAstEntity function) {
    Map<String, Object> values = HashMapFactory.make();
    Set<String> bad = HashSetFactory.make();
    collectConstants(function, values, bad);
    return values;
  }
}
