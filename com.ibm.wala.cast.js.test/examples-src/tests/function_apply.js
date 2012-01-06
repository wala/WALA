function biz() { return this; }

var q = biz;
var o = { m: function theOne() {} }
var r = q.apply(o);
o.m()

function id(y) { return y; }

function theTwo() {}
var p = id;
var s = p.apply(null, [theTwo]);
s();

// test invoking with non-array argsList
p.apply(null, o);
