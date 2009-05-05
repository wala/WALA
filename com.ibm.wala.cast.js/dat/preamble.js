// The following stuff has to be statically analyzed
// 1. readonly attributes
// 2. using setAttribute to register an event handler (this should be disallowed since it can be used like 'eval')
// 3. All methods properties assigned here are implicitly readonly
// 4. inheritance in this file is implemented by creating a new object for the prototype. Instead, the prototype object could be shared

// A combination of interfaces NodeList, NamedNodeMap, HTMLCollection
// implement a list of Nodes, accessible through names as well
function NamedNodeList() {
	var maxLength = 10;
	var local = new Array(10);
	var counter = -1;

	var checkAndIncrease = function checkAndIncrease() {
		if(counter >= maxLength - 1) {
			maxLength += 10;
			var temp = new Array(maxLength);
			for(traverse = 0; traverse <= counter; traverse++) {
				temp[traverse] = local[traverse];
			}
			local = temp;
		}
	}

        this.get = function get(index) {
                return local[ index ];
	}

	this.add = function add(elem) {
		checkAndIncrease();
		local[counter++] = elem;
	}

	this.getIndex = function getIndex(elem) {
		for(var traverse = 0; traverse <= counter; traverse++) {
			if(local[traverse] == elem) {
				return traverse;
			}
		}
		return -1;
	}

	this.remove = function remove(elem) {
		var found = getIndex(elem);
		if(found > -1) {
			for(traverse = found; traverse < counter; traverse++) {
				local[traverse] = local[traverse+1];
			}
			counter--;
		}
	}

	this.replace = function replace(newElem, oldElem) {
		var found = getIndex(oldElem);
		if(found > -1) {
			local[found] = newElem;
		}
	}

	this.insertBefore = function insertBefore(newElem, oldElem) {
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

	this.collect = function collect(predicate, result) {
          for(var traverse = 0; traverse <= counter; traverse++) {
            local[traverse].collect(predicate, result);
          }
        }
}

function DOMNode() { // An impostor for the Node class
	this.attributes = new NamedNodeList();
	this.childNodes = new NamedNodeList();
	this.insertBefore = function insertBefore(newChild, refChild) {
				this.childNodes.insertBefore(newChild, refChild);
			}
	this.replaceChild = function replaceChild(newChild, oldChild) {
				this.childNodes.replace(newChild, oldChild);
			}
	this.removeChild = function removeChild(oldChild) {
				this.childNodes.remove(oldChild);
			}
	this.appendChild = function appendChild(newChild) {
				this.childNodes.add(newChild);
				newChild.parentNode = this;
			}
	this.hasChildNodes = function hasChildNodes() {
				return this.childNodes.hasElements();
			}
	
	this.ownerDocument = document;

	this.collect = function collect(predicate, result) {
          if (predicate(this)) {
            result.add(this);
          }
          this.childNodes.collect(predicate, result);
        }
}

function DOMDocument() {
	this.temp = DOMNode;
	this.temp();

	this.createElement = function createElement(name) {
		// TODO : to be implemented accurately
		var toReturn = new DOMHTMLGenericElement(name);
		return toReturn;
	}

    this.getElementById = function getElementById(id) {
          var result = new NamedNodeList();
          this.collect(function check_id(x) { return x.id == id; }, result);
          return result.get(0);
	}
	
	this.write = function write_to_dom (stuff) {
		
	};
}

function DOMHTMLDocument() {
	this.temp = DOMDocument;
	this.temp();

}

// Creating the root document object
var document = new DOMHTMLDocument();

function DOMElement() { // An impostor for the Element class
	// inherits from Node
	this.temp = DOMNode;
	this.temp();

	// The get/set/remove attribute methods cannot be run using 'onclick','onmouseover', 'on...' kind of arguments for name.
	// since that would be used as a workaround for eval

	this.getAttribute = function getAttribute(name) {
		this.attributes.get(name);
	}
	this.setAttribute = function setAttribute(name, value) {
		this.attributes.set(name, value);
	}

	this.removeAttribute = function removeAttribute(name) {
		this.attributes.remove(name);
	}

}

function DOMHTMLElement() { // An impostor for the HTMLElement class
	// inherits from Element
	this.temp = DOMElement;
	this.temp();

	// Set HTML Attribute Defaults
	this.id = null;
	this.title = null;
	this.lang = null;
	this.dir = null;
	this.className = null;

	// Set Javascript properties
	this.getAttribute = function getAttribute(name) {
			if(name == "id") return this.id;
			else if(name == "title") return this.title;
			else if(name == "lang") return this.lang;
			else if(name == "dir") return this.dir;
			else if(name == "class") return this.className;
			else return this.attributes.get(name);
		}

	this.setAttribute = function setAttribute(name, value) {
			if(name == "id") this.id = value;
			else if(name == "title") this.title = value;
			else if(name == "lang")  this.lang = value;
			else if(name == "dir")  this.dir = value;
			else if(name == "class") this.className = value;
			else return this.attributes.set(name, value);
		}

	this.removeAttribute = function removeAttribute(name) {
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
	this.temp = DOMElement;
	this.temp();

	// Set just the tag name
	this.nodeName = tagName;
	this.nodeValue = null;
}

function DOMHTMLFormElement() {
	// inherits from HTMLElement
	this.temp = DOMHTMLElement;
	this.temp();

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
	this.getAttribute = function getAttribute(name) {
			if(name == "name") return this.name;
			else if(name == "accept-charset") return this.acceptCharset;
			else if(name == "action") return this.action;
			else if(name == "enctype") return this.enctype;
			else if(name == "method") return this.method;
			else if(name == "target") return this.target;
			else return this.prototype.getAttribute(name);
		}

	this.setAttribute = function setAttribute(name, value) {
			if(name == "name") this.name = value;
			else if(name == "accept-charset") this.acceptCharset = value;
			else if(name == "action") this.action = value;
			else if(name == "enctype") this.enctype = value;
			else if(name == "method") this.method = value;
			else if(name == "target") this.target = value;
			else return this.prototype.setAttribute(name, value);
		}

	this.removeAttribute = function removeAttribute(name) {
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
	this.temp = DOMHTMLElement;
	this.temp();

	this.rows = function() {
	}	
}