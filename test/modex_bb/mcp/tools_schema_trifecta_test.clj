(ns modex-bb.mcp.tools-schema-trifecta-test
  "Trifecta coverage for `tool-arg->property` (issue #2).

   A tool's inputSchema must be valid JSON Schema draft 2020-12, or the
   Claude API rejects the whole tool list with HTTP 400. Per property that
   means: no boolean `required` (it is an array on the object schema) and
   `description`, never `doc`. The mutation `select-keys` is the exact
   pre-fix shape; if it ever survives, the bug is back."
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.generators :as gen]
            [modex-bb.mcp.tools :as tools]
            [hive-test.trifecta :refer [deftrifecta]]))

;; ============================================================
;; Generators
;; ============================================================

(def ^:private gen-parameter
  (gen/fmap tools/map->Parameter
            (gen/hash-map :name     gen/keyword
                          :doc      (gen/one-of [gen/string-alphanumeric
                                                 (gen/return nil)])
                          :type     (gen/elements [:string :number])
                          :required gen/boolean
                          :default  (gen/return nil))))

;; ============================================================
;; Invariant: a draft 2020-12 property schema
;; ============================================================

(defn- valid-property?
  "Only JSON Schema keywords; `:type` always present; `:description` is
   present exactly when the parameter carries a doc."
  [property]
  (and (map? property)
       (contains? property :type)
       (every? #{:type :description} (keys property))
       (or (not (contains? property :description))
           (string? (:description property)))))

;; ============================================================
;; Mutation oracles
;; ============================================================

(defn- mut-select-keys
  "The pre-fix shape: leaks `:doc` and a boolean `:required`."
  [tool-arg]
  (select-keys tool-arg [:type :doc :required]))

(defn- mut-doc-key
  "Keeps the doc under the non-JSON-Schema key `:doc`."
  [tool-arg]
  (cond-> {:type (:type tool-arg)}
    (:doc tool-arg) (assoc :doc (:doc tool-arg))))

(defn- mut-drop-description
  "Loses the description entirely."
  [tool-arg]
  {:type (:type tool-arg)})

;; ============================================================
;; Trifecta: golden + property + mutation
;; ============================================================

(deftrifecta tool-arg-property-trifecta
  modex-bb.mcp.tools/tool-arg->property
  {:golden-path "test/golden/modex-bb/trifecta-tool-arg-property.edn"
   :cases       {:required-string (tools/map->Parameter
                                   {:name :sql :doc "SQL query"
                                    :type :string :required true})
                 :optional-number (tools/map->Parameter
                                   {:name :limit :doc "Max rows"
                                    :type :number :required false
                                    :default 10})
                 :no-doc          (tools/map->Parameter
                                   {:name :q :type :string :required true})}
   :gen         gen-parameter
   :pred        valid-property?
   :num-tests   200
   :mutations   [["select-keys"        mut-select-keys]
                 ["doc-key"            mut-doc-key]
                 ["drop-description"   mut-drop-description]]})

;; The issue's own example, end to end through the Tool record.
(deftest input-schema-matches-issue-2
  (is (= {:type       "object"
          :required   [:sql]
          :properties {:sql {:type :string :description "SQL query"}}}
         (tools/input-schema
          (tools/->Tool :query "Run SQL"
                        [(tools/map->Parameter {:name :sql :doc "SQL query"
                                                :type :string :required true})]
                        identity)))))
