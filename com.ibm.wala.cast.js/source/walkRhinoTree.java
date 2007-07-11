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
import java.io.FileReader;
import java.util.Map;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.FunctionNode;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.tools.ToolErrorReporter;

import com.ibm.wala.util.collections.HashMapFactory;

class walkRhinoTree {

    private final Map<Node, Integer> results = HashMapFactory.make();

    private int nextValue = 1;

    private int nextValueNumber() {
      return nextValue++;
    }

    private int setValue(Node n, int v) {
      results.put(n, new Integer(v));
      return v;
    }

    private int allocValue(Node n) {
      return setValue(n, nextValueNumber());
    }

    private int getValue(Node n) {
      return results.get(n).intValue();
    }

    private interface Symbol {
      int valueNumber();
      boolean isParameter();
    }

    private final static int TYPE_LOCAL = 1;
    private final static int TYPE_GLOBAL = 2;
    private final static int TYPE_SCRIPT = 3;
    private final static int TYPE_FUNCTION = 4;

    private interface Scope {
      int type();
      void declare(String name);
      boolean contains(String name);
      Symbol lookup(String name);
    }

    private static abstract class AbstractScope implements Scope {
      private final Scope parent;
      private final Map<String, Symbol> values = HashMapFactory.make();

      public void declare(String nm) {
	if (! contains(nm)) {
	  values.put(nm, makeSymbol(nm));
	}
      }

      AbstractScope(Scope parent) {
	this.parent = parent;
      }

      abstract protected Symbol makeSymbol(String nm);

      public Symbol lookup(String nm) {
	if (contains(nm))
	  return values.get(nm);
	else
	  return parent.lookup(nm);
      }
	  
      public boolean contains(String nm) {
	return values.containsKey(nm);
      }
    };

    Scope makeScriptScope(final ScriptOrFnNode s, Scope parent) {
      return new AbstractScope(parent) {
	public int type() { return TYPE_SCRIPT; }

	protected Symbol makeSymbol(final String nm) {
	  final int v = nextValueNumber();
	  return
	    new Symbol() {
	      public String toString() { return nm; }
	      public int valueNumber() { return v; }
	      public boolean isParameter() { return false; }
	    };
	}
      };
    }
	
    Scope makeLocalScope(Node s, Scope parent) {
      return new AbstractScope(parent) {
	public int type() { return TYPE_LOCAL; }

	protected Symbol makeSymbol(final String nm) {
	  final int v = nextValueNumber();
	  return
	    new Symbol() {
	      public String toString() { return nm; }
	      public int valueNumber() { return v; }
	      public boolean isParameter() { return false; }
	    };
	}
      };
    }
	
    Scope makeFunctionScope(final FunctionNode f, Scope parent) {
      Scope s =  new AbstractScope(parent) {
	public int type() { return TYPE_FUNCTION; }

        protected Symbol makeSymbol(final String nm) {
	  final int v = nextValueNumber();
	  return
	    new Symbol() {
	      public String toString() { return nm; }
	      public int valueNumber() { return v; }
	      public boolean isParameter() {
		return f.getParamOrVarIndex(nm)<f.getParamCount();
	      }
	    };
	}
      };

      for(int i = 0; i < f.getParamCount(); i++)
	s.declare( f.getParamOrVarName(i) );

      return s;

    }

    private static Scope makeGlobalScope() {
      return new Scope() {
        public int type() { return TYPE_GLOBAL; }

	public void declare(String nm) {
	  throw new UnsupportedOperationException();
	}

	public boolean contains(String nm) {
	  return nm.equals("Math");
	}

	public Symbol lookup(String nm) {
	  return new Symbol() {
	    public String toString() { return "Math"; }
	    public int valueNumber() { return -1; }
	    public boolean isParameter() { return false; }
	  };
 	}
      };
    }
	      
    private interface WalkContext {

      ScriptOrFnNode top();

      Scope scope();

      boolean resultWanted();

      Node getCatchFor(Object exceptionType);

      Node getBreakFor(Object label);

      Node getContinueFor(Object label);

      Node getFinally();
    }

    private abstract class DelegatingContext implements WalkContext {
      private final WalkContext parent;

      DelegatingContext(WalkContext parent) {
	this.parent = parent;
      }
      
      public ScriptOrFnNode top() { return parent.top(); }

      public Scope scope() { return parent.scope(); }

      public boolean resultWanted() { return parent.resultWanted(); }

      public Node getCatchFor(Object l) { return parent.getCatchFor(l); }

      public Node getBreakFor(Object l) { return parent.getBreakFor(l); }

      public Node getContinueFor(Object l) { return parent.getContinueFor(l); }

      public Node getFinally() { return parent.getFinally(); }

    }

    private class ScriptContext extends DelegatingContext {
      private final Scope scriptScope;

      ScriptContext(WalkContext parent, Scope scriptScope) {
	super(parent);
	this.scriptScope = scriptScope;
      }

      public Scope scope() { return scriptScope; }

    }

    private class LocalContext extends DelegatingContext {
      private final Scope localScope;

      LocalContext(WalkContext parent, Scope localScope) {
	super(parent);
	this.localScope = localScope;
      }

      public Scope scope() { return localScope; }

    }

    private class FunctionContext extends ScriptContext {
      private final ScriptOrFnNode topNode;

      FunctionContext(WalkContext parent, Scope funScope, ScriptOrFnNode s) {
	super(parent, funScope);
	this.topNode = s;
      }

      public ScriptOrFnNode top() { return topNode; }
    }

    private class LoopContext extends DelegatingContext {
      private final Node breakTo;
      private final Node continueTo;

      LoopContext(WalkContext parent, Node breakTo, Node continueTo) {
	super(parent);
	this.breakTo = breakTo;
	this.continueTo = continueTo;
      }

      public Node getBreakFor(Object l) {
	return (l == null)? breakTo: super.getBreakFor(l);
      }
	
      public Node getContinueFor(Object l) {
	return (l == null)? continueTo: super.getContinueFor(l);
      }
    }

    private class TryCatchContext extends DelegatingContext {
      private final Node catchNode;
      private final Node finallyNode;

      TryCatchContext(WalkContext parent, Node catchNode, Node finallyNode) {
	super(parent);
	this.catchNode = catchNode;
	this.finallyNode = finallyNode;
      }

      public Node getCatchFor(Object l) { return catchNode; }

      public Node getFinally() { return finallyNode; }
    }

    private void walkNodes(final Node n, WalkContext context) {
      if (n instanceof ScriptOrFnNode) {
	WalkContext child;
	if (n instanceof FunctionNode) {
	  FunctionNode f = (FunctionNode)n;
	  System.err.println("function " + f.getFunctionName());
	  child = new FunctionContext(context, makeFunctionScope(f, context.scope()), f);	  
	} else {
	  ScriptOrFnNode s = (ScriptOrFnNode)n;
	  System.err.println("program");
	  child = new ScriptContext(context, makeScriptScope(s, context.scope()));
	}
	
	for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	  walkNodes(c, child);

      } else {
	int NT = n.getType();
	switch (NT) {
	case Token.FUNCTION: {
	  int fnIndex = n.getExistingIntProp(Node.FUNCTION_PROP);
	  FunctionNode fn = context.top().getFunctionNode(fnIndex);

	  System.err.println("declaring " + fn.getFunctionName() );

	  context.scope().declare( fn.getFunctionName() );
	  walkNodes(fn, context);
	  break;
	}

	case Token.LOOP: {
	  Node contTo = ((Node.Jump)n).getContinue();
	  Node breakTo = ((Node.Jump)n).target;
	  WalkContext child = new LoopContext(context, breakTo, contTo);
	  for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	    walkNodes(c, child);

	  break;
	}

	case Token.LOCAL_BLOCK: {
	  WalkContext child = new LocalContext(context, makeLocalScope(n, context.scope()));
	  for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	    walkNodes(c, child);
	  
	  break;
	}

	case Token.CATCH_SCOPE: {
	  Node id = n.getFirstChild();
	  context.scope().declare( id.getString() );
	  System.err.println( context.scope().lookup( id.getString() ).valueNumber() + " = caught exception");
	  break;
	}

	case Token.TRY:  {
	  Node catchNode = ((Node.Jump)n).target;
	  Node finallyNode = ((Node.Jump)n).getFinally();
	  WalkContext child =
	    new TryCatchContext(context, catchNode, finallyNode);

	  Node c = n.getFirstChild();
	  for (; c != null; c = c.getNext()) {
	    if (c.getType() == Token.FINALLY) break;
	    walkNodes(c, child);
	  }

	  for (; c != null; c = c.getNext()) {
	    walkNodes(c, context);
	  }
	  
	  break;
	}

	case Token.JSR: {
	  Node jsrTarget = ((Node.Jump)n).target;
	  Node finallyBlock = context.getFinally();
	  if (jsrTarget != finallyBlock) throw new Error();
	  Node finallyNode = jsrTarget.getNext();
	  walkNodes(finallyNode, context);
	  break;
	}

	case Token.WITH: 
	case Token.FINALLY: 
	case Token.BLOCK: 
	case Token.LABEL: 
	case Token.EXPR_VOID: {
	  for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	    walkNodes(c, context);
	  
	  break;
	}

	case Token.CALL: {
	  int result = allocValue(n);
	  Node callee = n.getFirstChild();

	  int fun;
	  if (callee.getType() == Token.NAME)
	    fun = context.scope().lookup( callee.getString() ).valueNumber();
	  else { 
	    walkNodes( callee, context );
	    fun = getValue( callee );
	  }

	  int children = 0;
	  for (Node c = callee.getNext(); c != null; c = c.getNext()) {
	    walkNodes(c, context);
	    children++;
	  }
	    
	  int i = 0;
	  int arguments[] = new int[ children ];
	  for(Node arg = callee.getNext(); arg != null; arg = arg.getNext() ) 
	    arguments[i++] = getValue( arg );

	  System.err.print(result + " = CALL " + fun);
	  for(int j = 0; j < arguments.length; j++)
	    System.err.print( " " + arguments[j] );
	  System.err.print("\n");
	      
	  break;
	}

	case Token.NAME: { 
	  setValue(n, context.scope().lookup( n.getString() ).valueNumber() );
	  break;
	}
	
	case Token.STRING: {
	  int result = allocValue(n);
	  System.err.println(result + " = \"" + n.getString() + "\"");
	  break;
	}

	case Token.NUMBER: {
	  int result = allocValue(n);
	  System.err.println(result + " = " + n.getDouble());
	  break;
	}

	case Token.ADD:
	case Token.EQ:
	case Token.DIV:
	case Token.NE:
	case Token.GT:
	case Token.LT: {
	  int result = allocValue(n);

	  for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	    walkNodes(c, context);

	  Node l = n.getFirstChild();
	  Node r = l.getNext();
	  System.err.println(result + " = " + getValue(l) + " " + Token.name(NT) + " " + getValue(r));

	  break;
	}	    

	case Token.VAR: {
	  Node nm = n.getFirstChild();
	  context.scope().declare(nm.getString());
	  if (nm.getFirstChild() != null) {
	    walkNodes( nm.getFirstChild(), context );
	    System.err.println(context.scope().lookup(nm.getString()) + " = " + getValue(nm.getFirstChild()) );
	  }
	  break;
	}
	
	case Token.RETURN: {
	  Node val = n.getFirstChild();
	  if (val != null) {
	    walkNodes(val, context);
	    System.err.println("return " + getValue(val));
	  } else {
	    System.err.println("return");
	  }
	  break;
	}

	case Token.SETNAME: {
	  Node nm = n.getFirstChild();
	  walkNodes( nm.getNext(), context);
	  System.err.println("set " + context.scope().lookup(nm.getString()).valueNumber() + "(" + nm.getString() + ") = " + getValue( nm.getNext() ));
	  break;
	}
	    
	case Token.IFNE:
	case Token.IFEQ: {
	  Node expr = n.getFirstChild();
	  walkNodes( expr, context );
	  System.err.println(Token.name(NT) + " " + getValue(expr) + " GOTO " + ((Node.Jump)n).target);
	  break;
	}
	    
	case Token.GOTO: {
	  System.err.println("GOTO (" + Token.name(NT) + ") " + ((Node.Jump)n).target);
	  break;
	}
	    
	case Token.BREAK: {
	  System.err.println("GOTO (" + Token.name(NT) + ") " + ((Node.Jump)n).getJumpStatement().target);
	  break;
	}

	case Token.TARGET: {
	  System.err.println("LABEL " + n);
	  break;
	}

	case Token.OR: {
	  Object label = new Object() { public String toString() { return "OR:" + System.identityHashCode(n); } };
	  Node l = n.getFirstChild();
	  walkNodes(l, context);
	  System.err.println(getValue(n) + " = " + getValue(l));
	  System.err.println("IFEQ " + getValue(n) + " GOTO " + label);
	  Node r = l.getNext();
	  walkNodes(r, context);
	  System.err.println(getValue(n) + " = " + getValue(r));
	  System.err.println("LABEL " + label);
	  break;
	}

	case Token.INC: {
	  int result = context.resultWanted()? allocValue(n): -1;
	  Node l = n.getFirstChild();
	  walkNodes(l, context);
	  int flags = n.getIntProp( Node.INCRDECR_PROP, -1 );
	  String op = ((flags&Node.DECR_FLAG)!=0)? "-": "+";
	  if ((flags&Node.POST_FLAG)!=0) {
	    if (context.resultWanted())
	      System.err.println(result + " = " + getValue(l));
	    System.err.println(getValue(l) + " = " + getValue(l) + " " + op + " 1");
	  } else {
	    System.err.println(getValue(l) + " = " + getValue(l) + " " + op + " 1");
	    if (context.resultWanted()) 
	      System.err.println(result + " = " + getValue(l));
	  }

	  break;
	}

	case Token.EMPTY:
	case Token.ENTERWITH: 
	case Token.LEAVEWITH: {
	  break;
	}

	case Token.GETPROP:
	case Token.GETELEM: {
	  int result = allocValue(n);
	  Node receiver = n.getFirstChild();
	  walkNodes(receiver, context);
	  
	  Node elt = receiver.getNext();
	  if (elt.getType() == Token.STRING) {
	    System.err.println(result + " = getfield " + getValue(receiver) + "." + elt.getString());
	  } else {
	    walkNodes(elt, context);
	    System.err.println(result + " = reflective getfield " + getValue(receiver) + "." + getValue(elt));
	  }

	  break;
	}	      

	case Token.THROW: {
	  Node catchTarget = context.getCatchFor( null );
	  Node throwVal = n.getFirstChild();  
	  walkNodes(throwVal, context);
	  if (catchTarget != null) {
	    System.err.println("THROW of " + getValue(throwVal) + " to " + catchTarget);
	  } else {
	    System.err.println("THROW out of " + getValue(throwVal));
	  }
	  break;
	}

	default: {
	  System.err.println("looking at unhandled " + n + "(" + NT + ")" + " of " + n.getClass());
	  for (Node c = n.getFirstChild(); c != null; c = c.getNext())
	    System.err.println("  with child " + c);
	}
	}
      }
    }
    
    public static void main(String[] args) throws Exception {
	ToolErrorReporter reporter = new ToolErrorReporter(true);
	CompilerEnvirons compilerEnv = new CompilerEnvirons();
	compilerEnv.setErrorReporter(reporter);
	
	Parser P = new Parser(compilerEnv, compilerEnv.getErrorReporter());
	
	final ScriptOrFnNode N = P.parse(new FileReader(args[0]), args[0], 1);
	walkRhinoTree walker = new walkRhinoTree();

	final Scope G = makeGlobalScope();

	walker.walkNodes( N, 
	  new WalkContext() {
	    public ScriptOrFnNode top() { return N; }

	    public Scope scope() { return G; }

	    public boolean resultWanted() { return true; }

	    public Node getCatchFor(Object exceptionType) { return null; }

	    public Node getBreakFor(Object label) { return null; }

	    public Node getContinueFor(Object label) { return null; }

	    public Node getFinally() { return null; }
	  } );
    }
    }
    


	
  
