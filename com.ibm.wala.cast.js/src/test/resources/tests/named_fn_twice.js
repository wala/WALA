var x = {};
var y = 3;
// commented out the function statements here since their semantics are non-standard except
// under strict mode, see https://stackoverflow.com/a/10069457/1126796
if (y > 4) {
//    function testFunStmt() { return "firstStmt"; } ;
    x.m = function testFunExp() { return "first"; }
} else {
//    function testFunStmt() { return "secondStmt"; } ;
    x.m = function testFunExp() { return "second"; }
}
//testFunStmt();
x.m();
