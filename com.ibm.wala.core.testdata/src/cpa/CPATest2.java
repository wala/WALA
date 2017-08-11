package cpa;

public class CPATest2 {

    protected static abstract class N {
      abstract N op(int x, N other);
    }
    
    public static class I extends N {
      int I;
      
      I(int i) {
        this.I = i;
      }
      
      @Override
      N op(int x, N other) {
        if (other instanceof I) {
          return new I(I + ((I)other).I); 
        } else {
          return new F(I + ((F)other).F);
        }
      }
    }
    
    public static class F extends N {
      double F;

      public F(double f) {
        F = f;
      }
      
      @Override
      N op(int x, N other) {
        if (other instanceof I) {
          return new F(F + ((I)other).I); 
        } else {
          return new F(F + ((F)other).F);
        }
      }
    }
    
    public static N id(N x, int i) {
      return x;
    }
    
    @SuppressWarnings("unused")
    public static void main(String[] args) {
      F f = new F(3.4);
      I i = new I(7);
      F r1 = (F) id(f.op(0, f), 0);
      F r2 = (F) id(f.op(0, i), 0);
      I r3 = (I) id(i.op(0, f), 0);
      I r4 = (I) id(i.op(0, i), 0);
    }
}
