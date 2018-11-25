# ÞiŋLang
A toy interpreter for a programming language I came up with.

Wiki to come.

Requires Antlr 4.7.

Bonus features I'm too lazy to implement (yet):
 * Some way to directly access the metaobjects of the builtin types (maybe via `Symbol`?)
 * Move all those types (and presumably the metaobjects as well) to a `Types` object
 * Anything like importing
 * A `delete object[field]`/`delete variable` operator and a few other object utilities like a way to prevent the definition of more fields
 * A OO-style class syntax as candy (an example is in demo.thing) and an `instanceof` (or similar) operator
 * An array metaobject and type symbol
 * Operator "overloading"
