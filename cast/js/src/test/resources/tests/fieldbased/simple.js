function A() {}

function foo() {
  return bar() + 19;
}

function bar() {
  return 23;
}

var baz = function aluis() {
  aluis();
};

foo();
this.bar();
new A();