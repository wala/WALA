package com.ibm.wala.util.tables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TableTest {

  @Test
  void rendersMissingCellsAsBlanks() {
    Table<String> table = new Table<>(new String[] {"a", "b"});
    table.addRow(Map.of("a", "x"));

    assertThat(table.toString()).isEqualTo("a b \nx   \n");
  }

  @Test
  void rowMapIncludesNullForMissingCell() {
    Table<String> table = new Table<>(new String[] {"a", "b"});
    table.addRow(Map.of("a", "x"));

    assertThat(table.row2Map(0)).containsOnly(entry("a", "x"), entry("b", null));
  }

  @Test
  void removesRowWithMissingCell() {
    Table<String> table = new Table<>(new String[] {"a", "b"});
    table.addRow(Map.of("a", "x"));

    table.removeRow(table.row2Map(0));

    assertThat(table.getNumberOfRows()).isZero();
  }

  @Test
  void missingColumnHeadingReturnsNull() {
    Table<String> table = new Table<>(new String[] {"a"});

    assertThat(table.getColumnHeading(1)).isNull();
  }
}
