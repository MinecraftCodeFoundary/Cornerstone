# M3 地标与随机传送（0.3.0）

M3 在 M2 的 `cornerstone/teleports.dat` 中新增地标数据。已有 M2 存档会从格式 v1 自动迁移到 v2；不保存正在等待的传送请求或倒计时任务。

## 地标

| 命令                                            | 基础 LuckPerms 节点               | 说明                   |
| --------------------------------------------- | ----------------------------- | -------------------- |
| `/warp <name>`                                | `cornerstone.command.warp`    | 前往地标。                |
| `/setwarp <name> [public\|permission\|admin]` | `cornerstone.command.setwarp` | 创建或更新地标，默认 `public`。 |
| `/delwarp <name>`                             | `cornerstone.command.delwarp` | 删除地标。                |

所有地标名均为 1–32 个英文字母、数字、`_` 或 `-`，并保存为小写。访问策略如下：

- `public`：拥有基础 `/warp` 节点的玩家可使用。
- `permission`：还需要 `cornerstone.warp.<name>`。
- `admin`：还需要 `cornerstone.warp.admin.<name>`。

`teleport.warpCosts` 可为每个地标设置抽象价格，例如 `spawn|100`。价格放入 `TeleportContext.configuredCost()`；M2 的免费费用实现会忽略它，经济模块安装 `TeleportCostProvider` 后可据此执行独立扣费。

## 随机传送

`/rtp` 需要 `cornerstone.command.rtp`。维度规则使用 `teleport.rtp.dimensionRules`，格式为：

```json
{
  "teleport": {
    "rtp": {
      "dimensionRules": [
        "minecraft:overworld|1000|150|32|600",
        "examplemod:skylands|2000|200|48|1200"
      ]
    }
  }
}
```

五项依次为 `维度 ID|最大半径|距世界出生点最小距离|最大尝试次数|独立冷却 ticks`。没有规则的维度会明确拒绝 RTP。`teleport.rtp.blockedBiomes` 可列出禁止落点的生物群系资源 ID。

每次尝试先检查世界边界，加载候选区块并根据地表高度选择位置，再复用 M2 的碰撞、地面、危险方块、维度黑名单和保护限制检查。尝试次数耗尽时不会传送玩家，并告知实际的尝试上限。

## 保护集成

领地或保护 Mod 可调用 `CornerstoneApi.teleports().addLandingValidator(...)` 注册 `TeleportRestriction`。返回拒绝原因即可让固定地标和 RTP 都跳过/拒绝受保护的落点。费用提供者可重写接收 `TeleportContext` 的默认方法，从 `purpose`（例如 `warp:spawn`）和 `configuredCost` 判断收费规则。
