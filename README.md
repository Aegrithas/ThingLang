# ThingLang
A toy interpreter for a programming language I came up with.

I think much of the language is fairly intuitive for those familiar with other programming languages (particularly JavaScript and Lua), however there are a few things I think are worth mentioning. Please bear with me if I skip something important, though, because this isn't supposed to be a full explanation; for that, see the grammar and code.
  The top level contains only definitions and `do` blocks; `do` blocks and any expressions involved in the definitions except function bodies are run immidiately; the bodies of `do` blocks and functions are single expressions, including a `;` operator similar to C's `,` operator (that is, both operands are evaluated, and the value of the whole expression is the value of the second operand).
  Everything except top-level function definitions and `do` blocks are expressions. There are local function definitions, but they differ from top-level functions in three ways: 1) in local functions the name is optional (and in fact, a local `func name(...) ...` is simply syntax candy for `let name = func(...) ...`), 2) local function bodies (as with several other constructs including control flow expressions) cannot contain the `;` operator without being surrounded by parentheses and 3) local functions do not have a `.` following their body.
  The value of each control flow expressions is as expected, but there are a few things that ought to be explicitly pointed out:
 * The `else` branches of loops are only executed if the loop never executes
 * The value of a `try` expression is never the `finally` branch, but a `return`, `throw`, `continue` or `break` in the finally branch will preempt any of those in either of the other branches.
 * Variable definitions within a `with` block are defined in the containing scope, not on the "`with`'d" object.
 * Loops can be supplied with a combinator, which is either a binary operator or a function accepting two arguments.
   * Without a combinator, the value of the loop is the value of the last iteration.
   * With a combinator and only one iteration, the value of the loop is the value of that one iteration.
   * With combinator `f`, the value of the loop is `f(f(f(v0, v1), ...), vN)` where `vI` is the value of the `I`th iteration. A (potentially) simpler explanation of this is that the mathematical summation <img src="https://latex.codecogs.com/gif.latex?\sum_{i=n}^{m}e" title="\sum_{i=n}^{m}e" /> can be written as `for var i = n; n < m; i++ do + on e`.
 It's also worth noting the four different syntaxes for object creation:
  * Array notation: a comma separated list of the elements enclosed in square brackets; the key for each element is its zero-based index in that list.
 * Dictionary notation: a comma separated list of `key: value` pairs enclosed in square brackets.
 * "JSON" notation: similar to JSON; a comma separated list of `key: value` pairs enclosed in curly braces; however, unlike the dictionary notation, the keys are not precise expressions of the object's keys, but rather:
   * An id; the actual key is a string whose content is the name of this id
   * A simple literal (a string, a number, `true`, `false` or `nil`); this is the literal value of the key
   * Or any expression, surrounded by square brackets; the key is the value of the contained expression
 * "Object Oriented" notation: curly braces enclosing a sequence of top level-like definitions; however, the names of the definitions are specified in the same way as the keys of JSON notation.

Requires Antlr 4.7.

Known bugs that I didn't really want to bother fixing:
 * String escape sequences don't work; this is particularly bothersome in cases such as `"I say \"Hello World!\" sometimes."` because, while the "escaped" quotes don't end the string token, the backslashes are still present in the actual runtime string.
 * Operators do not have proper precedence.
 * Arrays are currently the only thing that can be iterated on; this should be revamped more like Java's iterable system.
 * There really should be more builtin functions etc.

Bonus features I'm too lazy to implement (yet):
 * Some way to directly access the metaobjects of the builtin types (maybe via `Symbol`?)
 * Move all those types (and presumably the metaobjects as well) to a `Types` object
 * Anything like importing
 * A `delete object[field]`/`delete variable` operator and a few other object utilities like a way to prevent the definition of more fields
 * A OO-style class syntax as candy (an example is in demo.thing) and an `instanceof` (or similar) operator
 * An array metaobject and type symbol
 * Operator overloading
