(ns fancy-lets.core)

(defn- throw-let-catch-exception [a b c d]
  (if (= a :validate)
    (throw (Exception. (str "Improper format for let-catch: each `:validate`"
                            " clause must be followed by an `:else` clause.")))
    (throw (Exception. (str "Improper format for let-catch: expected either a "
                            " simple symbol or `:validate`. Instead received " a)))))

(defmacro let-catch
  "Recursive version of `let` that can be configured to short-circuit.
   Adding the keyword `:validate` and an assoicated `:else` performs
   runs the validate expression. If the expression returns
   nil or false, the let statement is short-circuited and the
   else expression executed and returned.

   e.g. (let-catch [a 3
                    :validate (odd? a) :else (str a \" is not odd.\")
                    b 4
                    :validate (odd? b) :else (str b \" is not odd.\")]
         (+ a b))
         Would return: \"4 is not odd.\"

   See `fancy-lets.examples` for more examples."
  [bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    (let [[a b c d & _] bindings]
      (cond (symbol? a)
            `(let [~a ~b]
               (let-catch ~(->> bindings (drop 2) vec) ~@body))
            (= [:validate :else] [a c])
            `(if ~b
               (let-catch ~(->> bindings (drop 4) vec) ~@body)
               ~d)
            :else (throw-let-catch-exception a b c d)))))

(defmacro let-catch-all
  "Similar to `let-catch` except you provide a `pred` and `fail-f`. Each
   bound value is checked after it is bound. If the (pred val) returns
   nil or false, (fail-f val) is evaluated and the let statement short-circuits.

   e.g. The following example is identical to the above example:
        (let-catch-all odd? #(str % \" is not odd.\")
           [a 3
            b 4]
         (+ a b))
        Which would return: \"4 is not odd.\"

   See `fancy-lets.examples` for more examples."
  [pred fail-f bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    (let [[a b] bindings]
      `(let [~a ~b]
         (if (~pred ~a)
           (let-catch-all ~pred ~fail-f ~(->> bindings (drop 2) vec) ~@body)
           (~fail-f ~a))))))
