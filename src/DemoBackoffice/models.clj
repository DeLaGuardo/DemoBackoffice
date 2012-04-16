(ns DemoBackoffice.models
  (:require [simpledb.core :as db]
            [DemoBackoffice.models.user :as users]
            [DemoBackoffice.models.contract :as contracts]))

(defn initialize []
  (db/init)
  (when-not (db/get :users)
    ;;db values need to be initialized.. this should only happen once.
    (users/init!)
    (contracts/init!)))
