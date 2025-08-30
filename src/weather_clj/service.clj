(ns weather-clj.service
  (:require [weather-clj.api :as api]
            [weather-clj.config :as config]
            [clojure.tools.logging :as log]))

  (defn- try-api
  "尝试使用指定API获取天气数据
   返回统一的结果 map：{:success true :data ...} 或 {:success false :error ...}"
  [api-client city days max-retries]
  (loop [attempt 1]
    (let [result (try
                   (log/info "尝试获取天气数据，API:" (type api-client) "，尝试次数:" attempt)
                   (let [resp (api/get-forecast api-client city days)]
                     ;; API 实现可能返回 {:api <id> :data <seq>} 或直接返回 seq
                     (if (and (map? resp) (contains? resp :data))
                       {:success true :api (:api resp) :data (:data resp)}
                       {:success true :data resp}))
                   (catch Exception e
                     (log/warn e "API调用失败，尝试次数:" attempt)
                     {:success false :error e}))]
      (if (:success result)
        result                                   ; 返回统一的 map，包含 :api/:data
        (if (< attempt max-retries)
          (do
            (Thread/sleep (* 1000 attempt)) ; 指数退避
            (recur (inc attempt)))
          result)))))                             ; 返回最后一次失败的 map

(defn get-weather-forecast
  "获取天气预报，支持多API故障转移"
  ([city] (get-weather-forecast city 7))
  ([city days]
   (let [config (config/load-config)
         available-apis (config/get-available-apis config)
         max-retries (or (:retry-attempts config) 3)]
     (if (empty? available-apis)
       (throw (ex-info "没有可用的API配置" {:available-apis available-apis})))

     (log/info "开始获取天气预报" {:city city :days days :available-apis available-apis})

     (loop [apis available-apis]
       (if (empty? apis)
         (throw (ex-info "所有API都失败了" {:city city :days days}))

         (let [current-api (first apis)
               remaining-apis (rest apis)
               result (try
                        (log/info "尝试使用API:" current-api)
                        (let [api-client (api/create-api-client current-api config)
                              api-try-result (try-api api-client city days max-retries)]
                          (if (:success api-try-result)
                            (do
                              (log/info "成功获取天气数据，使用API:" current-api)
                              {:success true :api current-api :data (:data api-try-result)})
                            ;; try-api 已返回失败 map，抛出以进入 catch 分支处理
                            (throw (:error api-try-result))))
                        (catch Exception e
                          (log/error e "API失败:" current-api)
                          {:success false :error e}))]
           (if (:success result)
             ;; 统一返回带 :api 的 map（上层代码期待该结构）
             {:api (:api result) :data (:data result)}
             (if (empty? remaining-apis)
               (throw (ex-info "所有API都失败了" {:city city :days days :last-error (.getMessage (:error result))}))
               (do
                 (log/info "尝试下一个API...")
                 (recur remaining-apis))))))))))

(defn format-weather-output
  "格式化天气输出为表格形式"
  [weather-result]
  (let [{:keys [api data]} weather-result
        col-width 12 ; 每列宽度
        label-width 10 ; 标签列固定宽度
        separator "|"]

    (println (str "\n📍 天气预报 (数据来源: " (name api) ")"))
    (println (apply str (repeat (+ label-width 1 (* (count data) (+ col-width 1))) "=")))

    ;; 辅助函数：计算字符串显示宽度（中文字符占2个宽度）
    (letfn [(char-width [c]
              (let [code (int c)]
                (cond
                  ;; 中文字符
                  (and (>= code 0x4E00) (<= code 0x9FFF)) 2
                  ;; emoji 和特殊符号
                  (and (>= code 0x1F000) (<= code 0x1F9FF)) 2
                  ;; 其他符号
                  (and (>= code 0x2000) (<= code 0x2BFF)) 2
                  ;; 默认英文字符
                  :else 1)))

            (string-display-width [s]
              (reduce + (map char-width s)))

            (format-cell [content width]
              (let [content-str (str content)
                    display-width (string-display-width content-str)
                    padding (max 0 (- width display-width))]
                (if (> display-width width)
                  ;; 内容过长时截断
                  (loop [chars (seq content-str)
                         current-width 0
                         result []]
                    (if (or (empty? chars) (> (+ current-width (char-width (first chars))) (- width 1)))
                      (str (apply str result) "…")
                      (recur (rest chars)
                             (+ current-width (char-width (first chars)))
                             (conj result (first chars)))))
                  ;; 正常情况下添加填充
                  (str content-str (apply str (repeat padding " "))))))

            (print-row [label values]
              ;; 确保标签列有固定宽度
              (print (format-cell label label-width))
              (print separator)
              (doseq [value values]
                (print (format-cell value col-width))
                (print separator))
              (println))]

      ;; 表头：日期行（健壮处理空或短日期字符串）
      (let [format-date (fn [d]
                          (let [s (or d "")]
                            (if (and (string? s) (>= (count s) 5))
                              (subs s 5)
                              "--")))
            dates (map #(format-date (:date %)) data)] ; 只显示月-日
        (print-row "日期" dates))

      ;; 分隔线
      (println (apply str (repeat (+ label-width 1 (* (count data) (+ col-width 1))) "-")))

      ;; 温度行
      (let [temps (map #(format "%s°~%s°"
                                (if (:temp-min %) (format "%.0f" (:temp-min %)) "--")
                                (if (:temp-max %) (format "%.0f" (:temp-max %)) "--"))
                       data)]
        (print-row "🌡️ 温度" temps))

      ;; 天气描述行
      (let [descriptions (map #(or (:description %) "--") data)]
        (print-row "☁️ 天气" descriptions))

      ;; 湿度行
      (let [humidities (map #(if (:humidity %)
                               (str (:humidity %) "%")
                               "--")
                            data)]
        (print-row "💧 湿度" humidities))

      ;; 风速行
      (let [wind-speeds (map #(if (:wind-speed %)
                                (cond
                                  (and (number? (:wind-speed %)) (< (:wind-speed %) 20))
                                  (format "%.1fm/s" (:wind-speed %))
                                  (number? (:wind-speed %))
                                  (format "%.1fkm/h" (:wind-speed %))
                                  :else (str (:wind-speed %)))
                                "--")
                             data)]
        (print-row "💨 风速" wind-speeds))

      ;; 底部分隔线
      (println (apply str (repeat (+ label-width 1 (* (count data) (+ col-width 1))) "=")))
      (println))))

(defn get-weather-summary
  "获取天气摘要信息"
  [city days]
  (try
    (let [result (get-weather-forecast city days)]
      {:success true
       :api (:api result)
       :forecast (:data result)
       :summary (format "成功获取%s未来%d天天气预报" city days)})
    (catch Exception e
      (log/error e "获取天气预报失败")
      {:success false
       :error (.getMessage e)
       :summary (format "无法获取%s的天气预报: %s" city (.getMessage e))})))