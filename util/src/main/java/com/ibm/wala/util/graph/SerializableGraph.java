package com.ibm.wala.util.graph;

import java.io.Serializable;

public interface SerializableGraph<T extends Serializable> extends Graph<T>, Serializable {}
