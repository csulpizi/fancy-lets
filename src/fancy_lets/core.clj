(ns fancy-lets.core)

(defn- throw-let-catch-exception [a b c d]
  (if (= a :validate)
    (throw (Exception. (str "Improper format for let-catch: each `:validate`"
                            " clause must be followed by an `:else` clause.")))
    (throw (Exception. (str "Improper format for let-catch: expected either a "
                            " simple symbol or `:validate`. Instead received " a)))))

(defmacro let-catch
 "
   See `fancy-lets.examples` for examples."

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
  "
   See `fancy-lets.examples` for examples."

  [pred fail-f bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    (let [[a b] bindings]
      `(let [~a ~b]
         (if (~pred ~a)
           (let-catch-all ~pred ~fail-f ~(->> bindings (drop 2) vec) ~@body)
           (~fail-f ~a))))))
