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
package com.ibm.wala.cast.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;

public class CAstPrinter {
  private static final class StringWriter extends Writer {
    private final StringBuffer sb;

    private StringWriter(StringBuffer sb) {
      this.sb = sb;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
      sb.append(new String(cbuf, off, len));
    }

    @Override
    public void flush() {
      // do nothing 
    }

    @Override
    public void close() {
      // do nothing
    }
  }

  private static CAstPrinter instance= new CAstPrinter();

  public static void setPrinter(CAstPrinter printer) {
      instance= printer;
  }

  public static String kindAsString(int kind) {
      return instance.getKindAsString(kind);
  }

  public String getKindAsString(int kind) {
    switch (kind) {
    // statements
    case CAstNode.SWITCH: return "SWITCH";
    case CAstNode.LOOP: return "LOOP";
    case CAstNode.BLOCK_STMT: return "BLOCK";
    case CAstNode.TRY: return "TRY";
    case CAstNode.EXPR_STMT: return "EXPR_STMT";
    case CAstNode.DECL_STMT: return "DECL_STMT";
    case CAstNode.RETURN: return "RETURN";
    case CAstNode.GOTO: return "GOTO";
    case CAstNode.BREAK: return "BREAK";
    case CAstNode.CONTINUE: return "CONTINUE";
    case CAstNode.IF_STMT: return "IF_STMT";
    case CAstNode.THROW: return "THROW";
    case CAstNode.FUNCTION_STMT: return "FUNCTION_STMT";
    case CAstNode.ASSIGN: return "ASSIGN";
    case CAstNode.ASSIGN_PRE_OP: return "ASSIGN_PRE_OP";
    case CAstNode.ASSIGN_POST_OP: return "ASSIGN_POST_OP";
    case CAstNode.LABEL_STMT: return "LABEL_STMT";
    case CAstNode.IFGOTO: return "IFGOTO";
    case CAstNode.EMPTY: return "EMPTY";
    case CAstNode.YIELD_STMT: return "YIELD";
    case CAstNode.CATCH: return "CATCH";
    case CAstNode.UNWIND: return "UNWIND";
    case CAstNode.MONITOR_ENTER: return "MONITOR_ENTER";
    case CAstNode.MONITOR_EXIT: return "MONITOR_EXIT";
    case CAstNode.ECHO: return "ECHO";
    case CAstNode.FORIN_LOOP: return "FOR..IN";
	
    // expression kinds
    case CAstNode.FUNCTION_EXPR: return "FUNCTION_EXPR";
    case CAstNode.EXPR_LIST: return "EXPR_LIST";
    case CAstNode.CALL: return "CALL";
    case CAstNode.GET_CAUGHT_EXCEPTION: return "EXCEPTION";
    case CAstNode.BLOCK_EXPR: return "BLOCK_EXPR";
    case CAstNode.BINARY_EXPR: return "BINARY_EXPR";
    case CAstNode.UNARY_EXPR: return "UNARY_EXPR";
    case CAstNode.IF_EXPR: return "IF_EXPR";
    case CAstNode.ANDOR_EXPR: return "ANDOR_EXPR";
    case CAstNode.NEW: return "NEW";
    case CAstNode.NEW_ENCLOSING: return "NEW_ENCLOSING";
    case CAstNode.OBJECT_LITERAL: return "OBJECT_LITERAL";
    case CAstNode.VAR: return "VAR";
    case CAstNode.OBJECT_REF: return "OBJECT_REF";
    case CAstNode.CHOICE_EXPR: return "CHOICE_EXPR";
    case CAstNode.CHOICE_CASE: return "CHOICE_CASE";
    case CAstNode.SUPER: return "SUPER";
    case CAstNode.THIS: return "THIS";
    case CAstNode.ARRAY_LITERAL: return "ARRAY_LITERAL";
    case CAstNode.CAST: return "CAST";
    case CAstNode.INSTANCEOF: return "INSTANCEOF";
    case CAstNode.ARRAY_REF: return "ARRAY_REF";
    case CAstNode.ARRAY_LENGTH: return "ARRAY_LENGTH";
    case CAstNode.TYPE_OF: return "TYPE_OF";
    case CAstNode.EACH_ELEMENT_HAS_NEXT: return "EACH_ELEMENT_HAS_NEXT";
    case CAstNode.EACH_ELEMENT_GET: return "EACH_ELEMENT_GET";
    case CAstNode.LIST_EXPR: return "LIST_EXPR";
    case CAstNode.EMPTY_LIST_EXPR: return "EMPTY_LIST_EXPR";
    case CAstNode.IS_DEFINED_EXPR: return "IS_DEFINED_EXPR";
    case CAstNode.NARY_EXPR: return "NARY_EXPR";

    // explicit lexical scopes
    case CAstNode.LOCAL_SCOPE: return "SCOPE";
    case CAstNode.SPECIAL_PARENT_SCOPE: return "SPECIAL PARENT SCOPE";
    
    // literal expression kinds
    case CAstNode.CONSTANT: return "CONSTANT";
    case CAstNode.OPERATOR: return "OPERATOR";
	
    // special stuff
    case CAstNode.PRIMITIVE: return "PRIMITIVE";
    case CAstNode.VOID: return "VOID";
    case CAstNode.ERROR: return "ERROR";
    case CAstNode.ASSERT: return "ASSERT";
    
    default: return "UNKNOWN(" + kind + ")";
    }
  }

  public static String print(CAstNode top) {
      return instance.doPrint(top);
  }

  public String doPrint(CAstNode top) {
    return print(top, null);
  }
  public static String print(CAstNode top, CAstSourcePositionMap pos) {
      return instance.doPrint(top, pos);
  }

  public String doPrint(CAstNode top, CAstSourcePositionMap pos) {
    final StringBuffer sb = new StringBuffer();
    try (final StringWriter writer = new StringWriter(sb)) {
      printTo(top, pos, writer);
    }
    return sb.toString();
  }

  public String doPrint(CAstEntity ce) {
    final StringBuffer sb = new StringBuffer();
    try (final StringWriter writer = new StringWriter(sb)) {
      printTo(ce, writer);
    }
    return sb.toString();
  }

  public static String print(CAstEntity ce) {
      return instance.doPrint(ce);
  }

  public static void printTo(CAstNode top, Writer w) {
      instance.doPrintTo(top, w);
  }

  public void doPrintTo(CAstNode top, Writer w) {
    printTo(top, null, w, 0, false);
  }

  public static void printTo(CAstNode top, CAstSourcePositionMap pos, Writer w) {
      instance.doPrintTo(top, pos, w);
  }

  public void doPrintTo(CAstNode top, CAstSourcePositionMap pos, Writer w) {
    printTo(top, pos, w, 0, false);
  }

  public static void xmlTo(CAstNode top, Writer w) {
      instance.doXmlTo(top, w);
  }

  public void doXmlTo(CAstNode top, Writer w) {
    printTo(top, null, w, 0, true);
  }

  public static void xmlTo(CAstNode top, CAstSourcePositionMap pos, Writer w) {
      doXmlTo(top, pos, w);
  }

  private static void doXmlTo(CAstNode top, CAstSourcePositionMap pos, Writer w) {
    printTo(top, pos, w, 0, true);
  }

  private static String escapeForXML(String x, char from, String to) {
    return (x.indexOf(from) != -1) ? x.replaceAll(Character.toString(from), to) : x;
  }

  public static String escapeForXML(String x) {
    return 
      escapeForXML(
        escapeForXML(
	  escapeForXML(
	    escapeForXML(x, '&', "&amp;"),
	    '"', "&quot;"),
	  '<', "&lt;"),
	'>', "&gt;");
  }

  public static void printTo(CAstNode top, CAstSourcePositionMap pos, Writer w, int depth, boolean uglyBrackets) {
      instance.doPrintTo(top, pos, w, depth, uglyBrackets);
  }

  public void doPrintTo(CAstNode top, CAstSourcePositionMap pos, Writer w, int depth, boolean uglyBrackets) {
    try {
      CAstSourcePositionMap.Position p = (pos!=null)? pos.getPosition( top ): null;
      for(int i = 0; i < depth; i++) w.write("  ");
      if (top == null) {
	  w.write("(null)\n");
      } else if (top.getValue() != null) {
	if (uglyBrackets) {
	  w.write("<constant value=\"");
	  w.write( escapeForXML( top.getValue().toString() ) );
	  w.write("\" type=\"");
	  w.write( top.getValue().getClass().toString() );
	  w.write("\"");
	} else {
	  w.write( "\"" );
	  w.write( top.getValue().toString() );
	  w.write( "\"" );
	}
	if (p != null) {
	  if (uglyBrackets) 
	    w.write(" lineNumber=\"" + p + "\"");
	  else
	    w.write( " at " + p );
	}
	if (uglyBrackets) w.write("/>");
	w.write("\n");
      } else {
	if (uglyBrackets) w.write("<");
	w.write( kindAsString( top.getKind() ) );
	if (p != null)
	  if (uglyBrackets)
	    w.write( " position=\"" + p + "\"");
	  else
	    w.write( " at " + p );
	if (uglyBrackets) w.write(">");
	w.write("\n");
	for(int i = 0; i < top.getChildCount(); i++) {
	  doPrintTo( top.getChild(i), pos, w, depth+1, uglyBrackets );
	}
	if (uglyBrackets) {
	  for(int i = 0; i < depth; i++) w.write("  ");
	  w.write("</" + kindAsString(top.getKind()) + ">\n");
	}
      }
    } catch (java.io.IOException e) {
	
    }
  }

  public static String entityKindAsString(int kind) {
      return instance.getEntityKindAsString(kind);
  }

  public String getEntityKindAsString(int kind) {
    switch (kind) {
	case CAstEntity.FUNCTION_ENTITY: return "function";
	case CAstEntity.FIELD_ENTITY: return "field";
	case CAstEntity.FILE_ENTITY:  return "unit";
	case CAstEntity.TYPE_ENTITY:  return "type";
	case CAstEntity.SCRIPT_ENTITY: return "script";
	case CAstEntity.RULE_ENTITY: return "rule";
	default: return "<unknown entity kind>";
    }
  }

  public static void printTo(CAstEntity e, Writer w) {
    //anca: check if the writer is null
    if(w != null)
      instance.doPrintTo(e, w);
  }

  protected void doPrintTo(CAstEntity e, Writer w) {
      try {
	w.write(getEntityKindAsString(e.getKind()));
	w.write(": ");
	w.write(e.getName());
	w.write('\n');
	if (e.getArgumentNames().length > 0) {
	  w.write("(");
	  String[] names = e.getArgumentNames();
	  for (String name : names) {
	    w.write("  " + name);
	  }
	  w.write("  )\n");
	}
	if (e.getAST() != null) {
	    doPrintTo(e.getAST(), e.getSourceMap(), w);
	    w.write('\n');
	}
	for (Collection<CAstEntity> collection : e.getAllScopedEntities().values()) {
	  for (CAstEntity entity : collection) {
	    doPrintTo(entity, w);
	  }
	}
	w.flush();
    } catch (IOException e1) {
      System.err.println("unexpected I/O exception " + e1);
    }
  }
}
