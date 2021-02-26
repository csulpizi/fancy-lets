(ns fancy-lets.internals)

(defmacro xor
  "Given a set of expressions, validate that exactly
   one of the expressions is a non-false, non-nil value."
  [& exprs]
  (if (seq exprs)
    `(if ~(first exprs)
       (and ~@(map (partial list not) (rest exprs)))
       (xor ~@(rest exprs)))
    false))

(defn -valid-let-catch-2-bindings?
  "Check that :expr and :as are contained in the map,
   and that if :pred is supplied that so is :fail-f"
  [m]
  (and (contains? m :expr)
       (contains? m :as)
       (xor (and (contains? m :pred)
                 (contains? m :fail-f))
            (and (not (contains? m :pred))
                 (not (contains? m :fail-f))))))

(defn -throw-let-catch-2-error [m]
  (throw (Exception.
          (str ":fancy-lets.core/let-catch-2"
               " error. Must provide keys :expr and :as, and either"
               " a) both :pred and :fail-f"
               " b) neither :pred nor :fail-f."
               " Given: " m))))
