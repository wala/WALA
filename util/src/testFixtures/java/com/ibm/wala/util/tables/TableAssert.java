package com.ibm.wala.util.tables;

import org.assertj.core.annotation.CheckReturnValue;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;

/** Custom assertions for {@link Table} instances, for use in WALA's own tests. */
public class TableAssert<T> extends AbstractObjectAssert<TableAssert<T>, Table<T>> {

  public TableAssert(Table<T> actual) {
    super(actual, TableAssert.class);
  }

  @CheckReturnValue
  public static <T> TableAssert<T> assertThat(Table<T> actual) {
    return new TableAssert<>(actual);
  }

  public AbstractIntegerAssert<?> numberOfRows() {
    isNotNull();
    return Assertions.assertThat(actual.getNumberOfRows()).as("number of table rows");
  }

  public MapAssert<String, T> row(int row) {
    isNotNull();
    return Assertions.assertThat(actual.row2Map(row)).as("row %d", row);
  }
}
