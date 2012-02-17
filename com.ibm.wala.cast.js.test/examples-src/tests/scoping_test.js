function foo() {
  function baz() {
   return 3;
  }
  function boo() {
    baz = function biz() { return 4; }
  }
  boo();
  return baz();
}

foo();
