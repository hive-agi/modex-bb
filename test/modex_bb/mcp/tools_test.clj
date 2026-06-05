(ns modex-bb.mcp.tools-test
  (:require [clojure.test :refer [deftest is testing]]
            [modex-bb.mcp.tools :as tools]))

(deftest tool->json-schema-test
  (testing "emitted inputSchema conforms to JSON Schema draft 2020-12 (regression for #2)"
    (let [t (tools/tool [query "Run a SQL query"
                         [{:keys [sql]
                           :type {sql :string}
                           :doc  {sql "SQL query to run"}}]
                         sql])
          {:keys [inputSchema]} (tools/tool->json-schema t)
          {:keys [type required properties]} inputSchema]

      (is (= "object" type))
      ;; `required` is a property-name array at the schema level — never a
      ;; boolean on an individual property.
      (is (vector? required))
      (is (= ["sql"] (mapv name required)))

      (doseq [[prop-name prop] properties]
        (is (not (contains? prop :required))
            (str "property " prop-name " must not carry a boolean :required"))
        (is (not (contains? prop :doc))
            (str "property " prop-name " must use :description, not :doc"))
        (is (= #{:type :description} (set (keys prop)))
            (str "property " prop-name " keys must be exactly #{:type :description}"))))))
