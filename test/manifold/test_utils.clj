(ns manifold.test-utils
  (:require
    [clojure.test :as test]
    [criterium.core :as c]
    [manifold.debug :as debug]))

(defmacro long-bench [name & body]
  `(do
     (println "\n-----\n" ~name "\n-----\n")
     (c/bench
       (do ~@body)
       :reduce-with #(and %1 %2))))

(defmacro bench [name & body]
  `(do
     (println "\n-----\n" ~name "\n-----\n")
     (c/quick-bench
       (do ~@body)
       :reduce-with #(and %1 %2))))

(defn report-dropped-errors! [dropped-errors]
  (when (pos? dropped-errors)
    ;; We include the assertion here within the `when` form so that we don't add a mystery assertion
    ;; to every passing test (which is the common case).
    (test/is (zero? dropped-errors)
             "Dropped errors detected! See log output for details.")))

(defn instrument-test-fn-with-dropped-error-detection [tf]
  (if (::detect-dropped-errors? tf)
    tf
    (with-meta
      (fn []
        (debug/with-dropped-error-detection tf report-dropped-errors!))
      {::detect-dropped-errors? true})))

(defn instrument-tests-with-dropped-error-detection!
  "Instrument all tests in the current namespace dropped error detection by wrapping them in
  `manifold.debug/with-dropped-error-detection`. If dropped errors are detected, a corresponding (failing)
  assertion is injected into the test and the leak reports are logged at level `error`.

  Usually placed at the end of a test namespace.

  Note that this is intentionally not implemented as a fixture since there is no clean way to make a
  test fail from within a fixture: Neither a failing assertion nor throwing an exception will
  preserve which particular test caused it. See
  e.g. https://github.com/technomancy/leiningen/issues/2694 for an example of this."
  []
  (debug/always-log-dropped-errors!)
  (->> (ns-interns *ns*)
       vals
       (filter (comp :test meta))
       (run! (fn [tv]
               (alter-meta! tv update :test instrument-test-fn-with-dropped-error-detection)))))
