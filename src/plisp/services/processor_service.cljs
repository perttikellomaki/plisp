(ns plisp.services.processor-service
  (:require
      [re-frame.core :as re-frame]
      [plisp.asm.lisp :as lisp]
      [plisp.cosmac.processor :as processor]
         [plisp.util :refer [int16] :as util]))

(def default-tick 10000)

(defn- running? [processor]
  (= (:status processor) :running))

(defn- pc [processor]
  (-> processor
      :R
      (get (:P processor))
      int16))

(defn- reset [db _]
  (-> db
      (assoc-in [:processor]
                (processor/reset lisp/initial-lisp-memory 0x6000 []))
      (dissoc :lisp-output)))

(defn- run-processor-tick [{:keys [db]} [_ num-instructions]]
  (when (get-in db [:execution :running])
    (let [initial-state (:processor db)
          final-instruction-count (+ (:instruction-count initial-state) num-instructions)
          execution   (iterate processor/next-state initial-state)
          intermediate (-> (take-while
                            (fn [processor]
                              (and (running? processor)
                                   (<= (:instruction-count processor)
                                       final-instruction-count)
                                   (not= (pc processor) (get-in db [:execution :breakpoint]))))
                            execution)
                           last)
          final-state (if (and (running? intermediate)
                               (< (:instruction-count intermediate) final-instruction-count))
                        ;; presumably the intermediate state is one instruction short of breakpoint
                        (processor/next-state intermediate)
                        intermediate)
          at-breakpoint? (= (pc final-state)
                            (get-in db [:execution :breakpoint]))]
      (cond->
          {:db (-> db
                   (assoc-in [:execution :running] (not at-breakpoint?))
                   (assoc-in [:processor]
                             (assoc final-state :output-buffer []))
                   (update-in [:lisp-output] str (apply str (:output-buffer final-state))))}

        at-breakpoint?
        (assoc :clear-interval {:id ::processor-tick})))))

(defn- set-breakpoint [db [_ address]]
  (assoc-in db [:execution :breakpoint] address))

(defn- send-input-to-lisp [db [_ input]]
  (-> db
      (update-in [:processor :input-buffer]
                 #(into (apply vector %) (str input "\r")))
      (update-in [:lisp-output] str (str input "\n"))))

(defn- toggle-running [{:keys [db]}]
  (let [running (get-in db [:execution :running])]
    (cond-> {:db (update-in db [:execution :running] #(not %))}

      running
      (assoc :clear-interval {:id ::processor-tick})

      (not running)
      (assoc :dispatch-interval {:dispatch [::run-processor-tick default-tick]
                                 :id ::processor-tick
                                 :ms 1000}))))

(re-frame/reg-event-db
 ::reset
 reset)

(re-frame/reg-event-fx
 ::run-processor-tick
 run-processor-tick)

(re-frame/reg-event-db
 ::set-breakpoint
 set-breakpoint)

(re-frame/reg-event-db
 ::send-input-to-lisp
 send-input-to-lisp)

(re-frame/reg-event-fx
 ::toggle-running
 toggle-running)
