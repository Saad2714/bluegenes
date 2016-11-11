(ns redgenes.events.boot
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [redgenes.db :as db]
            [redgenes.mines :as default-mines]
            [imcljs.fetch :as fetch]))

(defn boot-flow [db]
  {:first-dispatch [:authentication/fetch-anonymous-token (get db :current-mine)]
   :rules          [
                    ; Fetch a token before anything else then load assets
                    {:when       :seen?
                     :events     :authentication/store-token
                     :dispatch-n [[:assets/fetch-model]
                                  [:assets/fetch-lists]
                                  [:assets/fetch-templates]
                                  [:assets/fetch-summary-fields]]}
                    ; When all assets are loaded let bluegenes know
                    {:when     :seen-all-of?
                     :events   [:assets/success-fetch-model
                                :assets/success-fetch-lists
                                :assets/success-fetch-templates
                                :assets/success-fetch-summary-fields]
                     :dispatch [:finished-loading-assets]
                     :halt?    true}]})

; Boot the application.
(reg-event-fx
  :boot
  (fn []
    (let [db (assoc db/default-db :mines default-mines/mines)]
      {:db         (assoc db/default-db
                     :mines default-mines/mines
                     :fetching-assets? true)
       :async-flow (boot-flow db)})))

(reg-event-fx
  :reboot
  (fn [{db :db}]
    {:db         db
     :async-flow (boot-flow db)}))

(reg-event-fx
  :finished-loading-assets
  (fn [{db :db}]
    {:db         (assoc db :fetching-assets? false)
     :dispatch-n [[:cache/fetch-organisms]
                  [:saved-data/load-lists]
                  [:regions/select-all-feature-types]]}))

; Store an authentication token for a given mine
(reg-event-db
  :authentication/store-token
  (fn [db [_ mine-kw token]]
    (assoc-in db [:mines mine-kw :service :token] token)))

; Fetch an anonymous token for a give
(reg-event-fx
  :authentication/fetch-anonymous-token
  (fn [{db :db} [_ mine-kw]]
    {:db           db
     :im-operation {:on-success [:authentication/store-token mine-kw]
                    :op         (partial fetch/session (get-in db [:mines mine-kw :service]))}}))

; Fetch model

(reg-event-db
  :assets/success-fetch-model
  (fn [db [_ mine-kw model]]
    (assoc-in db [:mines mine-kw :service :model] model)))

(reg-event-fx
  :assets/fetch-model
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/model (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-model (:current-mine db)]}}))

; Fetch lists

(reg-event-db
  :assets/success-fetch-lists
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :lists mine-kw] lists)))

(reg-event-fx
  :assets/fetch-lists
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/lists (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-lists (:current-mine db)]}}))

; Fetch templates

(reg-event-db
  :assets/success-fetch-templates
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :templates mine-kw] lists)))

(reg-event-fx
  :assets/fetch-templates
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/templates (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-templates (:current-mine db)]}}))

; Fetch summary fields

(reg-event-db
  :assets/success-fetch-summary-fields
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :summary-fields mine-kw] lists)))

(reg-event-fx
  :assets/fetch-summary-fields
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/summary-fields (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-summary-fields (:current-mine db)]}}))

