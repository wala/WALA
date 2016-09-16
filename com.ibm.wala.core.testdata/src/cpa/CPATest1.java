package cpa;

public class CPATest1 {

    protected static abstract class N {
      abstract N op(N other);
    }
    
    public static class I extends N {
      int I;
      
      I(int i) {
        this.I = i;
      }
      
      @Override
      N op(N other) {
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
      N op(N other) {
        if (other instanceof I) {
          return new F(F + ((I)other).I); 
        } else {
          return new F(F + ((F)other).F);
        }
      }
    }
    
    public static N id(N x) {
      return x;
    }
    
    public static void main(String[] args) {
      F f = new F(3.4);
      I i = new I(7);
      F r1 = (F) id(f.op(f));
      F r2 = (F) id(f.op(i));
      I r3 = (I) id(i.op(f));
      I r4 = (I) id(i.op(i));
    }
}
