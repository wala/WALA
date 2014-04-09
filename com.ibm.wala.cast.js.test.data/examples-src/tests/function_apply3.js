function foo() { alert("hello"); }

var y = Function.prototype.apply;

y.apply(foo);
