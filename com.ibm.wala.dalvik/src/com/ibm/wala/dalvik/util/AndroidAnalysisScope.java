package com.ibm.wala.dalvik.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.dex.util.config.DexAnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

public class AndroidAnalysisScope {
	
	public final static String STD_EXCLUSION_REG_EXP =
			"java\\/awt\\/.*\n"
			+ "javax\\/swing\\/.*\n"
			+ "java\\/nio\\/.*\n"
			+ "java\\/net\\/.*\n"
			+ "sun\\/awt\\/.*\n"
			+ "sun\\/swing\\/.*\n"
			+ "com\\/sun\\/.*\n"
			+ "sun\\/.*\n"
			+ "apple\\/awt\\/.*\n"
			+ "com\\/apple\\/.*\n"
			+ "org\\/omg\\/.*\n"
			+ "javax\\/.*\n";
	
	
	public static AnalysisScope setUpAndroidAnalysisScope(String androidLib, String classpath) throws IOException {
		AnalysisScope scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classpath, STD_EXCLUSION_REG_EXP);
		setUpAnalysisScope(scope, new File(androidLib).toURI());
		return scope;
	}
	
	public static AnalysisScope setUpAndroidAnalysisScope(String androidLib, String classpath, String exclusions) throws IOException {
		AnalysisScope scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classpath, exclusions);
		setUpAnalysisScope(scope, new File(androidLib).toURI());
		return scope;
	}
	
	public static AnalysisScope setUpAndroidAnalysisScope(URI androidLib, URI classpath, File exclusions) throws IOException {
		AnalysisScope scope = DexAnalysisScopeReader.makeAndroidBinaryAnalysisScope(classpath, exclusions);
		setUpAnalysisScope(scope, androidLib);
		return scope;
	}
	
	private static void setUpAnalysisScope(AnalysisScope scope, URI androidLib) throws IOException {
		scope.setLoaderImpl(ClassLoaderReference.Application,
				"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

		scope.setLoaderImpl(ClassLoaderReference.Primordial,
				"com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
		
		// TODO: this check is case-sensitive :(

		if (androidLib.getPath().endsWith(".dex")) { 
			Module dexMod = new DexFileModule(new File(androidLib));
			
//			Iterator<ModuleEntry> mitr = dexMod.getEntries();
//			while (mitr.hasNext()) {
//				ModuleEntry moduleEntry = (ModuleEntry) mitr.next();
//				logger.error("dex module: {}", moduleEntry.getName());
//			}

			scope.addToScope(ClassLoaderReference.Primordial, dexMod);
		} else {
			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
				androidLib)));
		}
	}
}
