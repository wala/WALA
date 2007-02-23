package com.ibm.wala.util.scope;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.Entrypoints;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Trace;

/**
 * This class represents entry points (@link
 * com.ibm.domo.ipa.callgraph.EnryPoints) of JUnit test methods. JUnit test
 * methods are those invoked by the JUnit framework reflectively The entry
 * points can be used to specify entry points of a call graph (through
 * 
 * @link com.ibm.domo.ipa.callgraph.AnalysisOptions).
 * 
 * @author aying
 */
public class JUnitEntryPoints {

  private static final boolean DEBUG = false;

  /**
   * Construct JUnit entrypoints for all the JUnit test methods in the given
   * scope.
   */
  public static Entrypoints make(ClassHierarchy cha) {

    final HashSet<Entrypoint> result = new HashSet<Entrypoint>();
    for (IClass klass : cha) {

      if (klass.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {

        try {
          // if the class is a subclass of the Junit TestCase
          if (isJUnitTestCase(klass)) {

            System.out.println("application class: " + klass);

            // return all the tests methods
            Collection methods = klass.getAllMethods();
            Iterator methodsIt = methods.iterator();

            while (methodsIt.hasNext()) {
              IMethod m = (IMethod) methodsIt.next();
              if (isJUnitMethod(m)) {
                result.add(new DefaultEntrypoint(m, cha));
                System.out.println("- adding test method as entry point: " + m.getName().toString());
              }
            }
          }
        } catch (ClassHierarchyException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
    return new Entrypoints() {
      public Iterator<Entrypoint> iterator() {
        return result.iterator();
      }
    };
  }

  /**
   * Construct JUnit entrypoints for the specified test method in a scope.
   */
  public static Entrypoints makeOne(ClassHierarchy cha, String targetPackageName, String targetSimpleClassName,
      String targetMethodName) {

    // assume test methods don't have parameters
    final Atom targetPackageAtom = Atom.findOrCreateAsciiAtom(targetPackageName);
    final Atom targetSimpleClassAtom = Atom.findOrCreateAsciiAtom(targetSimpleClassName);
    final TypeName targetType = TypeName.findOrCreateClass(targetPackageAtom, targetSimpleClassAtom);
    final Atom targetMethodAtom = Atom.findOrCreateAsciiAtom(targetMethodName);

    if (DEBUG) {
      Trace.println("finding entrypoint " + targetMethodAtom + " in " + targetType);
    }

    final Set<Entrypoint> entryPts = new HashSet<Entrypoint>();

    // TODO: improve this so that we don't need to check all the
    // classes and method to find a match
    try {
      for (IClass klass : cha) {
        TypeName klassType = klass.getName();
        if (klassType.equals(targetType) && isJUnitTestCase(klass)) {
          if (DEBUG) {
            Trace.println("found test class");
          }
          // add entry point corresponding to the target method
          for (Iterator methodsIt = klass.getDeclaredMethods().iterator(); methodsIt.hasNext();) {
            IMethod method = (IMethod) methodsIt.next();
            Atom methodAtom = method.getName();
            if (methodAtom.equals(targetMethodAtom)) {
              entryPts.add(new DefaultEntrypoint(method, cha));
              System.out.println("- adding entry point of the call graph: " + methodAtom.toString());
            }
          }

          // add entry points of setUp/tearDown methods
          Set<IMethod> setUpTearDowns = getSetUpTearDownMethods(klass);
          for (IMethod m : setUpTearDowns) {
            entryPts.add(new DefaultEntrypoint(m, cha));
          }
        }
      }
    } catch (ClassHierarchyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new Entrypoints() {
      public Iterator<Entrypoint> iterator() {
        return entryPts.iterator();
      }
    };
  }

  /**
   * Check if the given class is a JUnit test class. A JUnit test class is a
   * subclass of junit.framework.TestCase or junit.framework.TestSuite.
   */
  public static boolean isJUnitTestCase(IClass klass) throws ClassHierarchyException {
    final Atom junitPackage = Atom.findOrCreateAsciiAtom("junit/framework");
    final Atom junitClass = Atom.findOrCreateAsciiAtom("TestCase");
    final Atom junitSuite = Atom.findOrCreateAsciiAtom("TestSuite");
    final TypeName junitTestCaseType = TypeName.findOrCreateClass(junitPackage, junitClass);
    final TypeName junitTestSuiteType = TypeName.findOrCreateClass(junitPackage, junitSuite);

    IClass ancestor = klass.getSuperclass();
    while (ancestor != null) {
      TypeName t = ancestor.getName();
      if (t.equals(junitTestCaseType) || t.equals(junitTestSuiteType)) {
        return true;
      }
      ancestor = ancestor.getSuperclass();
    }
    return false;
  }

  /**
   * Check if the given method is a JUnit test method, assuming that it is
   * declared in a JUnit test class. A method is a JUnit test method if the name
   * has the prefix "test", or its name is "setUp" or "tearDown".
   */
  public static boolean isJUnitMethod(IMethod m) {
    Atom method = m.getName();
    String methodName = method.toString();
    return methodName.startsWith("test") || methodName.equals("setUp") || methodName.equals("tearDown");
  }

  /**
   * Get the "setUp" and "tearDown" methods in the given class
   */
  public static Set<IMethod> getSetUpTearDownMethods(IClass testClass) throws ClassHierarchyException {
    final Atom junitPackage = Atom.findOrCreateAsciiAtom("junit/framework");
    final Atom junitClass = Atom.findOrCreateAsciiAtom("TestCase");
    final Atom junitSuite = Atom.findOrCreateAsciiAtom("TestSuite");
    final TypeName junitTestCaseType = TypeName.findOrCreateClass(junitPackage, junitClass);
    final TypeName junitTestSuiteType = TypeName.findOrCreateClass(junitPackage, junitSuite);

    final Atom setUpMethodAtom = Atom.findOrCreateAsciiAtom("setUp");
    final Atom tearDownMethodAtom = Atom.findOrCreateAsciiAtom("tearDown");

    Set<IMethod> result = new HashSet<IMethod>();

    IClass currClass = testClass;
    while (currClass != null && !currClass.getName().equals(junitTestCaseType) && !currClass.getName().equals(junitTestSuiteType)) {

      for (Iterator methodsIt = currClass.getDeclaredMethods().iterator(); methodsIt.hasNext();) {

        IMethod method = (IMethod) methodsIt.next();
        final Atom methodAtom = method.getName();
        if (methodAtom.equals(setUpMethodAtom) || methodAtom.equals(tearDownMethodAtom) || method.isClinit() || method.isInit()) {
          result.add(method);
        }
      }
      currClass = currClass.getSuperclass();
    }
    return result;
  }
}
