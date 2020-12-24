// IIFE, named function
(function f1() {})();

// IIFE, anon function
(function () {})();

// parameters and returns
function id(p) {
  return p;
}

function callId() {
  var x = {};
  var y = id(x);
}

function callIdHigherOrder() {
  var fp = id;
  var x = {};
  var y = fp(x);
}

function voidFun() {
  return;
}

voidFun();

function functionPrototypeCallApply() {
  function nested() {}
  var x = id;
  var res1 = x.call(this, nested);
  var res2 = x.apply(this, [nested]);
}
