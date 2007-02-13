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
/* -*-java-*-  Prototype file of KM-yacc parser for Java.
 *
 * Written by MORI Koichiro
 *
 * This file is PUBLIC DOMAIN.
 */


/* Here goes %{ ... %} */

package com.ibm.wala.automaton.parser;

import java.io.*;
import java.nio.*;
import sun.io.*;
import java.util.*;
import java.lang.reflect.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.grammar.tree.*;
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.regex.tree.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;


@SuppressWarnings("unused")
public class AmtParser {


  public static final int YYERRTOK = 256;
  public static final int SYMBOL = 257;
  public static final int STRING = 258;
  public static final int VARIABLE = 259;
  public static final int DVARIABLE = 260;
  public static final int AND = 261;
  public static final int OR = 262;
  public static final int EQ = 263;
  public static final int INTEGER = 264;
  public static final int DOUBLE = 265;
  public static final int STATE = 266;
  public static final int ARROW = 267;
  public static final int NEW = 268;
  public static final int NULL = 269;
  public static final int AS = 270;
  public static final int REF = 271;
  public static final int CHARRANGE = 272;
  public static final int CHAR = 273;
  public static final int CHARSETEND = 274;
  public static final int SEARCH = 275;
  public static final int ALIAS = 276;
  public static final int DELETE = 277;
  public static final int TREEGRAMMAR = 278;

  
  /*
    #define yyclearin (yychar = -1)
    #define yyerrok (yyerrflag = 0)
    #define YYRECOVERING (yyerrflag != 0)
    #define YYERROR  goto yyerrlab
  */


  /** Debug mode flag **/
  static boolean yydebug = false;

  /** lexical element object **/
  private Object yylval;

  /** Semantic value */
  private Object yyval;

  /** Semantic stack **/
  private Object yyastk[];

  /** Syntax stack **/
  private short yysstk[];

  /** Stack pointer **/
  private int yysp;

  /** Error handling state **/
  private int yyerrflag;

  /** lookahead token **/
  private int yychar;

  /* code after %% in *.y */
  
  /* Lexical analyzer */
  
  static private ClassLoader CLASSLOADER = AmtParser.class.getClassLoader();
  static private int BUFF_SIZE = 256;
  private Stack ch = new Stack();
  private InputStream inputStream = System.in;
  private Map result = new HashMap();
  private Map alias = new HashMap();
  private List searchPath = new ArrayList(); { searchPath.add(""); };
  private int lineno = 1;
  private CharToByteConverter c2b = CharToByteConverter.getDefault();
  private int inStringPattern = -1;
  private Stack varStack = new Stack();
  private int nextChar = -1;
  private IVariable lastVariable = new Variable("_");
  
  /* 2006 10 16, tabee@jp.ibm.com */
  // They are initialized each time parsing a character-set expression
  private int inCharSetPattern = -1;
  private boolean charSetBegin = true;
  private boolean isComplementCharSet = false;
  
  public AmtParser() {
  }
  
  public AmtParser(String conv) {
      try {
          c2b = CharToByteConverter.getConverter(conv);
      } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          c2b = CharToByteConverter.getDefault();
      }
  }
  
  public List getSearchPath() {
    return searchPath;
  }
  
  public void setAlias(ISymbol s1, ISymbol s2) {
    alias.put(s1, s2);
  }
  
  public void setAlias(String s1, String s2) {
    setAlias(new StringSymbol(s1), new StringSymbol(s2));
  }
  
  public void unsetAlias(ISymbol s1) {
    alias.remove(s1);
  }
  
  public void unsetAlias(String s1) {
    unsetAlias(new StringSymbol(s1));
  }
  
  public Map getResult(){
    return result;
  }
  
  public void setInputStream(InputStream istream){
    inputStream = istream;
  }
  
  public Map parse(InputStream istream){
    setInputStream(istream);
    yyparse();
    return getResult();
  }
  
  public Map parse(String str) {
    char cs[] = str.toCharArray();
    byte bs[];
    try {
      bs = c2b.convertAll(cs);
    } catch (MalformedInputException e) {
      bs = new byte[0];
    }
    ByteArrayInputStream bstream = new ByteArrayInputStream(bs);
    return parse(bstream);
  }
  
  public Object getParsedResult(String str) {
    return parse(str).get(lastVariable);
  }
  
  private int getch() {
    int c = 0;
    if (ch.isEmpty()) {
      try {
        c = inputStream.read();
      } catch (java.io.IOException e) {
        throw new Error(e.getMessage());
      }
    }
    else{
      c = ((Integer)ch.pop()).intValue();
    }
    if( c == '\n' ){
      lineno = lineno + 1;
    }
    return c;
  }
  
  private void ungetch(int c) {
    if( c == '\n' ) {
      lineno = lineno - 1;
    }
    ch.push(new Integer(c));
  }
  
  static private boolean isVariableChar(char c){
    return Character.isJavaIdentifierPart((char)c)
        || (c == '\'') || (c == '~') || (c == '^');
  }
  
  static private boolean isSymbolChar(char c){
    return Character.isJavaIdentifierPart((char)c)
        || (c == '\'') || (c == '~') || (c == '^');
  }
  
  static private boolean isSymbolFirstChar(char c){
    return Character.isJavaIdentifierStart((char)c)
        || (c == '@');
  }
  
  static final char specials[] = new char[]{
      '(', ')', '[', ']', '|', '&', '*', '+', '-', '/', '~', '^', '!', '?',
  };
  static final char concatChars[] = new char[]{
      ')', ']', '*', '+', '?',
  };
  static final Map charMap = new HashMap();
  static {
    charMap.put("\\n", "\n");
    charMap.put("\\r", "\r");
    charMap.put("\\t", "\t");
    charMap.put("\\\\", "\\");
    charMap.put("\\.", ".");
    charMap.put(".", "\\.");
  }
  
  static private boolean isPatternSpecialChar(char c) {
    for (int i = 0; i < specials.length; i++) {
      if (c == specials[i]) return true;
    }
    return false;
  }
  static private boolean isPatternConcatChar(char c) {
    for (int i = 0; i < concatChars.length; i++) {
      if (c == concatChars[i]) return true;
    }
    return false;
  }
  
  static private boolean isPatternSymbol(char c) {
    return !isPatternSpecialChar(c);
  }
  
  /* 2006 10 16, tabee@jp.ibm.com */
  private IPattern charRange(Character[] cs) {
    IPattern pat = null;
    char c1 = cs[0].charValue();
    char c2 = cs[1].charValue();
    for (char c = c1; c <= c2; c++) {
      if (pat == null) {
        pat = new SymbolPattern(new CharSymbol(c));
      } else {
        IPattern p2 = new SymbolPattern(new CharSymbol(c));
        pat = new UnionPattern(pat, p2);
      }
    }
    if (pat == null) pat = new EmptyPattern();
    return pat;
  }
  
  int yylexForCharSetPattern() {
    yylval = null;
    for (;;) {
      int c = getch();
      if (c < 0){
        return 0;
      }
      if ((char)c == '^') {
        if (charSetBegin && !isComplementCharSet) {
          isComplementCharSet = true;
          yylval = Character.toString((char)c);
          return c;
        }
      }
      
      if ((char)c == ']') {
        if (charSetBegin) {
          yylval = new Character(']');
        } else {
          yylval = Character.toString((char)c);
          return CHARSETEND;
        }
      } else if ((char)c == '-') {
        if (charSetBegin) {
          yylval = new Character('-');
        } else {
          int lookahead = getch();
          if ((char)lookahead == ']') {
            ungetch(']');
  	      yylval = new Character('-');
  	      return CHAR;
          }
          yyerror("Unreachable code.");
        }
      } else {
      	yylval = new Character((char)c);
      }
  
      if (charSetBegin) {
      	charSetBegin = false;
      }
      
      /*
       If the next character is '-', it is a character-range expression
       unless it's not the last character inside '[...]'
      */
  	int lookahead = getch();
  	if ((char)lookahead == '-') {
  	  lookahead = getch();
  	  if ((char)lookahead == ']') {
  	    ungetch(']');
  	    ungetch('-');
  	    return CHAR;
  	  }
  	  yylval = new Character[] { (Character)yylval, new Character((char)lookahead) };
  	  return CHARRANGE;
  	} else {
  	  ungetch(lookahead);
  	  return CHAR;
  	}
    }
  }
  
  int yylexForStringPattern() {
    yylval = null;
    for (;;) {
      if (nextChar > 0) {
        int c = nextChar;
        nextChar = -1;
        return c;
      }
      int c = getch();
      if (c < 0){
        return 0;
      }
      else if (isPatternSymbol((char)c)) {
        yylval = Character.toString((char)c);
        if (c == '\\') {
          c = getch();
          if (c > 0) {
            yylval = yylval + Character.toString((char)c);
          }
          if (charMap.containsKey((String)yylval)) {
            yylval = charMap.get((String)yylval);
          }
          else if (isPatternSpecialChar((char)c)) {
            yylval = Character.toString((char)c);
          }
        }
        else {
          if (charMap.containsKey((String)yylval)) {
            yylval = charMap.get((String)yylval);
          }
        }
        nextChar = ',';
        return SYMBOL;
      }
      else {
        yylval = Character.toString((char)c);
        if (isPatternConcatChar((char)c)) {
          nextChar = ',';
        }
        if (charMap.containsKey((String)yylval)) {
          yylval = charMap.get((String)yylval);
          return SYMBOL;
        }
        else {
          return c;
        }
      }
    }
  }
  
  int yylex() {
    if (inCharSetPattern >= 0) {
      return yylexForCharSetPattern();
    }
    if (inStringPattern >= 0) {
      return yylexForStringPattern();
    }
  
    yylval = null;
    for (;;) {
      int c = getch();
      if (c < 0){
        return 0;
      }
      else if (c == '#') {
        while (c != 0) {
          if( c == '\n' ) break;
          c = getch();
        }
      }
      else if (c == ' ' || c == '\t' || c == '\r' || c == '\n' ){
      }
      else{
        if (c == '$') {
          int n = 0;
          boolean dvar = false;
          char[] buf = new char[BUFF_SIZE];
          buf[n++] = (char)c;
          c = getch();
          if (c == '$') {
            dvar = true;
            c = getch();
          }
          while (isVariableChar((char)c)) {
            buf[n++] = (char)c;
            c = getch();
          }
          ungetch(c);
          yylval = new String(buf, 1, n-1);
          return (dvar ? DVARIABLE : VARIABLE);
        }
        if (c == '<') {
          int n = 0;
          char[] buf = new char[BUFF_SIZE];
          buf[n++] = (char)c;
          c = getch();
          while (c != '>') {
            buf[n++] = (char)c;
            c = getch();
          }
          buf[n++] = (char)c;
          yylval = new String(buf, 1, n-2);
          return STATE;
        }
        if (c == '"' || c == '\'')  {
          int n = 0;
          char[] buf = new char[BUFF_SIZE];
          buf[n++] = (char)c;
          int c2 = getch();
          while (c2 != c) {
            buf[n++] = (char)c2;
            c2 = getch();
            if( c2 == '\\' ){
              c2 = getch();
              continue;
            }
          }
          buf[n++] = (char)c2;
          yylval = new String(buf, 1, n-2);
          if (c == '"') {
            return STRING;
          }
          else{
            return SYMBOL;
          }
        }
        if (c == '-') {
          int c2 = getch();
          if (c2 == '>') {
            yylval = "->";
            return ARROW;
          }
          else{
            ungetch(c2);
          }
        }
        if (Character.isDigit((char)c) || c == '-') {
          int n = 0;
          char[] buf = new char[BUFF_SIZE];
          buf[n++] = (char)c;
          c = getch();
          while (Character.isDigit((char)c) || c == '.') {
            buf[n++] = (char)c;
            c = getch();
          }
          ungetch(c);
          String numStr = new String(buf, 0, n);
          if( numStr.indexOf(".") >= 0 ){
            yylval = new Double(numStr);
            return DOUBLE;
          }
          else{
            yylval = new Long(numStr);
            return INTEGER;
          }
        }
        if (c == '&'){
          c = getch();
          if (c == '&') {
            return AND;
          }
          else{
            ungetch(c);
            yylval = Character.toString('&');
            return '&';
          }
        }
        if (c == '|'){
          c = getch();
          if (c == '|') {
            return OR;
          }
          else{
            ungetch(c);
            yylval = Character.toString('|');
            return '|';
          }
        }
        if (c == '='){
          c = getch();
          if (c == '=') {
            return EQ;
          }
          else{
            ungetch(c);
            yylval = Character.toString('=');
            return '=';
          }
        }
        if (isSymbolFirstChar((char)c)) {
          int n = 0;
          char[] buf = new char[BUFF_SIZE];
          buf[n++] = (char)c;
          c = getch();
          while (isSymbolChar((char)c)) {
            buf[n++] = (char)c;
            c = getch();
          }
          ungetch(c);
          yylval = new String(buf, 0, n);
          if (yylval.equals("new")) {
            return NEW;
          }
          else if (yylval.equals("null")) {
            return NULL;
          }
          else if (yylval.equals("as")) {
            return AS;
          }
          else if (yylval.equals("ref")) {
            return REF;
          }
          else if (yylval.equals("search")) {
            return SEARCH;
          }
          else if (yylval.equals("alias")) {
            return ALIAS;
          }
          else if (yylval.equals("delete")) {
            return DELETE;
          }
          else if (yylval.equals("TreeGrammar")) {
            return TREEGRAMMAR;
          }
          else {
            return SYMBOL;
          }
        }
        else{
          yylval = Character.toString((char)c);
          return c;
        }
      }
    }
  }
  
  
  void yyerror(String msg) {
    Object t = (yylval == null) ? "" : yylval;
    System.err.println("line " + lineno + "(before '" + t + "'): " + msg);
  }



  private static final byte yytranslate[] = {
      0,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   34,   26,   41,
     35,   36,   23,   21,   27,   22,   39,   24,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   40,   37,
     41,   20,   41,   29,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   32,   41,   33,   38,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   30,   25,   31,   28,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,   41,   41,   41,   41,
     41,   41,   41,   41,   41,   41,    1,    2,    3,    4,
      5,   41,   41,   41,    6,    7,    8,    9,   41,   10,
     11,   12,   13,   14,   15,   16,   17,   18,   19
  };
  private static final int YYBADCH = 41;
  private static final int YYMAXLEX = 279;
  private static final int YYTERMS = 41;
  private static final int YYNONTERMS = 41;

  private static final short yyaction[] = {
     72,   48,  214,  129,  130,  226,   72,-32766,    0,  129,
    130,  226,   68,   45,   59,   38,   56,   25,   71,  -99,
    219,  220,   56,   47,   19,  189,   48,    3,  169,    4,
    168,   73,    6,    3,   29,    4,  170,   73,    6,  227,
    109,   67,   25,   67,-32767,   71,-32767,   24,  162,   19,
    210,  110,-32767,   18,  111,   30,  191,   30,  190,  219,
    220,   26,   31,  138,   27,   28,  198,   45,   59,   38,
  -32767,  -42,-32767,   39,   40,   46,   61,   27,   28,  219,
    162,  220,  184,  182,    8,    7,   58,  143,    2,   35,
     10,   66, -101,   47,    0,    0,    5,    0,    0,    0,
      0,   23,   52,   54,    0,  126,    0,    0,  199,  125,
      0,   36,   16,   14,    0,  148,  127,  156,  157,    0,
      0,    1,    0,  144,  196,  176,  139,    0,  -46,   17,
     15,  -47,  -48,  227,    0,   49,    0,    0,   57
  };
  private static final int YYLAST = 139;

  private static final byte yycheck[] = {
      3,   12,    3,    6,    7,    8,    3,    5,    0,    6,
      7,    8,   10,   16,   17,   18,   19,   28,    2,   35,
      4,    5,   19,   39,   35,   36,   12,   30,   21,   32,
     23,   34,   35,   30,   27,   32,   29,   34,   35,   37,
     24,   11,   28,   11,   21,    2,   23,   28,   10,   35,
      2,   32,   29,    9,   35,   25,   21,   25,   23,    4,
      5,   22,   27,   31,   25,   26,   36,   16,   17,   18,
     21,   33,   23,   13,   14,   16,   17,   25,   26,    4,
     10,    5,   15,   15,   20,   20,   20,   24,   27,   27,
     32,   35,   35,   39,   -1,   -1,   27,   -1,   -1,   -1,
     -1,   30,   30,   30,   -1,   31,   -1,   -1,   31,   31,
     -1,   32,   32,   32,   -1,   33,   33,   33,   33,   -1,
     -1,   35,   -1,   36,   36,   36,   36,   -1,   37,   37,
     37,   37,   37,   37,   -1,   38,   -1,   -1,   40
  };

  private static final short yybase[] = {
     -3,    3,    3,    3,    3,    3,    3,    3,    3,   39,
     38,   32,   30,   52,   70,   70,   70,   70,    2,  -11,
     19,   19,   51,   14,   19,   14,   19,   19,   19,   19,
     14,   14,    7,    7,    7,   55,   55,   97,   59,   68,
     67,  -16,   23,   35,   35,   -1,   -1,   -1,   56,   60,
     91,   49,   76,   76,   48,   48,   76,   43,   43,   43,
     96,   43,   96,   96,   96,   96,   75,   75,   96,   96,
     96,   98,   57,   71,   73,   65,   86,   81,    8,   54,
     66,   72,   69,   74,   83,   90,   65,   78,   64,   94,
     95,   62,   82,   93,   81,   61,   87,   80,   92,   44,
     77,   63,   89,   88,   79,   84,   85,   80,   81,    0,
      0,    0,    0,   16,   16,   16,   16,   16,   16,   16,
     16,   16,    7,   16,   35,   35,    7,   16,   16,   16,
     16,   16,   43,   43,   43,   75,   43,   43,   43,   43,
     43,   43,   43,   43,   43,    0,    0,    0,   43,   43,
     60,   75,   60,   60,   58,    0,    0,    0,   43,   43,
     43,   75,    0,   58
  };
  private static final int YY2TBLSTATE = 51;

  private static final short yydefault[] = {
      4,  104,  104,   34,   34,   34,32767,32767,32767,   73,
     51,32767,32767,   60,   51,   51,   51,   51,  115,32767,
     74,   74,    4,32767,32767,32767,32767,32767,32767,   54,
  32767,32767,   58,   59,   53,   42,   42,   68,32767,   68,
     68,   18,   61,   80,   79,32767,32767,32767,32767,   68,
     37,   84,   89,   89,   95,   95,32767,32767,32767,32767,
    115,32767,  115,  115,  115,  115,32767,32767,  115,  115,
    115,  111,   15,32767,   11,   20,32767,   24,32767,   99,
  32767,32767,   33,32767,32767,32767,32767,32767,32767,   38,
     39,   41,32767,32767,   50,  103,32767,   46,32767,32767,
  32767,32767,32767,32767,   37,32767,32767,32767,32767,   28,
     64,   62,   29
  };

  private static final short yygoto[] = {
     50,  215,  183,  185,   97,   97,   97,   97,  107,  188,
    165,  165,  179,  188,  165,  188,  165,  165,  165,  165,
    188,  188,  153,   94,   75,  104,  104,   94,   94,   94,
     94,  108,   84,  145,   89,   79,   79,   79,  160,  160,
    160,  160,   69,   60,   62,  213,   86,  225,   65,  116,
     90,   80,  102,   63,  161,  161,  161,  161,  201,  151,
    151,  120,  119,  121,  123,  118,  122,  207,    0,  205,
    204,  203,  195,   81,    0,  152,  152,  115,  218,  218,
     11,    0,   51,   85,   64,  209,  149,   43,   44,    0,
    103,  194,  200,  200,  158,    0,  200,    0,  158,  158,
    158,  158,   70,   42,    0,   13,   32,   33,   34,    0,
      0,  149,  149,   98,  105,    0,  106
  };
  private static final int YYGLAST = 117;

  private static final byte yygcheck[] = {
      7,   11,   29,   29,    7,    7,    7,    7,    7,    7,
      7,    7,   29,    7,    7,    7,    7,    7,    7,    7,
      7,    7,   23,   31,    4,    7,    7,   31,   31,   31,
     31,   31,   10,   10,    4,    7,    7,    7,    4,    4,
      4,    4,    4,   17,   17,   17,    4,    7,    7,   33,
      5,    8,   27,    8,    5,    5,    5,    5,   21,    4,
      4,   35,    4,   35,   35,   35,   35,   14,   -1,   35,
     35,   35,    4,    6,   -1,    5,    5,    1,    1,    1,
     28,   -1,   28,    1,    1,    1,   18,   28,   28,   -1,
      4,    4,    5,    5,   24,   -1,    5,   -1,   24,   24,
     24,   24,   24,   26,   -1,   26,   26,   26,   26,   -1,
     -1,   18,   18,   25,   25,   -1,   25
  };

  private static final short yygbase[] = {
      0,   77,    0,    0,   24,   40,   17,  -10,   -8,    0,
     28,   -1,    0,    0,   12,    0,    0,   -2,   76,    0,
      0,    5,    0,  -13,   84,   99,   79,   31,   57,  -37,
      0,   13,    0,   27,    0,    1,    0,    0,    0,    0,
      0
  };

  private static final short yygdefault[] = {
  -32768,   82,   74,   95,  133,  134,   99,   41,  223,  132,
     83,   96,-32768,   55,   87,   88,   76,  211,  135,   91,
    140,  100,   53,   92,  136,   93,    9,  112,   12,  180,
    178,   77,   78,  114,   22,  206,   20,  101,   21,   37,
  -32768
  };

  private static final byte yylhs[] = {
      0,   32,   32,   33,   33,   34,   34,   34,   34,   34,
     34,    1,    1,    1,    1,    1,    1,    1,    1,    1,
      1,    1,    1,    1,    1,    1,    1,    1,   36,   37,
     31,    2,   10,   10,   10,   18,   19,   19,   19,   19,
     23,   23,   23,   24,   24,   25,   25,   25,   25,   25,
     25,   25,   26,   26,   26,   26,   26,   26,   26,   26,
     26,   26,   38,   26,   39,   26,   30,   30,   29,   29,
     29,   29,   29,   27,   27,   28,   28,   28,   28,   28,
     28,   28,   28,   28,   28,   28,   20,    6,   21,   21,
     22,   22,   22,   22,   14,   14,   13,   15,   16,   17,
     17,   17,   11,   11,   11,    3,    4,    5,   12,   12,
      7,    8,    8,    9,   35,   35,   40
  };

  private static final byte yylen[] = {
      1,    1,    1,    2,    0,    4,    2,    3,    4,    5,
      4,    1,    4,    3,    3,    1,    1,    1,    1,    1,
      1,    1,    1,    1,    1,    4,    3,    1,    0,    0,
      5,    4,    3,    1,    0,    4,    1,    1,    1,    1,
      3,    1,    0,    6,    6,    1,    1,    1,    1,    1,
      1,    0,    1,    3,    2,    2,    2,    2,    3,    3,
      3,    2,    0,    4,    0,    3,    2,    1,    0,    2,
      2,    2,    2,    1,    0,    1,    2,    2,    2,    3,
      3,    3,    2,    4,    2,    3,    5,    1,    2,    0,
      4,    4,    4,    3,    2,    0,    3,    1,    1,    1,
      3,    1,    3,    1,    0,    1,    1,    1,    3,    1,
      1,    1,    3,    1,    1,    0,    1
  };
  private static final int YYSTATES = 190;
  private static final int YYNLSTATES = 113;
  private static final int YYINTERRTOK = 1;
  private static final int YYUNEXPECTED = 32767;

  private static final int YYDEFAULT = -32766;

  private static final int YYDEFAULTSTACK = 512;

  /* Grow syntax and sematic stacks */
  private void growStack() {
    short[] tmpsstk = new short[yysp * 2];
    Object[] tmpastk = new Object[yysp * 2];
    for (int i = 0; i < yysp; i++) {
      tmpsstk[i] = yysstk[i];
      tmpastk[i] = yyastk[i];
    }
    yysstk = tmpsstk;
    yyastk = tmpastk;
  }

  /*
   * Parser entry point
   */
  public int yyparse() {
    int yyn;
    int yyp;
    int yyl;

    yyastk = new Object[YYDEFAULTSTACK];
    yysstk = new short[YYDEFAULTSTACK];

    int yystate = 0;
    int yychar1 = yychar = -1;

    yysp = 0;
    yysstk[yysp] = 0;
    yyerrflag = 0;
    for (;;) {
      if (yybase[yystate] == 0)
        yyn = yydefault[yystate];
      else {
        if (yychar < 0) {
          if ((yychar = yylex()) <= 0) yychar = 0;
          yychar1 = yychar < YYMAXLEX ? yytranslate[yychar] : YYBADCH;
        }

        if ((yyn = yybase[yystate] + yychar1) >= 0
            && yyn < YYLAST && yycheck[yyn] == yychar1
            || (yystate < YY2TBLSTATE
                && (yyn = yybase[yystate + YYNLSTATES] + yychar1) >= 0
                && yyn < YYLAST && yycheck[yyn] == yychar1)) {
          yyn = yyaction[yyn];
          /*
           * >= YYNLSTATE: shift and reduce
           * > 0: shift
           * = 0: accept
           * < 0: reduce
           * = -YYUNEXPECTED: error
           */
          if (yyn > 0) {
            /* shift */
            if (++yysp >= yysstk.length)
              growStack();

            yysstk[yysp] = (short)(yystate = yyn);
            yyastk[yysp] = yylval;
            yychar1 = yychar = -1;

            if (yyerrflag > 0)
              yyerrflag--;
            if (yyn < YYNLSTATES)
              continue;
            
            /* yyn >= YYNLSTATES means shift-and-reduce */
            yyn -= YYNLSTATES;
          } else
            yyn = -yyn;
        } else
          yyn = yydefault[yystate];
      }
      
      for (;;) {
        /* reduce/error */
        if (yyn == 0) {
          /* accept */
          return 0;
        }

	boolean yyparseerror = true;
	if (yyn != YYUNEXPECTED) {
          /* reduce */
	  yyparseerror = false;
          yyl = yylen[yyn];
          yyval = yyastk[yysp-yyl+1];
	  int yylrec = 0;
          /* Following line will be replaced by reduce actions */
          switch(yyn) {
          case 2:
{ result.put(lastVariable, ((Object)yyastk[yysp-(1-1)])); } break;
          case 5:
{ result.put(((IVariable)yyastk[yysp-(4-1)]), ((Object)yyastk[yysp-(4-3)])); } break;
          case 6:
{ result.remove(((IVariable)yyastk[yysp-(2-2)])); } break;
          case 7:
{ searchPath.add(((String)yyastk[yysp-(3-2)])); } break;
          case 8:
{ searchPath.remove(((String)yyastk[yysp-(4-3)])); } break;
          case 9:
{ alias.put(((ISymbol)yyastk[yysp-(5-2)]),((ISymbol)yyastk[yysp-(5-4)])); } break;
          case 10:
{ alias.remove(((ISymbol)yyastk[yysp-(4-3)])); } break;
          case 11:
{ yyval = ((Object)yyastk[yysp-(1-1)]); } break;
          case 12:
{
        Object instance = ((Object)yyastk[yysp-(4-1)]);
        Map propDefs = ((Map)yyastk[yysp-(4-3)]);
        for (Iterator i = propDefs.keySet().iterator(); i.hasNext(); ) {
            String propName = (String) i.next();
            Object propValue = propDefs.get(propName);
            try {
                String setterName = "set" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
                instance.getClass().getMethod(setterName, new Class[]{propValue.getClass()}).invoke(instance, new Object[]{propValue});
            } catch (NoSuchMethodException e) {
                try {
                    instance.getClass().getField(propName).set(instance, propValue);
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (NoSuchFieldException e1) {
                    e1.printStackTrace();
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        yyval = instance;
  } break;
          case 13:
{ yyval = new HashSet(((List)yyastk[yysp-(3-2)])); } break;
          case 14:
{ yyval = new ArrayList(((List)yyastk[yysp-(3-2)])); } break;
          case 15:
{ yyval = ((String)yyastk[yysp-(1-1)]); } break;
          case 16:
{ yyval = ((Long)yyastk[yysp-(1-1)]); } break;
          case 17:
{ yyval = ((Double)yyastk[yysp-(1-1)]); } break;
          case 18:
{ yyval = ((ISymbol)yyastk[yysp-(1-1)]); } break;
          case 19:
{ yyval = ((IState)yyastk[yysp-(1-1)]); } break;
          case 20:
{ yyval = result.get(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 21:
{ yyval = ((IVariable)yyastk[yysp-(1-1)]); } break;
          case 22:
{ yyval = ((ITree)yyastk[yysp-(1-1)]); } break;
          case 23:
{ yyval = ((IBinaryTree)yyastk[yysp-(1-1)]); } break;
          case 24:
{ yyval = ((IPattern)yyastk[yysp-(1-1)]); } break;
          case 25:
{ yyval = ((IPattern)yyastk[yysp-(4-3)]); } break;
          case 26:
{ yyval = ((Object)yyastk[yysp-(3-2)]); } break;
          case 27:
{ yyval = ((ITreeGrammar)yyastk[yysp-(1-1)]); } break;
          case 28:
{ inStringPattern = 0; } break;
          case 29:
{ inStringPattern = -1; } break;
          case 30:
{ yyval = ((IPattern)yyastk[yysp-(5-3)]); } break;
          case 31:
{
    String cname = ((String)yyastk[yysp-(4-1)]);
    List params = ((List)yyastk[yysp-(4-3)]);
    Class klasses[] = new Class[params.size()];
    Object objs[] = new Object[params.size()];
    for (ListIterator i = params.listIterator(); i.hasNext(); ) {
        int index = i.nextIndex();
        Object val = i.next();
        klasses[index] = val.getClass();
        objs[index] = val;
    }
    Object instance = null;
    for (Iterator i = searchPath.iterator(); i.hasNext(); ) {
      String path = (String) i.next();
      try {
          String fullcname = null;
          if (path == null || path.length() == 0) {
            fullcname = cname;
          }
          else if (path.charAt(path.length()-1) == '.') {
            fullcname = path + cname;
          }
          else {
            fullcname = path + "." + cname;
          }
          Class klass = CLASSLOADER.loadClass(fullcname);
          Constructor constructors[] = klass.getConstructors();
          for (int j = 0; j < constructors.length; j++) {
              try {
                  instance = constructors[j].newInstance(objs);
                  break;
              }
              catch(Exception e) {
                  if (j == constructors.length-1) {
                      throw(new RuntimeException(e));
                  }
              }
          }
          break;
      } catch (Exception e) {
          if(!i.hasNext()) {
              throw(new RuntimeException(e));
          }
      }
    }
    yyval = instance;
  } break;
          case 32:
{ List l = ((List)yyastk[yysp-(3-3)]); l.add(0, ((Object)yyastk[yysp-(3-1)])); yyval = l; } break;
          case 33:
{ List l = new ArrayList(); l.add(0, ((Object)yyastk[yysp-(1-1)])); yyval = l; } break;
          case 34:
{ yyval = new ArrayList(); } break;
          case 35:
{ Tree t = new Tree(((ISymbol)yyastk[yysp-(4-1)]), ((List)yyastk[yysp-(4-3)])); yyval = t; } break;
          case 36:
{ yyval = ((ITree)yyastk[yysp-(1-1)]); } break;
          case 37:
{ Tree t = new Tree(((ISymbol)yyastk[yysp-(1-1)])); yyval = t; } break;
          case 38:
{ yyval = result.get(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 39:
{ yyval = new TreeVariable(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 40:
{ List l = ((List)yyastk[yysp-(3-3)]); l.add(0, ((ITree)yyastk[yysp-(3-1)])); yyval = l; } break;
          case 41:
{ List l = new ArrayList(); l.add(((ITree)yyastk[yysp-(1-1)])); yyval = l; } break;
          case 42:
{ List l = new ArrayList(); yyval = l; } break;
          case 43:
{ BinaryTree t = new BinaryTree(((ISymbol)yyastk[yysp-(6-1)]), ((IBinaryTree)yyastk[yysp-(6-3)]), ((IBinaryTree)yyastk[yysp-(6-5)])); yyval = t; } break;
          case 44:
{ BinaryTree t = new BinaryTree(new StringPatternSymbol(((IPattern)yyastk[yysp-(6-1)])), ((IBinaryTree)yyastk[yysp-(6-3)]), ((IBinaryTree)yyastk[yysp-(6-5)])); yyval = t; } break;
          case 45:
{ yyval = ((IBinaryTree)yyastk[yysp-(1-1)]); } break;
          case 46:
{ BinaryTree t = new BinaryTree(((ISymbol)yyastk[yysp-(1-1)])); yyval = t; } break;
          case 47:
{ yyval = result.get(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 48:
{ yyval = new BinaryTreeVariable(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 49:
{ yyval = null; } break;
          case 50:
{ BinaryTree t = new BinaryTree(new StringPatternSymbol(((IPattern)yyastk[yysp-(1-1)]))); yyval = t; } break;
          case 51:
{ yyval = null; } break;
          case 52:
{ yyval = new SymbolPattern(((ISymbol)yyastk[yysp-(1-1)])); } break;
          case 53:
{ yyval = new ConcatenationPattern(((IPattern)yyastk[yysp-(3-1)]), ((IPattern)yyastk[yysp-(3-3)])); } break;
          case 54:
{ yyval = ((IPattern)yyastk[yysp-(2-1)]); } break;
          case 55:
{ yyval = new IterationPattern(((IPattern)yyastk[yysp-(2-1)]), true); } break;
          case 56:
{ yyval = new IterationPattern(((IPattern)yyastk[yysp-(2-1)]), false); } break;
          case 57:
{ yyval = new UnionPattern(((IPattern)yyastk[yysp-(2-1)]), new EmptyPattern()); } break;
          case 58:
{ yyval = new UnionPattern(((IPattern)yyastk[yysp-(3-1)]),((IPattern)yyastk[yysp-(3-3)])); } break;
          case 59:
{ yyval = new IntersectionPattern(((IPattern)yyastk[yysp-(3-1)]),((IPattern)yyastk[yysp-(3-3)])); } break;
          case 60:
{ yyval = new IntersectionPattern(((IPattern)yyastk[yysp-(3-1)]), new ComplementPattern(((IPattern)yyastk[yysp-(3-3)]))); } break;
          case 61:
{ yyval = new ComplementPattern(((IPattern)yyastk[yysp-(2-2)])); } break;
          case 62:
{ inStringPattern++; varStack.push(new Variable(Integer.toString(inStringPattern))); } break;
          case 63:
{ yyval = new VariableBindingPattern(((IPattern)yyastk[yysp-(4-3)]), (IVariable)varStack.pop()); } break;
          case 64:
{ inCharSetPattern++; charSetBegin = true; isComplementCharSet = false; } break;
          case 65:
{
		inCharSetPattern--;
		yyval = ((IPattern)yyastk[yysp-(3-3)]);
	} break;
          case 66:
{	yyval = new IntersectionPattern(new SymbolPattern(new CharPatternSymbol("\\.")), new ComplementPattern(((IPattern)yyastk[yysp-(2-2)]))); } break;
          case 67:
{ yyval = ((IPattern)yyastk[yysp-(1-1)]); } break;
          case 69:
{
		yyval = charRange((Character[])((Character[])yyastk[yysp-(2-1)]));
	} break;
          case 70:
{
		IPattern p1 = charRange((Character[])((Character[])yyastk[yysp-(2-1)]));
		yyval = new UnionPattern(p1, ((IPattern)yyastk[yysp-(2-2)]));
	} break;
          case 71:
{ Character c = (Character)((Character)yyastk[yysp-(2-1)]); yyval = new SymbolPattern(new CharSymbol(c.charValue())); } break;
          case 72:
{
		Character c = (Character)((Character)yyastk[yysp-(2-1)]); 
		IPattern p1 = new SymbolPattern(new CharSymbol(c.charValue()));	
		yyval= new UnionPattern(p1, ((IPattern)yyastk[yysp-(2-2)]));
	} break;
          case 73:
{ yyval = ((IPattern)yyastk[yysp-(1-1)]); } break;
          case 74:
{ yyval = new EmptyPattern(); } break;
          case 75:
{ yyval = new SymbolPattern(((ISymbol)yyastk[yysp-(1-1)])); } break;
          case 76:
{ yyval = new EmptyPattern(); } break;
          case 77:
{ yyval = new IterationPattern(((IPattern)yyastk[yysp-(2-1)]), true); } break;
          case 78:
{ yyval = new IterationPattern(((IPattern)yyastk[yysp-(2-1)]), false); } break;
          case 79:
{ yyval = new ConcatenationPattern(((IPattern)yyastk[yysp-(3-1)]), ((IPattern)yyastk[yysp-(3-3)])); } break;
          case 80:
{ yyval = new UnionPattern(((IPattern)yyastk[yysp-(3-1)]),((IPattern)yyastk[yysp-(3-3)])); } break;
          case 81:
{ yyval = new VariableBindingPattern(((IPattern)yyastk[yysp-(3-1)]),((IVariable)yyastk[yysp-(3-3)])); } break;
          case 82:
{ yyval = new VariableReferencePattern(((IVariable)yyastk[yysp-(2-2)])); } break;
          case 83:
{ yyval = new VariableReferencePattern(((IVariable)yyastk[yysp-(4-3)])); } break;
          case 84:
{ yyval = new ComplementPattern(((IPattern)yyastk[yysp-(2-2)])); } break;
          case 85:
{ yyval = ((IPattern)yyastk[yysp-(3-2)]); } break;
          case 86:
{ yyval = new TreeGrammar(((IBinaryTreeVariable)yyastk[yysp-(5-2)]), ((Set)yyastk[yysp-(5-4)])); } break;
          case 87:
{ yyval = new BinaryTreeVariable(((IVariable)yyastk[yysp-(1-1)])); } break;
          case 88:
{ Set rules = ((Set)yyastk[yysp-(2-2)]); rules.add(((IProductionRule)yyastk[yysp-(2-1)])); yyval = rules; } break;
          case 89:
{ yyval = new HashSet(); } break;
          case 90:
{ yyval = new ProductionRule(((IBinaryTreeVariable)yyastk[yysp-(4-1)]), ((IBinaryTree)yyastk[yysp-(4-3)])); } break;
          case 91:
{ yyval = new ProductionRule(((IBinaryTreeVariable)yyastk[yysp-(4-1)]), (IBinaryTree)result.get(((IVariable)yyastk[yysp-(4-3)]))); } break;
          case 92:
{ yyval = new ProductionRule(((IBinaryTreeVariable)yyastk[yysp-(4-1)]), BinaryTree.LEAF); } break;
          case 93:
{ yyval = new ProductionRule(((IBinaryTreeVariable)yyastk[yysp-(3-1)]), BinaryTree.LEAF); } break;
          case 94:
{
    Map m1 = ((Map)yyastk[yysp-(2-1)]);
    Map m2 = ((Map)yyastk[yysp-(2-2)]);
    for (Iterator i = m1.keySet().iterator(); i.hasNext(); ) {
        Object key = i.next();
        Object val = m1.get(key);
        m2.put(key, val);
    }
    yyval = m2;
  } break;
          case 95:
{ yyval = new HashMap(); } break;
          case 96:
{
    Map m = new HashMap();
    m.put(((String)yyastk[yysp-(3-1)]), ((Object)yyastk[yysp-(3-3)]));
    yyval = m;
  } break;
          case 97:
{ yyval = ((String)yyastk[yysp-(1-1)]); } break;
          case 98:
{ yyval = ((String)yyastk[yysp-(1-1)]); } break;
          case 99:
{ yyval = ((ISymbol)yyastk[yysp-(1-1)]).getName(); } break;
          case 100:
{ yyval = ((ISymbol)yyastk[yysp-(3-1)]).getName() + "." + ((String)yyastk[yysp-(3-3)]); } break;
          case 101:
{ yyval = ((String)yyastk[yysp-(1-1)]); } break;
          case 102:
{ List l = ((List)yyastk[yysp-(3-3)]); l.add(0, ((Object)yyastk[yysp-(3-1)])); yyval = l; } break;
          case 103:
{ List l = new ArrayList(); l.add(0, ((Object)yyastk[yysp-(1-1)])); yyval = l; } break;
          case 104:
{ yyval = new ArrayList(); } break;
          case 105:
{ yyval = ((Object)yyastk[yysp-(1-1)]); } break;
          case 106:
{ yyval = new Variable(((String)yyastk[yysp-(1-1)])); } break;
          case 107:
{ yyval = new Variable(((String)yyastk[yysp-(1-1)])); } break;
          case 108:
{ ((List)yyastk[yysp-(3-3)]).add(0, ((ISymbol)yyastk[yysp-(3-1)])); yyval = ((List)yyastk[yysp-(3-3)]); } break;
          case 109:
{ List l = new LinkedList(); l.add(((ISymbol)yyastk[yysp-(1-1)])); yyval = l; } break;
          case 110:
{
    ISymbol s = ((ISymbol)yyastk[yysp-(1-1)]);
    if (alias.containsKey(s)) {
      s = (ISymbol) alias.get(s);
    }
    yyval = s;
  } break;
          case 111:
{
    String s = ((String)yyastk[yysp-(1-1)]);
    if (inStringPattern >= 0) {
      if (s.charAt(0) == '\\' && s.length() > 1) {
        yyval = new CharPatternSymbol(s);
      }
      else {
        yyval = new CharSymbol(s);
      }
    }
    else {
      yyval = new StringSymbol(s);
    }
  } break;
          case 112:
{ yyval = new PrefixedSymbol(new StringSymbol(((String)yyastk[yysp-(3-1)])), ((ISymbol)yyastk[yysp-(3-3)])); } break;
          case 113:
{ yyval = new State(((String)yyastk[yysp-(1-1)])); } break;
          }
	  if (!yyparseerror) {
            /* Goto - shift nonterminal */
            yysp -= yyl;
            yyn = yylhs[yyn];
            if ((yyp = yygbase[yyn] + yysstk[yysp]) >= 0 && yyp < YYGLAST
                && yygcheck[yyp] == yyn)
              yystate = yygoto[yyp];
            else
              yystate = yygdefault[yyn];
          
            if (++yysp >= yysstk.length)
              growStack();

            yysstk[yysp] = (short)yystate;
            yyastk[yysp] = yyval;
	  }
	}

	if (yyparseerror) {
          /* error */
          switch (yyerrflag) {
          case 0:
            yyerror("syntax error");
          case 1:
          case 2:
            yyerrflag = 3;
            /* Pop until error-expecting state uncovered */

            while (!((yyn = yybase[yystate] + YYINTERRTOK) >= 0
                     && yyn < YYLAST && yycheck[yyn] == YYINTERRTOK
                     || (yystate < YY2TBLSTATE
                         && (yyn = yybase[yystate + YYNLSTATES] + YYINTERRTOK) >= 0
                         && yyn < YYLAST && yycheck[yyn] == YYINTERRTOK))) {
              if (yysp <= 0)
                return 1;
              yystate = yysstk[--yysp];
            }
            yyn = yyaction[yyn];
            yysstk[++yysp] = (short)(yystate = yyn);
            break;

          case 3:
            if (yychar1 == 0)
              return 1;
	    yychar1 = yychar = -1;
            break;
          }
        }
        
        if (yystate < YYNLSTATES)
          break;
        /* >= YYNLSTATES means shift-and-reduce */
        yyn = yystate - YYNLSTATES;
      }
    }
  }

}




