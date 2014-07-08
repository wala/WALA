function dead_code() {

}

function f() {
 lbl: {
   break lbl;
   dead_code();
 }
}
