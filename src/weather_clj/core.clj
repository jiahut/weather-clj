(ns weather-clj.core
  (:require [weather-clj.service :as service]
            [weather-clj.config :as config]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  "命令行天气查询工具"
  [& args]
  (let [cli-options [["-c" "--city CITY" "城市名称"
                      :id :city
                      :default "Shanghai" ; 使用英文城市名确保API兼容性
                      :desc "要查询的城市名称"]
                     ["-d" "--days DAYS" "天数"
                      :id :days
                      :default 7
                      :parse-fn #(Integer/parseInt %)
                      :validate [#(<= 1 % 10) "天数必须在1-10之间"]
                      :desc "预报天数 (1-10天)"]
                     ["-h" "--help" "显示帮助信息"
                      :id :help]]
        {:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]

    (cond
      (:help options)
      (do
        (println "天气查询工具")
        (println "============")
        (println summary)
        (println "\n使用示例:")
        (println "  lein run                        # 查询Shanghai未来7天天气")
        (println "  lein run --city Beijing          # 查询Beijing未来7天天气")
        (println "  lein run --city Guangzhou --days 3 # 查询Guangzhou未来3天天气")
        (println "\n支持的天气API供应商 (按优先级):")
        (println "  1. WeatherAPI.com (免费100万次/月) - 推荐")
        (println "  2. OpenWeatherMap (免费1000次/天)")
        (println "\n配置API密钥:")
        (println "  设置环境变量:")
        (println "    WEATHERAPI_API_KEY=your-key     # 推荐")
        (println "    OPENWEATHER_API_KEY=your-key")
        (println "\n获取API密钥:")
        (println "  WeatherAPI: https://www.weatherapi.com/signup.aspx")
        (println "  OpenWeatherMap: https://openweathermap.org/api")

        (println "\n城市名称建议:")
        (println "  推荐使用英文城市名以确保所有API兼容")
        (println "  示例: Shanghai, Beijing, Guangzhou, Shenzhen")
        (println "  也可使用 '城市,国家代码' 格式: Shanghai,CN"))

      errors
      (do
        (doseq [error errors]
          (println "错误:" error))
        (println "\n使用 --help 查看帮助信息")
        (System/exit 1))

      :else
      (let [city (:city options)
            days (:days options)]
        (try
          (println (format "正在查询%s未来%d天的天气预报..." city days))
          (let [result (service/get-weather-forecast city days)]
            (service/format-weather-output result))
          (catch Exception e
            (let [error-data (ex-data e)
                  error-type (:error-type error-data)]
              (log/error e "程序执行出错")

              ;; 根据错误类型提供具体的解决方案
              (case error-type
                :invalid-api-key
                (do
                  (println "❌ API密钥无效或未授权")
                  (println "\n解决方案:")
                  (println "  1. 检查环境变量是否正确设置:")
                  (println "     echo $WEATHERAPI_API_KEY")
                  (println "     echo $OPENWEATHER_API_KEY")
                  (println "  2. 确认API密钥格式正确（无空格、无引号）")
                  (println "  3. 验证API密钥是否有效（未过期、未撤销）")
                  (println "  4. 获取新的API密钥:")
                  (println "     WeatherAPI: https://www.weatherapi.com/signup.aspx")
                  (println "     OpenWeatherMap: https://openweathermap.org/api"))

                :rate-limit
                (do
                  (println "❌ API请求频率超限")
                  (println "\n解决方案:")
                  (println "  1. 等待几分钟后重试")
                  (println "  2. 升级到付费计划获得更高配额")
                  (println "  3. 使用其他API供应商"))

                :forbidden
                (do
                  (println "❌ API访问被禁止")
                  (println "\n解决方案:")
                  (println "  1. 检查API密钥权限设置")
                  (println "  2. 确认账户状态正常")
                  (println "  3. 联系API供应商客服"))

                :not-found
                (do
                  (println "❌ 未找到指定城市")
                  (println "\n解决方案:")
                  (println "  1. 检查城市名称拼写")
                  (println "  2. 尝试使用英文城市名称")
                  (println "  3. 使用'城市,国家代码'格式，如'Shanghai,CN'")
                  (println "  4. 常用中国城市英文名：Shanghai, Beijing, Guangzhou, Shenzhen"))

                :network-error
                (do
                  (println "❌ 网络连接失败")
                  (println "\n解决方案:")
                  (println "  1. 检查网络连接")
                  (println "  2. 检查防火墙设置")
                  (println "  3. 尝试使用VPN（如果在限制网络环境中）"))

                ;; 默认错误处理
                (do
                  (println (format "❌ 查询失败: %s" (.getMessage e)))
                  (println "\n通用解决方案:")
                  (println "  1. 检查网络连接")
                  (println "  2. 确认API密钥是否正确配置")
                  (println "  3. 尝试其他城市名称（推荐英文城市名）")
                  (println "  4. 稍后重试")))

              (System/exit 1))))))))
