(ns fancy-lets.examples
  (:require [fancy-lets.core :refer [let-catch let-catch-2]]))

;; Example:
;;  Our program needs to access and update a datastore.
;;  That keeps track of customers and their balances.
;;  It needs to perform validation between the calls.

;; Let's quickly define a custom failure type.
(defn ->Failure [detail] {:type ::failure :detail detail})
(defn success? [object] (not= ::failure (get object :type)))
(defn print-failure-detail [object] (println "ERROR:" (get object :detail)))

;; Here is the mock data
(def customer-ids {"Albert" :alb "Billie" :bil "Candice" :can "Danielle" :dan})
(def init-customer-balances {:alb 9 :bil 3 :can 4 :dan 10})
(def customer-balances (atom init-customer-balances))

;; Here are some mock functions that the datastore uses.
(defn get-customer-id [name]
  (if-let [id (get customer-ids name)]
    id
    (->Failure (str "Customer " name " does not exist."))))

(defn get-balance [id]
  (if-let [amt (get @customer-balances id)]
    amt
    (->Failure (str "Customer " id " does not have a balance."))))

(defn set-balance! [id amt]
  (if (get-balance id)
    (swap! customer-balances assoc id amt)
    (->Failure (str "Customer " id " does not have a balance."))))

(defn subtract-balance! [id amt]
  (let [current-balance (get-balance id)]
    (if (nat-int? (- current-balance amt))
      (-> customer-balances
          (swap! update id - amt)
          (get id))
      (->Failure (str "Customer " id " does not have enough balance.")))))

;; ==============================================================
;; Using the regular `let` function...
;; This is a bit cumbersome and hard to follow, particularly if more
;; operations need to take place.
;; ==============================================================
(defn adjust-customers-balance! [name amt]
  (let [id   (get-customer-id name)]
    (if (success? id)
      (let [new-balance (subtract-balance! id amt)]
        (if (success? new-balance)
          (println "Success! New balance in" (str name "'s")
                   "account is" new-balance)
          (print-failure-detail new-balance)))
      (print-failure-detail id))))

(reset! customer-balances init-customer-balances)
(adjust-customers-balance! "Albert" 7)
;; => Success! New balance in Albert's account is 2
(adjust-customers-balance! "Candice" 7)
;; => ERROR: Customer :can does not have enough balance.
(adjust-customers-balance! "Dale" 7)
;; => ERROR: Customer Dale does not exist.

;; ==============================================================
;; Instead we can use `let-catch` to make it more streamlined.
;; ==============================================================
(defn adjust-customers-balance! [name amt]
  (let-catch success? print-failure-detail
             [id          (get-customer-id name)
              new-balance (subtract-balance! id amt)]
             (println "Success! New balance in" (str name "'s")
                      "account is" new-balance)))

(reset! customer-balances init-customer-balances)
(adjust-customers-balance! "Albert" 7)
;; => Success! New balance in Albert's account is 2
(adjust-customers-balance! "Candice" 7)
;; => ERROR: Customer :can does not have enough balance.
(adjust-customers-balance! "Dale" 7)
;; => ERROR: Customer Dale does not exist.

;; ==============================================================
;; Below is an example of using `let-catch-2` to perform
;; the same operation slightly similarly. Note that `let-catch-2`
;; allows us to perform validation on a line by line basis
;; Note that in this example we can validate that the balance is
;; sufficient in-line
;; ==============================================================
(defn adjust-customers-balance! [name amt]
  (let-catch-2
      [{:as id          :expr (get-customer-id name)
        :pred success? :fail-f print-failure-detail}
       {:as balance     :expr (get-balance id)
        :pred success? :fail-f print-failure-detail}
       {:as new-balance :expr (- balance amt)
        :pred nat-int? :fail-f
        (fn [_] (println "ERROR: Customer" id "does not have enough balance."))}]
    (println "Success! New balance in" (str name "'s") "account is" new-balance)))

(reset! customer-balances init-customer-balances)
(adjust-customers-balance! "Albert" 7)
;; => Success! New balance in Albert's account is 2
(adjust-customers-balance! "Candice" 7)
;; => ERROR: Customer :can does not have enough balance.
(adjust-customers-balance! "Dale" 7)
;; => ERROR: Customer Dale does not exist.

;; =============================================================
;; Lastly, here is just a very quick example that demonstrates
;; `let-catch-2`'s functionality if you don't give every row
;; all keys. Note that by default `:pred` is (constantly true),
;; which means that if you do not provide `:pred` for a row that
;; row will always "succeed" and continue
;; =============================================================
(defn square-then-divide [x y]
  (let-catch-2 [{:as x :expr x
                 :pred number?    :fail-f #(str % " is not a number")}
                {:as x :expr (* x x)}
                {:as y :expr y
                 :pred number?    :fail-f #(str % " is not a number")}
                {:as y :expr (inc y)
                 :pred #(not= 0 %) :fail-f (constantly "Cannot divide by 0")}]
    (/ x y)))

(println (square-then-divide 7 2))
;; => 49/3
(println (square-then-divide :b 2))
;; => :b is not a number
(println (square-then-divide 7 :c))
;; => :c is not a number
(println (square-then-divide 7 -1))
;; => Cannot divide by 0
