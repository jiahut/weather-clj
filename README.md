*weather-clj* 是一个功能丰富的命令行天气查询工具，提供中文界面与便捷的多城市、多天预报查询能力。

## 功能特性
- 主要功能
   - 多天天气预报：支持查询 1–10 天的天气预报。
   - 多城市支持：可查询任意城市，默认城市为上海。
   - 中文友好界面：命令行界面与帮助信息均为中文。
   - 智能城市识别：支持中英文城市名输入，优先使用国际标准城市名以减少歧义。
- 技术特点
   - 支持多个天气 API 供应商，可按需切换或备用。
   - 易于在 Windows、Linux 与 macOS 上配置与运行。
   - 可打包为独立可执行的 uberjar，便于部署与分发。

## 支持的 API 供应商
- WeatherAPI.com（推荐）：免费额度高（约 100 万次/月）。
- OpenWeatherMap：免费额度约 1000 次/天。

### 获取免费API密钥（推荐 WeatherAPI.com）

1. **WeatherAPI.com (最佳选择)**
   - 访问: https://www.weatherapi.com/signup.aspx
   - 免费注册账户
   - 获取API密钥 (100万次/月免费额度)

2. **OpenWeatherMap**
   - 访问: https://home.openweathermap.org/users/sign_up
   - 免费注册账户
   - 获取API密钥 (1000次/天免费额度)

## 配置API密钥

### Windows:
```cmd
set WEATHERAPI_API_KEY=your-weatherapi-key-here
set OPENWEATHER_API_KEY=your-key

```

### Linux/Mac:
```bash
export WEATHERAPI_API_KEY=your-weatherapi-key-here
export OPENWEATHER_API_KEY=your-key
```

## 测试运行

```bash
# 基本查询
lein run

# 查询其他城市
lein run --city 北京
lein run --city "New York"

# 自定义天数
lein run --city 深圳 --days 3
```

## 构建可执行文件

```bash
lein uberjar
java -jar target/uberjar/weather-clj-0.1.0-SNAPSHOT-standalone.jar --city 上海
```
