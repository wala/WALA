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
/**
 * RTLComparator.java -- compare two tree grammars of binary trees.
 */
package com.ibm.wala.automaton.grammar.tree;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;


public class RTLComparator implements IRTLComparator {
  static public boolean debug = false;

  static private class Subtype {
    // represent t1 <: t2
    public Set left;
    public Set right;

    public Subtype(IBinaryTree t1, IBinaryTree t2){
      Set l1 = new HashSet();
      Set l2 = new HashSet();
      l1.add(t1);
      l2.add(t2);
      this.left = l1;
      this.right = l2;
    }

    public Subtype(IBinaryTree t1, Set t2){
      Set l1 = new HashSet();
      l1.add(t1);
      this.left = l1;
      this.right = t2;
    }

    public Subtype(Set t1, Set t2){
      this.left = t1;
      this.right = t2;
    }

    public String toString(){
      return left + " <: " + right;
    }

    public int hashCode(){
      return left.hashCode() + right.hashCode();
    }

    public boolean equals(Object obj){
      if (this == obj) return true;
      if (!getClass().equals(obj.getClass())) return false;
      Subtype ty = (Subtype) obj;
      if (left.equals(ty.left) && right.equals(ty.right)) {
        return true;
      }
      else {
        return false;
      }
    }
  }

  static private class Context {
    final public ITreeGrammar left;
    final public ITreeGrammar right;
    final private Set set;
    final private List stackTrace;
    final private IMatchContext matchContext;

    public Context(ITreeGrammar g1, ITreeGrammar g2){
      this(g1, g2, new HashSet(), new ArrayList());
    }

    public Context(ITreeGrammar g1, ITreeGrammar g2, Set set, List stackTrace){
      this.left = g1;
      this.right = g2;
      this.set = set;
      this.stackTrace = stackTrace;
      this.matchContext = new MatchContext();
    }

    public Set getLeftTrees(IBinaryTreeVariable v) {
      return getLeftTrees(v, new HashSet());
    }

    public Set getLeftTrees(IBinaryTreeVariable v, Set history) {
      if (history.contains(v)) {
        return new HashSet();
      }
      history.add(v);
      Set s = collectTrees(TreeGrammars.getRules(left, v));
      Set ss = new HashSet();
      for (Iterator i = s.iterator(); i.hasNext(); ) {
        IBinaryTree t = (IBinaryTree) i.next();
        if (t instanceof IBinaryTreeVariable) {
          ss.addAll(getLeftTrees((IBinaryTreeVariable)t, history));
        }
        else {
          ss.add(t);
        }
      }
      return ss;
    }

    public Set getRightTrees(IBinaryTreeVariable v) {
      return getRightTrees(v, new HashSet());
    }

    public Set getRightTrees(IBinaryTreeVariable v, Set history) {
      if (history.contains(v)) {
        return new HashSet();
      }
      history.add(v);
      Set s = collectTrees(TreeGrammars.getRules(right, v));
      Set ss = new HashSet();
      for (Iterator i = s.iterator(); i.hasNext(); ) {
        IBinaryTree t = (IBinaryTree) i.next();
        if (t instanceof IBinaryTreeVariable) {
          ss.addAll(getRightTrees((IBinaryTreeVariable)t, history));
        }
        else {
          ss.add(t);
        }
      }
      return ss;
    }

    public Set getLeftTrees(Set trees) {
      return getLeftTrees(trees, new HashSet());
    }

    public Set getLeftTrees(Set trees, Set history) {
      HashSet s = new HashSet();
      for (Iterator i = trees.iterator(); i.hasNext(); ) {
        IBinaryTree t = (IBinaryTree) i.next();
        if (t instanceof IBinaryTreeVariable) {
          s.addAll(getLeftTrees((IBinaryTreeVariable)t, history));
        }
        else {
          s.add(t);
        }
      }
      return s;
    }

    public Set getRightTrees(Set trees) {
      return getRightTrees(trees, new HashSet());
    }

    public Set getRightTrees(Set trees, Set history) {
      HashSet s = new HashSet();
      for (Iterator i = trees.iterator(); i.hasNext(); ) {
        IBinaryTree t = (IBinaryTree) i.next();
        if (t instanceof IBinaryTreeVariable) {
          s.addAll(getRightTrees((IBinaryTreeVariable)t, history));
        }
        else {
          s.add(t);
        }
      }
      return s;
    }

    private Set collectTrees(Set rules) {
      HashSet s = new HashSet();
      for (Iterator i = rules.iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        s.add(rule.getRight(0));
      }
      return s;
    }

    public IMatchContext getMatchContext() {
      return matchContext;
    }

    public void add(Subtype ty){
      set.add(ty);
    }

    public boolean contains(Subtype ty){
      return set.contains(ty);
    }

    public Iterator iterator(){
      return set.iterator();
    }

    public void addContext(Context ctx){
      for( Iterator i = ctx.iterator(); i.hasNext(); ){
        Subtype subtype = (Subtype) i.next();
        add(subtype);
      }
    }

    public void addTrace(Subtype subtype){
      stackTrace.add(0, subtype);
    }

    public void popTrace(){
      stackTrace.remove(0);
    }

    public Iterator stackTraceIterator(){
      return stackTrace.iterator();
    }

    public String toString(){
      return "#context:" + set.toString();
    }

    public Context dup(){
      return new Context(left, right, new HashSet(set), new ArrayList(stackTrace));
    }
  }

  public int compare(Object o1, Object o2) {
    ITreeGrammar tg1 = (ITreeGrammar) o1;
    ITreeGrammar tg2 = (ITreeGrammar) o2;
    if (check(tg1, tg2)) {
      if (check(tg2, tg1)) {
        return 0;
      }
      else { // tg1 < tg2
        return -1;
      }
    }
    else {
      if (check(tg2, tg1)) { // tg2 < tg1
        return 1;
      }
      else {
        return 0; // can't compare
      }
    }
  }

  public boolean contains(ITreeGrammar g1, ITreeGrammar g2) {
    return check(g2, g1);
  }

  public boolean check(ITreeGrammar g1, ITreeGrammar g2) {
    Context ctx = new Context(g1, g2);
    return check(
      (IBinaryTreeVariable) g1.getStartSymbol(),
      (IBinaryTreeVariable) g2.getStartSymbol(),
      ctx);
  }

  /**
   * @param t1    types of tree
   * @param t2    types of tree
   * @return        return the result of t1 <: t2
   */
  private boolean check(IBinaryTree t1, IBinaryTree t2, Context ctx){
    trace("check", t1, t2, ctx);
    if( checkHyp(t1, t2, ctx) ){
      return true;
    }
    if( checkAssum(t1, t2, ctx) ){
      return true;
    }
    return false;
  }

  private boolean checkHyp(IBinaryTree t1, IBinaryTree t2, Context ctx){
    trace("checkHyp", t1, t2, ctx);
    Subtype subtype = new Subtype(t1, t2);
    if( ctx.contains(subtype) ){
      return true;
    }
    else{
      return false;
    }
  }

  private boolean checkAssum(IBinaryTree t1, IBinaryTree t2, Context ctx){
    trace("checkAssum", t1, t2, ctx);
    Subtype subtype = new Subtype(t1, t2);
    Context ctx2 = ctx.dup();
    ctx2.add(subtype);
    if( check2(t1, t2, ctx2) ){
      ctx.addContext(ctx2);
      return true;
    }
    else{
      return false;
    }
  }

  private boolean check2(IBinaryTree t1, IBinaryTree t2, Context ctx){
    trace("check2(IBinaryTree,IBinaryTree)", t1, t2, ctx);
    Set l2 = new HashSet();
    l2.add(t2);
    return check2(t1, l2, ctx);
  }

  private boolean check2(Set t1, Set t2, Context ctx){
    trace("check2(Set,Set)", t1, t2, ctx);
    switch (t1.size()) {
    case 0:
      if( checkEmpty(t1, t2, ctx) ){
        return true;
      }
      else{
        return false;
      }
    case 1:
      IBinaryTree u1 = pop(t1);
      if( check2(u1, t2, ctx) ){
        return true;
      }
      else{
        return false;
      }
    default:
      if( checkSplit(t1, t2, ctx) ){
        return true;
      }
      else{
        return false;
      }
    }
  }

  private boolean check2(IBinaryTree t1, Set t2, Context ctx){
    trace("check2(IBinaryTree,Set)", t1, t2, ctx);
    if( t1 instanceof IBinaryTreeVariable ){
      Set l1 = ctx.getLeftTrees((IBinaryTreeVariable)t1);
      if( check2(l1, t2, ctx) ){
        return true;
      }
      else{
        return false;
      }
    }
    else {
      if( t1 instanceof IParentBinaryTree ){
        if( checkRec((IParentBinaryTree)t1, t2, ctx) ){
          return true;
        }
        else{
          return false;
        }
      }
      else{
        if( checkLeaf(t1, t2, ctx) ){
          return true;
        }
        else{
          return false;
        }
      }
    }
  }

  private boolean checkEmpty(Set t1, Set t2, Context ctx){
    trace("checkEmpty", t1, t2, ctx);
    if( t1.isEmpty() ){
      Subtype subtype = new Subtype(t1, t2);
      ctx.add(subtype);
      return true;
    }
    return false;
  }

  private boolean checkSplit(Set t1, Set t2, Context ctx){
    trace("checkSplit", t1, t2, ctx);
    Set tl1 = new HashSet(t1);
    IBinaryTree hd1 = pop(tl1);
    Context ctx2 = ctx.dup();
    if( check2(hd1, t2, ctx2) ){
      if( check2(tl1, t2, ctx2) ){
        ctx.addContext(ctx2);
        return true;
      }
    }
    return false;        
  }

  private boolean checkLeaf(IBinaryTree t1, Set t2, Context ctx){
    trace("checkLeaf", t1, t2, ctx);
    if( !(t1 instanceof IParentBinaryTree) ){
      t2 = ctx.getRightTrees(t2);
      trace("checkLeaf'", t1, t2, ctx);
      for( Iterator i = t2.iterator(); i.hasNext(); ){
        IBinaryTree s = (IBinaryTree) i.next();
        if( !(s instanceof IParentBinaryTree) ){
          Subtype subtype = new Subtype(t1, t2);
          ctx.add(subtype);
          return true;
        }
      }
    }
    return false;
  }

  private boolean checkRec(IParentBinaryTree t1, Set t2, Context ctx){
    trace("checkRec", t1, t2, ctx);

    ISymbol label1 = t1.getLabel();
    IBinaryTree left1 = t1.getLeft();
    IBinaryTree right1 = t1.getRight();
    t2 = ctx.getRightTrees(t2);
    trace("checkRec'", t1, t2, ctx);
    Set l2 = new HashSet();
    for( Iterator i = t2.iterator(); i.hasNext(); ){
      IBinaryTree s = (IBinaryTree) i.next();
      if( s instanceof IParentBinaryTree ){
        ISymbol label2 = s.getLabel();
        if( !label2.matches(label1, ctx.getMatchContext()) ){
          continue;
        }
      }
      else{
        continue;
      }
      l2.add(s);
    }

    trace("checkRec''", t1, l2, ctx);
    if( l2.isEmpty() ){
      return false;
    }
    Set a = new HashSet();
    Set b = new HashSet(l2);
    Context ctx2 = ctx.dup();
    while( true ){
      if( !checkRec(left1, right1, a, b, ctx2) ){
        return false;
      }
      if( b.isEmpty() ){
        break;
      }
      IBinaryTree t = pop(b);
      a.add(t);
    }
    ctx.addContext(ctx2);
    return true;
  }

  private boolean checkRec(IBinaryTree left1, IBinaryTree right1, Set a, Set b, Context ctx){
    trace("checkRec'''", left1, a, ctx);
    trace("checkRec'''", right1, b, ctx);
    Context ctx2 = ctx.dup();
    for( Iterator i = a.iterator(); i.hasNext(); ){
      IParentBinaryTree s = (IParentBinaryTree) i.next();
      IBinaryTree left2 = s.getLeft();
      IBinaryTree right2 = s.getRight();
      if( check(left1, left2, ctx2) ){
        ctx.addContext(ctx2);
        return true;
      }
    }
    for( Iterator i = b.iterator(); i.hasNext(); ){
      IParentBinaryTree s = (IParentBinaryTree) i.next();
      IBinaryTree left2 = s.getLeft();
      IBinaryTree right2 = s.getRight();
      if( check(right1, right2, ctx2) ){
        ctx.addContext(ctx2);
        return true;
      }
    }
    return false;
  }

  private void trace(String msg, IBinaryTree t1, IBinaryTree t2, Context ctx){
    Set l1 = new HashSet();
    l1.add(t1);
    Set l2 = new HashSet();
    l2.add(t2);
    trace(msg, l1, l2, ctx);
  }

  private void trace(String msg, IBinaryTree t1, Set t2, Context ctx){
    Set l1 = new HashSet();
    l1.add(t1);
    trace(msg, l1, t2, ctx);
  }

  private void trace(String msg, Set t1, Set t2, Context ctx){
    trace(msg + ": " + traceSymbolString(t1) + " <: " + traceSymbolString(t2));
    trace(ctx.toString());
  }

  private void trace(String msg){
    if (debug) {
      System.err.println(msg);
    }
  }

  private String traceSymbolString(Set l){
    if( l.isEmpty() ){
      return "{}";
    }
    else{
      StringBuffer buff = new StringBuffer();
      for (Iterator i = l.iterator(); i.hasNext(); ) {
        ISymbol s = (ISymbol) i.next();
        buff.append(s);
        if (i.hasNext()) {
          buff.append("|");
        }
      }
      return buff.toString();
    }
  }

  private IBinaryTree pop(Set trees) {
    Iterator i = trees.iterator();
    if (i.hasNext()) {
      IBinaryTree t = (IBinaryTree) i.next();
      i.remove();
      return t;
    }
    else {
      return null;
    }
  }
  
  public boolean isEmpty(ITreeGrammar tg) {
    ITreeGrammar emptyPattern = new TreeGrammar(
      new BinaryTreeVariable("G"),
      new IProductionRule[]{
        new ProductionRule(new BinaryTreeVariable("G"), BinaryTree.LEAF),
      });
    return contains(emptyPattern, tg);
  }

  public static RTLComparator defaultComparator = new RTLComparator();
}
