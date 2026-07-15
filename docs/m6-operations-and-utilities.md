# M6 运维与实用工具（0.6.0）

## 维护模式

`cornerstone/operations.dat` 保存维护模式开关和显式玩家允许列表，并在每次变更后立即原子写入。开启维护模式后，未在允许列表且没有 LuckPerms 节点 `cornerstone.maintenance.bypass` 的玩家会在登录后断开。

| 命令 | 权限节点 | 说明 |
| --- | --- | --- |
| `/maintenance` | `cornerstone.command.maintenance` | 查看维护状态。 |
| `/maintenance on\|off` | 同上 | 开启或关闭维护模式。 |
| `/maintenance allow <player>` | 同上 | 切换玩家允许列表状态；支持已记录的离线玩家。 |

## 停服与重启计划

| 命令 | 权限节点 |
| --- | --- |
| `/restart <duration>` | `cornerstone.command.restart` |
| `/shutdown <duration>` | `cornerstone.command.shutdown` |
| `/cancelshutdown` | `cornerstone.command.cancelshutdown` |

期限格式为 `30s`、`5m`、`1h`、`1d` 或 `1w`。同一时间只能有一个计划；新计划会替换旧计划。倒计时会在 5 分钟间隔、60 秒、30 秒、最后 10 秒及执行时通知在线玩家。`restart` 与 `shutdown` 都会正常停止服务器；重启需要外部进程管理器重新启动 Java 进程。计划不会跨服务器重启保留。

## 实用工具

| 命令 | 权限节点 | 说明 |
| --- | --- | --- |
| `/invsee <player>` | `cornerstone.command.invsee` | 打开目标 36 格背包的只读视图。 |
| `/endersee <player>` | `cornerstone.command.endersee` | 打开目标末影箱的只读视图。 |
| `/clear <player>` | `cornerstone.command.clear` | 清空目标背包。 |
| `/heal <player>` | `cornerstone.command.heal` | 恢复生命并熄灭。 |
| `/feed <player>` | `cornerstone.command.feed` | 恢复饥饿和饱和。 |
| `/gamemode <mode> <player>` | `cornerstone.command.gamemode` | 设置模式：`survival`、`creative`、`adventure`、`spectator`。 |

`/invsee` 与 `/endersee` 的槽位同时禁止放入、取出和 shift 快速移动，直接展示目标容器而不复制或重写其中的模组物品、数据组件或容器数据。`/clear` 是唯一会有意删除背包内容的工具命令，所有这些操作都写入审计日志。

## 命令缩略名

`commands.aliases` 可配置已支持命令的缩略名，格式为 `原命令=别名1,别名2`。目前支持 `gamemode`，默认配置为：

```json
{
  "commands": {
    "aliases": ["gamemode=gm"]
  }
}
```

因此 `/gm creative Steve` 与 `/gamemode creative Steve` 使用完全相同的 LuckPerms 节点、参数校验和审计记录。修改后重启服务器使命令树重新注册。
