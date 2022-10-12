var m = function () { return "hi"; } // 1
var x = function () { return m; } // 2
var y = function () { return x; } // 3
var z = y; // 4
var n = z(); // 5 -- bound 1
var o = n() // 6 -- bound 2
o.call(this); // 7 -- bound 3