(ns trout.core
  (:require [clojure.string :as string]
            [trout.settings :as cfg]
            [trout.route :as r]))


;; Default configuration

(set! cfg/*navigator* (fn [path-str]
                        (.replace js/location path-str)))


;; This pattern is the base identifier of named param matches.
;; Ours are just like express-style, but we also allow hyphens.
;; See https://github.com/pillarjs/path-to-regexp#named-parameters 
(def ^:private npbase "^:([A-Za-z0-9_-]+)")


(def ^:private re
  (comp re-pattern str))

(def ^:private str-conversions
  {(re npbase "$")           keyword                        ; named param
   (re npbase "\\?$")        #(keyword % "?")               ; optional param
   (re npbase "\\*$")        #(keyword % "*")               ; zero or more
   (re npbase "\\+$")        #(keyword % "+")               ; one or more
   (re npbase "(\\(.*\\))$") #(vector (keyword %1) (re %2)) ; custom match pattern
   #"(^\(.*\)$)"             #(re-pattern %)                ; unnamed param
   #"^\*$"                   #(re-pattern "(.*)")           ; shorthand unnamed param
   })


;; Helpers

(defn- separator-regexp []
  (re-pattern cfg/*path-separator*))

(defn- not-separator-regexp []
  (re-pattern (str "\\((.*)" cfg/*path-separator* "(.*)\\)")))

(defn- de-sentinel [x]
  (string/replace x #"\^\^\^" cfg/*path-separator*))

(defn- str->route-segment [s]
  (or (some (fn [[identifier replacement]]
              (when-let [match (re-matches identifier s)]
                (apply replacement (rest match))))
            str-conversions)
      s))

(defn- str->pathv [s]
  (let []
    (-> s
        ;; Replace inline occurances of path separator that are not
        ;; actually path separators with a sentinel value (ie. something
        ;; that couldn't feasibly appear in a valid regex).
        ;; We use ^^^ for the sentinel. (three carets would be \^\^\^)
        ;; In this case, inline occurances will always be inside
        ;; parentheses, whereas valid path separators will never be, so
        ;; it's simple to identify them.
        (string/replace (not-separator-regexp) "($1^^^$2)")

        ;; Now that inline occurances of path separator are out of the
        ;; way, we can simply split our string and treat each piece as
        ;; a path segment.
        (string/split (separator-regexp))

        ;; Blank strings are invalid path segments, so discard them.
        ;; (usually they're due to slashes at either end)
        (#(remove empty? %))

        ;; Now clean up our sentinel values from earlier
        (#(map de-sentinel %))

        ;; Turn our string segments into clojure data types
        (#(map str->route-segment %))

        vec)))


;; API

(defn route [path]
  (cond
    (vector? path) (r/Route. path nil)
    (seq? path) (r/Route. (vec path) nil)
    (string? path) (r/Route. (str->pathv path) nil)
    :else (throw (str "Routes can only be made from strings or collections. Instead, path is a " (type path)))))

(defn matches? [-route path]
  (r/matches? -route path))

(defn ->str [-route & [{:as argv}]]
  (r/path-str -route argv))

(defn route-map [m]
  {})

(defn navigate! [-route & [args]]
  {:pre [(implements? -route r/IRoute)]}
  (let [path (.path-str -route args)]
    (try (cfg/*navigator* path)
         (catch js/Object e
           (.log js/console "Cannot navigate!")))))
