```
DO NOT USE IN PRODUCTION. Trout is a work in progress. Some things are missing/broken.
```
<br>

<img src="http://f.cl.ly/items/0T1e2c243a1F1S2k1033/trout.svg" align=right width=200>

# Trout

Trout is a bi-directional "routes are data"
route matching/lookup library for ClojureScript that aims to be intuitive to use.

- Route [syntax](#route-syntax) inspired by Express, et al.
- Routes are just vectors - even ones created via strings.<br>
  Extend & compose them to your heart's content.
- No keeping track of routes for you; no macros necessary.
- Routes can be "nested" a la compojure's `context`

[<img src="http://clojars.org/trout/latest-version.svg" height=34>](http://clojars.org/trout)

## TL;DR

##### Syntax [...](#route-syntax)
```clojure
"/foo"            ["foo"]                  ; string path segment 
"/foo/:bar"       ["foo" :bar]             ; named parameter
"/foo/:bar?"      ["foo" :bar/?]           ; named parameter (optional)
"/foo/:bar*"      ["foo" :bar/*]           ; named parameter (zero or more)
"/foo/:bar+"      ["foo" :bar/+]           ; named parameter (one or more)
"/foo/:bar(\\d+)" ["foo" [:bar #"(\d+)"]]  ; named parameter w/ custom pattern
"/foo/(.*)"       ["foo" #"(.*)"]          ; unnamed parameter
"/foo/*"          ["foo" '*]               ; unnamed parameter (shorthand for (.*))
```

##### Usage [...](#usage)

```clojure
(require '[trout.core :as t])

(def my-route (t/route "/user/:user-id/invoices/:invoice-id(inv-\d+)"))

(t/matches? my-route "/user/abc123/invoices/inv-456") ;;=> true
(t/match my-route "/user/abc123/invoices/inv-456")    ;;=> {:user-id "abc123", :invoice-id "inv-456"}

(t/->str my-route {:user-id "xyz789"
                   :invoice-id "inv-123"})   ;;=> "/user/xyz789/invoices/inv-123
```


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

Routes are vectors. Do vector things to them:

```clojure
(let [route (t/route "/user/:id/settings")]

  (str (cons "my-site" route))  ;;=> "/my-site/user/:id/settings"
  (str (conj route "billing"))  ;;=> "/user/:id/settings/billing"
  (str (assoc route 1 :ident))  ;;=> "/user/:ident/settings"
  (str (pop route))             ;;=> "/user/:id"

  ;; seq functions return seqs. call (route) on them again:

  (t/route (reverse route))            ;;=> /settings/:id/user
  (t/route (rest route))               ;;=> :id/settings
  (t/route (concat route
                   (t/route "/foo")))  ;;=> /user/:id/settings/foo
  ))
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

#### Route Handlers

Trout can call route handlers for you, provided a map of routes and a matching map of handlers. Handlers will be passed the parsed parameters as a map:

```clojure
(defn show-page [{:keys [page-id]}]
  (js/alert (str "Welcome to " page-id " page")))

(defn greet-user [{:keys [user-id]}]
  (js/alert (str "Welcome, User " user-id)))


(let [routes {:home (t/route "/home/:page-id")
              :user (t/route "/user/:user-id")}
      handlers {:home show-page
                :user greet-user}]

  (t/handle! routes handlers "/home/features") ;;=> Welcome to features page
  (t/handle! routes handlers "/user/123")      ;;=> Welcome, User 123
  )
```

If you prefer, you can use indexed collections instead of maps:

```clojure
(let [routes [(t/route "/home/:page-id"),
              (t/route "/user/:user-id")]
      handlers [show-page
                greet-user]]

  (t/handle! routes handlers "/home/pricing")  ;;=> Welcome to page pricing
  (t/handle! routes handlers "/user/abc-123")  ;;=> Welcome, User abc-123
  )
```

#### Generating Strings

You can generate a string from a route + arguments:

```clojure
(let [route (t/route "/user/:id")]

  (t/->str route {:id "123"}) ;;=> "/user/123"
  (t/->str route {:id 123})   ;;=> "/user/123"
  )
```

Invalid input will throw a `js/TypeError`:

```clojure
(let [route (t/route "/user/:id(\d+)")]

  (t/->str route {})           ;;=> throws js/TypeError
  (t/->str route {:id "xyz"})  ;;=> throws js/TypeError
  )
```

Modifiers like optional & repeat are respected:

```clojure
(let [route (t/route "/user/:uid(\d+)/~/:folder-name+/:filename?")]
  
  (t/->str route {:uid 123
                  :folder-name ["docs" "invoices"]  ;;=> "/user/123/~/docs/invoices/Jan2015.pdf"
                  :filename "Jan2015.pdf"})

  (t/->str route {:uid 123
                  :folder-name []                   ;;=> throws js/TypeError (folder-name is empty)
                  :filename "whoops.pdf"})
  )
```

#### Miscellany

Trout can change the browser's location for you:

```clojure
(let [route (t/route "/user/:id")]

  (t/navigate! route {:id 123})  ;;=> document location will be ".../user/123"
  )
```

To match against a whole URL instead of a relative path:

```clojure
(t/matches? route (t/url "http://site.com/user/123"))
```

You can also match against [Location](https://developer.mozilla.org/en-US/docs/Web/API/Location) objects:

```clojure
(let [route (t/route "/user/:id")
      loc (.-location js/document)] ;; href = "http://site.com/user/123"

  (t/matches? route loc)  ;;=> true
  )
```

> Note: Actually any javascript object with a `.-href` will do.

To use a hash-bang prefix ([more](#configuration)) 

```clojure
(set! trout.settings/*prefix* "#!")

(let [route (t/route "/user/:id")]
  
  (t/matches? route "/#!/user/123")  ;;=> true
  (t/->str route {:id "456"})        ;;=> "/#!/user/456"
)
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

;; A (string) path segment to expect on inputs & include in outputs
(set! ts/*prefix* nil)
```

## Development

From a `lein repl` (or `cider-jack-in`), choose from:

```clojure
;; Node repls are the easiest to work with
(cljs-repl :node)

;; Phantom repl works great for eval, but cider-load-[buffer|file] don't work,
;; making the development workflow a bit clunky. But sometimes you need a browser:
(cljs-repl :phantom)
```

Run tests with `lein test`. You'll need [phantom](http://phantomjs.org/) installed.

Helpful lein aliases: `lein clean-build`, `lein clean-repl`


## Links

- [path-to-regexp](https://github.com/pillarjs/path-to-regexp)

## (Un) License

This is free and unencumbered software released into the public domain. For more information see UNLICENSE or [unlicense.org](http://unlicense.org).

## Homepage

File an issue or pull-request at http://github.com/Rafflecopter/trout

<hr>

#### Notes:

<a name="1">1</a>: Right now we're aiming for "mostly-compatible", but might try to provide rigorous support in the future. PR's welcome ;)


[route-syntax]: https://github.com/pillarjs/path-to-regexp
