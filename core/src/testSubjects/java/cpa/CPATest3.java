package cpa;

public class CPATest3 {

  protected abstract static class N {
    abstract N op(N other);
  }

  public static class I extends N {
    int I;

    I(int i) {
      this.I = i;
    }

    @Override
    N op(N other) {
      if (other instanceof I i) {
        return new I(I + i.I);
      } else {
        return new F(I + ((F) other).F);
      }
    }
  }

  public static class F extends N {
    double F;

    public F(double f) {
      F = f;
    }

    @Override
    N op(N other) {
      if (other instanceof I i) {
        return new F(F + i.I);
      } else {
        return new F(F + ((F) other).F);
      }
    }
  }

  public static N op(N x) {
    F f = new F(3.4);
    I i = new I(7);
    N y = (i.I > 2) ? f : i;
    return id(y.op(x));
  }

  public static N id(N x) {
    return x;
  }

  public static void main(String[] args) {
    F f = new F(3.4);
    I i = new I(7);
    N x = (i.I > 2) ? f : i;
    N y = id(op(id(x)));
  }
}
