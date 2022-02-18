(ns plisp.asm.memory)

;;;
;;; Lay out instructions in memory.
;;;

(defn- add-debug [debug-info address {:keys [source-line-number] :as _instruction}]
  (assoc debug-info source-line-number address))

(defn- layout-iter [{:keys [address memory debug-info]} {:keys [op] :as instruction}]
  (cond (= op :address)
        {:address    (:address instruction)
         :memory     memory
         :debug-info (add-debug debug-info address instruction)}

        (= op :empty)
        {:address     address
         :memory     memory
         :debug-info (add-debug debug-info address instruction)}

        :else
        {:address    (+ address (:bytes instruction))
         :memory     (assoc-in memory [address] instruction)
         :debug-info (add-debug debug-info address instruction)}))

(defn- expand-string [{:keys [op value] :as instruction}]
  (if (= op :string)
    (map (fn [c] (assoc instruction
                        :op    :byte
                        :value (.charCodeAt c)
                        :bytes 1))
         value)
    instruction))

(defn- layout-simple [instructions]
  (reduce layout-iter
          {:address      0
           :memory       {}
           :debug-info   {}}
          instructions))

(defn layout [instructions]
  (let [{:keys [memory debug-info]}
        (->> instructions
             (map expand-string)
             flatten
             layout-simple)]
    {:memory      memory
     :debug-info  debug-info}))
