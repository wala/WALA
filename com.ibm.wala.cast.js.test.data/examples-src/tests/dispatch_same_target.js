var x = function f1(p, q) {
	p.call(p, q);
};

var y = function f2(q) { x(q, null); };

var q = function f4() {}
var z = function f3() { q(); };

x(y, z);

