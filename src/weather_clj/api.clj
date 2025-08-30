(ns weather-clj.api
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

;; 通用API协议
(defprotocol WeatherAPI
  "天气API通用接口"
  (get-forecast [this city days] "获取天气预报"))

;; 通用HTTP请求函数
(defn- make-request
  "发起HTTP请求"
  [url params timeout]
  (try
    (let [response (http/get url {:query-params params
                                  :socket-timeout timeout
                                  :conn-timeout timeout
                                  :accept :json})]
      (case (:status response)
        200 (json/parse-string (:body response) true)
        401 (let [body (try (json/parse-string (:body response) true)
                            (catch Exception _ (:body response)))]
              (throw (ex-info "API密钥无效或未授权"
                              {:status 401
                               :error-type :invalid-api-key
                               :message "请检查API密钥是否正确配置"
                               :response-body body
                               :url url})))
        403 (throw (ex-info "API访问被禁止"
                            {:status 403
                             :error-type :forbidden
                             :message "API密钥可能超出使用限制或权限不足"}))
        404 (throw (ex-info "API端点未找到"
                            {:status 404
                             :error-type :not-found
                             :message "请检查城市名称是否正确"}))
        429 (throw (ex-info "API请求频率限制"
                            {:status 429
                             :error-type :rate-limit
                             :message "API请求过于频繁，请稍后重试"}))
        (throw (ex-info "API请求失败"
                        {:status (:status response)
                         :reason (:reason-phrase response)
                         :response-body (:body response)}))))
    (catch clojure.lang.ExceptionInfo e
      ;; 重新抛出我们自己的异常
      (throw e))
    (catch Exception e
      (log/error e "HTTP请求失败")
      (throw (ex-info "网络请求失败"
                      {:error-type :network-error
                       :message "请检查网络连接"
                       :original-error (.getMessage e)})))))

;; OpenWeatherMap数据解析
(defn- parse-openweather-forecast
  [data days]
  (let [forecasts (:list data)
        daily-forecasts (->> forecasts
                             (group-by #(.substring (:dt_txt %) 0 10))
                             (take days)
                             (map (fn [[date items]]
                                    (let [temps (map #(get-in % [:main :temp]) items)
                                          weather (first items)]
                                      {:date date
                                       :temp-max (apply max temps)
                                       :temp-min (apply min temps)
                                       :description (get-in weather [:weather 0 :description])
                                       :humidity (get-in weather [:main :humidity])
                                       :wind-speed (get-in weather [:wind :speed])}))))]
    daily-forecasts))

;; WeatherAPI数据解析
(defn- parse-weatherapi-forecast
  [data days]
  (->> (get-in data [:forecast :forecastday])
       (take days)
       (map (fn [day]
              {:date (:date day)
               :temp-max (get-in day [:day :maxtemp_c])
               :temp-min (get-in day [:day :mintemp_c])
               :description (get-in day [:day :condition :text])
               :humidity (get-in day [:day :avghumidity])
               :wind-speed (get-in day [:day :maxwind_kph])}))
       (vec)))

;; OpenWeatherMap API实现
(defrecord OpenWeatherMapAPI [config]
  WeatherAPI
  (get-forecast [this city days]
    (let [api-key (get-in config [:apis :openweathermap :api-key])
          base-url (get-in config [:apis :openweathermap :base-url])
          timeout (:timeout config)]
      (if-not api-key
        (throw (ex-info "OpenWeatherMap API密钥未配置" {})))
      (let [params {:q city
                    :appid api-key
                    :units "metric"
                    :lang "zh_cn"}
            forecast-data (make-request (str base-url "/forecast") params timeout)]
        {:api :openweathermap
         :data (parse-openweather-forecast forecast-data days)}))))

;; WeatherAPI.com API实现
(defrecord WeatherAPIClient [config]
  WeatherAPI
  (get-forecast [this city days]
    (let [api-key (get-in config [:apis :weatherapi :api-key])
          base-url (get-in config [:apis :weatherapi :base-url])
          timeout (:timeout config)]
      (if-not api-key
        (throw (ex-info "WeatherAPI密钥未配置" {})))
      (let [params {:key api-key
                    :q city
                    :days (min days 10) ; WeatherAPI最多支持10天
                    :lang "zh"}
            forecast-data (make-request (str base-url "/forecast.json") params timeout)]
        {:api :weatherapi
         :data (parse-weatherapi-forecast forecast-data days)}))))

;; API客户端工厂函数
(defn create-api-client
  "创建API客户端"
  [api-type config]
  (case api-type
    :openweathermap (->OpenWeatherMapAPI config)
    :weatherapi (->WeatherAPIClient config)
    (throw (ex-info "不支持的API类型" {:api-type api-type}))))