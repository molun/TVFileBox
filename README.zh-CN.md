# TVFileBox

<a href="README.md">English <img src="https://flagcdn.com/gb.svg" height="14" alt="英国国旗"></a> | <a href="README.zh-CN.md">简体中文 <img src="https://flagcdn.com/cn.svg" height="14" alt="中国国旗"></a>

TVFileBox 是一款面向 Android 电视盒子的轻量文件管理与局域网上传应用，兼容 Android 4.0 老设备。

应用界面会跟随 Android 系统语言：中文系统显示简体中文，其他语言系统统一显示英文；手机上传网页也遵循同样规则。

## 功能

- 使用电视遥控器或触控屏浏览设备默认共享存储目录。
- 在文件浏览器当前显示的目录中新建文件夹，并通过电视输入法命名。
- 点击“首页”返回初始共享存储目录，例如 `/storage/emulated/0`。
- 按遥控器返回键进入上一级目录。
- 按确认键/Enter 打开文件或进入文件夹。
- 长按确认键/Enter，或者在触控屏长按文件行，弹出二次确认窗口后删除文件或文件夹。
- 按遥控器设置键/菜单键，打开当前选中行的“重命名/删除”菜单。
- 使用 Android 系统安装器打开并安装 APK 文件。
- 打开“远程传输”后显示二维码，同一局域网内的手机可以通过浏览器上传文件。
- 显示上传进度、完整保存路径、上传历史以及打开/删除操作。
- 离开远程传输页后立即停止内置 HTTP 服务。

## 遥控器操作

| 操作 | 遥控器按键 |
| --- | --- |
| 上下选择文件 | 方向键上/下 |
| 打开文件或进入文件夹 | 确认键、方向键中心键或 Enter |
| 删除选中项目 | 长按确认键、方向键中心键或 Enter |
| 打开重命名/删除菜单 | 设置键或菜单键 |
| 返回上一级 | 返回键 |
| 返回初始目录 | 应用顶部“首页”按钮 |

所有删除操作都会显示二次确认窗口，并且删除后无法恢复。

## APK 选择

- `TVFileBox-v1.2.4-armeabi-v7a.apk`：32 位 ARM 老电视盒子。
- `TVFileBox-v1.2.4-arm64-v8a.apk`：64 位 ARM 设备。
- `TVFileBox-v1.2.4-universal.apk`：适合一般安装的通用版本。

TVFileBox 当前不包含原生 `.so` 库，因此按要求生成的三个 ABI 命名 APK 内容完全相同，均可在 ARMv7 和 ARM64 设备上运行。普通用户建议下载 universal 版本。

最低 Android 版本：Android 4.0（API 14）。

Release 中的 APK 使用调试签名，仅供测试；可以覆盖安装此前使用相同调试签名的 TVFileBox 版本。

## 打开与构建

1. 使用 Android Studio 打开本仓库。
2. 等待 Gradle 同步完成。
3. 连接 Android 电视盒子或模拟器。
4. 运行 `app`，或者在 PowerShell 中构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

## 存储与兼容性说明

- 默认上传目录通过 `getExternalFilesDir("uploads")` 获取；卸载应用时 Android 会删除其中的文件。
- 普通未 Root 应用无法浏览 `/data` 等受保护目录。
- Android 4.4 对外置 SD 卡的写入可能受到设备固件限制。
- APK 无法静默安装，必须在 Android 系统安装器中由用户确认。
- Android 8 及以上版本可能需要为 TVFileBox 开启“安装未知应用”权限。
- 当前 `targetSdk` 为 28，用于保留老电视盒子的传统直接路径访问行为；此配置适合侧载，不适合直接提交 Google Play。
- 部分厂商会拦截遥控器设置键，因此 TVFileBox 同时支持菜单键作为备用。

## 安全措施

- 上传地址包含随机会话令牌。
- 内置 HTTP 服务只在远程传输页面可见期间运行。
- 上传文件名会经过清理，防止路径穿越。
- 上传时先写入临时 `.part` 文件，完成后才生成正式文件。
- 手机与电视必须在同一可互访局域网；路由器的 AP 隔离功能可能导致无法访问。

## 图标与第三方软件

应用图标使用采用 Apache License 2.0 的 Google `folder_open` Material Icon。项目还使用 NanoHTTPD 和 ZXing。来源和许可证详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 项目许可证

TVFileBox 源代码目前尚未指定许可证。第三方组件继续遵循各自的许可证。
