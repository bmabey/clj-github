(ns #^{:doc "Core of clj-github. This namespace is used by the API files. Only thing useful
            it provides to users is the with-auth convenience function."}
  clj-github.core
  (:use [clojure.contrib [string :only [join]]]
	[clj-http.client :only [request]]
        [clojure.contrib.json :only [read-json]])
  (:import java.net.URI))

(defn create-url [rest gist? & {special :special}]
  (URI. "http" (if gist? "gist.github.com" "github.com")
        (if special
          (str special rest)
          (str "/api/" (if gist? "v1" "v2") "/json/" rest))
        nil))

(def #^{:doc "This var will be rebound to hold authentication information."}
     *authentication* {})

(defn slash-join
  "Returns a string of it's arguments separated by forward slashes."
  [& args]
  (join "/" args))

(defn t-to-n
  "Returns 1 for true and 0 for false"
  [n]
  (if n 1 0))

(defn handle
  "Checks for an error, and if one has happened, returns the message.
  Otherwise, spits out results."
  ([data sifter] (if-let [result (:error data)] result (if sifter (sifter data) data)))
  ([data] (handle data identity)))

(defn make-request
  "Constructs a basic authentication request. Path is either aseq of URL segments that will
  be joined together with slashes, or a full string depicting a path that will be used directly.
  The path should never start with a forward slash. It's added automatically."
  [path & {:keys [type data sift raw? gist? special] :or {type :get data {} raw? false gist? false}}]
  (let [req (request
             {:method type
              :url (str (create-url (if (string? path) path (apply slash-join (filter identity path)))
                                    gist? :special special))
              :query-params data
              :basic-auth [(if (:token *authentication*)
                             (str (:user *authentication*) "/token")
                             (:user *authentication*))
                           (or (:token *authentication*) (:password *authentication*))]})]
    (if raw?
      (->> req :body (interpose "\n") (apply str))
      (try (-> req :body read-json (handle sift))
           (catch Exception e
             (if (.startsWith "java.lang.Exception: EOF while reading" (.getMessage e))
               (str "If you're seeing this message, it means that Github threw down a "
                    "Page Not Found page that could not be parsed as JSON. This is usually "
                    "caused by giving invalid information that caused the URL to route to an "
                    "unknown page.")
               (throw e)))))))

(defmacro with-auth [auth body]
  `(binding [*authentication* ~auth] ~body))