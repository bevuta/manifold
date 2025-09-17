(ns manifold.debug
  {:no-doc true}
  (:require [clojure.tools.logging :as log]))

(def ^:dynamic *dropped-error-logging-enabled?* true)

(defn enable-dropped-error-logging! []
  (.bindRoot #'*dropped-error-logging-enabled?* true))

(defn disable-dropped-error-logging! []
  (.bindRoot #'*dropped-error-logging-enabled?* false))

(def ^:dynamic *always-log-dropped-errors?* false)

(defn always-log-dropped-errors! []
  (.bindRoot #'*always-log-dropped-errors?* true))

(def dropped-errors nil)

(defn log-dropped-error! [error]
  (some-> dropped-errors (swap! inc))
  (log/warn error "unconsumed deferred in error state, make sure you're using `catch`."))

(defn with-dropped-error-detection
  [f handle-dropped-errors]
  (with-redefs [dropped-errors (atom 0)]
    (f)
    (System/gc)
    (System/runFinalization)
    (handle-dropped-errors @dropped-errors)))
