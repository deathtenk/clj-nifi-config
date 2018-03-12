#!/usr/local/bin/clojure
(ns tls
 (:require  [clojure.java.io :as io]
            [environ.core :refer [env]]
            [clojure.java.shell :refer [sh]]
            [cheshire.core :as json]
            [clojurewerkz.propertied.properties :as p]))

;; CLJ_NIFI_PROPERTIES_PATH
;; CLJ_NIFI_KEYSTORE_CONFIG_PATH
;; CA_SERVER_PORT
;; CA_SERVER_HOSTNAME
;; CA_TOKEN
;; NIFI_IP_ADDRESS
;; NIFI_HOSTNAME

(def nifi-properties-path (or (:clj-nifi-properties-path env) "./nifi.properties"))

(def config-path (or (:clj-nifi-keystore-config-path env) 
                     "./config.json"))

(def properties 
  (p/properties->map
   (p/load-from (io/file nifi-properties-path)) true))

(def ca-server-port (or (:ca-server-port env) "9999"))

(def ca-server-hostname (or (str (:ca-server-hostname env)) 
                            (throw (Exception. "need CA_SERVER_HOSTNAME to be set"))))

(def ca-token (or (:ca-token env)
                  (throw (Exception. "need CA_TOKEN to be set"))))


(def ipaddress (or (:nifi-ip-address env)
                   (throw (Exception. "Need NIFI_IP_ADDRESS to be set"))))

(def hostname (or (:nifi-hostname env)
                  (throw (Exception. "need NIFI_HOSTNAME to be set"))))


(defn download-toolkit []
  (with-open [in (io/input-stream "https://archive.apache.org/dist/nifi/1.5.0/nifi-toolkit-1.5.0-bin.tar.gz")
              out (io/output-stream "./nifi-toolkit.tar.gz")]
    (io/copy in out)))


(defn extract-toolkit []
  (sh "mkdir" "./nifi-toolkit")
  (sh "tar" "-xf" "./nifi-toolkit.tar.gz" "-C" "./nifi-toolkit" "--strip-components" "1"))

(defn get-keystore []
  (sh "./nifi-toolkit/bin/tls-toolkit.sh" "client" "-c" ca-server-hostname 
                                                   "-t" ca-token 
                                                   "--subjectAlternativeNames" (str ipaddress "," hostname)
                                                   "-D" (str "CN=" hostname ",OU=NIFI") 
                                                   "-T" "PKCS12"))


(defn translate-key [k]
  (cond
    (= k :keyStore) :nifi.security.keystore
    (= k :keyStoreType) :nifi.security.keystoreType
    (= k :keyStorePassword) :nifi.security.keystorePasswd
    (= k :keyPassword) :nifi.security.keyPasswd
    (= k :trustStore) :nifi.security.truststore
    (= k :trustStoreType) :nifi.security.truststoreType
    (= k :trustStorePassword) :nifi.security.truststorePasswd
    :else nil))


(defn read-and-convert-json-to-props []
  (let [config (json/parse-string (slurp config-path) true)]
    (reduce-kv (fn [acc k v]
                (if-let [tk (translate-key k)]
                 (if v 
                   (assoc acc tk v)
                   acc)
                 acc)) {} config)))


(defn get-new-properties [] 
  (merge properties 
         (read-and-convert-json-to-props)))

(defn set-properties []
  (do (io/copy (io/file nifi-properties-path) (io/file "./nifi.properties.old"))
      (p/store-to (get-new-properties) nifi-properties-path)))


(defn secure-with-tls []
  (do 
    (download-toolkit)
    (extract-toolkit)
    (get-keystore)
    (set-properties)
    (System/exit 0)))


(secure-with-tls)

