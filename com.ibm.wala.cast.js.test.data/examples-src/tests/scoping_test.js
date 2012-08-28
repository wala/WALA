function foo() {
  function baz() {
   return function f1() {};
  }
  function boo() {
    baz = function biz() { return function i_am_reachable() {}; }
  }
  boo();
  return baz();
}

var x = foo();
x();
