function outer() {
  function foo() {
    function bar() { return 3; }
    foo = bar;
    return 4;
  }
  foo();
  foo();
}

outer();
