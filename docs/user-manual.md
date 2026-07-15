# Cornerstone Mod 使用手册

适用于 Cornerstone `0.7.1`、Minecraft `1.21.1` 与 NeoForge `21.1.235`。Cornerstone 是服务端基础设施 Mod，提供传送、地标、社交、管理、运维和经济功能。

## 1. 安装与首次启动

1. 将 Cornerstone JAR 放入服务器的 `mods` 目录。
2. 安装 LuckPerms `5.4+`（Cornerstone 的必需前置），并完成服务器启动。
3. 首次启动会在 `<服务器目录>/config/` 生成 `cornerstone.json`。
4. 停服后编辑配置，再启动服务器使模块和命令树重新加载。

Cornerstone 使用 LuckPerms 权限节点授权。原版 OP 等级 `4` 默认拥有所有 Cornerstone 权限；非 OP 玩家和管理组应通过 LuckPerms 授权。

例如，为默认玩家组授予家与传送请求功能：

```text
/lp group default permission set cornerstone.command.home true
/lp group default permission set cornerstone.command.sethome true
/lp group default permission set cornerstone.command.delhome true
/lp group default permission set cornerstone.command.tpa true
/lp group default permission set cornerstone.command.tpaccept true
/lp group default permission set cornerstone.command.tpdeny true
/lp group default permission set cornerstone.command.tpacancel true
```

## 2. 配置与模块

配置文件为 `config/cornerstone.json`。其中的 `_comment` 是中文说明字段，不影响功能。旧版 `cornerstone-common.toml` 不再读取。

主要模块位于 `modules`：

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

| 模块 | 内容 | 依赖 |
| --- | --- | --- |
| `core` | 玩家资料、权限、审计、`/cornerstone` | 无 |
| `teleport` | 出生点、家、传送请求、返回 | `core` |
| `warps` | 地标、随机传送 | `core`、`teleport` |
| `social` | 私信、AFK、规则、公告、聊天格式 | `core` |
| `moderation` | 封禁、禁言、警告、隐身、冻结 | `core` |
| `operations` | 维护模式、停服计划、管理工具 | `core` |
| `economy` | 余额、转账、流水和 API | `core` |

设为 `false` 的模块不会注册命令，不会加载相应存档，也不会运行相关事件处理和定时任务。关闭 `core` 会禁用全部其他模块；关闭 `teleport` 会禁用 `warps`。

完整配置字段说明参见 [configuration-and-modules.md](configuration-and-modules.md)。

## 3. 权限规则

### 命令权限速查

| 命令 | LuckPerms 权限 |
| --- | --- |
| `/cornerstone status` | `cornerstone.command.status` |
| `/cornerstone save` | `cornerstone.command.save` |
| `/spawn` | `cornerstone.command.spawn` |
| `/setspawn` | `cornerstone.command.setspawn` |
| `/home` | `cornerstone.command.home` |
| `/sethome` | `cornerstone.command.sethome` |
| `/delhome` | `cornerstone.command.delhome` |
| `/tpa` | `cornerstone.command.tpa` |
| `/tpaccept` | `cornerstone.command.tpaccept` |
| `/tpdeny` | `cornerstone.command.tpdeny` |
| `/tpacancel` | `cornerstone.command.tpacancel` |
| `/back` | `cornerstone.command.back` |
| `/warp` | `cornerstone.command.warp`（权限地标另需 `cornerstone.warp.<名称>`；管理员地标另需 `cornerstone.warp.admin.<名称>`） |
| `/setwarp` | `cornerstone.command.setwarp` |
| `/delwarp` | `cornerstone.command.delwarp` |
| `/rtp` | `cornerstone.command.rtp` |
| `/msg` | `cornerstone.command.msg` |
| `/reply` | `cornerstone.command.reply` |
| `/ignore` | `cornerstone.command.ignore` |
| `/afk` | `cornerstone.command.afk` |
| `/seen` | `cornerstone.command.seen` |
| `/list` | `cornerstone.command.list` |
| `/rules` | `cornerstone.command.rules` |
| `/balance` | `cornerstone.command.balance` |
| `/pay` | `cornerstone.command.pay` |
| `/economy history` | `cornerstone.command.economy.history` |
| `/economy export` | `cornerstone.command.economy.export` |
| `/kick` | `cornerstone.command.kick` |
| `/ban` | `cornerstone.command.ban` |
| `/tempban` | `cornerstone.command.tempban` |
| `/unban` | `cornerstone.command.unban` |
| `/mute` | `cornerstone.command.mute` |
| `/tempmute` | `cornerstone.command.tempmute` |
| `/unmute` | `cornerstone.command.unmute` |
| `/warn` | `cornerstone.command.warn` |
| `/history` | `cornerstone.command.history` |
| `/vanish` | `cornerstone.command.vanish` |
| `/freeze` | `cornerstone.command.freeze` |
| `/maintenance` | `cornerstone.command.maintenance` |
| `/restart` | `cornerstone.command.restart` |
| `/shutdown` | `cornerstone.command.shutdown` |
| `/cancelshutdown` | `cornerstone.command.cancelshutdown` |
| `/invsee` | `cornerstone.command.invsee` |
| `/endersee` | `cornerstone.command.endersee` |
| `/clear` | `cornerstone.command.clear` |
| `/heal` | `cornerstone.command.heal` |
| `/feed` | `cornerstone.command.feed` |
| `/gamemode`、`/gm` | `cornerstone.command.gamemode` |

每个命令使用 `cornerstone.command.<命令名>` 节点。例如 `/spawn` 使用 `cornerstone.command.spawn`，`/ban` 使用 `cornerstone.command.ban`。地标有额外访问节点：

- 权限地标：`cornerstone.warp.<地标名>`
- 管理员地标：`cornerstone.warp.admin.<地标名>`
- 维护模式绕过：`cornerstone.maintenance.bypass`

所有命令会校验权限与参数；管理和经济变更会写入 Cornerstone 审计或流水数据。

## 4. 玩家功能

### 传送、家与出生点

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/spawn` | 前往当前维度出生点；没有时回退到全局出生点。 | `cornerstone.command.spawn` |
| `/home [名称]` | 前往名为 `名称` 的家；省略时使用 `home`。 | `cornerstone.command.home` |
| `/sethome [名称]` | 将当前位置设为家。数量由 `teleport.maxHomes` 限制。 | `cornerstone.command.sethome` |
| `/delhome <名称>` | 删除一个家。 | `cornerstone.command.delhome` |
| `/tpa <玩家>` | 向在线玩家发送传送请求。 | `cornerstone.command.tpa` |
| `/tpaccept`、`/tpdeny`、`/tpacancel` | 接受、拒绝或取消传送请求。 | `cornerstone.command.tpaccept`、`cornerstone.command.tpdeny`、`cornerstone.command.tpacancel` |
| `/back` | 返回最近记录的死亡位置；可配置为也记录传送前位置。 | `cornerstone.command.back` |

传送会遵守延迟、冷却、移动取消、维度黑名单、区块加载和安全落点检查。

### 地标与随机传送

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/warp <名称>` | 前往地标。 | `cornerstone.command.warp`；权限地标另需 `cornerstone.warp.<名称>`，管理员地标另需 `cornerstone.warp.admin.<名称>` |
| `/rtp` | 按当前维度的随机传送规则寻找安全落点。 | `cornerstone.command.rtp` |

`/rtp` 只在 `teleport.rtp.dimensionRules` 包含当前维度时可用。规则格式为：

```text
维度ID|最大半径|最小距离|尝试次数|冷却刻数
```

示例：`minecraft:overworld|1000|150|32|600`。

### 社交与信息

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/msg <玩家> <内容>` | 向在线玩家发送私信。 | `cornerstone.command.msg` |
| `/reply <内容>` | 回复最后一位私信联系人。 | `cornerstone.command.reply` |
| `/ignore <玩家>` | 切换对该在线玩家的私信屏蔽。 | `cornerstone.command.ignore` |
| `/afk [原因]` | 切换 AFK 状态；聊天或移动会自动解除。 | `cornerstone.command.afk` |
| `/seen <玩家>` | 查询在线情况或最后在线时间。 | `cornerstone.command.seen` |
| `/list` | 查看在线玩家与 AFK 标记。 | `cornerstone.command.list` |
| `/rules` | 显示配置中的服务器规则。 | `cornerstone.command.rules` |

## 5. 管理功能

### 出生点与地标管理

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/setspawn [global\|dimension]` | 设定全局或当前维度出生点。 | `cornerstone.command.setspawn` |
| `/setwarp <名称> [public\|permission\|admin]` | 创建或更新地标；默认 `public`。 | `cornerstone.command.setwarp` |
| `/delwarp <名称>` | 删除地标。 | `cornerstone.command.delwarp` |

地标名仅允许 1–32 个小写英文字母、数字、`_` 或 `-`。

### 处罚与在线管理

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/kick <玩家> [原因]` | 踢出在线玩家。 | `cornerstone.command.kick` |
| `/ban <玩家> [原因]`、`/unban <玩家>` | 永久封禁或解除封禁。 | `cornerstone.command.ban`、`cornerstone.command.unban` |
| `/tempban <玩家> <时长> [原因]` | 临时封禁。 | `cornerstone.command.tempban` |
| `/mute <玩家> [原因]`、`/unmute <玩家>` | 禁言或解除禁言。 | `cornerstone.command.mute`、`cornerstone.command.unmute` |
| `/tempmute <玩家> <时长> [原因]` | 临时禁言。 | `cornerstone.command.tempmute` |
| `/warn <玩家> [原因]` | 记录警告。 | `cornerstone.command.warn` |
| `/history <玩家>` | 查询处罚历史。 | `cornerstone.command.history` |
| `/vanish` | 切换自身隐身。 | `cornerstone.command.vanish` |
| `/freeze <玩家>` | 切换目标冻结状态。 | `cornerstone.command.freeze` |

临时处罚的时长格式为正整数加单位：`s`、`m`、`h`、`d`、`w`，例如 `30m`、`7d`。

### 运维与实用工具

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/maintenance` | 查看维护模式。 | `cornerstone.command.maintenance` |
| `/maintenance on\|off` | 开启或关闭维护模式。 | `cornerstone.command.maintenance` |
| `/maintenance allow <玩家>` | 切换玩家维护白名单。 | `cornerstone.command.maintenance` |
| `/restart <时长>`、`/shutdown <时长>` | 创建重启或停服倒计时。 | `cornerstone.command.restart`、`cornerstone.command.shutdown` |
| `/cancelshutdown` | 取消当前停服或重启计划。 | `cornerstone.command.cancelshutdown` |
| `/invsee <玩家>`、`/endersee <玩家>` | 打开目标背包或末影箱的只读视图。 | `cornerstone.command.invsee`、`cornerstone.command.endersee` |
| `/clear <玩家>` | 清空在线玩家背包。 | `cornerstone.command.clear` |
| `/heal <玩家>`、`/feed <玩家>` | 恢复在线玩家生命或饥饿值。 | `cornerstone.command.heal`、`cornerstone.command.feed` |
| `/gamemode <模式> <玩家>` | 设置在线玩家的游戏模式。 | `cornerstone.command.gamemode` |

`/restart` 与 `/shutdown` 只会正常停止服务器进程；自动拉起服务器需要由面板、脚本或进程管理器负责。

### 命令缩略名

在 `commands.aliases` 配置缩略名。默认配置让 `/gm` 等同于 `/gamemode`：

```json
{
  "commands": {
    "aliases": ["gamemode=gm"]
  }
}
```

缩略命令使用与原命令相同的权限和审计规则。

## 6. 经济功能

| 命令 | 说明 | LuckPerms 权限 |
| --- | --- | --- |
| `/balance` | 查看自己的余额。 | `cornerstone.command.balance` |
| `/pay <玩家> <金额>` | 向已知玩家转账；金额必须为正整数。 | `cornerstone.command.pay` |
| `/economy history <玩家>` | 查看玩家经济流水。 | `cornerstone.command.economy.history` |
| `/economy export` | 将流水导出为 CSV。 | `cornerstone.command.economy.export` |

初始余额通过 `economy.startingBalance` 设置。经济金额使用服务器定义的最小货币单位，Cornerstone 本身不定义货币显示格式。

## 7. 诊断、数据与排错

- `/cornerstone status`：检查核心服务是否已启动。
- `/cornerstone save`：立即保存已启用模块的数据。
- 持久化数据保存在世界目录的 `cornerstone` 子目录；不要在服务器运行时直接编辑 `.dat` 文件。
- 修改 JSON 后若命令没有变化，请确认已完全重启服务器，而不是只执行 `/reload`。
- 模块关闭后该模块的命令会消失，这是预期行为。若需使用 `/warp`，同时确认 `core`、`teleport`、`warps` 都为 `true`。
- 若普通玩家收到无权限提示，请检查其 LuckPerms 组是否被授予对应的 `cornerstone.command.*` 节点。
