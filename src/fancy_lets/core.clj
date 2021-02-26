(ns fancy-lets.core
  (:require [fancy-lets.internals :refer
             [xor -valid-let-catch-2-bindings? -throw-let-catch-2-error]]))

(defmacro let-catch
  "Recursive version of let that verifies that each bound value passes
   `pred`. If (pred expr) for any of the bound expressions fails, the
   let function short circuits and returns (fail-f expr).
   If all of the expressions pass the pred, the body is evaluated.

   See `fancy-lets.examples` for examples."

  [pred fail-f bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    (let [k (first bindings)
          v (second bindings)]
      `(let [~k ~v]
         (if (~pred ~k)
           (let-catch ~pred ~fail-f ~(->> bindings (drop 2) vec) ~@body)
           (~fail-f ~k))))))

(defmacro let-catch-2
  "Similar to `let-catch`, however the each binding is fed in the form
   of a map with keys :as, :expr, :pred, :fail-f. This allows you to fine
   tune the behaviour of let-catch.

   If a row has `:pred` then it must ALSO have `:fail-f`
   If `:pred` is not provided, it will default to (constantly true)

   See `fancy-lets.examples` for examples."

  [bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    (let [{:keys [expr as
                  pred fail-f] :as m} (first bindings)
          pred   (or pred (fn [_] true))
          fail-f (or fail-f    identity)]
      (if (-valid-let-catch-2-bindings? m)
        `(let [~as ~expr]
           (if (~pred ~as)
             (let-catch-2 ~(rest bindings) ~@body)
             (~fail-f ~as)))
        (-throw-let-catch-2-error m)))))
