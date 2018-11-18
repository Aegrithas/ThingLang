# ThingLang
A toy interpreter for a programming language I came up with.

The basic language is as follows:
  The top level contains only definitions and `do` blocks; `do` blocks and any expressions involved in the definitions except function bodies are run immidiately; the bodies of `do` blocks and functions are single expressions, including a `;` operator similar to C's `,` operator (that is, both operands are evaluated, and the value of the whole expression is the value of the second operand).
  Everything except top-level function definitions and `do` blocks are expressions. There are local function definitions, but they differ from top-level functions in two ways: 1) in local functions the name is optional (and in fact, a local `func name(...) ...` is simply syntax candy for `let name = func(...) ...`) and 2) local function bodies (as with several other constructs including control flow expressions) cannot contain the `;` operator without being surrounded by parentheses.
  The value of each control flow expressions is as expected, but there are a few things that ought to be explicitly pointed out:
 * The `else` branches of loops are only executed if the loop never executes
 * The value of a `try` expression is never the `finally` branch, but a `return`, `throw`, `continue` or `break` in the finally branch will preempt any of those in either of the other branches.
 * Loops can be supplied with a combinator, which is either a binary operator or a function accepting two arguments
   * Without a combinator, the value of the loop is the value of the last iteration
   * With a combinator and only one iteration, the value of the loop is the value of that one iteration
   * With combinator `f`, the value of the loop is `f(f(f(v0, v1), ...), vN)` where `vI` is the value of the `I`th iteration. A (potentially) simpler explanation of this is that the mathematical summation <img src="https://latex.codecogs.com/gif.latex?\sum_{i=n}^{m}e" title="\sum_{i=n}^{m}e" /> can be written as `for var i = n; n < m; i++ do + on e`.

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
