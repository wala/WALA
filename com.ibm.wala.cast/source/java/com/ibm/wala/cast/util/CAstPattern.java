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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.tree.visit.CAstVisitor.Context;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

public class CAstPattern {
  private static boolean DEBUG_PARSER = false;

  private static boolean DEBUG_MATCH = false;

  private final static int CHILD_KIND = -1;

  private final static int CHILDREN_KIND = -2;

  private final static int REPEATED_PATTERN_KIND = -3;

  private final static int ALTERNATIVE_PATTERN_KIND = -4;

  private final static int OPTIONAL_PATTERN_KIND = -5;

  private final static int REFERENCE_PATTERN_KIND = -6;
  
  private final static int IGNORE_KIND = -99;

  private final String name;

  private final int kind;

  private final Object value;

  private final CAstPattern[] children;

  private final Map<String, CAstPattern> references;

  public static class Segments extends TreeMap<String,Object> {

    private static final long serialVersionUID = 4119719848336209576L;

    public CAstNode getSingle(String name) {
      assert containsKey(name) : name;
      return (CAstNode) get(name);
    }

    @SuppressWarnings("unchecked")
    public List<CAstNode> getMultiple(String name) {
      if (!containsKey(name)) {
        return Collections.emptyList();
      } else {
        Object o = get(name);
        if (o instanceof CAstNode) {
          return Collections.singletonList((CAstNode)o);
        } else {
          assert o instanceof List;
          return (List<CAstNode>) o;
        }
      }
    }

    private void addAll(Segments other) {
      for (Map.Entry<String, Object> e : other.entrySet()) {
        String name = e.getKey();
        if (e.getValue() instanceof CAstNode) {
          add(name, (CAstNode) e.getValue());
        } else {
          @SuppressWarnings("unchecked")
          final List<CAstNode> nodes = (List<CAstNode>) e.getValue();
          for (CAstNode v : nodes) {
            add(name, v);
          }
        }
      }
    }

    @SuppressWarnings("unchecked")
    private void add(String name, CAstNode result) {
      if (containsKey(name)) {
        Object o = get(name);
        if (o instanceof List) {
          ((List<CAstNode>) o).add(result);
        } else {
          assert o instanceof CAstNode;
          List<Object> x = new ArrayList<>();
          x.add(o);
          x.add(result);
          put(name, x);
        }
      } else {
        put(name, result);
      }
    }
  }

  public CAstPattern(String name, int kind, CAstPattern[] children) {
    this.name = name;
    this.kind = kind;
    this.value = null;
    this.children = children;
    this.references = null;
  }

  public CAstPattern(String name, Object value) {
    this.name = name;
    this.kind = IGNORE_KIND;
    this.value = value;
    this.children = null;
    this.references = null;
  }

  public CAstPattern(String patternName, Map<String, CAstPattern> references) {
    this.name = null;
    this.kind = REFERENCE_PATTERN_KIND;
    this.value = patternName;
    this.references = references;
    this.children = null;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();

    if (name != null) {
      sb.append("<").append(name).append(">");
    }

    if (value != null) {
      if (kind == REFERENCE_PATTERN_KIND) {
        sb.append("ref:").append(value);
      } else if (value instanceof Pattern) {
        sb.append("/").append(value).append("/");
      } else {
        sb.append("literal:").append(value);
      }
    } else if (kind == CHILD_KIND) {
      sb.append("*");
    } else if (kind == CHILDREN_KIND) {
      sb.append("**");
    } else if (kind == REPEATED_PATTERN_KIND) {
      sb.append("@");
    } else if (kind == ALTERNATIVE_PATTERN_KIND) {
      sb.append("|");
    } else if (kind == OPTIONAL_PATTERN_KIND) {
      sb.append("?");
    } else {
      sb.append(CAstPrinter.kindAsString(kind));
    }

    if (children != null) {
      sb.append("(");
      for (int i = 0; i < children.length; i++) {
        sb.append(children[i].toString());
        if (i == children.length - 1) {
          sb.append(")");
        } else {
          sb.append(",");
        }
      }
    }

    return sb.toString();
  }

  private static boolean matchChildren(CAstNode tree, int i, CAstPattern[] cs, int j, Segments s) {
    if (i >= tree.getChildCount() && j >= cs.length) {
      return true;
    } else if (i < tree.getChildCount() && j >= cs.length) {
      return false;
    } else if (i >= tree.getChildCount() && j < cs.length) {
      switch (cs[j].kind) {
      case CHILDREN_KIND:
      case OPTIONAL_PATTERN_KIND:
      case REPEATED_PATTERN_KIND:
        return matchChildren(tree, i, cs, j + 1, s);

      default:
        return false;
      }
    } else {
      if (cs[j].kind == CHILD_KIND) {

        if (DEBUG_MATCH) {
          System.err.println(("* matches " + CAstPrinter.print(tree.getChild(i))));
        }

        if (s != null && cs[j].name != null) {
          s.add(cs[j].name, tree.getChild(i));
        }
        return matchChildren(tree, i + 1, cs, j + 1, s);

      } else if (cs[j].kind == CHILDREN_KIND) {
        if (tryMatchChildren(tree, i, cs, j + 1, s)) {

          if (DEBUG_MATCH) {
            System.err.println("** matches nothing");
          }

          return true;

        } else {

          if (DEBUG_MATCH) {
            System.err.println(("** matches " + CAstPrinter.print(tree.getChild(i))));
          }

          if (s != null && cs[j].name != null) {
            s.add(cs[j].name, tree.getChild(i));
          }

          return matchChildren(tree, i + 1, cs, j, s);
        }

      } else if (cs[j].kind == REPEATED_PATTERN_KIND) {
        CAstPattern repeatedPattern = cs[j].children[0];
        if (repeatedPattern.tryMatch(tree.getChild(i), s)) {
          if (s != null && cs[j].name != null) {
            s.add(cs[j].name, tree.getChild(i));
          }

          if (DEBUG_MATCH) {
            System.err.println((cs[j] + " matches " + CAstPrinter.print(tree.getChild(i))));
          }

          return matchChildren(tree, i + 1, cs, j, s);

        } else {

          if (DEBUG_MATCH) {
            System.err.println((cs[j] + " matches nothing"));
          }

          return matchChildren(tree, i, cs, j + 1, s);
        }

      } else if (cs[j].kind == OPTIONAL_PATTERN_KIND) {
        if (tryMatchChildren(tree, i, cs, j + 1, s)) {

          if (DEBUG_MATCH) {
            System.err.println((cs[j] + " matches nothing"));
          }

          return true;
        } else {
          CAstPattern optionalPattern = cs[j].children[0];
          if (optionalPattern.tryMatch(tree.getChild(i), s)) {

            if (DEBUG_MATCH) {
              System.err.println((cs[j] + " matches " + CAstPrinter.print(tree.getChild(i))));
            }

            return matchChildren(tree, i + 1, cs, j + 1, s);
          } else {
            return false;
          }
        }

      } else {
        return cs[j].match(tree.getChild(i), s) && matchChildren(tree, i + 1, cs, j + 1, s);
      }
    }
  }

  public boolean match(CAstNode tree, Segments s) {
    if (DEBUG_MATCH) {
      System.err.println(("matching " + this + " against " + CAstPrinter.print(tree)));
    }

    if (kind == REFERENCE_PATTERN_KIND) {
      return references.get(value).match(tree, s);

    } else if (kind == ALTERNATIVE_PATTERN_KIND) {
      for (CAstPattern element : children) {
        if (element.tryMatch(tree, s)) {

          if (s != null && name != null)
            s.add(name, tree);

          return true;
        }
      }

      if (DEBUG_MATCH) {
        System.err.println("match failed (a)");
      }
      return false;

    } else {
      if ((value == null) ? tree.getKind() != kind : 
          (tree.getKind() != CAstNode.CONSTANT || 
           (value instanceof Pattern 
               ? !((Pattern)value).matcher(tree.getValue().toString()).matches()
               : !value.equals(tree.getValue().toString())))) 
      {
        if (DEBUG_MATCH) {
          System.err.println("match failed (b)");
        }

        return false;
      }

      if (s != null && name != null)
        s.add(name, tree);

      if (children == null || children.length == 0) {
        if (DEBUG_MATCH && tree.getChildCount() != 0) {
          System.err.println("match failed (c)");
        }
        return tree.getChildCount() == 0;
      } else {
        return matchChildren(tree, 0, children, 0, s);
      }
    }
  }

  private static boolean tryMatchChildren(CAstNode tree, int i, CAstPattern[] cs, int j, Segments s) {
    if (s == null) {
      return matchChildren(tree, i, cs, j, s);
    } else {
      Segments ss = new Segments();
      boolean result = matchChildren(tree, i, cs, j, ss);
      if (result)
        s.addAll(ss);
      return result;
    }
  }

  private boolean tryMatch(CAstNode tree, Segments s) {
    if (s == null) {
      return match(tree, s);
    } else {
      Segments ss = new Segments();
      boolean result = match(tree, ss);
      if (result)
        s.addAll(ss);
      return result;
    }
  }

  public static Segments match(CAstPattern p, CAstNode n) {
    Segments s = new Segments();
    if (p.match(n, s)) {
      return s;
    } else {
      return null;
    }
  }

  public static CAstPattern parse(String patternString) {
    try {
      return (new Parser(patternString)).parse();
    } catch (NoSuchFieldException e) {
      Assertions.UNREACHABLE("no such kind in pattern: " + e.getMessage());
      return null;
    } catch (IllegalAccessException e) {
      Assertions.UNREACHABLE("internal error in CAstPattern" + e);
      return null;
    }
  }

  public static Collection<Segments> findAll(final CAstPattern p, final CAstEntity e) {
    return p.new Matcher().findAll(new Context() {
      @Override
      public CAstEntity top() {
        return e;
      }
      @Override
      public CAstSourcePositionMap getSourceMap() {
        return e.getSourceMap();
      }
    }, e.getAST());
  }
  
  public class Matcher extends CAstVisitor<Context> {
    private final Collection<Segments> result = HashSetFactory.make();
    
    @Override
    public void leaveNode(CAstNode n, Context c, CAstVisitor<Context> visitor) {
      Segments s = match(CAstPattern.this, n);
      if (s != null) {
        result.add(s);
      }
    }

    public Collection<Segments> findAll(final Context c, final CAstNode top) {
      visit(top, c, this);   
      return result;
    }
  }
    
  private static class Parser {
    private final Map<String, CAstPattern> namedPatterns = HashMapFactory.make();

    private final String patternString;

    private int start;

    private int end;

    private Parser(String patternString) {
      this.patternString = patternString;
    }

    // private Parser(String patternString, int start) {
    // this(patternString);
    // this.start = start;
    // }

    private String parseName(boolean internal) {
      if (patternString.charAt(start) == (internal ? '{' : '<')) {
        int nameStart = start + 1;
        int nameEnd = patternString.indexOf(internal ? '}' : '>', nameStart);
        start = nameEnd + 1;
        return patternString.substring(nameStart, nameEnd);
      } else {
        return null;
      }
    }

    public CAstPattern parse() throws NoSuchFieldException, IllegalAccessException {
      if (DEBUG_PARSER) {
        System.err.println(("parsing " + patternString.substring(start)));
      }

      String internalName = parseName(true);
      String name = parseName(false);

      CAstPattern result;
      if (patternString.charAt(start) == '`') {
        int strEnd = patternString.indexOf('`', start + 1);
        end = strEnd + 1;
        String patternName = patternString.substring(start + 1, strEnd);
        assert internalName == null;
        result = new CAstPattern(patternName, namedPatterns);

      } else if (patternString.charAt(start) == '"') {
        int strEnd = patternString.indexOf('"', start + 1);
        end = strEnd + 1;
        result = new CAstPattern(name, patternString.substring(start + 1, strEnd));

      } else if (patternString.charAt(start) == '/') {
        int strEnd = patternString.indexOf('/', start + 1);
        end = strEnd + 1;
        result = new CAstPattern(name, Pattern.compile(patternString.substring(start + 1, strEnd)));

      } else if (patternString.startsWith("**", start)) {
        end = start + 2;
        result = new CAstPattern(name, CHILDREN_KIND, null);

      } else if (patternString.startsWith("*", start)) {
        end = start + 1;
        result = new CAstPattern(name, CHILD_KIND, null);

      } else if (patternString.startsWith("|(", start)) {
        List<CAstPattern> alternatives = new ArrayList<>();
        start += 2;
        do {
          alternatives.add(parse());
          start = end + 2;
        } while (patternString.startsWith("||", end));
        assert patternString.startsWith(")|", end) : patternString;
        end += 2;
        result = new CAstPattern(name, ALTERNATIVE_PATTERN_KIND, alternatives.toArray(new CAstPattern[alternatives.size()]));

      } else if (patternString.startsWith("@(", start)) {
        start += 2;
        CAstPattern children[] = new CAstPattern[] { parse() };
        assert patternString.startsWith(")@", end);
        end += 2;

        if (DEBUG_PARSER) {
          System.err.println(("repeated pattern: " + children[0]));
        }

        result = new CAstPattern(name, REPEATED_PATTERN_KIND, children);

      } else if (patternString.startsWith("?(", start)) {
        start += 2;
        CAstPattern children[] = new CAstPattern[] { parse() };
        assert patternString.startsWith(")?", end);
        end += 2;

        if (DEBUG_PARSER) {
          System.err.println(("optional pattern: " + children[0]));
        }

        result = new CAstPattern(name, OPTIONAL_PATTERN_KIND, children);

      } else {
        int kindEnd = patternString.indexOf('(', start);
        String kindStr = patternString.substring(start, kindEnd);
        Field kindField = CAstNode.class.getField(kindStr);
        int kind = kindField.getInt(null);

        if (patternString.charAt(kindEnd + 1) == ')') {
          end = kindEnd + 2;
          result = new CAstPattern(name, kind, null);

        } else {
          List<CAstPattern> children = new ArrayList<>();
          start = patternString.indexOf('(', start) + 1;
          do {
            children.add(parse());
            start = end + 1;

            if (DEBUG_PARSER) {
              System.err.println(("parsing children: " + patternString.substring(end)));
            }

          } while (patternString.charAt(end) == ',');

          assert patternString.charAt(end) == ')';
          end++;

          result = new CAstPattern(name, kind, children.toArray(new CAstPattern[children.size()]));
        }
      }

      if (internalName != null) {
        namedPatterns.put(internalName, result);
      }

      return result;
    }
  }
}
