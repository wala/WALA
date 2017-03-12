/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.js.ipa.callgraph.correlations.Correlation;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.CorrelationSummary;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.EscapeCorrelation;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.ReadWriteCorrelation;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * An {@link ExtractionPolicy} that specifies that correlated pairs should be extracted.
 * 
 * In principle, extracting an arbitrary correlated pair can be very difficult. We restrict
 * our attention to the case where both read and write occur within the same block of
 * statements, with the read preceding the write. In practice, most correlations are of
 * this form.
 * 
 * TODO: The code for finding the correlated instructions is broken since Rhino only gives
 * us line number positions. Consequently, it fails to find the relevant instructions every
 * once in a while.
 * 
 * @author mschaefer
 *
 */
public class CorrelatedPairExtractionPolicy extends ExtractionPolicy {
  private static final boolean DEBUG = true;
  private final Map<CAstNode, List<ExtractionRegion>> region_map = HashMapFactory.make();
  
  private CorrelatedPairExtractionPolicy() {}

  private static void findNodesAtPos(int kind, Position pos, CAstSourcePositionMap spmap, ChildPos nodep, Set<ChildPos> res) {
    CAstNode node = nodep.getChild();
    if(node == null)
      return;
    Position ndpos = spmap.getPosition(node);
    if(ndpos != null) {
      // if we are in the wrong file or past the position pos, abort search
      if(!ndpos.getURL().equals(pos.getURL()))
        return;
      
      if(pos.getLastLine() >= 0 && ndpos.getFirstLine() > pos.getLastLine())
        return;
      
      //if(node.getKind() == kind && ndpos.getFirstLine() == pos.getFirstLine() && ndpos.getLastLine() == pos.getLastLine())
      if(node.getKind() == kind && ndpos.getFirstOffset() == pos.getFirstOffset() && ndpos.getLastOffset() == pos.getLastOffset())
        res.add(nodep);
    }
    for(int i=0;i<node.getChildCount();++i)
      findNodesAtPos(kind, pos, spmap, nodep.getChildPos(i), res);
  }
  
  private static Set<ChildPos> findNodesAtPos(int kind, Position pos, CAstEntity entity) {
    Set<ChildPos> res = HashSetFactory.make();
    CAstSourcePositionMap spmap = entity.getSourceMap();
    CAstNode ast = entity.getAST();
    for(int i=0;i<ast.getChildCount();++i)
      findNodesAtPos(kind, pos, spmap, new ChildPos(ast, i, new RootPos()), res);
    return res;
  }
  
  // create an ExtractRegion for the given correlation
  private boolean addCorrelation(CAstEntity entity, Correlation corr, CorrelationSummary correlations) {
    Position startPos = corr.getStartPosition(correlations.getPositions()),
             endPos = corr.getEndPosition(correlations.getPositions());
    
    // TODO: enable these assertions; currently we're getting getLastLine() == -1 a lot
    assert startPos.getFirstLine() != -1;
    //assert startPos.getLastLine() != -1;
    assert endPos.getFirstLine() != -1;
    //assert endPos.getLastLine() != -1;
    
    Set<ChildPos> startNodes = null,
                  endNodes = null;
    if(!entity.getPosition().getURL().equals(startPos.getURL()))
      return true;
    startNodes = findNodesAtPos(CAstNode.OBJECT_REF, startPos, entity);
    if(corr instanceof ReadWriteCorrelation) {
      endNodes = findNodesAtPos(CAstNode.ASSIGN, endPos, entity);
    } else if(corr instanceof EscapeCorrelation) {
      int arity = ((EscapeCorrelation)corr).getNumberOfArguments();
      endNodes = findNodesAtPos(CAstNode.CALL, endPos, entity);
      for(Iterator<ChildPos> iter=endNodes.iterator();iter.hasNext();) {
        CAstNode candidate = iter.next().getChild();
        // need to deduct three here: one for the function expression, one for "do"/"ctor", and one for the receiver expression
        if(candidate.getChildCount() - 3 != arity)
          iter.remove();
      }
    } else {
      throw new IllegalArgumentException("Unknown correlation type.");
    }
    if(startNodes.isEmpty() || endNodes.isEmpty()) {
      if(DEBUG)
        System.err.println("Couldn't find any " + (startNodes.isEmpty()? endNodes.isEmpty()? "boundary": "start": "end") + " nodes for correlation " + corr.pp(correlations.getPositions()));
      return true;
    }
    ChildPos startNode, endNode;
    filterNames(startNodes, corr.getIndexName());
    filterNames(endNodes, corr.getIndexName());
    Iterator<ChildPos> iter = startNodes.iterator();
    if(startNodes.size() == 2 && endNodes.equals(startNodes)) {
      startNode = iter.next();
      endNode = iter.next();
    } else if(startNodes.size() > 1 || startNodes.size() == 0) {
      if(DEBUG)
        System.err.println("Couldn't find unique start node for correlation " + corr.pp(correlations.getPositions()));
      return false;
    } else if(endNodes.size() > 1 || endNodes.size() == 0) {
      if(DEBUG)
        System.err.println("Couldn't find unique end node for correlation " + corr.pp(correlations.getPositions()));
      return false;
    } else {
      startNode = startNodes.iterator().next();
      endNode = endNodes.iterator().next();
    }
    
    List<String> locals = corr.getFlownThroughLocals().size() == 1 ? Collections.singletonList(corr.getFlownThroughLocals().iterator().next()) 
                                                                   : Collections.<String>emptyList();
    Pair<CAstNode, ? extends ExtractionRegion> region_info = findClosestContainingBlock(entity, startNode, endNode, corr.getIndexName(), locals);
    if(region_info == null) {
      if(DEBUG)
        System.err.println("Couldn't find enclosing block for correlation " + corr.pp(correlations.getPositions()));
      return false;
    }
    
    List<ExtractionRegion> regions = region_map.get(region_info.fst);
    if(regions == null)
      region_map.put(region_info.fst, regions = new LinkedList<>());
    for(int i=0;i<regions.size();++i) {
      ExtractionRegion region2 = regions.get(i);
      if(region2.getEnd() <= region_info.snd.getStart())
        continue;
      if(region2.getStart() < region_info.snd.getEnd()) {
        if(region_info.snd.getParameters().equals(region2.getParameters())) {
          region2.setStart(Math.min(region_info.snd.getStart(), region2.getStart()));
          region2.setEnd(Math.max(region_info.snd.getEnd(), region2.getEnd()));
          if(DEBUG)
            System.err.println("Successfully processed correlation " + corr.pp(correlations.getPositions()));
          return true;
        }
        if(DEBUG)
          System.err.println("Overlapping regions.");
        return false;
      }
      regions.add(i, region_info.snd);
      if(DEBUG)
        System.out.println("Successfully processed correlation " + corr.pp(correlations.getPositions()));
      return true;
    }
    if(DEBUG)
      System.out.println("Successfully processed correlation " + corr.pp(correlations.getPositions()));
    regions.add(region_info.snd);
    return true;
  }
  
  private void filterNames(Set<ChildPos> nodes, String indexName) {
    for(Iterator<ChildPos> iter=nodes.iterator();iter.hasNext();) {
      CAstNode node = iter.next().getChild();
      if(node.getKind() == CAstNode.OBJECT_REF) {
        CAstNode index = node.getChild(1);
        if(index.getKind() != CAstNode.VAR || !index.getChild(0).getValue().equals(indexName)) {
          iter.remove();
        }
      }
    }
  }

  private Pair<CAstNode, ? extends ExtractionRegion> findClosestContainingBlock(CAstEntity entity, ChildPos startNode, ChildPos endNode, String parmName, List<String> locals) {
    ChildPos pos = startNode;
    CAstNode block = null;
    int start = -1, end = 0;
    int start_inner = -1, end_inner = -1;
    
    do {
      if(pos != startNode && pos.getParentPos() instanceof ChildPos)
        pos = (ChildPos)pos.getParentPos();
      // find the next closest block around the node at position "pos"
      while(pos.getParent().getKind() != CAstNode.BLOCK_STMT) {
        if(pos.getParentPos() instanceof ChildPos)
          pos = (ChildPos)pos.getParentPos();
        else
          return null;
      }
      block = pos.getParent();
      start = pos.getIndex();
      end = getCoveringChildIndex(block, start, endNode.getChild()) + 1;
    } while(end == 0);
    
    // expand region to include forward goto targets
    CAstControlFlowMap cfg = entity.getControlFlow();
    for(int i=start;i<end;++i) {
      CAstNode stmt = block.getChild(i);
      for(Object targetLabel : cfg.getTargetLabels(stmt)) {
        CAstNode target = cfg.getTarget(stmt, targetLabel);
        int targetIndex = getCoveringChildIndex(block, start, target);
        if(targetIndex >= end)
          end = targetIndex + 1;
      }
    }
    
    // special hack to handle "var p = ..., x = y[p];", where startNode = "y[p]"
    if(block.getChild(0).getKind() == CAstNode.BLOCK_STMT && start == 0) {
      for(start_inner=0;start_inner<block.getChild(0).getChildCount();++start_inner)
        if(NodePos.inSubtree(startNode.getChild(), block.getChild(0).getChild(start_inner)))
          return Pair.make(block, new TwoLevelExtractionRegion(start, end, start_inner, end_inner, Collections.singletonList(parmName), locals));
    }
    // special hack to handle the case where we're extracting the body of a local scope
    else if(block.getChild(start).getKind() == CAstNode.LOCAL_SCOPE && end == start+1) {
      return Pair.make(block, new TwoLevelExtractionRegion(start, end, 0, -1, Collections.singletonList(parmName), locals));
    }

    return Pair.make(block, new ExtractionRegion(start, end, Collections.singletonList(parmName), locals));
  }
  
  private static int getCoveringChildIndex(CAstNode parent, int start, CAstNode child) {
    for(int i=start;i<parent.getChildCount();++i)
      if(NodePos.inSubtree(child, parent.getChild(i)))
        return i;
    return -1;
  }
  
  private static CorrelatedPairExtractionPolicy addCorrelations(CAstEntity entity, Map<Position, CorrelationSummary> summaries, CorrelatedPairExtractionPolicy policy) {
    // add correlations for this entity
    if(entity.getAST() != null && summaries.containsKey(entity.getPosition())) {
      CorrelationSummary correlations = summaries.get(entity.getPosition());
      for(Correlation corr : correlations.getCorrelations())
        policy.addCorrelation(entity, corr, correlations);
    }
    // recursively add correlations for scoped entities
    Map<CAstNode, Collection<CAstEntity>> allScopedEntities = entity.getAllScopedEntities();
    for(Collection<CAstEntity> scopedEntities : allScopedEntities.values())
      for(CAstEntity scopedEntity : scopedEntities)
        if(addCorrelations(scopedEntity, summaries, policy) == null)
          return null;
    return policy;
  }
  
  public static CorrelatedPairExtractionPolicy make(CAstEntity entity, Map<IMethod, CorrelationSummary> summaries) {
    CorrelatedPairExtractionPolicy policy = new CorrelatedPairExtractionPolicy();
    Map<Position, CorrelationSummary> summary_map = HashMapFactory.make();
    for(Map.Entry<IMethod, CorrelationSummary> e : summaries.entrySet()) {
      if(e.getKey() instanceof AstMethod) {
        Position pos = ((AstMethod)e.getKey()).getSourcePosition();
        if(pos != null)
          summary_map.put(pos, e.getValue());
      }
    }
    return addCorrelations(entity, summary_map, policy);
  }
  
  @Override
  public List<ExtractionRegion> extract(CAstNode node) {
    return region_map.get(node);
  }
}
