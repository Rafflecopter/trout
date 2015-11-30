```
DO NOT USE IN PRODUCTION. Trout is a work in progress. Some things are missing/broken.
```
<br>

<img src="http://f.cl.ly/items/0T1e2c243a1F1S2k1033/trout.svg" align=right width=200>

# Trout

Trout is a bi-directional "routes are data"
route matching/lookup library for ClojureScript that aims to be intuitive to use.

- Embraces route [syntax](#route-syntax).
- Routes are just vectors - even ones created via route-strings.<br>
  Extend & compose them to your heart's content.
- No keeping track of routes for you. No `defroute`s or macros necessary.
- Routes can be "nested" a la compojure's `context`
- Uses [Schema](https://github.com/Prismatic/schema) for parameter type conversion


## Usage

Require the library:

```clojure
(require '[trout.core :as t])
```

Make a simple route:

```clojure
(t/route "/user/:id")
(t/route ["user" :id]) ;; equivalent route in vector notation
```

#### Routes are Vectors

... do vector things to them:

```clojure
(let [route (t/route "/user/:id/settings")]

  (str (cons "my-site" route))  ;;=> "/my-site/user/:id/settings"
  (str (conj route "billing"))  ;;=> "/user/:id/settings/billing"
  (str (assoc route 1 :ident))  ;;=> "/user/:ident/settings"
  (str (pop route))             ;;=> "/user/:id"

  ;; reverse gives you a new route, not a seq
  (str (reverse route))         ;;=> "/settings/:id/user")
  )
```

#### Matching and Parsing

Test a route against some paths:

```clojure
(let [route (t/route "/user/:id")]

  (t/matches? route "/user/123")  ;;=> true
  (t/matches? route "/user/123/") ;;=> true  (trailing slashes are configurable)

  (t/matches? route "/some/other/route") ;;=> false
  )
```

Extract the parameters:

```clojure
(let [route (t/route "/user/:user-id/dirs/:dirname")]

  (t/match route "/user/123/dirs/foobar") ;;=> {:user-id "123", :dirname "foobar"}
  (t/match route "/user/xyz/dirs/789")    ;;=> {:user-id "xyz", :dirname "789"}

  (t/match route "/not/a/valid/match")    ;;=> nil
  )
```

Test multiple routes at once:

```clojure
(let [routes [(t/route "/user/:id")                ;; route-string syntax
              (t/route ["user" :id "settings"])]]  ;; vector syntax

  (t/matches? routes "/user/123/settings") ;;=> true
  (t/match routes "/user/123")             ;;=> {:id "123"}

  (t/matches? routes "/user/settings")     ;;=> false
  (t/match routes "/log-me-in")            ;;=> nil
  )
```

#### Generating

You can generate a string from a route + arguments:

```clojure
(let [route (t/route "/user/:id")]

  (t/->str route {:id 123}) ;;=> "/user/123"
  )
```

#### Navigating

Trout can change the browser's location for you:

```clojure
(let [route (t/route "/user/:id")]

  (t/navigate! route {:id 123})  ;;=> document location will be ".../user/123"
  )
```

#### Miscellany


To match against a whole URL instead of a relative path:

```clojure
(t/matches? route "http://site.com/user/123" :url true)
```

You can also match against `js/Location` objects:

```clojure
(let [route (t/route "/user/:id")
      loc (.-location js/document)] ;; href = "http://site.com/user/123"

  (t/matches? route loc) ;;=> true
  )
```

Get the actual route that matched:

```clojure
(let [routes {:usr (t/route "/user/:id")}]

  (t/match routes "/user/123")        ;;=> #object[trout.core.Route]
  (str (t/match routes "/user/123"))) ;;=> "/user/:id"
```


### Maps

When matching multiple routes, you can use maps instead of seqs. Trout will match against `(vals x)`:

```clojure
(let [routes {:usr      (t/route "/user/:id")
              :my-route (t/route ["user" :id "settings"])}]

  (t/matches? routes "/user/123/settings") ;;=> true
  (t/matches? routes "/not-a-route")       ;;=> false

  (t/->str (:my-route routes) {:id 123})   ;;=> "/usr/123/settings"
  )
```

You use nested maps to describe nested routes a la compojure's `context`:

```clojure
(t/route-map
 {:user ["/user/:id"
         {:settings ["/settings" 
                     {:acct "/acct"}]
          :profile "/profile"
          :home "/home"}]}) ;=> {:user                "/user/:id"
                            ;    :user/home           "/user/:id/home"
                            ;    :user/profile        "/user/:id/profile"
                            ;    :user/settings       "/user/:id/settings"
                            ;    :user/settings/acct  "/user/:id/settings/acct"}
```

Matching is straightforward:

```clojure
(t/matches? my-routemap "/user/123/settings") ;;=> true
(t/match my-routemap "/user/123/settings")    ;;=> #object[trout.core.Route]
```


## Route Syntax

Trout implements<sup>[1](#1)</sup> **[Express-style route syntax](https://github.com/pillarjs/path-to-regexp)**.  The vector syntax stays as close as possible to string syntax, so there's nothing new to learn.

This section assumes you're familiar with the syntax, and provides a mapping from string <-> vector notations.

#### Named Parameters

```clojure
(= (route "/foo/:bar")
   (route ["foo" :bar])) ;;=> true
```

#### Suffixed Parameters

Optional

```clojure
(= (route "/foo/:bar?")
   (route ["foo" :bar/?])) ;;=> true
```

Zero or more

```clojure
(= (route "/foo/:bar*")
   (route ["foo" :bar/*])) ;;=> true
```

One or more

```clojure
(= (route "/foo/:bar+")
   (route ["foo" :bar/+])) ;;=> true
```

#### Custom Match Parameters

```clojure
(= (route "/foo/:bar(\\d+)")
   (route ["foo" [:bar #(\d+)]])) ;;=> true
```

> Note: make sure to enclose your pattern in parens

#### Unnamed Parameters

```clojure
(= (route "/foo/(.*)")
   (route ["foo" #"(.*)"])) ;;=> true
```

> Note: make sure to enclose your pattern in parens

#### Asterisk

```clojure
(= (route "/foo/*")
   (route ["/foo" '*])) ;;=> true
```

## Configuration

You can configure various aspects of Trout by `set!`ing the variables in `trout.settings`:

```clojure
(require '[trout.settings :as ts])

;;;; These are the default values for each setting. 
;;;; You can change them to suit your needs.

;; Change the separator used to split & join paths
(set! ts/*path-separator* "/")

;; Set to `false` for stricter matching
(set! ts/*allow-trailing-slashes* true)

;; A function that takes a path string, and presumably side-effects
(set! ts/*navigator* (fn [path-str]
                       (.replace js/location path-str)))
```

## Development

Get a clj repl with `lein repl` and then a cljs repl with `(node-repl)`

Run tests with `lein doo node [once]`.


## Links

Thanks to these fine projects:
- [path-to-regexp](https://github.com/pillarjs/path-to-regexp)
- [Compojure](https://github.com/weavejester/compojure)
- [Schema](https://github.com/Prismatic/schema)

## (Un) License

This is free and unencumbered software released into the public domain. For more information see UNLICENSE or [unlicense.org](http://unlicense.org).

## Homepage

File an issue or pull-request at http://github.com/Rafflecopter/trout

<hr>

#### Notes:

<a name="1">1</a>: Right now we're aiming for "mostly-compatible", but might try to provide rigorous support in the future. PR's welcome ;)
