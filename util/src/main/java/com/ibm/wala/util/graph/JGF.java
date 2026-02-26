package com.ibm.wala.util.graph;

import org.json.JSONArray;
import org.json.JSONObject;

public class JGF {

  public interface EntityTypes<T> {
    JSONObject obj(T entity);

    String label(Graph<T> entity);

    String label(T from, T to);
  }

  public static <T> JSONObject toJGF(NumberedGraph<T> G, EntityTypes<T> labels) {
    JSONObject jgf = new JSONObject();
    jgf.put("directed", true);
    jgf.put("label", labels.label(G));
    
    JSONObject nodes = new JSONObject();
    jgf.put("nodes", nodes);
    for(T n : G) {
      JSONObject node = labels.obj(n);
      nodes.put(""+G.getNumber(n), node);
    }

    JSONArray edges = new JSONArray();
    jgf.put("edges", edges);
    for(T from : G) {
      G.getSuccNodeNumbers(from).foreach(to -> { 
        JSONObject edge = new JSONObject();
        edges.put(edge);
        edge.put("source", ""+G.getNumber(from));
        edge.put("target", ""+to);
        edge.put("label", labels.label(from, G.getNode(to)));
      });
    }
    
    return jgf;
  }
}
