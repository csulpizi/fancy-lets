# fancy-lets

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

Args: `bindings & body`

`let-catch` is used similarly to let, except you can provide it validation expressions in-line in the form `:validate (expression) :else (expression)`. The bindings and validations are performed in order. If any of the validations fail, the `:else` clause of that validation is called and the `let-catch` statement short-circuits. Otherwise, the statement continues as expected. 

Let's say you have functions, `f`, `g` and `h` that return failure objects when something went wrong, a `success?` function that checks that a return object is not a failure, and a `log-failure` function logs out any errors. You could write the following, for example:

```
(let-catch
 [a (f)
  :validate (success? a) :else (log-failure a)
  b (g)
  :validate (success? b) :else (log-failure b)
  c (h a b)
  :validate (success? c) :else (log-failure c)]
 c)
```
Note that this function behaves similarly to the earlier example, but the pattern is much cleaner and easier to read.

Assuming that `a`, `b` and `c` were all not failure objects, the statement above would return `c`. If `a` was a failure, however, the statement would short-circuit and call `handle-failure` on `a`. Since the statement was short-circuited, the functions `g` and `h` were never even called.

Note that you do not need to put in any validating statements. You could write the following
```
(let-catch 
 [a 1 b 2 c 3] 
 (+ a b c))
```
which, as you might expect, would return 6.

Similarly, you do not need to include validation statements for every line, or even bindings for that matter. Both of the following would be okay implementations of `let-catch`'s bindings:
```
[a (f)
 :validate (number? a) :else "Not a number"
 :validate (pos? a)    :else "Not positive"
 b (g a)]
```
```
[:validate (pos? 1) :else "Something is wrong with the universe."
 :validate (neg? 1) :else "This is expected"]
```

### `let-catch-all`

Args: `pred fail-f bindings & body`

`let-catch-all` is a more generalized version of `let-catch`. Instead of providing validation statements in specific spots, you provide a `pred` and a `fail-f` at the start, and each bound value is evaluated. 

So something like
```
(let-catch-all 
 pos-int? 
 (fn [x] (println "ERROR!" x "is not a positive integer")
 [a (f)
  b (g)
  c (+ a b)]
 c)
 ```
 would behave identically to
 ```
 (let-catch 
  [a (f)
   :validate (pos-int? a)
   :else (println "ERROR!" a "is not a positive integer")
   b (g)
   :validate (pos-int? b)
   :else (println "ERROR!" b "is not a positive integer")
   c (+ a b)
   :validate (pos-int? c)
   :else (println "ERROR!" c "is not a positive integer")]
  c)
 ```
This can be useful if you want to validate each form with the same pred and the same failure handler. Perhaps a good use case would be when using a custom failure framework.
