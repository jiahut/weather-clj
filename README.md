*weather-clj* 是一个功能丰富的命令行天气查询工具，用中文界面为用户提供便捷的天气信息服务。

项目功能特色
🌤️ 主要功能
多天天气预报：支持查询1-10天的天气预报信息
多城市支持：可查询任意城市的天气，默认为上海
中文友好界面：完全中文化的命令行界面和帮助信息
智能城市识别：支持中英文城市名，建议使用国际标准城市名
🔧 技术特点
多API供应商支持：

WeatherAPI.com（主推）：免费100万次/月
OpenWeatherMap：免费1000次/天

## 获取免费API密钥（推荐 WeatherAPI.com）

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
```

### Linux/Mac:
```bash
export WEATHERAPI_API_KEY=your-weatherapi-key-here
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
