# 配置与功能模块

Cornerstone 的服务端配置文件为 `<服务器目录>/config/cornerstone.json`。首次启动时会自动生成默认文件；文件中的 `_comment` 字段是中文说明，JSON 读取时会忽略它们。

旧版 `cornerstone-common.toml` 不再被读取。请在停服后将需要保留的设置迁移到 `cornerstone.json`，再重启服务器使配置和命令树生效。

## 模块开关

`modules` 中的每项都可独立设置为 `true` 或 `false`：

```json
{
  "modules": {
    "core": true,
    "teleport": true,
    "warps": true,
    "social": true,
    "moderation": true,
    "operations": true,
    "economy": true
  }
}
```

| 模块 | 提供的功能 |
| --- | --- |
| `core` | 玩家资料、权限、审计和 Cornerstone 基础命令 |
| `teleport` | 出生点、家、传送请求与 `/back` |
| `warps` | 地标和 `/rtp`；依赖 `teleport` |
| `social` | 私信、AFK、规则、公告与聊天格式 |
| `moderation` | 封禁、禁言、警告、隐身和冻结 |
| `operations` | 维护模式、停服计划和实用管理命令 |
| `economy` | 余额、转账、流水与经济 API |

关闭某个模块后，它的命令不会注册，相关存档、事件处理和定时任务也不会启动。关闭 `core` 会同时关闭所有其他模块；关闭 `teleport` 会同时关闭 `warps`。

## 命令缩略名

在 `commands.aliases` 中以 `原命令=别名1,别名2` 定义缩略名。当前支持 `gamemode`，默认将其配置为 `/gm`：

```json
{
  "commands": {
    "aliases": ["gamemode=gm"]
  }
}
```

缩略命令与原命令使用相同的 LuckPerms 权限、参数校验和审计记录。
