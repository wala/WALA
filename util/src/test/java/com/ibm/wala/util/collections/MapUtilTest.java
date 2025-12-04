package com.ibm.wala.util.collections;

import static com.ibm.wala.util.collections.MapUtil.groupKeysByValue;
import static com.ibm.wala.util.collections.MapUtil.inverseMap;
import static com.ibm.wala.util.collections.MapUtil.invertOneToOneMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MapUtil}. */
class MapUtilTest {

  // ------------------------------------------------------------
  // inverseMap
  // ------------------------------------------------------------

  @Test
  void testInverseMapNormal() {
    assertThat(inverseMap(Map.of('A', Set.of(1, 2), 'B', Set.of(1))))
        .containsExactlyInAnyOrderEntriesOf(Map.of(1, Set.of('A', 'B'), 2, Set.of('A')));
  }

  @Test
  void testInverseMapEmpty() {
    assertThat(inverseMap(Map.of())).isEmpty();
  }

  @Test
  void testInverseMapValueSetEmpty() {
    assertThat(inverseMap(Map.of('A', Set.of()))).isEmpty();
  }

  @Test
  void testInverseMapNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> inverseMap(null));
  }

  // ------------------------------------------------------------
  // invertOneToOneMap
  // ------------------------------------------------------------

  @Test
  void testInvertOneToOneMapNormal() {
    assertThat(invertOneToOneMap(Map.of('A', 1, 'B', 2, 'C', 3)))
        .containsExactlyInAnyOrderEntriesOf(Map.of(1, 'A', 2, 'B', 3, 'C'));
  }

  @Test
  void testInvertOneToOneMapDuplicateValueThrows() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> invertOneToOneMap(Map.of('A', 1, 'B', 1)));
  }

  @Test
  void testInvertOneToOneMapEmpty() {
    assertThat(invertOneToOneMap(Collections.emptyMap())).isEmpty();
  }

  @Test
  void testInvertOneToOneMapNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> invertOneToOneMap(null));
  }

  // ------------------------------------------------------------
  // groupKeysByValue
  // ------------------------------------------------------------

  @Test
  void testGroupKeysByValueNormal() {
    assertThat(groupKeysByValue(Map.of('A', 1, 'B', 2, 'C', 1)))
        .containsExactlyInAnyOrderEntriesOf(Map.of(Set.of('A', 'C'), 1, Set.of('B'), 2));
  }

  @Test
  void testGroupKeysByValueEmpty() {
    assertThat(groupKeysByValue(Collections.emptyMap())).isEmpty();
  }

  @Test
  void testGroupKeysByValueNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> groupKeysByValue(null));
  }
}
