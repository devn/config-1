(ns hooks.rum
  (:require [clj-kondo.hooks-api :as api]))

(defn rewrite
  ([node] (rewrite node false))
  ([node defcs?]
   (let [args (rest (:children node))
         component-name (first args)
         ?docstring (when (string? (api/sexpr (second args)))
                      (second args))
         args (if ?docstring
                (nnext args)
                (next args))
         body
         (loop [args* args
                mixins []]
           (if (seq args*)
             (let [a (first args*)]
               (if (vector? (api/sexpr a))
                 (if defcs?
                   (let [[state-arg & rest-args] (:children a)
                         ;; the original vector without the state argument
                         fn-args (assoc a :children rest-args)
                         body (rest args*)
                         body (api/list-node
                               (list* (api/token-node 'let*)
                                      (api/vector-node [state-arg (api/token-node nil)])
                                      state-arg
                                      body))]
                     (concat (when ?docstring [?docstring])
                             [fn-args]
                             (conj mixins body)))
                   (concat (when ?docstring [?docstring])
                           [a]
                           (concat mixins (rest args*))))
                 (recur (rest args*)
                        (conj mixins a))))
             args))
         new-node (with-meta
                    (api/list-node (list* (api/token-node 'defn) component-name body))
                    (meta node))]
     new-node)))

(defn defc [{:keys [:node]}]
  (let [new-node (rewrite node)]
    {:node new-node}))

(defn defcs [{:keys [:node]}]
  (let [new-node (rewrite node true)]
    {:node new-node}))
