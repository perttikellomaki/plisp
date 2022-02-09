(ns plisp.services.processor-service
  (:require
      [re-frame.core :as re-frame]
      [plisp.asm.lisp :as lisp]
      [plisp.cosmac.processor :as processor]))

(defn- reset [db _]
  (-> db
      (assoc-in [:processor]
                (processor/reset lisp/initial-lisp-memory 0x6000 []))
      (dissoc :lisp-output)))

(defn- run [db & _]
  (let [execution   (iterate processor/next-state
                             (:processor db))
        final-state (-> (take-while
                         (fn [processor]
                           (= (:status processor) :running))
                         execution)
                        last)]
    (-> db
        (assoc-in [:processor]
                  (assoc final-state :output-buffer []))
        (update-in [:lisp-output] str (apply str (:output-buffer final-state))))))

(defn- send-input-to-lisp [db [_ input]]
  (run (-> db
           (update-in [:processor :input-buffer]
                      #(into % (str input "\r")))
           (update-in [:lisp-output] str (str input "\n")))))

(re-frame/reg-event-db
 ::reset
 reset)

(re-frame/reg-event-db
 ::run
 run)

(re-frame/reg-event-db
 ::send-input-to-lisp
 send-input-to-lisp)

