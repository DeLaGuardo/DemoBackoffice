(ns DemoBackoffice.models.contract
  (:require [simpledb.core :as db]
            [clj-time.core :as ctime]
            [clj-time.format :as tform]
            [clj-time.coerce :as coerce]
            [clojure.string :as string]
            [DemoBackoffice.models.user :as users]
            [noir.validation :as vali]
            [noir.session :as session])
  (:import com.petebevin.markdown.MarkdownProcessor))

(def contracts-per-page 10)
(def date-format (tform/formatter "MM/dd/yy" (ctime/default-time-zone)))
(def time-format (tform/formatter "h:mma" (ctime/default-time-zone)))
(def mdp (MarkdownProcessor.))

;; Gets

(defn total []
  (count (db/get :contract-ids)))

(defn id->contract [id]
  (db/get-in :contracts [id]))

(defn ids->contracts [ids]
  (map id->contract ids))

(defn moniker->contract [moniker]
  (id->contract (db/get-in :contract-monikers [moniker])))

(defn get-page [page]
  (let [page-num (dec (Integer. page)) ;; make it 1-based indexing
        prev (* page-num contracts-per-page)]
    (ids->contracts (take contracts-per-page (drop prev (db/get :contract-ids))))))

(defn get-latest []
  (get-page 1))

;; Mutations and checks

(defn next-id []
  (str (db/update! :next-contract-id inc)))

(defn gen-moniker [title]
  (-> title
    (string/lower-case)
    (string/replace #"[^a-zA-Z0-9\s]" "")
    (string/replace #" " "-")))

(defn new-moniker? [moniker]
  (not (contains? (db/get :contract-monikers) moniker)))

(defn perma-link [moniker]
  (str "/blog/view/" moniker))

(defn edit-url [{:keys [id]}]
  (str "/blog/admin/contract/edit/" id))

(defn md->html [text]
  (. mdp (markdown text)))

(defn wrap-moniker [{:keys [title] :as contract}]
  (let [moniker (gen-moniker title)]
    (-> contract
      (assoc :moniker moniker)
      (assoc :perma-link (perma-link moniker)))))
    
(defn wrap-markdown [{:keys [body] :as contract}]
  (assoc contract :md-body (md->html body)))

(defn wrap-time [contract]
  (let [ts (ctime/now)]
    (-> contract
      (assoc :ts (coerce/to-long ts))
      (assoc :date (tform/unparse date-format ts))
      (assoc :tme (tform/unparse time-format ts)))))

(defn prepare-new [{:keys [title body] :as contract}]
  (let [id (next-id)
        ts (ctime/now)]
    (-> contract
      (assoc :id id)
      (assoc :username (users/me))
      (wrap-time)
      (wrap-moniker)
      (wrap-markdown))))

(defn valid? [{:keys [title body]}]
  (vali/rule (vali/has-value? title)
             [:title "There must be a title"])
  (vali/rule (new-moniker? (gen-moniker title))
             [:title "That title is already taken."])
  (vali/rule (vali/has-value? body)
             [:body "There's no contract content."])
  (not (vali/errors? :title :body)))

;; Operations

(defn add! [contract]
  (when (valid? contract)
    (let [{:keys [id moniker] :as final} (prepare-new contract)]
      (db/update! :contracts assoc id final)
      (db/update! :contract-ids conj id)
      (db/update! :contract-monikers assoc moniker id))))

(defn edit! [{:keys [id title] :as contract}]
  (let [{orig-moniker :moniker :as original} (id->contract id)
        {:keys [moniker] :as final} (-> contract
                                      (wrap-moniker)
                                      (wrap-markdown))]
    (db/update! :contracts assoc id (merge original final))
    (db/update! :contract-monikers dissoc orig-moniker) ;; remove the old moniker entry in case it changed
    (db/update! :contract-monikers assoc moniker id)))

(defn remove! [id]
  (let [{:keys [moniker]} (id->contract id)
        neue-ids (remove #{id} (db/get :contract-ids))]
    (db/put! :contract-ids neue-ids) 
    (db/update! :contracts dissoc id)
    (db/update! :contract-monikers dissoc moniker)))

(defn init! []
    (db/put! :next-contract-id -1)
    (db/put! :contracts {})
    (db/put! :contract-ids (list))
    (db/put! :contract-monikers {}))
