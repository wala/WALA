function A() {}

A.prototype.f = function reachable() {
	print("reachable");
};

var a = new A();
a.g = function also_reachable() {
	print("also reachable");
};
for(var p in a)
	a[p]();