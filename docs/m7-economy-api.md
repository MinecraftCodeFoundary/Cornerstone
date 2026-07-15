# M7 经济 API（0.7.0）

Cornerstone 只提供账户、余额与可追踪转账基础设施，不实现商店、拍卖行或货币玩法。余额使用 `long` 类型，单位由服务器定义为“最小货币单位”；不要使用浮点数存储或传递金额。

## 玩家命令

| 命令                          | LuckPerms 节点                          | 说明                                   |
| --------------------------- | ------------------------------------- | ------------------------------------ |
| `/balance`                  | `cornerstone.command.balance`         | 查看自己的余额。                             |
| `/pay <player> <amount>`    | `cornerstone.command.pay`             | 向在线或已有资料的玩家转账。                       |
| `/economy history <player>` | `cornerstone.command.economy.history` | 查询账户相关账本。                            |
| `/economy export`           | `cornerstone.command.economy.export`  | 导出 `cornerstone/economy-ledger.csv`。 |

`economy.startingBalance` 是新账户初次创建时使用的余额，默认 `0`。

## Java API

通过 `CornerstoneApi.economy()` 获取 `EconomyService`。核心调用为：

```java
UUID id = UUID.randomUUID();
EconomyTransferResult result = CornerstoneApi.economy()
        .transfer(id, sourceAccount, targetAccount, 250L, "quest reward transfer");
```

事务 ID 是幂等键。网络重试必须复用同一个 ID：如果源账户、目标账户和金额一致，服务返回原始结果且不再次移动余额；若相同 ID 携带不同参数则失败。转账成功后才发布两个 `BalanceChangedEvent`（借方和贷方各一个）。

经济数据保存在 `cornerstone/economy.dat`，使用版本化校验、原子替换和备份恢复。每笔完成或失败的有效转账都会进入账本；落盘失败时会恢复转账前的内存余额与账本状态。外部 Mod 只能使用服务 API、事件与查询接口，不应读写经济存档文件。
