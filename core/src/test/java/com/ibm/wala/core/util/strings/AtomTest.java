package com.ibm.wala.core.util.strings;

import static com.ibm.wala.core.util.strings.Atom.concat;
import static com.ibm.wala.core.util.strings.Atom.findOrCreate;
import static com.ibm.wala.core.util.strings.Atom.findOrCreateAsciiAtom;
import static com.ibm.wala.core.util.strings.Atom.findOrCreateUnicodeAtom;
import static com.ibm.wala.core.util.strings.Atom.isArrayDescriptor;
import static com.ibm.wala.core.util.strings.AtomAssert.assertThat;
import static java.lang.Integer.MAX_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit tests of the {@link Atom} class. */
@SuppressWarnings("UnnecessaryUnicodeEscape")
public final class AtomTest {

  // --- Factories

  @Test
  public void testFindOrCreateUnicodeAtom() {
    assertThat(Atom.findOrCreateUnicodeAtom("hello")).hasUnicodeString("hello").hasByteLength(5);
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreateUnicodeAtom(null));
  }

  @Test
  public void testFindOrCreateAsciiAtom() {
    assertThat(Atom.findOrCreateAsciiAtom("hello")).hasUnicodeString("hello");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Atom.findOrCreateAsciiAtom(null))
        .withMessage("str is null");
  }

  @Test
  public void testFindOrCreateUtf8Atom() {
    assertThat(Atom.findOrCreateUtf8Atom("hello".getBytes(StandardCharsets.UTF_8)))
        .hasUnicodeString("hello");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Atom.findOrCreateUtf8Atom(null))
        .withMessage("utf8 is null");
  }

  @Test
  public void testFindOrCreateByteArray() {
    assertThat(Atom.findOrCreate("hello".getBytes(StandardCharsets.UTF_8)))
        .hasUnicodeString("hello");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Atom.findOrCreate((byte[]) null))
        .withMessage("bytes is null");
  }

  @Test
  public void testFindOrCreateByteArraySlice() {
    final byte[] bytes = "abcdef".getBytes(StandardCharsets.UTF_8);
    assertThat(Atom.findOrCreate(bytes, 2, 2)).hasUnicodeString("cd");
    assertThat(Atom.findOrCreate(bytes, 0, 6)).hasUnicodeString("abcdef");
    assertThat(Atom.findOrCreate(bytes, 6, 0)).isEmpty();
  }

  @Test
  public void testFindOrCreateByteArraySliceValidation() {
    final byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate((byte[]) null, 0, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(bytes, 0, -1));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(bytes, -1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(bytes, 3, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(bytes, 2, 2));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(bytes, MAX_VALUE, 1));
  }

  @Test
  public void testFindOrCreateImmutableByteArray() {
    assertThat(Atom.findOrCreate(ImmutableByteArray.make("hello"))).hasUnicodeString("hello");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> findOrCreate((ImmutableByteArray) null))
        .withMessage("b is null");
  }

  @Test
  public void testFindOrCreateImmutableByteArraySlice() {
    final ImmutableByteArray array = ImmutableByteArray.make("abcdef");
    assertThat(Atom.findOrCreate(array, 2, 2)).hasUnicodeString("cd");
    assertThat(Atom.findOrCreate(array, 0, 6)).hasUnicodeString("abcdef");
    assertThat(Atom.findOrCreate(array, 6, 0)).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> findOrCreate((ImmutableByteArray) null, 0, 1));
  }

  @Test
  public void testEmptyAtom() {
    final Atom empty = Atom.findOrCreate(new byte[0]);
    assertThat(empty)
        .isEmpty()
        .hasToString("")
        .hasValArray()
        .isNotReservedMemberName()
        .isNotClassDescriptor()
        .isNotArrayDescriptor()
        .isNotMethodDescriptor()
        .doesNotContain((byte) 'a')
        .hasRIndex((byte) 'a', -1)
        .startsWith(empty);
  }

  // --- Known aliasing quirks
  //
  // findOrCreate(byte[]) stores the caller's array without copying it, so Atoms alias their input
  // arrays. This is a known quirk of the current implementation, documented here so that a future
  // reimplementation can decide whether to preserve or fix it.

  @Test
  public void testFindOrCreateByteArrayAliasesInput() {
    final byte[] bytes = "aliasMe".getBytes(UTF_8);
    final Atom atom = findOrCreate(bytes);
    bytes[0] = 'A';
    assertThat(atom).hasUnicodeString("AliasMe").hasByteAt(0, (byte) 'A');
  }

  @Test
  public void testAliasingCorruptsInterning() {
    final byte[] bytes = "0123456789".getBytes(StandardCharsets.UTF_8);
    final Atom atom = Atom.findOrCreate(bytes);
    bytes[0] = 'a';
    assertThat(atom).hasUnicodeString("a123456789");
    final Atom again = Atom.findOrCreate("a123456789".getBytes(StandardCharsets.UTF_8));
    assertThat(atom).isNotInternedAs(again);
  }

  // --- Interning and canonicalization

  @Test
  public void testCanonicalizationAcrossFactories() {
    assertThat(Atom.findOrCreateUnicodeAtom("foo"))
        .isInternedAs(Atom.findOrCreateAsciiAtom("foo"))
        .isInternedAs(Atom.findOrCreateUtf8Atom("foo".getBytes(StandardCharsets.UTF_8)))
        .isInternedAs(Atom.findOrCreate("foo".getBytes(StandardCharsets.UTF_8)))
        .isInternedAs(Atom.findOrCreate(ImmutableByteArray.make("foo")));
  }

  @Test
  public void testCanonicalizationDistinguishesContent() {
    final Atom foo = Atom.findOrCreateUnicodeAtom("foo");
    final Atom bar = Atom.findOrCreateUnicodeAtom("bar");
    assertThat(foo).isNotInternedAs(bar);
    assertThat(foo.equals(bar)).isFalse();
    assertThat(foo).isNotInternedAs(Atom.findOrCreateUnicodeAtom("fo"));
  }

  @Test
  public void testFindOrCreateDistinguishesHashCollisions() {
    // Two distinct byte arrays whose AtomKey hash codes collide, so interning must compare their
    // contents rather than assuming distinct hashes mean distinct atoms.
    final Atom firstAtom = Atom.findOrCreate(new byte[] {27, (byte) 234, 56, (byte) 221});
    final Atom secondAtom = Atom.findOrCreate(new byte[] {(byte) 178, 42, 126, 2});
    assertThat(firstAtom.hashCode()).isEqualTo(secondAtom.hashCode());
    assertThat(firstAtom).isNotInternedAs(secondAtom);
  }

  @Test
  public void testFindOrCreateDistinguishesHashCollisionsOfDifferentLengths() {
    // Colliding hashes with different byte lengths: interning must compare lengths too.
    final Atom twoByteAtom = Atom.findOrCreate(new byte[] {(byte) 214, 95});
    final Atom threeByteAtom = Atom.findOrCreate(new byte[] {79, (byte) 228, (byte) 134});
    assertThat(twoByteAtom.hashCode()).isEqualTo(threeByteAtom.hashCode());
    assertThat(twoByteAtom).isNotInternedAs(threeByteAtom);
  }

  @Test
  public void testEmptyAtomsAreCanonical() {
    assertThat(Atom.findOrCreate(new byte[0]))
        .isInternedAs(Atom.findOrCreate(new byte[0]))
        .isInternedAs(Atom.findOrCreateUnicodeAtom(""))
        .isInternedAs(Atom.findOrCreate("abc".getBytes(StandardCharsets.UTF_8), 3, 0));
  }

  @Test
  public void testEqualsAndHashCode() {
    final Atom a = findOrCreateUnicodeAtom("same");
    final Atom b = findOrCreateUnicodeAtom("same");
    final Atom c = findOrCreateUnicodeAtom("different");
    assertThat(a).isInternedAs(b);
    assertThat(a.equals(a)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode()).isEqualTo(a.hashCode());
    assertThat(a).isNotInternedAs(c);
    assertThat(c.hashCode()).isNotEqualTo(a.hashCode());
  }

  // --- Accessors

  @Test
  public void testToString() {
    assertThat(Atom.findOrCreateUnicodeAtom("hello")).hasToString("hello");
    assertThat(Atom.findOrCreateUnicodeAtom("")).hasToString("");
  }

  @Test
  public void testToUnicodeString() throws UTFDataFormatException {
    assertThat(Atom.findOrCreateUnicodeAtom("hello").toUnicodeString()).isEqualTo("hello");
    assertThat(Atom.findOrCreateUnicodeAtom("\u00e9").toUnicodeString()).isEqualTo("\u00e9");
    assertThat(Atom.findOrCreateUnicodeAtom("a\u0000b").toUnicodeString()).isEqualTo("a\u0000b");
    assertThat(Atom.findOrCreateUnicodeAtom("\ud83d\ude00").toUnicodeString())
        .isEqualTo("\ud83d\ude00");
  }

  @Test
  public void testToUnicodeStringRejectsInvalidUtf8() {
    final Atom truncated = Atom.findOrCreate(new byte[] {(byte) 0xC2});
    assertThatThrownBy(truncated::toUnicodeString).isInstanceOf(UTFDataFormatException.class);
    final Atom dangling = Atom.findOrCreate(new byte[] {'a', (byte) 0x80});
    assertThatThrownBy(dangling::toUnicodeString).isInstanceOf(UTFDataFormatException.class);
  }

  @Test
  public void testLengthCountsBytes() {
    assertThat(Atom.findOrCreateUnicodeAtom("foo")).hasByteLength(3);
    assertThat(Atom.findOrCreateUnicodeAtom("\u00e9")).hasByteLength(2);
    assertThat(Atom.findOrCreateUnicodeAtom("a\u0000b")).hasByteLength(4);
    assertThat(Atom.findOrCreateUnicodeAtom("\ud83d\ude00")).hasByteLength(6);
  }

  @Test
  public void testUtf8RoundTripAcrossCharacterRanges() {
    final String original =
        "\u0001\u007f\u0080\u07ff\u0800\uffff" + "abcdef" + "\u00e9\u4e00\ud83d\ude00";
    assertThat(Atom.findOrCreateUnicodeAtom(original))
        .hasByteLength(UTF8Convert.utfLength(original))
        .hasUnicodeString(original);
  }

  @Test
  public void testUtf8BytesPreserved() {
    final byte[] bytes = {'h', 'i', 0x00, (byte) 0xC2, (byte) 0xA9};
    assertThat(Atom.findOrCreateUtf8Atom(bytes)).hasValArray(bytes);
    assertThat(Atom.findOrCreate(bytes)).hasValArray(bytes);
  }

  @Test
  public void testNullByteDistinguishesUnicodeFromAsciiFactory() {
    final Atom unicode = findOrCreateUnicodeAtom("a\u0000b");
    final Atom ascii = findOrCreateAsciiAtom("a\u0000b");
    assertThat(unicode).isNotInternedAs(ascii).hasByteLength(4);
    assertThat(ascii).hasByteLength(3);
  }

  @Test
  public void testGetVal() {
    final Atom atom = Atom.findOrCreateUnicodeAtom("abc");
    assertThat(atom).hasByteAt(0, (byte) 'a').hasByteAt(2, (byte) 'c');
    assertThatIllegalArgumentException().isThrownBy(() -> atom.getVal(3));
    assertThatIllegalArgumentException().isThrownBy(() -> atom.getVal(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> findOrCreate(new byte[0]).getVal(0));
  }

  @Test
  public void testGetValArrayIsDefensiveCopy() {
    final Atom atom = Atom.findOrCreateUnicodeAtom("abc");
    final byte[] array = atom.getValArray();
    assertThat(atom).hasValArray("abc".getBytes(StandardCharsets.UTF_8));
    array[0] = 'z';
    assertThat(atom).hasToString("abc").hasByteAt(0, (byte) 'a');
  }

  @Test
  public void testContains() {
    assertThat(Atom.findOrCreateUnicodeAtom("abcdef"))
        .contains((byte) 'a')
        .contains((byte) 'f')
        .doesNotContain((byte) 'z')
        .doesNotContain((byte) 'A');
  }

  @Test
  public void testRIndex() {
    assertThat(Atom.findOrCreateUnicodeAtom("abc"))
        .hasRIndex((byte) 'a', 3)
        .hasRIndex((byte) 'b', 2)
        .hasRIndex((byte) 'c', 1)
        .hasRIndex((byte) 'z', -1);
    final Atom repeated = Atom.findOrCreateUnicodeAtom("aab");
    assertThat(repeated).hasRIndex((byte) 'a', 2);
    assertThat(Atom.findOrCreate(new byte[0])).hasRIndex((byte) 'a', -1);
  }

  // --- Slicing

  @Test
  public void testLeft() {
    final Atom atom = Atom.findOrCreateUnicodeAtom("abcdef");
    assertThat(atom.left(2)).hasUnicodeString("ab");
    assertThat(atom.left(0)).isEmpty();
    assertThat(atom.left(6)).isInternedAs(atom);
    assertThat(atom.left(2)).isInternedAs(Atom.findOrCreateUnicodeAtom("ab"));
    assertThatIllegalArgumentException().isThrownBy(() -> atom.left(7));
    assertThatIllegalArgumentException().isThrownBy(() -> atom.left(-1));
  }

  @Test
  public void testRight() {
    final Atom atom = Atom.findOrCreateUnicodeAtom("abcdef");
    assertThat(atom.right(2)).hasUnicodeString("ef");
    assertThat(atom.right(0)).isEmpty();
    assertThat(atom.right(6)).isInternedAs(atom);
    assertThat(atom.right(2)).isInternedAs(Atom.findOrCreateUnicodeAtom("ef"));
    assertThatIllegalArgumentException().isThrownBy(() -> atom.right(7));
    assertThatIllegalArgumentException().isThrownBy(() -> atom.right(-1));
  }

  @Test
  public void testStartsWith() {
    final Atom atom = findOrCreateUnicodeAtom("abcdef");
    final Atom empty = findOrCreate(new byte[0]);
    assertThat(atom)
        .startsWith(findOrCreateUnicodeAtom("ab"))
        .startsWith(atom)
        .startsWith(empty)
        .doesNotStartWith(findOrCreateUnicodeAtom("abd"))
        .doesNotStartWith(findOrCreateUnicodeAtom("abcdefg"));
    assertThat(empty).doesNotStartWith(findOrCreateUnicodeAtom("a"));
  }

  @Test
  public void testStartsWithRejectsNull() {
    final Atom atom = Atom.findOrCreateUnicodeAtom("abc");
    assertThatThrownBy(() -> atom.startsWith(null)).isInstanceOf(AssertionError.class);
  }

  @Test
  public void testByteLevelSlicingOfMultibyteAtom() {
    final Atom atom = findOrCreateUnicodeAtom("\u00e9x");
    assertThat(atom).hasByteLength(3).hasValArray((byte) 0xC3, (byte) 0xA9, (byte) 'x');
    assertThat(atom.left(1)).hasValArray((byte) 0xC3);
    assertThat(atom.right(1)).hasValArray((byte) 'x');
    assertThat(atom.right(2)).hasValArray((byte) 0xA9, (byte) 'x');
  }

  // --- Descriptors

  @Test
  public void testArrayDescriptorFromElementDescriptor() {
    assertThat(Atom.findOrCreateUnicodeAtom("I").arrayDescriptorFromElementDescriptor())
        .hasUnicodeString("[I");
    assertThat(
            Atom.findOrCreateUnicodeAtom("Ljava/lang/Object;")
                .arrayDescriptorFromElementDescriptor())
        .hasUnicodeString("[Ljava/lang/Object;");
    assertThat(Atom.findOrCreate(new byte[0]).arrayDescriptorFromElementDescriptor())
        .hasUnicodeString("[");
    assertThat(Atom.findOrCreateUnicodeAtom("[I").arrayDescriptorFromElementDescriptor())
        .hasUnicodeString("[[I");
  }

  @Test
  public void testIsReservedMemberName() {
    assertThat(Atom.findOrCreateUnicodeAtom("<init>")).isReservedMemberName();
    assertThat(Atom.findOrCreateUnicodeAtom("<clinit>")).isReservedMemberName();
    assertThat(Atom.findOrCreateUnicodeAtom("foo")).isNotReservedMemberName();
    assertThat(Atom.findOrCreate(new byte[0])).isNotReservedMemberName();
  }

  @Test
  public void testIsClassDescriptor() {
    assertThat(Atom.findOrCreateUnicodeAtom("Ljava/lang/Object;")).isClassDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("I")).isNotClassDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("[I")).isNotClassDescriptor();
    assertThat(Atom.findOrCreate(new byte[0])).isNotClassDescriptor();
  }

  @Test
  public void testIsArrayDescriptorInstance() {
    assertThat(Atom.findOrCreateUnicodeAtom("[I")).isArrayDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("[[I")).isArrayDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("I")).isNotArrayDescriptor();
    assertThat(Atom.findOrCreate(new byte[0])).isNotArrayDescriptor();
  }

  @Test
  public void testIsMethodDescriptor() {
    assertThat(Atom.findOrCreateUnicodeAtom("(I)V")).isMethodDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("()V")).isMethodDescriptor();
    assertThat(Atom.findOrCreateUnicodeAtom("V")).isNotMethodDescriptor();
    assertThat(Atom.findOrCreate(new byte[0])).isNotMethodDescriptor();
  }

  @Test
  public void testParseForArrayElementDescriptor() {
    assertThat(Atom.findOrCreateUnicodeAtom("[I").parseForArrayElementDescriptor())
        .hasUnicodeString("I");
    assertThat(
            Atom.findOrCreateUnicodeAtom("[[Ljava/lang/String;").parseForArrayElementDescriptor())
        .hasUnicodeString("[Ljava/lang/String;");
    assertThat(Atom.findOrCreateUnicodeAtom("[").parseForArrayElementDescriptor()).isEmpty();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> findOrCreate(new byte[0]).parseForArrayElementDescriptor());
  }

  @Test
  public void testParseForArrayDimensionality() {
    assertThat(Atom.findOrCreateUnicodeAtom("[I")).hasArrayDimensionality(1);
    assertThat(Atom.findOrCreateUnicodeAtom("[[I")).hasArrayDimensionality(2);
    assertThat(Atom.findOrCreateUnicodeAtom("[[[J")).hasArrayDimensionality(3);
    assertThat(Atom.findOrCreateUnicodeAtom("I")).hasArrayDimensionality(0);
    assertThat(Atom.findOrCreateUnicodeAtom("Ljava/lang/String;")).hasArrayDimensionality(0);
    assertThatIllegalStateException()
        .isThrownBy(() -> Atom.findOrCreateUnicodeAtom("[[").parseForArrayDimensionality());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Atom.findOrCreate(new byte[0]).parseForArrayDimensionality());
  }

  @Test
  public void testParseForInnermostArrayElementDescriptor() {
    assertThat(Atom.findOrCreateUnicodeAtom("[[[D").parseForInnermostArrayElementDescriptor())
        .hasUnicodeString("D");
    assertThat(
            Atom.findOrCreateUnicodeAtom("[[Ljava/lang/String;")
                .parseForInnermostArrayElementDescriptor())
        .hasUnicodeString("Ljava/lang/String;");
    assertThat(Atom.findOrCreateUnicodeAtom("I").parseForInnermostArrayElementDescriptor())
        .hasUnicodeString("I");
    assertThatThrownBy(
            () -> Atom.findOrCreateUnicodeAtom("[[").parseForInnermostArrayElementDescriptor())
        .isInstanceOf(IllegalStateException.class);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> findOrCreate(new byte[0]).parseForInnermostArrayElementDescriptor());
  }

  // --- Concatenation

  @Test
  public void testConcatByteAndImmutableByteArray() {
    assertThat(Atom.concat((byte) '(', ImmutableByteArray.make("I)V"))).hasUnicodeString("(I)V");
    assertThat(Atom.concat((byte) 'x', new ImmutableByteArray(new byte[0]))).hasUnicodeString("x");
    assertThatIllegalArgumentException().isThrownBy(() -> concat((byte) '(', null));
  }

  @Test
  public void testConcatAtoms() {
    assertThat(
            Atom.concat(Atom.findOrCreateUnicodeAtom("foo"), Atom.findOrCreateUnicodeAtom("bar")))
        .hasUnicodeString("foobar");
    assertThat(Atom.concat(Atom.findOrCreate(new byte[0]), Atom.findOrCreateUnicodeAtom("x")))
        .isInternedAs(Atom.findOrCreateUnicodeAtom("x"));
    assertThat(Atom.concat(Atom.findOrCreateUnicodeAtom("x"), Atom.findOrCreate(new byte[0])))
        .isInternedAs(Atom.findOrCreateUnicodeAtom("x"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> concat(null, findOrCreateUnicodeAtom("x")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> concat(findOrCreateUnicodeAtom("x"), null));
  }

  @Test
  public void testIsArrayDescriptorStatic() {
    assertThat(Atom.isArrayDescriptor(ImmutableByteArray.make("[I"))).isTrue();
    assertThat(Atom.isArrayDescriptor(ImmutableByteArray.make("I"))).isFalse();
    assertThat(Atom.isArrayDescriptor(new ImmutableByteArray(new byte[0]))).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> isArrayDescriptor(null));
  }

  // --- Serialization

  @Test
  public void testSerializationRoundTripsToCanonicalInstance()
      throws ClassNotFoundException, IOException {
    final Atom original = Atom.findOrCreateUnicodeAtom("someAtom");
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (final ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    try (final ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      final Object deserialized = in.readObject();
      assertThat(deserialized).isInstanceOf(Atom.class);
      assertThat((Atom) deserialized).isSameAs(original);
    }
  }

  @Test
  public void testSerializationOfEmptyAtom() throws ClassNotFoundException, IOException {
    final Atom empty = Atom.findOrCreate(new byte[0]);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (final ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(empty);
    }
    try (final ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      assertThat((Atom) in.readObject()).isSameAs(empty);
    }
  }
}
