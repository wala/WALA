
var fs = [ function one() { return "one" }, 
           function two() { return "two" }, 
           function three() { return "three" }, 
           function four() { return "four" } ];

var fs2 = [];

(function _loop(fs1, fs2) {
  for(var i = 0; i < fs.length; i++) {
    fs2[i] = i;	
  }
})(fs1, fs2);
  
(fs[fs2[2]])();
(fs[fs2[3]])();
