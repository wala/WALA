function Obj(x) {
  var state = x;

  this.set = function setit(x) { state = x; };

  this.get = function getit() { return state; };

};


var obj = new Obj( function tester1() { return 3; } );

var test1 = ( obj.get() )();

obj.set( function tester2() { return 7; } );	

var test2 = ( obj.get() )();
