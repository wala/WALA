function f(x) {
   return function ff(y) {
     return function fff(z) {
       return x+y+z;
     }
   }
}

var g1 = f(1);
var g2 = g1(2);
var g3 = g2(3);
