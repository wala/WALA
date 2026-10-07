/*
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.io;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.io.MoreFiles;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

/** Simple utilities for accessing files. */
public class FileUtil {

  /**
   * List all the files in a directory that match a regular expression
   *
   * @param recurse recurse to subdirectories?
   * @throws IllegalArgumentException if dir is null
   */
  public static Collection<File> listFiles(
      String dir, @Language("RegExp") String regex, boolean recurse) {
    if (dir == null) {
      throw new IllegalArgumentException("dir is null");
    }
    File d = new File(dir);
    Pattern p = null;
    if (regex != null) {
      p = Pattern.compile(regex);
    }
    return listFiles(d, recurse, p);
  }

  private static Collection<File> listFiles(File directory, boolean recurse, @Nullable Pattern p) {
    try (Stream<Path> paths = Files.walk(directory.toPath(), recurse ? Integer.MAX_VALUE : 1)) {
      return paths
          .skip(1) // Files.walk yields the root itself, which callers never expect
          .map(Path::toFile)
          .filter(f -> p == null || p.matcher(f.getAbsolutePath()).matches())
          .collect(Collectors.toCollection(HashSetFactory::make));
    } catch (IOException e) {
      throw new UncheckedIOException("failed to list " + directory, e);
    }
  }

  /**
   * Copies a file from the specified source path to the destination path, replacing the destination
   * if it already exists.
   *
   * @param srcFileName the path of the source file
   * @param destFileName the path of the destination file
   * @throws IOException if an I/O error occurs during the copy operation
   */
  public static void copy(String srcFileName, String destFileName) throws IOException {
    checkArgument(srcFileName != null, "srcFileName is null");
    checkArgument(destFileName != null, "destFileName is null");
    Files.copy(
        Paths.get(srcFileName), Paths.get(destFileName), StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * delete all files (recursively) in a directory. This is dangerous. Use with care.
   *
   * @throws IOException if there's a problem deleting some file
   */
  public static void deleteContents(String directory) throws IOException {
    Path path = Paths.get(directory);
    if (Files.exists(path)) {
      // Do not pass ALLOW_INSECURE: when the platform offers SecureDirectoryStream, Guava
      // deletes via the secure path, and without that it fails rather than deleting
      // files that a concurrent symlink swap could move outside the directory.
      MoreFiles.deleteDirectoryContents(path);
    }
  }

  /**
   * Create a {@link FileOutputStream} corresponding to a particular file name. Delete the existing
   * file if one exists.
   */
  public static FileOutputStream createFile(String fileName) throws IOException {
    checkArgument(fileName != null, "null file");
    Path path = Paths.get(fileName);
    MoreFiles.createParentDirectories(path);
    Files.deleteIfExists(path);
    // createFile fails rather than clobbering, so no TOCTOU window on a concurrent create
    return new FileOutputStream(Files.createFile(path).toFile());
  }

  /** read fully the contents of s and return a byte array holding the result */
  public static byte[] readBytes(InputStream s) throws IOException {
    checkArgument(s != null, "null s");
    return s.readAllBytes();
  }

  /** write string s into file f */
  public static void writeFile(File f, String content) throws IOException {
    Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
  }

  public static void recurseFiles(Consumer<File> action, final Predicate<File> filter, File top) {
    if (!top.isDirectory()) {
      action.accept(top);
      return;
    }
    try (Stream<Path> paths = Files.walk(top.toPath())) {
      paths.map(Path::toFile).filter(f -> !f.isDirectory() && filter.test(f)).forEach(action);
    } catch (IOException e) {
      throw new UncheckedIOException("failed to walk " + top, e);
    }
  }
}
