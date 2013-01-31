/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */
package org.scandroid;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author creswick
 *
 */
public class JarInspector {

	private static String OUTPUT_DIR = "results";

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		final String appJar = args[0];
		final Multimap<String, String> pkgMethods = getMethodsByPackage(appJar);
		
		int count = 0;
		for (final String pkg : pkgMethods.keySet()) {
			System.out.println(pkg+": "+pkgMethods.get(pkg).size());
			count += pkgMethods.get(pkg).size();
		}
		System.out.println("Methods: "+count);
	}

	/**
	 * Calculate a map of package name to method descriptor for all interesting
	 * methods in the supplied jar file (which must also be on the classpath)
	 * 
	 * @param jarFile
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public static Multimap<String, String> getMethodsByPackage(String jarFile) 
			throws ClassNotFoundException, IOException {
		Multimap<String, String> pkgMap = HashMultimap.create();
		List<String> classes = getClasses(jarFile);
		
		for (String c : classes ) {
			//System.out.println(c);
			Method[] methods;
			Class<?> clazz;
			try {
				clazz = Class.forName(c);
		
				
				// skip a bunch of class types that don't really have code:
				if (clazz.isEnum() 
				    || clazz.isInterface() 
				    || clazz.isAnnotation() 
					|| clazz.isAnonymousClass()
					|| clazz.isPrimitive()
					|| clazz.isSynthetic()
					) {
					continue;
				}
				
				methods = clazz.getMethods();
			} catch (Throwable e) {
				System.err.println("Could not load class: "+c);
				e.printStackTrace();
				continue;
			}
			for (Method m : methods) {				
				// skip methods that are declared on other classes (eg: java/lang/Object)
				if ( !m.getDeclaringClass().equals(clazz)
						// skip bridge methods (what /are/ these?)
						|| m.isBridge()
						// skip synthetic methods (native?)
						|| m.isSynthetic()
						// skip varargs -- not sure summaries can support that.
						|| m.isVarArgs()) { 
					continue;
				}
                
				// only look at public methods:
				if (! Modifier.isPublic(m.getModifiers()) ){
					continue;
				}
				
				StringBuilder desc = new StringBuilder(c);

				desc.append("."+m.getName());
				
				desc.append("(");
				for( Class<?> pType : m.getParameterTypes() ) {
					desc.append(toDescStr(pType));
				}
				desc.append(")");
				
				desc.append(toDescStr(m.getReturnType()));
				
				String packageName = clazz.getPackage().getName();
				pkgMap.put(packageName, desc.toString());

			}
		}
		return pkgMap;
	}
	
	private static String toDescStr(Class<?> pType) {
		Map<Class<?>, String> primitives = Maps.newHashMap(); 
		primitives.put(Void.TYPE, "V");
		primitives.put(float.class, "F");
        primitives.put(double.class, "D");
        primitives.put(byte.class, "B");
        primitives.put(short.class, "S");
        primitives.put(int.class, "I");
        primitives.put(long.class, "J");
        primitives.put(boolean.class, "Z");
        primitives.put(char.class, "C");
		
		if (primitives.containsKey(pType)) {
			return (String)primitives.get(pType);
		}
	
		StringBuilder typeName = new StringBuilder();
		if (pType.isArray()){
			typeName.append(pType.getName().replace('.', '/'));
		} else {
			typeName.append("L");
			typeName.append(pType.getName().replace('.', '/'));
			typeName.append(";");
		}
		return typeName.toString();
	}

	private static List<String> getClasses(String jarFile) throws IOException {
		List<String> classes = Lists.newArrayList();
		
		Multimap<String, String> packages = getPackages(jarFile);
		for(String pkg : packages.keySet() ){
			//System.out.println(pkg);
			for (String c : packages.get(pkg)) {
				classes.add(pkg+"."+c);
			}
		}
		return classes;
	}

	private static Multimap<String, String> getPackages(String appJar) throws IOException {
		Multimap<String, String> packages = HashMultimap.create();
		
		JarFile jf = new JarFile(appJar);
		Enumeration<JarEntry> entries = jf.entries();
		while (entries.hasMoreElements()) {
			JarEntry je = entries.nextElement();
			String name = je.getName();
			
			// skip everything but class files:
			if ( !name.endsWith(".class") ) {
				continue;
			}
			
			int slashIdx = name.lastIndexOf("/");
			String className = name.substring(slashIdx + 1, name.lastIndexOf('.'));
			String pkgName = name.substring(0, slashIdx);
			
			String dottedPkg = pkgName.replace('/', '.');

			packages.put(dottedPkg, className);
		}
		
		return packages;
	}
}
