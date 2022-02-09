(ns plisp.events
  (:require
   [re-frame.core :as re-frame]
   [plisp.db :as db]
   [plisp.services.processor-service]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
