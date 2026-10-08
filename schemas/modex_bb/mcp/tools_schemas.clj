(ns modex-bb.mcp.tools-schemas
  "Malli value objects for the tool-definition surface of `modex-bb.mcp.tools`.

   Kept OFF `:paths` (test alias only): modex-bb runs under Babashka in every
   consumer, and none of them should pay malli's load time for a contract that
   only the test suite checks."
  (:require [malli.core :as m]
            [modex-bb.mcp.tools :as tools]))

(def ArgType
  "Argument types the `handler` macro admits."
  [:enum :string :number])

(def Parameter
  "One tool argument, as `tool-v1` / `tool-v2-argmap` build it."
  [:map
   [:name :keyword]
   [:doc {:optional true} [:maybe :string]]
   [:type ArgType]
   [:required :boolean]
   [:default {:optional true} :any]])

(def PropertySchema
  "One entry of an inputSchema's `properties`, valid JSON Schema draft 2020-12.
   Closed: `required` belongs to the object schema, and `doc` is no keyword."
  [:map {:closed true}
   [:type ArgType]
   [:description {:optional true} :string]])

(m/=> tools/tool-arg->property [:=> [:cat Parameter] PropertySchema])
