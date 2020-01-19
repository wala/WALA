
function collection() {
    // initially empty collection
    this.forall = function forall_base (f) { }; 

    // allow adding items
    this.add = function collection_add (new_item) {
	var oldforall = this.forall;
	this.forall = function forall_elt (f) {
	    oldforall(f);
	    f(new_item);
	};
    };
}