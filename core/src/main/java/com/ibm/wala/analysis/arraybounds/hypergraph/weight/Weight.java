package com.ibm.wala.analysis.arraybounds.hypergraph.weight;

/**
 * A weight may be not set, a number or unlimited, note that the meaning of unlimited is given by
 * the chosen order (see {@link NormalOrder} and {@link ReverseOrder}).
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public record Weight(Type type, int number) {
  public enum Type {
    NUMBER,
    NOT_SET,
    UNLIMITED
  }

  public static final Weight UNLIMITED = new Weight(Type.UNLIMITED, 0);
  public static final Weight NOT_SET = new Weight(Type.NOT_SET, 0);

  public static final Weight ZERO = new Weight(Type.NUMBER, 0);

  public Weight(int number) {
    this(Type.NUMBER, number);
  }

  /**
   * Returns this + other. If this is not Number this will be returned, if other is not number other
   * will be returned
   *
   * @return this + other
   */
  public Weight add(Weight other) {
    final Weight result;
    if (this.type() == Type.NUMBER) {
      if (other.type() == Type.NUMBER) {
        result = new Weight(Type.NUMBER, this.number() + other.number());
      } else {
        result = other;
      }
    } else {
      result = this;
    }

    return result;
  }

  /**
   * @deprecated Use {@link #number()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public int getNumber() {
    return number();
  }

  /**
   * @deprecated Use {@link #type()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public Type getType() {
    return type();
  }

  @Override
  public String toString() {
    if (this.type == Type.NUMBER) {
      return Integer.toString(this.number);
    } else {
      if (this.type == Type.NOT_SET) {
        return "NOT_SET";
      } else if (this.type == Type.UNLIMITED) {
        return "UNLIMITED";
      } else {
        return "Type: " + this.type;
      }
    }
  }
}
