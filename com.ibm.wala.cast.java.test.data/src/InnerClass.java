public class InnerClass {
    public static void main(String[] args) {
      (new InnerClass()).method();
    }

    public int fromInner(int v) {
      return v + 1;
    }

    public int fromInner2(int v) {
      return v + 3;
    }

    public void method() {
	WhatsIt w= new WhatsIt();
    }

    class WhatsThat {
      private int otherValue;

      WhatsThat() {
	otherValue = 3;
	fromInner2( otherValue );
      }
    }

    class WhatsIt {
	private int value;

	public WhatsIt() {
	  value= 0;
	  fromInner(value);
	  anotherMethod();
	}

	private NotAgain anotherMethod() {
	  return new NotAgain();
	}

	class NotAgain {
	  Object x;

	  public NotAgain() {
	    x = new WhatsThat();
	  }

	}
    }
}
