# M5 管理与处罚（0.5.0）

处罚数据存储在世界目录的 `cornerstone/moderation.dat`。它使用 M1 的版本、校验和、原子写入与备份恢复机制。处罚记录为追加式：解除封禁或禁言会标记旧记录为撤销，因此历史查询仍然完整。

## 命令与 LuckPerms 节点

| 命令 | 权限节点 |
| --- | --- |
| `/kick <player> [reason]` | `cornerstone.command.kick` |
| `/ban <player> [reason]` | `cornerstone.command.ban` |
| `/tempban <player> <duration> [reason]` | `cornerstone.command.tempban` |
| `/unban <player>` | `cornerstone.command.unban` |
| `/mute <player> [reason]` | `cornerstone.command.mute` |
| `/tempmute <player> <duration> [reason]` | `cornerstone.command.tempmute` |
| `/unmute <player>` | `cornerstone.command.unmute` |
| `/warn <player> [reason]` | `cornerstone.command.warn` |
| `/history <player>` | `cornerstone.command.history` |
| `/vanish` | `cornerstone.command.vanish` |
| `/freeze <player>` | `cornerstone.command.freeze` |

临时处罚期限格式为正整数加单位：`s`、`m`、`h`、`d` 或 `w`，例如 `30m`、`12h`、`7d`。`/kick` 与 `/freeze` 要求目标在线；其他处罚可以指定在线玩家、已记录的玩家名或 UUID。

## 行为与审计

- 封禁会在玩家登录后立即断开；到期封禁不再阻止登录。
- 禁言会取消公共聊天事件，也会阻止 `/msg` 和 `/reply`。
- `/history` 显示处罚类型、时间、执行者、原因、撤销状态或到期时间。
- 所有管理命令经过统一审计，记录执行者、目标、维度、参数摘要与结果。
- `/vanish` 是会话级实体不可见状态；`/freeze` 会锁定目标的位置和速度。两者都不会跨断线或重启保留。
