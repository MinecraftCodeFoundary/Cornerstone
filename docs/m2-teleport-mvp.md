# M2 传送 MVP（0.2.0）

M2 的传送数据位于世界目录 `cornerstone/teleports.dat`，使用 M1 的版本化、校验和、原子写入与备份恢复机制。待确认的 `/tpa` 请求和倒计时传送是瞬态状态，服务器重启后会安全地取消。

## 命令与 LuckPerms 节点

| 命令                                 | 权限节点                           | 行为                              |
| ---------------------------------- | ------------------------------ | ------------------------------- |
| `/spawn`                           | `cornerstone.command.spawn`    | 前往当前维度出生点，不存在时回退至全局出生点。         |
| `/setspawn [global\|dimension]`    | `cornerstone.command.setspawn` | 设置全局或当前维度出生点。无参数等同 `dimension`。 |
| `/home [name]`                     | `cornerstone.command.home`     | 前往名为 `name` 的家；默认名为 `home`。     |
| `/sethome [name]`                  | `cornerstone.command.sethome`  | 保存当前位置为家。                       |
| `/delhome <name>`                  | `cornerstone.command.delhome`  | 删除一个家。                          |
| `/tpa <player>`                    | `cornerstone.command.tpa`      | 请求传送到在线玩家。                      |
| `/tpaccept`、`/tpdeny`、`/tpacancel` | 对应的 `cornerstone.command.*` 节点 | 接受、拒绝或取消请求。                     |
| `/back`                            | `cornerstone.command.back`     | 返回最近死亡位置；可选地也返回传送前位置。           |

家名称仅允许 1–32 个英文字母、数字、`_` 或 `-`，并会标准化为小写。

## 安全与配置

传送执行前会请求加载目标区块，然后检查目标维度、世界边界、构建高度、玩家碰撞箱、头顶空间、脚下支撑方块及火、岩浆、仙人掌、营火等危险方块。配置中的黑名单维度会在排队前直接拒绝。

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `teleport.delayTicks` | `60` | 执行前倒计时。 |
| `teleport.cooldownTicks` | `100` | 成功传送后的冷却。 |
| `teleport.cancelOnMove` | `true` | 倒计时期间移动超过一格则取消。 |
| `teleport.tpaExpirySeconds` | `60` | 请求过期时间。 |
| `teleport.maxHomes` | `3` | 每位玩家的家数量上限。 |
| `teleport.recordOriginForBack` | `false` | 是否记录成功传送前位置供 `/back` 使用。 |
| `teleport.blockedDimensions` | `[]` | 禁止作为目标的维度资源标识。 |

## 集成点

保护或战斗模块可通过 `CornerstoneApi.teleports().addRestriction(...)` 注册 `TeleportRestriction`，返回拒绝原因即可阻止传送。经济模块可通过 `installCostProvider(...)` 提供余额检查和扣费；费用提供者也可重写接收 `TeleportContext` 的方法以识别传送用途和配置价格。M2 的默认实现不收费。
