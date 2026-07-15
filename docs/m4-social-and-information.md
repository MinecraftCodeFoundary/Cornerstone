# M4 社交与信息（0.4.0）

## 命令与 LuckPerms 节点

每个命令使用同名的 `cornerstone.command.*` 权限节点。

| 命令                        | 权限节点                         | 说明                |
| ------------------------- | ---------------------------- | ----------------- |
| `/msg <player> <message>` | `cornerstone.command.msg`    | 向在线玩家发送私信。        |
| `/reply <message>`        | `cornerstone.command.reply`  | 回复最后一位私信联系人。      |
| `/ignore <player>`        | `cornerstone.command.ignore` | 切换对在线玩家的屏蔽状态。     |
| `/afk [reason]`           | `cornerstone.command.afk`    | 切换 AFK；聊天或移动自动解除。 |
| `/seen <player>`          | `cornerstone.command.seen`   | 查看在线状态或最后在线时间。    |
| `/list`                   | `cornerstone.command.list`   | 查看在线玩家和 AFK 标记。   |
| `/rules`                  | `cornerstone.command.rules`  | 显示配置的服务器规则。       |

忽略关系存储在该玩家的 M1 资料中，因此重启后仍生效。私信会经过敏感词过滤；审计日志仅记录目标玩家和 `<redacted>`，不会记录正文。

## 配置

配置文件为 `<服务器目录>/config/cornerstone.json`。

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `social.motd` | `A Cornerstone server` | 服务器列表 MOTD。 |
| `social.joinLeaveMessages` | `true` | 开关 Cornerstone 的进出服提示。 |
| `social.welcomeMessages` | `Welcome, {player}!` | 玩家加入时接收的消息；支持 `{player}`。 |
| `social.rules` | `Be respectful.` | `/rules` 显示的多行规则。 |
| `social.announcements` | `[]` | 轮播公告内容；空列表关闭公告。 |
| `social.announcementIntervalTicks` | `12000` | 公告间隔。 |
| `social.filteredWords` | `[]` | 不区分大小写的敏感词；空列表关闭过滤。 |

## 扩展聊天格式

外部 Mod 可通过 `CornerstoneApi.social().installChatFormatter(...)` 临时安装 `ChatFormatter`。格式化器收到已过滤的原始文本并返回 `Component`；关闭返回的句柄会恢复先前格式。不要在格式化器中改写持久化资料或执行阻塞操作。
