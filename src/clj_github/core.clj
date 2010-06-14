(ns clj-github.core
  (:use [clojure.contrib [string :only [join]] [base64 :only [encode-str]]]
	[clojure-http.client :only [request add-query-params]]
	[org.danlarkin.json :only [decode-from-str]])
  (:import java.net.URI))

(defn create-url [rest auth]
  (str (if (seq auth)
	 (URI. "http" (str (:user auth) ":" (:pass auth)) "github.com" 80 (str "/api/v2/json/" rest) nil nil)
	 (URI. "http" "github.com" (str "/api/v2/json/" rest) nil))))

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
  [path & {:keys [type data sift raw?] :or {type "GET" data {} raw? false}}]
  (let [req (request (add-query-params
                      (create-url (if (string? path) path (apply slash-join (filter identity path)))
                                  *authentication*)
                      data)
                     type)]
    (if raw?
      (-> req :body-seq first)
      (-> req :body-seq first decode-from-str (handle sift)))))

(defmacro with-auth [auth body]
  `(binding [*authentication* ~auth] ~body))