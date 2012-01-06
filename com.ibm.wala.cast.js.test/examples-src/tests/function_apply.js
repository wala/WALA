//function bar(y) { return y + 3; }

//var p = bar;
//p.apply(null, [7]);

function biz() { return this; }

var q = biz;
var o = { m: function theOne() {} }
var r = q.apply(o);
o.m()