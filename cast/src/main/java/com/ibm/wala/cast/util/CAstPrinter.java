/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.util;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public class CAstPrinter {
  private static final class StringWriter extends Writer {
    private final StringBuilder sb;

    private StringWriter(StringBuilder sb) {
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

  private static CAstPrinter instance = new CAstPrinter();

  public static void setPrinter(CAstPrinter printer) {
    instance = printer;
  }

  public static String kindAsString(int kind) {
    return instance.getKindAsString(kind);
  }

  public String getKindAsString(int kind) {
    return switch (kind) {
      // statements
      case CAstNode.SWITCH -> "SWITCH";
      case CAstNode.LOOP -> "LOOP";
      case CAstNode.BLOCK_STMT -> "BLOCK";
      case CAstNode.TRY -> "TRY";
      case CAstNode.EXPR_STMT -> "EXPR_STMT";
      case CAstNode.DECL_STMT -> "DECL_STMT";
      case CAstNode.RETURN -> "RETURN";
      case CAstNode.GOTO -> "GOTO";
      case CAstNode.BREAK -> "BREAK";
      case CAstNode.CONTINUE -> "CONTINUE";
      case CAstNode.IF_STMT -> "IF_STMT";
      case CAstNode.THROW -> "THROW";
      case CAstNode.FUNCTION_STMT -> "FUNCTION_STMT";
      case CAstNode.ASSIGN -> "ASSIGN";
      case CAstNode.ASSIGN_PRE_OP -> "ASSIGN_PRE_OP";
      case CAstNode.ASSIGN_POST_OP -> "ASSIGN_POST_OP";
      case CAstNode.LABEL_STMT -> "LABEL_STMT";
      case CAstNode.IFGOTO -> "IFGOTO";
      case CAstNode.EMPTY -> "EMPTY";
      case CAstNode.YIELD_STMT -> "YIELD";
      case CAstNode.CATCH -> "CATCH";
      case CAstNode.UNWIND -> "UNWIND";
      case CAstNode.MONITOR_ENTER -> "MONITOR_ENTER";
      case CAstNode.MONITOR_EXIT -> "MONITOR_EXIT";
      case CAstNode.ECHO -> "ECHO";
      case CAstNode.FORIN_LOOP -> "FOR..IN";

      // expression kinds
      case CAstNode.FUNCTION_EXPR -> "FUNCTION_EXPR";
      case CAstNode.EXPR_LIST -> "EXPR_LIST";
      case CAstNode.CALL -> "CALL";
      case CAstNode.GET_CAUGHT_EXCEPTION -> "EXCEPTION";
      case CAstNode.BLOCK_EXPR -> "BLOCK_EXPR";
      case CAstNode.BINARY_EXPR -> "BINARY_EXPR";
      case CAstNode.UNARY_EXPR -> "UNARY_EXPR";
      case CAstNode.IF_EXPR -> "IF_EXPR";
      case CAstNode.ANDOR_EXPR -> "ANDOR_EXPR";
      case CAstNode.NEW -> "NEW";
      case CAstNode.NEW_ENCLOSING -> "NEW_ENCLOSING";
      case CAstNode.OBJECT_LITERAL -> "OBJECT_LITERAL";
      case CAstNode.VAR -> "VAR";
      case CAstNode.OBJECT_REF -> "OBJECT_REF";
      case CAstNode.CHOICE_EXPR -> "CHOICE_EXPR";
      case CAstNode.CHOICE_CASE -> "CHOICE_CASE";
      case CAstNode.SUPER -> "SUPER";
      case CAstNode.THIS -> "THIS";
      case CAstNode.ARRAY_LITERAL -> "ARRAY_LITERAL";
      case CAstNode.CAST -> "CAST";
      case CAstNode.INSTANCEOF -> "INSTANCEOF";
      case CAstNode.ARRAY_REF -> "ARRAY_REF";
      case CAstNode.ARRAY_LENGTH -> "ARRAY_LENGTH";
      case CAstNode.TYPE_OF -> "TYPE_OF";
      case CAstNode.EACH_ELEMENT_HAS_NEXT -> "EACH_ELEMENT_HAS_NEXT";
      case CAstNode.EACH_ELEMENT_GET -> "EACH_ELEMENT_GET";
      case CAstNode.LIST_EXPR -> "LIST_EXPR";
      case CAstNode.EMPTY_LIST_EXPR -> "EMPTY_LIST_EXPR";
      case CAstNode.IS_DEFINED_EXPR -> "IS_DEFINED_EXPR";
      case CAstNode.NARY_EXPR -> "NARY_EXPR";
      case CAstNode.TYPE_LITERAL_EXPR -> "TYPE_LITERAL_EXPR";

      // explicit lexical scopes
      case CAstNode.LOCAL_SCOPE -> "SCOPE";
      case CAstNode.SPECIAL_PARENT_SCOPE -> "SPECIAL PARENT SCOPE";

      // literal expression kinds
      case CAstNode.CONSTANT -> "CONSTANT";
      case CAstNode.OPERATOR -> "OPERATOR";

      // special stuff
      case CAstNode.PRIMITIVE -> "PRIMITIVE";
      case CAstNode.VOID -> "VOID";
      case CAstNode.ERROR -> "ERROR";
      case CAstNode.ASSERT -> "ASSERT";
      default -> "UNKNOWN(" + kind + ')';
    };
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
    final StringBuilder sb = new StringBuilder();
    try (final StringWriter writer = new StringWriter(sb)) {
      printTo(top, pos, writer);
    }
    return sb.toString();
  }

  public String doPrint(CAstEntity ce) {
    final StringBuilder sb = new StringBuilder();
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
    return escapeForXML(
        escapeForXML(escapeForXML(escapeForXML(x, '&', "&amp;"), '"', "&quot;"), '<', "&lt;"),
        '>',
        "&gt;");
  }

  public static void printTo(
      CAstNode top, CAstSourcePositionMap pos, Writer w, int depth, boolean uglyBrackets) {
    instance.doPrintTo(top, pos, w, depth, uglyBrackets);
  }

  public void doPrintTo(
      CAstNode top, CAstSourcePositionMap pos, Writer w, int depth, boolean uglyBrackets) {
    try {
      CAstSourcePositionMap.Position p = (pos != null) ? pos.getPosition(top) : null;
      for (int i = 0; i < depth; i++) w.write("  ");
      if (top == null) {
        w.write("(null)\n");
      } else if (top.getValue() != null) {
        if (uglyBrackets) {
          w.write("<constant value=\"");
          w.write(escapeForXML(top.getValue().toString()));
          w.write("\" type=\"");
          w.write(top.getValue().getClass().toString());
          w.write("\"");
        } else {
          w.write("\"");
          w.write(top.getValue().toString());
          w.write("\"");
        }
        if (p != null) {
          if (uglyBrackets) w.write(" lineNumber=\"" + p + '"');
          else w.write(" at " + p);
        }
        if (uglyBrackets) w.write("/>");
        w.write("\n");
      } else {
        if (uglyBrackets) w.write("<");
        w.write(kindAsString(top.getKind()));
        if (p != null)
          if (uglyBrackets) w.write(" position=\"" + p + '"');
          else w.write(" at " + p);
        if (uglyBrackets) w.write(">");
        w.write("\n");
        for (CAstNode child : top.getChildren()) {
          doPrintTo(child, pos, w, depth + 1, uglyBrackets);
        }
        if (uglyBrackets) {
          for (int i = 0; i < depth; i++) w.write("  ");
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
    return switch (kind) {
      case CAstEntity.FUNCTION_ENTITY -> "function";
      case CAstEntity.FIELD_ENTITY -> "field";
      case CAstEntity.FILE_ENTITY -> "unit";
      case CAstEntity.TYPE_ENTITY -> "type";
      case CAstEntity.SCRIPT_ENTITY -> "script";
      case CAstEntity.RULE_ENTITY -> "rule";
      default -> "<unknown entity kind>";
    };
  }

  public static void printTo(CAstEntity e, Writer w) {
    // anca: check if the writer is null
    if (w != null) instance.doPrintTo(e, w);
  }

  protected void doPrintTo(CAstEntity e, Writer w) {
    try {
      w.write(getEntityKindAsString(e.getKind()));
      w.write(": ");
      w.write(e.getName());
      w.write('\n');
      if (e.getArgumentNames().length > 0) {
        int i = 0;
        w.write("(");
        String[] names = e.getArgumentNames();
        CAstType type = e.getType();
        java.util.List<CAstType> types =
            (type instanceof CAstType.Function function) ? function.getArgumentTypes() : null;
        for (String name : names) {
          w.write("  " + name);
          if (types != null) {
            w.write(" ");
            w.write(
                (i == 0 && type instanceof CAstType.Method
                    ? e.getType().toString()
                    : types.get(type instanceof CAstType.Method ? i - 1 : i).toString()));
          }
          i++;
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
