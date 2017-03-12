package special;

class A {
  String name;
  
  A() {
    setX("silly");
  }
  
  public void setX(String name) {
    this.name = name;
  }
  
  @Override
  public
  String toString() {
    return name;
  }
  
  interface Ctor<T> {
    T make();
  }
  
  public static void main(String[] args) {
    Ctor<A> o = A::new;
    Object a = o.make();
    a.toString();
  }
  
}
