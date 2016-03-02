(ns trout.route
  (:require [clojure.string :as string]
            [trout.settings :as cfg]
            [trout.generate :as gen]))


(defn- with-trailing-slash [s]
  (if cfg/*allow-trailing-slashes*
    (str s "(?:" cfg/*path-separator* "(?=$))?")
    s))

(defn- prefixed [pathv]
  (if cfg/*prefix*
    (vec (cons cfg/*prefix* pathv))
    pathv))

(defn- sep+ [& xs]
  (str cfg/*path-separator* (apply str xs)))

(defn- kw->pattern [kw]
  (let [pattern (str "[^" cfg/*path-separator* "]+?")]
    (if-not (namespace kw)
      (sep+ "(" pattern ")")
      (condp = (name kw)
        "?" (str "(?:" (sep+ "(" pattern ")") ")?")
        "*" (str "(?:" (sep+ "(" pattern "(?:" (sep+ pattern) ")*))?"))
        "+" (sep+ "(" pattern "(?:" (sep+ pattern) ")*)")))))

(defn- segment->pattern [x]
  (cond (string? x)  (sep+ x)
        (keyword? x) (kw->pattern x)
        (vector? x)  (sep+ (.-source (second x)))
        (= '* x)     (sep+ "(.*)")
        (regexp? x)  (sep+ (.-source x)) ;; TODO: this is cljs only. clj would be (.pattern x)
        ))

(defn- segment->varname [i x]
  (cond (string? x)   nil
        (keyword? x) (keyword (or (namespace x) (name x)))
        (vector? x)  (first x)
        (= '* x)     i
        (regexp? x)  i))

(defn- pathv->varnames [pathv]
  (loop [pathv pathv
         names []
         i 0]
    (if-not (empty? pathv)
      (let [name (segment->varname i (first pathv))]
        (recur (rest pathv)
               (conj names name)
               (if (number? name) (inc i) i)))
      names)))

(defn pathv->regexp [pathv]
  (->> pathv
       (prefixed)
       (#(map segment->pattern %))
       (apply str)
       (with-trailing-slash)
       (#(str "^" % "$"))
       (re-pattern)))


(defprotocol IRoute
  (-regexp [this])
  (path-str [this args])
  (match [this path]))

(deftype Route [pathv ^:mutable __regex]
  Object
  (toString [_]
    (str cfg/*path-separator* (string/join cfg/*path-separator* (prefixed pathv))))
  (equiv [_ other]
    (-equiv pathv other))

  IRoute
  (-regexp [_]
    (let [r __regex]
      ;; Regex is lazily computed, and then cached. All methods that
      ;; result in a new route also set the regex to nil again so that
      ;; it will be re-computed.
      (if (nil? r)
        (let [r (pathv->regexp pathv)]
          (set! __regex r)
          r)
        r)))
  (path-str [this args]
    (let [args (or args {})
          pathv (prefixed pathv)
          names (pathv->varnames pathv)
          segnames (map vector pathv names) ;; [[segment varname], ...]
          ->str (partial gen/segment->str args)]
      (let [s (string/join (map ->str segnames))]
        (if-not (empty? s) s cfg/*path-separator*))))
  (match [this path]
    (when-let [found (re-matches (-regexp this) path)]
      (let [names (remove nil? (pathv->varnames pathv))
            vals (rest found)]
        (zipmap names vals))))

  ICloneable
  (-clone [_] (Route. pathv __regex))

  IWithMeta
  (-with-meta [_ m]
    (Route. (-with-meta pathv m) __regex))

  IMeta
  (-meta [_] (meta pathv))

  IStack
  (-peek [_] (peek pathv))
  (-pop [_]
    (Route. (pop pathv) nil))

  ICollection
  (-conj [_ o]
    (Route. (conj pathv o) nil))

  IEmptyableCollection
  (-empty [_] (Route. (empty pathv) nil))

  ISequential
  IEquiv
  (-equiv [_ other] (-equiv pathv other))

  IHash
  (-hash [_] (hash pathv)) 

  ISeqable
  (-seq [_] (seq pathv))

  ICounted
  (-count [_] (-count pathv))

  IIndexed
  (-nth [_ n] (-nth pathv n))
  (-nth [_ n not-found] (-nth pathv n not-found))

  ILookup
  (-lookup [_ k] (-lookup pathv k))

  IMapEntry
  (-key [_] (-key pathv))
  (-val [_] (-val pathv))

  IAssociative
  (-assoc [_ k v] (Route. (-assoc pathv k v) nil))

  IVector
  (-assoc-n [_ n val] (Route. (-assoc-n pathv n val) nil))

  IReduce
  (-reduce [_ f] (-reduce pathv f))
  (-reduce [_ f init] (-reduce pathv f init))

  IKVReduce
  (-kv-reduce [_ f init] (-kv-reduce pathv f init))

  IFn
  (-invoke [_ k] (-invoke pathv k))
  (-invoke [_ k not-found] (-invoke pathv k not-found))

  IEditableCollection
  (-as-transient [_] (-as-transient pathv))

  IIterable
  (-iterator [this] (-iterator pathv))
  )


;; TODO: uncomment this when cljs gets print-method
;;       or figure out how to tie into various cljs repls  
#_(defmethod print-method Route [o writer]
    (.write writer (str "Route:[" (string/join (.-pathv o) cfg/*path-separator*) "]")))
