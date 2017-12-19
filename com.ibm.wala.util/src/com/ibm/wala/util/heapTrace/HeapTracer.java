/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.heapTrace;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * Simple utility that uses reflection to trace memory
 */
public class HeapTracer {

    private static final boolean DEBUG = false;

    /**
     * Names of all classes considered for analysis
     */
    final private static String[] rootClasses = generateRootClassesFromWorkspace();

    /**
     * Object instances that should be considered roots of the heap trace
     */
    private final Collection<?> rootInstances;

    /**
     * Stack of instance objects discovered but not yet traced.
     */
    private final Stack<Object> scalarWorkList = new Stack<>();

    /**
     * Stack of array objects discovered but not yet traced.
     */
    private final Stack<Object> arrayWorkList = new Stack<>();

    /**
     * How many bytes do we assume the JVM wastes on each instance?
     */
    private static final int BYTES_IN_HEADER = 12;

    /**
     * Should all static fields be considered roots of the heap traversal?
     */
    private final boolean traceStatics;

    /**
     * Map: Class -&gt; Integer, the size of each class
     */
    private final HashMap<Class<?>, Integer> sizeMap = HashMapFactory.make();

    private static final Object DUMMY = new Object();

    /**
     * Classes that we should understand because they're internal to a container
     * object
     */
    private static final HashSet<Class<?>> internalClasses = HashSetFactory
	    .make();
    static {
	try {
	    internalClasses.add(Class.forName("java.lang.String"));
	    internalClasses.add(Class.forName("java.util.HashMap$Entry"));
	    internalClasses.add(Class.forName("java.util.HashMap"));
	    internalClasses.add(Class.forName("java.util.HashSet"));
	    internalClasses.add(Class.forName("java.util.Vector"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.collections.SmallMap"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.collections.SimpleVector"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.intset.SimpleIntVector"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.intset.BasicNaturalRelation"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.intset.SparseIntSet"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.collections.SparseVector"));
	    internalClasses
		    .add(Class
			    .forName("com.ibm.wala.util.intset.MutableSharedBitVectorIntSet"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.intset.MutableSparseIntSet"));
	    internalClasses.add(Class
		    .forName("com.ibm.wala.util.collections.TwoLevelVector"));
	    internalClasses
		    .add(Class
			    .forName("com.ibm.wala.util.graph.impl.DelegatingNumberedNodeManager"));
	    internalClasses
		    .add(Class
			    .forName("com.ibm.wala.util.graph.impl.SparseNumberedEdgeManager"));
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    Assertions.UNREACHABLE();
	}
    }

    /**
     * @param traceStatics
     *            Should all static fields be considered roots of the heap
     *            traversal?
     */
    HeapTracer(boolean traceStatics) {
	rootInstances = Collections.emptySet();
	this.traceStatics = traceStatics;
    }

    /**
     * @param c
     * @param traceStatics
     */
    public HeapTracer(Collection<?> c, boolean traceStatics) {
	rootInstances = c;
	this.traceStatics = traceStatics;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	try {
	    Result r = (new HeapTracer(true)).perform();
	    System.err.println(r.toString());
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    /**
     * @return the name of each class that's in the classpath
     */
    private static String[] generateRootClassesFromWorkspace() {
	String classpath = System.getProperty("java.class.path");
	Object[] binDirectories = extractBinDirectories(classpath);
	HashSet<String> classFileNames = HashSetFactory.make();
	for (Object binDirectorie : binDirectories) {
	    String dir = (String) binDirectorie;
	    File fdir = new File(dir);
	    classFileNames.addAll(findClassNames(dir, fdir));
	}
	String[] result = new String[classFileNames.size()];
	Iterator<String> it = classFileNames.iterator();
	for (int i = 0; i < result.length; i++) {
	    result[i] = it.next();
	}
	return result;
    }

    /**
     * @param fdir
     * @return {@link Collection}&lt;{@link String}&gt; representing the class names in a particular
     *         file
     */
    /**
     * @param rootDir
     *            root of the classpath governing file f
     * @param f
     *            a File or directory
     * @return {@link Collection}&lt;{@link String}&gt; representing the class names in f
     */
    private static Collection<String> findClassNames(String rootDir, File f) {
	HashSet<String> result = HashSetFactory.make();
	if (f.isDirectory()) {
	    File[] files = f.listFiles();
	    for (File file : files) {
		result.addAll(findClassNames(rootDir, file));
	    }
	} else {
	    if (f.getName().indexOf(".class") > 0) {
		String p = f.getAbsolutePath();
		p = p.substring(rootDir.length() + 1);
		// trim the last 6 characters which hold ".class"
		p = p.substring(0, p.length() - 6);
		p = p.replace('\\', '.');
		return Collections.singleton(p);
	    }
	}
	return result;
    }

    /**
     * @param classpath
     * @return set of strings that are names of directories that contain "bin"
     */
    private static Object[] extractBinDirectories(String classpath) {
	StringTokenizer t = new StringTokenizer(classpath, ";");
	HashSet<String> result = HashSetFactory.make();
	while (t.hasMoreTokens()) {
	    String n = t.nextToken();
	    if (n.indexOf("bin") > 0) {
		result.add(n);
	    }
	}
	return result.toArray();
    }

    /**
     * Trace the heap and return the results
     */
    public Result perform() throws ClassNotFoundException,
	    IllegalArgumentException, IllegalAccessException {
	Result result = new Result();
	IdentityHashMap<Object, Object> objectsVisited = new IdentityHashMap<>();
	if (traceStatics) {
	    for (String rootClasse : rootClasses) {
		Class<?> c = Class.forName(rootClasse);
		Field[] fields = c.getDeclaredFields();
		for (Field field : fields) {
		    if (isStatic(field)) {
			traverse(field, result, objectsVisited);
		    }
		}
	    }
	}
	for (Object instance : rootInstances) {
	    Class<?> c = instance.getClass();
	    Set<Field> fields = getAllInstanceFields(c);
	    for (Field f : fields) {
		traverse(f, instance, result, objectsVisited);
	    }
	}

	return result;
    }

    /**
     * @param o
     * @return the estimated size of the object
     */
    private static int computeSizeOf(Object o) {
	int result = BYTES_IN_HEADER;
	Class<?> c = o.getClass();
	if (c.isArray()) {
	    Class<?> elementType = c.getComponentType();
	    int length = Array.getLength(o);
	    result += length * sizeOfSlot(elementType);
	} else if (c.isPrimitive()) {
	    throw new Error();
	} else {
	    Collection<Field> fields = getAllInstanceFields(c);
	    for (Field f : fields) {
		result += sizeOfSlot(f.getType());
	    }
	}
	return result;
    }

    /**
     * @param o
     * @return the estimated size of the object
     */
    private int sizeOf(Object o) {
	Class<?> c = o.getClass();
	if (c.isArray()) {
	    return computeSizeOf(o);
	} else {
	    Integer S = sizeMap.get(c);
	    if (S == null) {
		S = Integer.valueOf(computeSizeOf(o));
		sizeMap.put(c, S);
	    }
	    return S.intValue();
	}
    }

    /**
     * @param c
     * @return size of a field of type c, in bytes
     */
    private static int sizeOfSlot(Class<?> c) {
	if (!c.isPrimitive()) {
	    return 4;
	} else {
	    if (c.equals(Boolean.TYPE) || c.equals(Character.TYPE)
		    || c.equals(Byte.TYPE)) {
		return 1;
	    } else if (c.equals(Short.TYPE)) {
		return 2;
	    } else if (c.equals(Integer.TYPE) || c.equals(Float.TYPE)) {
		return 4;
	    } else if (c.equals(Long.TYPE) || c.equals(Double.TYPE)) {
		return 8;
	    } else {
		throw new Error();
	    }
	}
    }

    /**
     * Traverse the heap starting at a static field
     * 
     * @param result
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    private void traverse(Field root, Result result,
	    IdentityHashMap<Object, Object> objectsVisited)
	    throws IllegalArgumentException, IllegalAccessException {
	if (DEBUG) {
	    System.err.println(("traverse root " + root));
	}
	root.setAccessible(true);
	Object contents = root.get(null);
	if (contents != null && (objectsVisited.get(contents) == null)) {
	    objectsVisited.put(contents, DUMMY);
	    Class<?> c = contents.getClass();
	    if (c.isArray()) {
		result.registerReachedFrom(root, root, contents);
		arrayWorkList.push(Pair.make(root, contents));
	    } else {
		result.registerReachedFrom(root, root, contents);
		if (internalClasses.contains(c)) {
		    contents = Pair.make(root, contents);
		}
		scalarWorkList.push(contents);
	    }
	}

	drainWorkLists(root, result, objectsVisited);
    }

    private void traverse(Field root, Object instance, Result result,
	    IdentityHashMap<Object, Object> objectsVisited)
	    throws IllegalArgumentException, IllegalAccessException {

	traverseFieldOfScalar(root, root, instance, null, objectsVisited,
		result);
	drainWorkLists(root, result, objectsVisited);
    }

    @SuppressWarnings("rawtypes")
    private void drainWorkLists(Field root, Result result,
	    IdentityHashMap<Object, Object> objectsVisited)
	    throws IllegalAccessException {
	while (!scalarWorkList.isEmpty() || !arrayWorkList.isEmpty()) {
	    if (!scalarWorkList.isEmpty()) {
		Object scalar = scalarWorkList.pop();
		if (scalar instanceof Pair) {
		    Pair p = (Pair) scalar;
		    traverseScalar(root, p.snd, p.fst, result, objectsVisited);
		} else {
		    traverseScalar(root, scalar, null, result, objectsVisited);
		}
	    }
	    if (!arrayWorkList.isEmpty()) {
		Pair p = (Pair) arrayWorkList.pop();
		traverseArray(root, p.snd, p.fst, result, objectsVisited);
	    }
	}
    }

    private void traverseArray(Field root, Object array, Object container,
	    Result result, IdentityHashMap<Object, Object> objectsVisited)
	    throws IllegalArgumentException {
	if (DEBUG) {
	    System.err.println(("traverse array " + array.getClass()));
	}
	Class<?> elementKlass = array.getClass().getComponentType();
	if (elementKlass.isPrimitive()) {
	    return;
	}
	assert container != null;
	int length = Array.getLength(array);
	for (int i = 0; i < length; i++) {
	    Object contents = Array.get(array, i);

	    if (contents != null && (objectsVisited.get(contents) == null)) {
		objectsVisited.put(contents, DUMMY);
		Class<?> klass = contents.getClass();
		if (klass.isArray()) {
		    result.registerReachedFrom(root, container, contents);
		    contents = Pair.make(container, contents);
		    arrayWorkList.push(contents);
		} else {
		    result.registerReachedFrom(root, container, contents);
		    contents = Pair.make(container, contents);
		    scalarWorkList.push(contents);
		}
	    }
	}
    }

    private void traverseScalar(Field root, Object scalar, Object container,
	    Result result, IdentityHashMap<Object, Object> objectsVisited)
	    throws IllegalArgumentException, IllegalAccessException {

	Class<?> c = scalar.getClass();
	if (DEBUG) {
	    System.err.println(("traverse scalar " + c));
	}
	Field[] fields = getAllReferenceInstanceFields(c);
	for (Field field : fields) {
	    traverseFieldOfScalar(root, field, scalar, container,
		    objectsVisited, result);
	}
    }

    private final Object OK = new Object();

    private final Object BAD = new Object();

    private final HashMap<Package, Object> packageStatus = HashMapFactory
	    .make();

    private final boolean isInBadPackage(Class<?> c) {
	Package p = c.getPackage();
	if (p == null) {
	    return false;
	}
	Object status = packageStatus.get(p);
	if (status == OK) {
	    return false;
	} else if (status == BAD) {
	    return true;
	} else {
	    if (p.getName() != null && p.getName().indexOf("sun.reflect") != -1) {
		if (DEBUG) {
		    System.err.println(("making " + p + " a BAD package"));
		}
		packageStatus.put(p, BAD);
		return true;
	    } else {
		if (DEBUG) {
		    System.err.println(("making " + p + " an OK package"));
		}
		packageStatus.put(p, OK);
		return false;
	    }
	}
    }

    /**
     * @param root
     * @param f
     * @param scalar
     * @param container
     * @param objectsVisited
     * @param result
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private void traverseFieldOfScalar(Field root, Field f, Object scalar,
	    Object container, IdentityHashMap<Object, Object> objectsVisited,
	    Result result) throws IllegalArgumentException,
	    IllegalAccessException {
	// The following atrocious hack is needed to prevent the IBM 141 VM
	// from crashing
	if (isInBadPackage(f.getType())) {
	    return;
	}
	if (container == null) {
	    container = f;
	}
	f.setAccessible(true);
	Object contents = f.get(scalar);
	if (contents != null && (objectsVisited.get(contents) == null)) {
	    try {
		objectsVisited.put(contents, DUMMY);
	    } catch (Exception e) {
		e.printStackTrace();
		return;
	    }
	    Class<?> klass = contents.getClass();
	    if (klass.isArray()) {
		result.registerReachedFrom(root, container, contents);
		contents = Pair.make(container, contents);
		arrayWorkList.push(contents);
	    } else {
		result.registerReachedFrom(root, container, contents);
		if (internalClasses.contains(klass)) {
		    contents = Pair.make(container, contents);
		}
		scalarWorkList.push(contents);
	    }
	}
    }

    private static HashSet<Field> getAllInstanceFields(Class<?> c) {
	HashSet<Field> result = HashSetFactory.make();
	Class<?> klass = c;
	while (klass != null) {
	    Field[] fields = klass.getDeclaredFields();
	    for (int i = 0; i < fields.length; i++) {
		if (!isStatic(fields[i])) {
		    result.add(fields[i]);
		}
	    }
	    klass = klass.getSuperclass();
	}
	return result;
    }

    final private HashMap<Class<?>, Field[]> allReferenceFieldsCache = HashMapFactory
	    .make();

    /**
     * @return Field[] representing reference instance fields of a class
     */
    private Field[] getAllReferenceInstanceFields(Class<?> c) {
	if (allReferenceFieldsCache.containsKey(c))
	    return allReferenceFieldsCache.get(c);
	else {
	    HashSet<Field> s = HashSetFactory.make();
	    Class<?> klass = c;
	    while (klass != null) {
		Field[] fields = klass.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
		    if (!isStatic(fields[i])) {
			Class<?> fc = fields[i].getType();
			if (!fc.isPrimitive()) {
			    s.add(fields[i]);
			}
		    }
		}
		klass = klass.getSuperclass();
	    }
	    Field[] result = new Field[s.size()];
	    Object[] temp = s.toArray();
	    for (int i = 0; i < result.length; i++) {
		result[i] = (Field) temp[i];
	    }
	    allReferenceFieldsCache.put(c, result);
	    return result;
	}
    }

    private static boolean isStatic(Field field) {
	return Modifier.isStatic(field.getModifiers());
    }

    public static void analyzeLeaks() {
	analyzeLeaks(true);
    }

    /**
     * Trace the heap and dump the output to the tracefile
     * 
     * @param traceStatics
     *            should all static fields be considered roots?
     */
    public static void analyzeLeaks(boolean traceStatics) {
	try {
	    System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
	    long t = Runtime.getRuntime().totalMemory();
	    long f = Runtime.getRuntime().freeMemory();
	    System.err.println(("Total Memory:     " + t));
	    System.err.println(("Occupied Memory:  " + (t - f)));
	    HeapTracer.Result r = (new HeapTracer(traceStatics)).perform();
	    System.err.println("HeapTracer Analysis:");
	    System.err.println(r.toString());
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
    }

    /**
     * Trace the heap and dump the output to the tracefile
     * 
     * @param instances
     *            instances to be considered roots of the heap traversal
     * @param traceStatics
     *            should all static fields be considered roots?
     */
    public static HeapTracer.Result traceHeap(Collection<?> instances,
	    boolean traceStatics) {
	try {
	    System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
	    long t = Runtime.getRuntime().totalMemory();
	    long f = Runtime.getRuntime().freeMemory();
	    System.err.println(("Total Memory:     " + t));
	    System.err.println(("Occupied Memory:  " + (t - f)));
	    HeapTracer.Result r = (new HeapTracer(instances, traceStatics))
		    .perform();
	    System.err.println("HeapTracer Analysis:");
	    System.err.println(r.toString());
	    return r;
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * @author sfink
     * 
     *         Statistics about objects reached from a single root
     */
    class Demographics {
	/**
	 * mapping: Object (key) -&gt; Integer (number of instances in a partition)
	 */
	private final HashMap<Object, Integer> instanceCount = HashMapFactory
		.make();

	/**
	 * mapping: Object (key) -&gt; Integer (bytes)
	 */
	private final HashMap<Object, Integer> sizeCount = HashMapFactory
		.make();

	/**
	 * Total number of instances discovered
	 */
	private int totalInstances = 0;

	/**
	 * Total number of bytes discovered
	 */
	private int totalSize = 0;

	/**
	 * @param key
	 *            a name for the heap partition to which o belongs
	 * @param o
	 *            the object to register
	 */
	public void registerObject(Object key, Object o) {
	    Integer I = instanceCount.get(key);
	    int newCount = (I == null) ? 1 : I.intValue() + 1;
	    instanceCount.put(key, Integer.valueOf(newCount));
	    totalInstances++;

	    I = sizeCount.get(key);
	    int s = sizeOf(o);
	    int newSizeCount = (I == null) ? s : I.intValue() + s;
	    sizeCount.put(key, Integer.valueOf(newSizeCount));
	    totalSize += s;
	}

	@Override
	public String toString() {
	    StringBuffer result = new StringBuffer();
	    result.append("Totals: " + totalInstances + " " + totalSize + "\n");
	    TreeSet<Object> sorted = new TreeSet<>(new SizeComparator());
	    sorted.addAll(instanceCount.keySet());
	    for (Object key : sorted) {
		Integer I = instanceCount.get(key);
		Integer bytes = sizeCount.get(key);
		result.append("  ").append(I).append("   ").append(bytes)
			.append("   ");
		result.append(bytes.intValue() / I.intValue()).append("   ");
		result.append(key);
		result.append("\n");
	    }
	    return result.toString();
	}

	/**
	 * compares two keys based on the total size of the heap traced from
	 * that key
	 */
	private class SizeComparator implements Comparator<Object> {
	    /*
	     * @see java.util.Comparator#compare(java.lang.Object,
	     * java.lang.Object)
	     */
	    @Override
      public int compare(Object o1, Object o2) {
		Integer i1 = sizeCount.get(o1);
		Integer i2 = sizeCount.get(o2);
		return i2.intValue() - i1.intValue();
	    }
	}

	/**
	 * @return Returns the totalSize.
	 */
	public int getTotalSize() {
	    return totalSize;
	}

	/**
	 * @return Returns the totalInstances.
	 */
	public int getTotalInstances() {
	    return totalInstances;
	}
    }

    /**
     * @author sfink
     * 
     *         Results of the heap trace
     */
    public class Result {

	/**
	 * a mapping from Field (static field roots) -&gt; Demographics object
	 */
	private final HashMap<Field, Demographics> roots = HashMapFactory
		.make();

	/**
	 * @param root
	 * @return the Demographics object tracking objects traced from that
	 *         root
	 */
	private Demographics findOrCreateDemographics(Field root) {
	    Demographics d = roots.get(root);
	    if (d == null) {
		d = new Demographics();
		roots.put(root, d);
	    }
	    return d;
	}

	public void registerReachedFrom(Field root, Object predecessor,
		Object contents) {
	    Demographics d = findOrCreateDemographics(root);
	    d.registerObject(Pair.make(predecessor, contents.getClass()),
		    contents);
	}

	public int getTotalSize() {
	    int totalSize = 0;
	    for (Demographics d : roots.values()) {
totalSize += d.getTotalSize();
  }
	    return totalSize;
	}

	@Override
	public String toString() {
	    StringBuffer result = new StringBuffer();
	    result.append("Assuming " + BYTES_IN_HEADER
		    + " header bytes per object\n");
	    int totalInstances = 0;
	    int totalSize = 0;
	    for (Demographics d : roots.values()) {
totalInstances += d.getTotalInstances();
totalSize += d.getTotalSize();
  }
	    result.append("Total instances: " + totalInstances + "\n");
	    result.append("Total size(bytes): " + totalSize + "\n");

	    TreeSet<Field> sortedDemo = new TreeSet<>(new SizeComparator());
	    sortedDemo.addAll(roots.keySet());
	    for (Field field : sortedDemo) {
		Object root = field;
		Demographics d = roots.get(root);
		if (d.getTotalSize() > 10000) {
		    result.append(" root: ").append(root).append("\n");
		    result.append(d);
		}
	    }

	    return result.toString();
	}

	/**
	 * compares two keys based on the total size of the heap traced from
	 * that key
	 */
	private class SizeComparator implements Comparator<Field> {
	    /*
	     * @see java.util.Comparator#compare(java.lang.Object,
	     * java.lang.Object)
	     */
	    @Override
      public int compare(Field o1, Field o2) {
		Demographics d1 = roots.get(o1);
		Demographics d2 = roots.get(o2);
		return d2.getTotalSize() - d1.getTotalSize();
	    }
	}
    }

}
