(ns DemoBackoffice.views.dashboard
  (:use noir.core
        hiccup.core
        hiccup.page-helpers)
  (:require [DemoBackoffice.models.contract :as contracts]
            [DemoBackoffice.views.common :as common]
            [DemoBackoffice.models.user :as user]
            [noir.response :as resp]))

;; Page structure

(defpartial contract-item [{:keys [perma-link title md-body date tme] :as contract}]
            (when contract
              [:li.contract
               [:h2 (link-to perma-link title)]
               [:ul.datetime
                [:li date]
                [:li tme]
                (when (user/admin?)
                  [:li (link-to (contracts/edit-url contract) "edit")])]
               [:div.content md-body]]))

(defpartial dashboard-page [items]
            (common/main-layout
              [:ul.contracts
               (map contract-item items)]))

;; dashboard pages

(defpage "/" []
         (resp/redirect "/dashboard/"))

(defpage "/dashboard/" []
         (dashboard-page (contracts/get-latest)))

(defpage "/dashboard/page/:page" {:keys [page]}
         (dashboard-page (contracts/get-page page)))

(defpage "/dashboard/page/" []
         (render "/dashboard/page/:page" {:page 1}))

(defpage "/dashboard/view/:perma" {:keys [perma]}
         (if-let [cur (contracts/moniker->contract perma)]
           (dashboard-page [cur])))
