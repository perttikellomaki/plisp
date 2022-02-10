(ns plisp.asm.memory)

;;;
;;; Lay out instructions in memory.
;;;

(defn- layout-iter [{:keys [address memory]} {:keys [op] :as instruction}]
  (cond (= op :address)
        {:address (:address instruction)
         :memory   memory}

        (= op :empty)
        {:adress address
         :memory memory}

        :else
        {:address (+ address (:bytes instruction))
         :memory  (assoc-in memory [address] instruction)}))

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
          {:address 0
           :memory  {}}
          instructions))

(defn layout [instructions]
  (->> instructions
      (map expand-string)
      flatten
      layout-simple
      :memory))
