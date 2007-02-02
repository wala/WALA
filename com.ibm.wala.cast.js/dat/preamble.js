// The following stuff has to be statically analyzed
// 1. readonly attributes
// 2. using setAttribute to register an event handler (this should be disallowed since it can be used like 'eval')
// 3. All methods properties assigned here are implicitly readonly
// 4. inheritance in this file is implemented by creating a new object for the prototype. Instead, the prototype object could be shared

// A combination of interfaces NodeList, NamedNodeMap, HTMLCollection
function NamedNodeList() {
	var maxLength = 10;
	var local = new Array(10);
	var counter = -1;
	function add(elem) {
		checkAndIncrease();
		local[counter++] = elem;
	}

	function getIndex(elem) {
		for(var traverse = 0; traverse <= counter; traverse++) {
			if(local[traverse] == elem) {
				return traverse;
			}
		}
		return -1;
	}

	function remove(elem) {
		var found = getIndex(elem);
		if(found > -1) {
			for(traverse = found; traverse < counter; traverse++) {
				local[traverse] = local[traverse+1];
			}
			counter--;
		}
	}

	function replace(newElem, oldElem) {
		var found = getIndex(oldElem);
		if(found > -1) {
			local[found] = newElem;
		}
	}

	function insertBefore(newElem, oldElem) {
		var found = getIndex(oldElem);
		if(found > -1) {
			checkAndIncrease();
			var prev = newElem;
			for(var traverse = counter + 1; traverse > found; traverse--) {
				local[traverse] = local[traverse-1];
			}
			local[found] = newElem;
		}
	}

	var checkAndIncrease = function() {
		if(counter >= maxLength - 1) {
			maxLength += 10;
			var temp = new Array(maxLength);
			for(traverse = 0; traverse <= counter; traverse++) {
				temp[traverse] = local[traverse];
			}
			local = temp;
		}
	}

	// implement a list of Nodes, accessible through names as well
	
}

function DOMNode() { // An impostor for the Node class
	this.attributes = new NamedNodeList();
	this.childNodes = new NamedNodeList();
	this.insertBefore = function(newChild, refChild) {
				this.childNodes.insertBefore(newChild, refChild);
			}
	this.replaceChild = function(newChild, oldChild) {
				this.childNodes.replace(newChild, oldChild);
			}
	this.removeChild = function(oldChild) {
				this.childNodes.remove(oldChild);
			}
	this.appendChild = function(newChild) {
				this.childNodes.add(newChild);
				newChild.parentNode = this;
			}
	this.hasChildNodes = function() {
				return this.childNodes.hasElements();
			}
	
	this.ownerDocument = document;
}

function DOMDocument() {
	this.prototype = new DOMNode();
	this.createElement = function(name) {
		// TODO : to be implemented accurately
		var toReturn = new DOMHTMLGenericElement(name);
		return toReturn;
	}
}

function DOMHTMLDocument() {
	this.prototype = new DOMDocument();
	this.getElementsByName = function(name) {
		// get the node in the tree with name attribute == name
	}
}

// Creating the root document object
var document = new DOMHTMLDocument();

function DOMElement() { // An impostor for the Element class
	// inherits from Node
	this.prototype = new DOMNode();

	// The get/set/remove attribute methods cannot be run using 'onclick','onmouseover', 'on...' kind of arguments for name.
	// since that would be used as a workaround for eval

	this.getAttribute = function(name) {
		this.attributes.get(name);
	}
	this.setAttribute = function(name, value) {
		this.attributes.set(name, value);
	}

	this.removeAttribute = function(name) {
		this.attributes.remove(name);
	}

	this.getElementsByTagName = function(name) {
		var toReturn = new NamedNodeList();
	}
}

function DOMHTMLElement() { // An impostor for the HTMLElement class
	// inherits from Element
	this.prototype = new DOMElement();

	// Set HTML Attribute Defaults
	this.id = null;
	this.title = null;
	this.lang = null;
	this.dir = null;
	this.className = null;

	// Set Javascript properties
	this.getAttribute = function(name) {
			if(name == "id") return this.id;
			else if(name == "title") return this.title;
			else if(name == "lang") return this.lang;
			else if(name == "dir") return this.dir;
			else if(name == "class") return this.className;
			else return this.attributes.get(name);
		}

	this.setAttribute = function(name, value) {
			if(name == "id") this.id = value;
			else if(name == "title") this.title = value;
			else if(name == "lang")  this.lang = value;
			else if(name == "dir")  this.dir = value;
			else if(name == "class") this.className = value;
			else return this.attributes.set(name, value);
		}

	this.removeAttribute = function(name) {
			if(name == "id") this.id = null;
			else if(name == "title") this.title = null;
			else if(name == "lang")  this.lang = null;
			else if(name == "dir") this.dir = null;
			else if(name == "class") this.className = null;
			else return this.attributes.remove(name);
		}
}

// Just a hack until all HTML elements have corresponding constructors
function DOMHTMLGenericElement(tagName) {
	// inherits from Element
	this.prototype = new DOMElement();

	// Set just the tag name
	this.nodeName = tagName;
	this.nodeValue = null;
}

function DOMHTMLFormElement() {
	// inherits from HTMLElement
	this.prototype = new DOMHTMLElement();

	// Set Javascript properties
	this.nodeName = "FORM";
	this.elements = new NamedNodeList();
	this.length = function() {
		return this.elements.length;
	}
	this.submit = function() {
	}
	this.reset = function() {
	}

	// Set HTML Attribute Defaults
	this.name = null;
	this.acceptCharset = "UNKNOWN";
	this.action = null;
	this.enctype = "application/x-www-form-urlencoded";
	this.method = "get";
	this.target = null;

	// Set Javascript properties
	this.getAttribute = function(name) {
			if(name == "name") return this.name;
			else if(name == "accept-charset") return this.acceptCharset;
			else if(name == "action") return this.action;
			else if(name == "enctype") return this.enctype;
			else if(name == "method") return this.method;
			else if(name == "target") return this.target;
			else return this.prototype.getAttribute(name);
		}

	this.setAttribute = function(name, value) {
			if(name == "name") this.name = value;
			else if(name == "accept-charset") this.acceptCharset = value;
			else if(name == "action") this.action = value;
			else if(name == "enctype") this.enctype = value;
			else if(name == "method") this.method = value;
			else if(name == "target") this.target = value;
			else return this.prototype.setAttribute(name, value);
		}

	this.removeAttribute = function(name) {
			if(name == "name") this.name = null;
			else if(name == "accept-charset") this.acceptCharset = null;
			else if(name == "action") this.action = null;
			else if(name == "enctype") this.enctype = null;
			else if(name == "method") this.method = null;
			else if(name == "target") this.target = null;
			else return this.prototype.removeAttribute(name);
		}
}

function DOMHTMLTableElement () {
	// inherits from HTMLElement
	this.prototype = new DOMHTMLElement();

	this.rows = function() {
	}	
}