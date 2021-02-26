# fancy-Lets

A Clojure library that provides short-circuiting lets. These macros allow you to perform validation in-line between each binding. See `fancy-lets.examples` for examples.

## Rationale

Sometimes validation needs to be performed between each step of a function. Refer to the following case for example:
```
(let [a (f)
      b (g)]
 (if (pred? a b)
  (let [c (h a b)]
   (if (pred? c)
    c
    :error))
  :error))
```
This pattern is problematic. This snippet is a fairly simple operation, but it is cumbersome and difficult to read. This library provides `let-catch` and `let-catch-2` to alleviate these problems.

## Usage

### `let-catch`

`let-catch` is used similarly to let, except you provide it with a predicate function `pred` and a failure function `fail-f`. Each binding is handled one-by-one, and the bound values are checked after being evaluated to see if the value matches the condition defined by pred. If a bound value fails, the statement short circuits and the `fail-f` function is called on that value. On the other hand, if all of the values pass the predicate, everything will continue as it would in a regular `let` statement.

This is particularly useful if you have a custom failure object. Imagine that you have functions `success?` which checks that an object is not a failure object, and `handle-failure` which performs some action based on that failure. Then you could write the following statement, for example:

```
(let-catch success? handle-failure
   [a (f)
    b (g)
    c (h a b)]
   c)
```
Note that this function behaves similarly to the earlier example, but the pattern is much cleaner and easier to read.

Assuming that `a`, `b` and `c` were all not failure objects, the statement above would return `c`. If `a` was a failure, however, the statement would short-circuit and call `handle-failure` on `a`. Since the statement was short circuited, the functions `g` and `h` were never even called.

### `let-catch-2`

`let-catch-2` is a more specialized version of `let-catch`. Instead of feeding it a single predicate and a single failure function, each bound value is given its own predicate and its own failure function. This gives you more control over exactly how your binding statement behaves. You could write the following statement, for example:
```
(let-catch-2 [a         (f)
              :validate (pos-int? a)
              :else     (println "Failure. `a` is not a positive integer.")
              b         (g)
              c         (h a b)
              :validate (success? c)
              :else     (handle-failure c)]
   c)
```
The statement below would short-circuit if a 
