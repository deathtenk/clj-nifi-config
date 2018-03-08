#!/usr/local/bin/clj

(ns set-properties
 (:require  [clojurewerkz.propertied.properties :as p]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))

(def nifi-properties-path (or (:clj-nifi-properties-path env) "./nifi.properties"))

(def properties 
  (p/properties->map
   (p/load-from (io/file nifi-properties-path)) true))


(def prop-keys-as-env
  (map (fn [el] 
          (as-> el k
            (name k) 
            (clojure.string/split k #"\.")
            (clojure.string/join "-" k)
            (keyword k))) (keys properties)))


(defn convert-env-to-props [m]
  (reduce-kv (fn [acc k v] (assoc acc (as-> k s
                                        (name s)
                                        (clojure.string/split s #"-")
                                        (clojure.string/join "." s)
                                        (keyword s)) v)) {} m))


; Here I'm using the current nifi.properties file as a reference
; for finding relevant configuration in the environment
(def configured-properties 
    (-> (select-keys env prop-keys-as-env)
        convert-env-to-props))


(def new-properties 
  (merge properties 
         configured-properties))

(defn set-properties []
  (do (io/copy (io/file nifi-properties-path) (io/file "./nifi.properties.old"))
      (p/store-to new-properties nifi-properties-path)))

(set-properties)
