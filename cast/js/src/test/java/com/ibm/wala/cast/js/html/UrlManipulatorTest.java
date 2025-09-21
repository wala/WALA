package com.ibm.wala.cast.js.html;

import static com.ibm.wala.cast.js.html.UrlManipulator.relativeToAbsoluteUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

/** Tests for {@link UrlManipulator#relativeToAbsoluteUrl(String, URL)} */
public class UrlManipulatorTest {

  private static void check(String context, String found, String expected) {
    try {
      // We use `hasToString` here instead of `isEqualTo` because `URL.equals` entails DNS
      // resolution.
      assertThat(relativeToAbsoluteUrl(found, new URL(context))).hasToString(expected);
    } catch (MalformedURLException problem) {
      fail(problem);
    }
  }

  private static void checkNull(URL context, String found) {
    assertThatCode(() -> relativeToAbsoluteUrl(found, context))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void testAbsoluteUrl() {
    check(
        "https://example.org/path/",
        "https://example.com/resource",
        "https://example.com/resource");
  }

  @Test
  public void testProtocolRelativeUrl() {
    check("https://example.org/path/", "//example.com/resource", "https://example.com/resource");
  }

  @Test
  public void testProtocolRelativeUrlWithoutPath() {
    check("https://example.org/path/", "//example.com", "https://example.com");
  }

  @Test
  public void testRootRelativeUrl() {
    check("https://example.com/dir/", "/resource", "https://example.com/resource");
  }

  @Test
  public void testRelativePathWithDirectoryContext() {
    check("https://example.com/dir/", "resource", "https://example.com/dir/resource");
  }

  @Test
  public void testRelativePathWithFileContext() {
    check("https://example.com/dir/file.html", "resource", "https://example.com/dir/resource");
  }

  @Test
  public void testParentDirectoryNavigation() {
    check("https://example.com/dir/subdir/", "../resource", "https://example.com/dir/resource");
  }

  @Test
  public void testMultipleParentDirectoryNavigation() {
    check("https://example.com/dir/subdir/", "../../resource", "https://example.com/resource");
  }

  @Test
  public void testParentDirectoryNavigationWithFileContext() {
    check(
        "https://example.com/dir/subdir/file.html",
        "../../resource",
        "https://example.com/resource");
  }

  @Test
  public void testBackslashes() {
    check(
        "https://example.org/",
        "https://example\\.com/dir\\subdir\\resource",
        "https://example/.com/dir/subdir/resource");
  }

  @Test
  public void testEmptyRelativeUrl() {
    check("https://example.com/dir/subdir/", "", "https://example.com/dir/subdir/");
  }

  @Test
  public void testNullUrlFound() throws MalformedURLException {
    checkNull(new URL("https://example.com/dir/"), null);
  }

  @Test
  public void testNullContext() {
    checkNull(null, "resource");
  }

  @Test
  public void testInvalidContextFormatting() {
    check("https:///test", "resource", "https:/resource");
  }

  @Test
  public void testComplexRelativePath() {
    check(
        "https://example.com/dir/subdir/",
        "../../subdir2/resource/../../file",
        "https://example.com/file");
  }

  @Test
  public void testAbsoluteUrlWithUpperCase() {
    check(
        "https://example.org/path/",
        "HTTPS://EXAMPLE.COM/RESOURCE",
        "https://example.com/resource");
  }
}
