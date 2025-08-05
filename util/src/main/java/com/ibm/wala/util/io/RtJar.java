package com.ibm.wala.util.io;

import com.ibm.wala.util.PlatformUtil;
import com.ibm.wala.util.collections.ArrayIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.nullability.NullabilityUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.jar.JarFile;
import org.jspecify.annotations.Nullable;

/** Utility class to find the file holding the classes for the core Java standard library. */
public class RtJar {

  private RtJar() {}

  /**
   * Returns the file holding the classes for the core Java standard library from the provided jar
   * files. The file may be a jar file or a jmod file.
   *
   * @param x an iterator over jar files
   * @return the file holding the classes for the core Java standard library, or null if not found
   */
  public static @Nullable JarFile getRtJar(Iterator<JarFile> x) {
    while (x.hasNext()) {
      JarFile JF = x.next();
      switch (Paths.get(JF.getName()).getFileName().toString()) {
        case "core.jar":
        case "java.base.jmod":
        case "rt.jar":
          return JF;
        case "classes.jar":
          if (PlatformUtil.onMacOSX()) return JF;
        // $FALL-THROUGH$
        default:
      }
    }

    return null;
  }

  @SuppressWarnings("resource")
  public static void main(String[] args) {
    JarFile rt =
        getRtJar(
            new MapIterator<>(
                new FilterIterator<>(
                    new ArrayIterator<>(
                        System.getProperty("sun.boot.class.path").split(File.pathSeparator)),
                    t -> t.endsWith(".jar")),
                object -> {
                  try {
                    return new JarFile(object);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }));

    System.err.println(NullabilityUtil.castToNonNull(rt).getName());
  }
}
