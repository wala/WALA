
function objects_are_fun(arg1, arg2) {
  var local = new Object();
  var g = 7;

  local.f = arg1.foo;
  local.f();

  local.otherMethod = function nothing(arg1) {
    return arg1 - 7;
  };

  local[g] = arg2[ "bar" ];
  local[g]();

}

var arg1;
var arg2 = new Object();

arg1 = {
  foo: function whatever() {
    return 3 + 7;
  }
}

arg2.bar = function other() {
  return this.otherMethod( 3 );
}

arg2.otherMethod = function something(arg1) {
  return arg1 + 5;
}

arg2.bar( );

objects_are_fun( arg1, arg2 );

var numObj = new Number(4);
var strObj = new String("whatever");

var foo = strObj.toLowerCase();

var whatnot = [ , , , 7, numObj, arg2, strObj ];

whatnot[ 5 ].otherMethod( 7 );

delete arg2.bar();
delete whatnot[ 5 ];
delete arg2;
