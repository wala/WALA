package com.ibm.wala.util.io;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.MapIterator;

public class RtJar {

  public static JarFile getRtJar(Iterator<JarFile> x) {
    while (x.hasNext()) {
      JarFile JF = x.next();
      if (JF.getName().endsWith(File.separator + "rt.jar")) {
        return JF;
      }
      if (JF.getName().endsWith(File.separator + "core.jar")) {
        return JF;
      }
      // hack for Mac
      if (PlatformUtil.onMacOSX() && JF.getName().endsWith(File.separator + "classes.jar")) {
        return JF;
      }
    }
    
    return null;
  }

  public static void main(String[] args) {
    @SuppressWarnings("resource")
    JarFile rt = getRtJar(new MapIterator<>(
        new FilterIterator<String>(
            new ArrayIterator<>(System.getProperty("sun.boot.class.path").split(File.pathSeparator)),
            new Predicate<String>() {
              @Override
              public boolean test(String t) {
                return t.endsWith(".jar");
              } }),
        new Function<String,JarFile>() {
          @Override
          public JarFile apply(String object) {
            try {
              return new JarFile(object);
            } catch (IOException e) {
              assert false : e.toString();
              return null;
            }
          } 
        }));
    
    System.err.println(rt.getName());
  }
}
