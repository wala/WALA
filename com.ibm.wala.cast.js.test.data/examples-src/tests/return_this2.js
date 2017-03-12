function A() {}
A.prototype.f = function foo() {
	return this;
};

var a1 = new A(), a2 = new A();

a1.g = function bar1() {};
a2.g = function bar2() {};

var x1 = a1.f();
var x2 = a2.f();

(function test1() { x1.g(); })();
(function test2() { x2.g(); })();