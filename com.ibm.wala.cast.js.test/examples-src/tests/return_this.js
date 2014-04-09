var o = {
	f: function foo() { return this; },
	g: function bar() {}
}

o.f().g();
