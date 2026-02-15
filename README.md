# modex-bb

Babashka MCP server framework. Build [Model Context Protocol](https://modelcontextprotocol.io/) servers that run on [Babashka](https://babashka.org/) with zero JVM startup time.

## Features

- **Stdio JSON-RPC transport** — reads from stdin, writes to stdout
- **Tool DSL** — declarative `(tools ...)` macro for defining MCP tools with typed parameters, docs, defaults, and validation
- **MCP 2024-11-05 protocol** — initialize, tools/list, tools/call, prompts/list, resources/list
- **Thread-safe** — locking on stdout, async request handling via futures
- **Lightweight** — single dependency (cheshire for JSON)

## Quick Start

Add to your `bb.edn`:

```clojure
{:deps {io.github.hive-agi/modex-bb {:git/tag "v0.1.0"
                                      :git/sha "439ff17ea854dce14a3e3963d6c86c5400c72962"}}}
```

Define tools and start the server:

```clojure
(ns my-server
  (:require [modex-bb.mcp.server :as mcp-server]
            [modex-bb.mcp.tools :refer [tools]]))

(def my-tools
  (tools
   (greet "Say hello to someone"
          [{:keys [name]
            :type {:name :string}
            :doc  {:name "Person to greet"}}]
          [{:message (str "Hello, " name "!")}])

   (add "Add two numbers"
        [{:keys [a b]
          :type {:a :number :b :number}
          :doc  {:a "First number" :b "Second number"}}]
        [{:result (+ a b)}])))

(def server
  (mcp-server/->server
   {:name    "my-server"
    :version "0.1.0"
    :tools   my-tools}))

(defn -main [& _]
  (mcp-server/start-server! server))
```

Run it:

```bash
bb --config bb.edn -m my-server
```

## Tool DSL

The `tools` macro supports map-destructured arguments with `:type`, `:doc`, and `:or` (defaults):

```clojure
(tools
 (my_tool "Description of my tool"
          [{:keys [required_param optional_param]
            :type {:required_param :string :optional_param :number}
            :doc  {:required_param "This is required"
                   :optional_param "This has a default"}
            :or   {optional_param 42}}]
          ;; Body — return a vector of result maps
          [{:output (str required_param " = " optional_param)}]))
```

Parameters without `:or` defaults are marked as required in the MCP schema. Types are `:string` or `:number`.

## Architecture

```
modex_bb/mcp/
  server.clj     — JSON-RPC stdio loop, request routing, AServer reify
  tools.clj      — Tool DSL macros, parameter validation, invocation
  protocols.clj  — AServer protocol definition
  schema.clj     — MCP protocol version, error codes
  json_rpc.clj   — JSON-RPC message constructors
  log.clj        — Logging shim (stderr)
```

## Projects Using modex-bb

- [clj-kondo-mcp](https://github.com/hive-agi/clj-kondo-mcp) — Static analysis via clj-kondo
- [scc-mcp](https://github.com/hive-agi/scc-mcp) — Code metrics via scc
- [basic-tools-mcp](https://github.com/hive-agi/basic-tools-mcp) — Clojure dev tools (delimiter repair, nREPL eval, formatting)

## License

MIT
