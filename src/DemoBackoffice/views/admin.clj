(ns DemoBackoffice.views.admin
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [noir.response :as resp]
            [clojure.string :as string]
            [DemoBackoffice.models.contract :as contracts]
            [DemoBackoffice.models.user :as users]
            [DemoBackoffice.views.common :as common]))

;; Links

(def contract-actions [{:url "/dashboard/admin/contract/add" :text "Add a contract"}])
(def user-actions [{:url "/dashboard/admin/user/add" :text "Add a user"}])

;; Partials

(defpartial error-text [errors]
            [:span (string/join "" errors)])

(defpartial contract-fields [{:keys [title body]}]
            (vali/on-error :title error-text)
            (text-field {:placeholder "Title"} :title title)
            (vali/on-error :body error-text)
            (text-area {:placeholder "Body"} :body body))

(defpartial user-fields [{:keys [username] :as usr}]
            (vali/on-error :username error-text)
            (text-field {:placeholder "Username"} :username username)
            (vali/on-error :password error-text)
            (password-field {:placeholder "Password"} :password))

(defpartial contract-item [{:keys [title] :as contract}]
            [:li
             (link-to (contracts/edit-url contract) title)])

(defpartial action-item [{:keys [url text]}]
            [:li
             (link-to url text)])

(defpartial user-item [{:keys [username]}]
            [:li
             (link-to (str "/dashboard/admin/user/edit/" username) username)])

;; Admin pages

;;force you to be an admin to get to the admin section
(pre-route "/dashboard/admin*" {}
           (when-not (users/admin?)
             (resp/redirect "/dashboard/login")))

(defpage "/dashboard/login" {:as user}
         (if (users/admin?)
           (resp/redirect "/dashboard/admin")
           (common/main-layout
             (form-to [:post "/dashboard/login"]
                      [:ul.actions
                       [:li (link-to {:class "submit"} "/" "Login")]]
                      (user-fields user)
                      (submit-button {:class "submit"} "submit")))))

(defpage [:post "/dashboard/login"] {:as user}
         (if (users/login! user)
           (resp/redirect "/dashboard/admin")
            (render "/dashboard/login" user)))

(defpage "/dashboard/logout" {}
         (session/clear!)
         (resp/redirect "/dashboard/"))

;; contract pages

(defpage "/dashboard/admin" {}
         (common/admin-layout
           [:ul.actions
            (map action-item contract-actions)]
           [:ul.items
            (map contract-item (contracts/get-latest))])
         )

(defpage "/dashboard/admin/contract/add" {:as contract}
         (common/admin-layout
           (form-to [:post "/dashboard/admin/contract/add"]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Add")]]
                    (contract-fields contract)
                    (submit-button {:class "submit"} "add contract"))))

(defpage [:post "/dashboard/admin/contract/add"] {:as contract}
           (if (contracts/add! contract)
             (resp/redirect "/dashboard/admin")
             (render "/dashboard/admin/contract/add" contract)))

(defpage "/dashboard/admin/contract/edit/:id" {:keys [id]}
         (if-let [contract (contracts/id->contract id)]
           (common/admin-layout
             (form-to [:post (str "/dashboard/admin/contract/edit/" id)]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Submit")]
                        [:li (link-to {:class "delete"} (str "/dashboard/admin/contract/remove/" id) "Remove")]]
                      (contract-fields contract)
                      (submit-button {:class "submit"} "submit")))))

(defpage [:post "/dashboard/admin/contract/edit/:id"] {:keys [id] :as contract}
         (if (contracts/edit! contract)
           (resp/redirect "/dashboard/admin")
           (render "/dashboard/admin/contract/edit/:id" contract)))

(defpage "/dashboard/admin/contract/remove/:id" {:keys [id]}
         (contracts/remove! id)
         (resp/redirect "/dashboard/admin"))

;; User pages

(defpage "/dashboard/admin/users" {}
         (common/admin-layout
           [:ul.actions
            (map action-item user-actions)]
           [:ul.items
            (map user-item (users/all))]))

(defpage "/dashboard/admin/user/add" {:as user}
         (common/admin-layout
           (form-to [:post "/dashboard/admin/user/add"]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Add")]]
                    (user-fields user)
                    (submit-button {:class "submit"} "add user"))))

(defpage [:post "/dashboard/admin/user/add"] {:keys [username password] :as new-user}
         (if (users/add! new-user)
           (resp/redirect "/dashboard/admin/users")
           (render "/dashboard/admin/user/add" new-user)))

(defpage "/dashboard/admin/user/edit/:old-name" {:keys [old-name]}
         (let [user (users/get-username old-name)]
           (common/admin-layout
             (form-to [:post (str "/dashboard/admin/user/edit/" old-name)]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Submit")]
                        [:li (link-to {:class "delete"} (str "/dashboard/admin/user/remove/" old-name) "Remove")]]
                      (user-fields user)))))

(defpage [:post "/dashboard/admin/user/edit/:old-name"] {:keys [old-name] :as user}
         (if (users/edit! user)
           (resp/redirect "/dashboard/admin/users")
           (render "/dashboard/admin/user/edit/:old-name" user)))

(defpage "/dashboard/admin/user/remove/:id" {:keys [id]}
         (users/remove! id)
         (resp/redirect "/dashboard/admin/users"))
