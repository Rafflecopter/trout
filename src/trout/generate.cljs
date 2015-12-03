(ns trout.generate
  (:require [clojure.string :as string]
            [trout.settings :as cfg]
            goog.string.format ; have to require this separately
            [goog.string :as gstring]))


(defn- invalid! [s & args]
  (throw (js/TypeError. (apply gstring/format s args))))


(defn- segment-operand [x]
  (let [kw (cond (keyword? x) x
                 (coll? x) (first x)
                 :else nil)]
    (when (and kw (some? (namespace kw)))
      (name x))))

(defn- segment-re [x]
  (cond (regexp? x) x
        (coll? x) (second x)
        (= '* x) #".*"
        :else nil))

(defn- describe-segment [segment]
  (let [operand (segment-operand segment)]
    {:repeat? (contains? #{"+" "*"} operand)
     :optional? (contains? #{"?" "*"} operand)
     :match (segment-re segment)}))

(defn- validate-segment! [segment varname arg]
  (let [{:keys [repeat? optional? match]} (describe-segment segment)]
    (when (and (not repeat?) (coll? arg))
      (invalid! "Expected %s not to repeat, but received %s" varname arg))
    (when (and (not optional?) (nil? arg))
      (invalid! "Expected %s to be defined" varname))
    (when (and (not optional?) (coll? arg) (empty? arg))
      (invalid! "Expected %s to not be empty" varname))
    (when (and match (nil? (re-matches match (str arg))))
      (invalid! "Expected %s to match %s, but received %s" varname match arg))))


(defn segment->str [args [segment varname]]
  (str cfg/*path-separator*
       (if (nil? varname)
         segment
         (let [arg (args varname)]
           (validate-segment! segment varname arg)
           (if (coll? arg)
             (string/join cfg/*path-separator* arg)
             arg)))))

