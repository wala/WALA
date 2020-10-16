var x = {};
var y = 3;
if (y > 4) {
    function testFunStmt() { return "firstStmt"; } ;
    x.m = function testFunExp() { return "first"; }
} else {
    function testFunStmt() { return "secondStmt"; } ;
    x.m = function testFunExp() { return "second"; }
}
testFunStmt();
x.m();