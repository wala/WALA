package com.ibm.wala.util.intset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BitVectorRepository}.
 *
 * <p>The repository is a process-wide singleton with static state that accumulates across tests.
 * Each test therefore uses its own, mutually disjoint range of bit indices so no test's
 * representatives can be subsets of another test's queries.
 */
class BitVectorRepositoryTest {

  /** Distinct content is never a subset of another test's content: give each test its own range. */
  private static final int BASE_1 = 1_000_000;

  private static final int BASE_2 = 2_000_000;

  private static final int BASE_3 = 3_000_000;

  private static final int BASE_4 = 4_000_000;

  private static final int BASE_5 = 5_000_000;

  private static BitVectorIntSet of(int... values) {
    BitVectorIntSet result = new BitVectorIntSet();
    for (int value : values) {
      result.add(value);
    }
    return result;
  }

  @Test
  void rejectsNullValue() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BitVectorRepository.findOrCreateSharedSubset(null));
  }

  @Test
  void returnsSameCanonicalRepresentativeForEqualContents() {
    BitVectorIntSet first = of(BASE_1, BASE_1 + 1, BASE_1 + 2, BASE_1 + 3, BASE_1 + 4);
    BitVectorIntSet second = of(BASE_1, BASE_1 + 1, BASE_1 + 2, BASE_1 + 3, BASE_1 + 4);

    BitVectorIntSet rep = BitVectorRepository.findOrCreateSharedSubset(first);
    BitVectorIntSet repAgain = BitVectorRepository.findOrCreateSharedSubset(second);

    assertThat(rep.sameValue(second)).isTrue();
    assertThat(repAgain).isSameAs(rep);
  }

  @Test
  void doesNotConfuseDistinctSetsOfEqualCardinality() {
    BitVectorIntSet registered = of(BASE_2, BASE_2 + 1, BASE_2 + 2, BASE_2 + 3);
    BitVectorIntSet different = of(BASE_2 + 10, BASE_2 + 11, BASE_2 + 12, BASE_2 + 13);

    BitVectorIntSet rep = BitVectorRepository.findOrCreateSharedSubset(registered);
    BitVectorIntSet result = BitVectorRepository.findOrCreateSharedSubset(different);

    assertThat(result).isNotSameAs(rep);
    assertThat(result.sameValue(different)).isTrue();
  }

  @Test
  void returnsDeltaSubsetRepresentativeWhenPresent() {
    BitVectorIntSet subset = of(BASE_3, BASE_3 + 1, BASE_3 + 2);
    BitVectorIntSet superset = of(BASE_3, BASE_3 + 1, BASE_3 + 2, BASE_3 + 3);

    BitVectorIntSet subsetRep = BitVectorRepository.findOrCreateSharedSubset(subset);
    BitVectorIntSet result = BitVectorRepository.findOrCreateSharedSubset(superset);

    assertThat(result).isSameAs(subsetRep);
  }

  @Test
  void prefersExactSizeMatchBeforeDeltaMatch() {
    BitVectorIntSet exact = of(BASE_4, BASE_4 + 1, BASE_4 + 2, BASE_4 + 3);
    BitVectorIntSet subset = of(BASE_4 + 1, BASE_4 + 2, BASE_4 + 3);
    BitVectorIntSet query = of(BASE_4, BASE_4 + 1, BASE_4 + 2, BASE_4 + 3);

    BitVectorIntSet exactRep = BitVectorRepository.findOrCreateSharedSubset(exact);
    BitVectorIntSet subsetRep = BitVectorRepository.findOrCreateSharedSubset(subset);
    BitVectorIntSet result = BitVectorRepository.findOrCreateSharedSubset(query);

    assertThat(result).isSameAs(exactRep).isNotSameAs(subsetRep);
  }

  @Test
  void firstRegisteredDeltaMatchWinsWithinABucket() {
    BitVectorIntSet firstMatch = of(BASE_5, BASE_5 + 2);
    BitVectorIntSet secondMatch = of(BASE_5 + 1, BASE_5 + 3);
    BitVectorIntSet query = of(BASE_5, BASE_5 + 1, BASE_5 + 2, BASE_5 + 3);

    BitVectorIntSet firstRep = BitVectorRepository.findOrCreateSharedSubset(firstMatch);
    BitVectorRepository.findOrCreateSharedSubset(secondMatch);
    BitVectorIntSet result = BitVectorRepository.findOrCreateSharedSubset(query);

    // Both two-element candidates are subsets of the four-element query; the first registered
    // must be returned to keep representative choice deterministic.
    assertThat(result).isSameAs(firstRep);
  }
}
