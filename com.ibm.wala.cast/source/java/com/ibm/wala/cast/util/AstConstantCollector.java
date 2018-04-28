package com.ibm.wala.cast.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

public class AstConstantCollector {

  public static final CAstPattern simplePreUpdatePattern = CAstPattern.parse("ASSIGN_PRE_OP(VAR(<name>CONSTANT()),**)");

  public static final CAstPattern simplePostUpdatePattern = CAstPattern.parse("ASSIGN_POST_OP(VAR(<name>CONSTANT()),**)");

  public static final CAstPattern simpleValuePattern = CAstPattern.parse("ASSIGN(VAR(<name>CONSTANT()),<value>*)");
 
  public static Map<String,Object> collectConstants(CAstEntity function, Map<String,Object> values) {
    if (function.getAST() != null) {
    Set<String> bad = HashSetFactory.make();
    for(Segments s : CAstPattern.findAll(simplePreUpdatePattern, function)) {
      bad.add((String) s.getSingle("name").getValue());
    }
    for(Segments s : CAstPattern.findAll(simplePostUpdatePattern, function)) {
      bad.add((String) s.getSingle("name").getValue());
    }
    for(Segments s : CAstPattern.findAll(simpleValuePattern, function)) {
      String var = (String) s.getSingle("name").getValue();
      if (s.getSingle("value").getKind() != CAstNode.CONSTANT) {
        bad.add(var);
      } else {
        Object val = s.getSingle("value").getValue();
        if (! bad.contains(var)) {
          if (values.containsKey(var)) {
            if (!values.get(var).equals(val)) {
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
    
    for(Collection<CAstEntity> ces : function.getAllScopedEntities().values()) {
      for(CAstEntity ce : ces) {
        collectConstants(ce, values);
      }
    }

    return values;
  }

  public static Map<String,Object> collectConstants(CAstEntity function) {
    Map<String,Object> values = HashMapFactory.make();
    collectConstants(function, values);
    return values;
  }
}
