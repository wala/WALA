package com.ibm.wala.util.tables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class QueryTest {

  private static StringTable tableWithMissingCell() {
    StringTable table = new StringTable(new String[] {"key", "optional"});
    table.addRow(Map.of("key", "other", "optional", "present"));
    table.addRow(Map.of("key", "match"));
    return table;
  }

  @Test
  void equalitySelectionPreservesMissingCells() {
    assertThat(Query.selectStarWhereEquals(tableWithMissingCell(), "key", "match"))
        .singleElement()
        .satisfies(
            row -> assertThat(row).containsOnly(entry("key", "match"), entry("optional", null)));
  }

  @Test
  void projectionIncludesNullForMissingAttribute() {
    assertThat(Query.selectWhereEquals(tableWithMissingCell(), "optional", "key", "match"))
        .containsExactly((String) null);
  }

  @Test
  void predicateCanSelectMissingCell() {
    assertThat(Query.selectStarWhere(tableWithMissingCell(), "optional", Objects::isNull))
        .singleElement()
        .satisfies(
            row -> assertThat(row).containsOnly(entry("key", "match"), entry("optional", null)));
  }

  @Test
  void tableViewPreservesMissingCells() {
    Table<String> source = tableWithMissingCell();

    Table<String> view = Query.viewWhereEquals(source, "key", "match");

    assertThat(view.row2Map(0)).containsOnly(entry("key", "match"), entry("optional", null));
  }

  @Test
  void stringTableViewPreservesMissingCells() {
    StringTable view = Query.viewWhereEquals(tableWithMissingCell(), "key", "match");

    assertThat(view.row2Map(0)).containsOnly(entry("key", "match"), entry("optional", null));
  }
}
