function biz(p) { return p; }

var z = (function foo() { return this; })();

var x = z.biz(3);