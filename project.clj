(defproject weather-clj "0.1.0-SNAPSHOT"
  :description "命令行天气查询工具，支持多个天气API供应商"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [cheshire "5.12.0"] ; JSON 处理
                 [clj-http "3.12.3"] ; HTTP 客户端
                 [org.clojure/tools.cli "1.0.219"] ; 命令行参数解析
                 [org.clojure/tools.logging "1.2.4"]] ; 日志
  :main ^:skip-aot weather-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
