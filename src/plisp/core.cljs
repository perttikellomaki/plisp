(ns plisp.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [district0x.re-frame.interval-fx]
   [plisp.events :as events]
   [plisp.views :as views]
   [plisp.config :as config]
   [plisp.services.processor-service :as processor-service]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch [::processor-service/reset])
  (dev-setup)
  (mount-root))
