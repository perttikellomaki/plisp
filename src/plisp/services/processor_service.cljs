(ns plisp.services.processor-service
  (:require
      [re-frame.core :as re-frame]
      [plisp.asm.lisp :as lisp]
      [plisp.cosmac.processor :as processor]))

(def tick 1000)

(defn- reset [db _]
  (-> db
      (assoc-in [:processor]
                (processor/reset lisp/initial-lisp-memory 0x6000 []))
      (dissoc :lisp-output)))

(defn- run-processor-tick [db & _]
  (let [initial-state (:processor db)
        execution   (iterate processor/next-state initial-state)
        final-state (-> (take-while
                         (fn [processor]
                           (and (= (:status processor) :running)
                                (<= (:instruction-count processor)
                                    (+ (:instruction-count initial-state) tick))))
                         execution)
                        last)]
    (-> db
        (assoc-in [:processor]
                  (assoc final-state :output-buffer []))
        (update-in [:lisp-output] str (apply str (:output-buffer final-state))))))

(defn- send-input-to-lisp [db [_ input]]
  (-> db
      (update-in [:processor :input-buffer]
                 #(into % (str input "\r")))
      (update-in [:lisp-output] str (str input "\n"))))

(defn- toggle-running [{:keys [db]}]
  (let [running (get-in db [:execution :status])]
    (cond-> {:db (update-in db [:execution :status] #(not %))}

      running
      (assoc :clear-interval {:id ::processor-tick})

      (not running)
      (assoc :dispatch-interval {:dispatch [::run-processor-tick]
                                 :id ::processor-tick
                                 :ms 1000}))))

(re-frame/reg-event-db
 ::reset
 reset)

(re-frame/reg-event-db
 ::run-processor-tick
 run-processor-tick)

(re-frame/reg-event-db
 ::send-input-to-lisp
 send-input-to-lisp)

(re-frame/reg-event-fx
 ::toggle-running
 toggle-running)
