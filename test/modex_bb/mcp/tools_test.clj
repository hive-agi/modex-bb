(ns modex-bb.mcp.tools-test
  "Coverage for the inputSchema a tool advertises (issue #2).

   A tool's inputSchema must be valid JSON Schema draft 2020-12, or the
   Claude API rejects the whole tool list with HTTP 400. Per property that
   means: no boolean `required` (it is an array on the object schema) and
   `description`, never `doc`.

   `Parameter` and `PropertySchema` are imported from the schemas namespace,
   never restated here, so generator, oracle and mutants come from one
   declaration."
  (:require [clojure.test :refer [deftest is]]
            [hive-schemas.test :refer [deftrifecta-from-schema]]
            [hive-test.mutation :refer [deftest-mutations]]
            [modex-bb.mcp.tools :as tools]
            [modex-bb.mcp.tools-schemas :as s]))

;; `:rel` pins the one fact the output schema cannot: the description IS the
;; parameter's doc, and it is absent exactly when the doc is.
(deftrifecta-from-schema tool-arg->property tools/tool-arg->property
  {:in  s/Parameter
   :out s/PropertySchema
   :rel (fn [param out]
          (and (= (:type param) (:type out))
               (= (:doc param) (:description out))
               (= (some? (:doc param)) (contains? out :description))))})

;; The subject mutated directly. `select-keys` is the exact pre-fix shape:
;; if it ever survives, the bug is back.
(deftest-mutations tool-arg->property-kills-pre-fix-shapes
  tools/tool-arg->property
  [["select-keys"      (fn [p] (select-keys p [:type :doc :required]))]
   ["doc-key"          (fn [p] (cond-> {:type (:type p)}
                                 (:doc p) (assoc :doc (:doc p))))]
   ["drop-description" (fn [p] {:type (:type p)})]]
  (fn []
    (is (= {:type :string :description "SQL query"}
           (tools/tool-arg->property
            (tools/map->Parameter {:name :sql :doc "SQL query"
                                   :type :string :required true}))))
    (is (= {:type :number}
           (tools/tool-arg->property
            (tools/map->Parameter {:name :n :type :number :required false}))))))

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
