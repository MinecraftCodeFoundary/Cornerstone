# M1 核心服务层（0.1.0）

Cornerstone 的 M1 是后续功能的服务端基础层。功能模块应通过 `CornerstoneApi` 使用玩家资料、权限和审计服务；不得读取或修改 `world/cornerstone` 下的文件。

## 公共 API

`org.yeyao.cornerstone.api.CornerstoneApi` 提供以下稳定入口：

- `players()`：读取玩家资料、保存模块自己的字符串键值数据，并订阅 `PlayerDataUpdatedEvent`。
- `permissions()`：通过 LuckPerms 权限节点检查命令源。
- `audit()`：记录可查询的审计事件。

模块数据的模块 ID 必须为小写的 `a-z`、数字、`.`、`_` 或 `-`，长度最多 64；键长度最多 128；值最多 8192 个字符。资料的主键永远是玩家 UUID，不使用名称作为身份标识。

## 存储与恢复

数据保存在世界目录的 `cornerstone/players.dat`，包含格式版本、长度、CRC32 校验和和载荷。写入先落到 `.tmp`，再执行原子替换；原文件保留为 `.bak`。主文件无法读取时会自动尝试最后的备份文件。`AtomicFileStorage` 也提供逐版本 `DataMigration` 接口，新增数据格式时必须为旧格式提供迁移。

审计记录保存到 `cornerstone/audit.log`，并保留最近 2000 条内存记录供管理功能查询。

## 权限与命令

LuckPerms `5.4+` 是 Cornerstone 的必需服务端前置。每个命令只声明一个 LuckPerms 权限节点；玩家权限由 LuckPerms 的缓存权限数据判定，不回退到原版 OP 等级。非玩家命令源（包括控制台与 RCON）由服务器视为受信任的操作源。

| 命令 | LuckPerms 权限节点 |
| --- | --- |
| `/cornerstone status` | `cornerstone.command.status` |
| `/cornerstone save` | `cornerstone.command.save` |

`CommandFramework` 统一检查权限、参数错误、冷却、用户可见结果和审计。异常参数会转换为失败结果，不能向服务器传播未处理异常。

## 配置

`storage.autoSaveTicks` 默认 `6000`（5 分钟），范围 `200` 至 `72000`。正常停止和 `/cornerstone save` 都会立即保存玩家资料。
