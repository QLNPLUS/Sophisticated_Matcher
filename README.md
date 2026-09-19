# Sophisticated Matcher

用于 Sophisticated Storage / Sophisticated Backpacks 的单条 NBT 或组件匹配物品。

## 功能

- 使用 `nbt_matcher` 后打开独立 GUI。
- 左上槽位是幽灵槽：放入物品只复制 1 个，不消耗玩家物品；槽位不可取出，只能替换。
- 1.21.1 分支显示物品顶层 `DataComponentMap` 条目；Forge 1.20.1 分支显示物品顶层 NBT 键。
- 点击条目选中，再点击保存按钮，将键和值写入匹配器。
- 匹配器只比较保存的组件/NBT 值，不比较物品 ID。未安装 Sophisticated Core 时，匹配器本身仍可打开 GUI，但不会注入 Sophisticated 的筛选逻辑。

当前实现只处理最外层条目，不递归展开嵌套 compound。列表支持滚轮滚动。

## 构建

NeoForge 1.21.1 使用 JDK 21：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
./gradlew.bat build --no-daemon
```

Forge 1.20.1 位于独立 worktree `D:\projects\Sophisticated_Matcher-forge-1.20.1`，使用 JDK 17。

构建产物：

- `build/libs/sophisticated_matcher-neoforge-1.21.1-0.1.0.jar`
- `D:\projects\Sophisticated_Matcher-forge-1.20.1\build\libs\sophisticated-matcher-forge-1.20.1-0.1.0.jar`

Forge 1.20.1 分支声明 Sophisticated Core 最低版本为 `1.3.6.1514`。
