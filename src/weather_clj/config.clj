(ns weather-clj.config
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def default-config
  {:apis {:weatherapi {:base-url "https://api.weatherapi.com/v1"
                       :api-key (System/getenv "WEATHERAPI_API_KEY")
                       :priority 1} ; WeatherAPI优先级最高（100万次/月）
          :openweathermap {:base-url "https://api.openweathermap.org/data/2.5"
                           :api-key (System/getenv "OPENWEATHER_API_KEY")
   :city {:default "Shanghai" ; 使用英文城市名确保API兼容性
          :country "CN"}
   :timeout 10000
   :retry-attempts 3}}})

(defn load-config
  "加载配置文件，如果不存在则使用默认配置"
  []
  (let [config-file "config.edn"]
    (if (.exists (io/file config-file))
      (try
        (merge default-config (edn/read-string (slurp config-file)))
        (catch Exception e
          (println "配置文件解析错误，使用默认配置:" (.getMessage e))
          default-config))
      default-config)))

(defn get-api-config
  "获取指定API的配置"
  [config api-name]
  (get-in config [:apis api-name]))

(defn get-available-apis
  "获取有效的API列表（按优先级排序）"
  [config]
  (->> (:apis config)
       (filter (fn [[_ api-config]] (:api-key api-config)))
       (sort-by (fn [[_ api-config]] (:priority api-config)))
       (map first)))
