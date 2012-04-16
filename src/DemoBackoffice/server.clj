(ns DemoBackoffice.server
  (:require [noir.server :as server]
            [DemoBackoffice.models :as models]))

(server/load-views "src/DemoBackoffice/views/")

(defn -main [& m]
  (let [mode (or (first m) :dev)
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (models/initialize)
    (server/start port {:mode (keyword mode)
                        :ns 'DemoBackoffice})))

