# Sophisticated Matcher

用于 Sophisticated Storage / Sophisticated Backpacks 的单条 NBT 或组件匹配物品。

## 功能

- 使用 `nbt_matcher` 后打开独立 GUI。
- 左上槽位是幽灵槽：放入物品只复制 1 个，不消耗玩家物品；槽位不可取出，只能替换。
- 1.21.1 分支显示物品顶层 `DataComponentMap` 条目；Forge 1.20.1 分支显示物品顶层 NBT 键。
- 点击条目选中，再点击保存按钮，将键和值写入匹配器。
- 匹配器只比较保存的组件/NBT 值，不比较物品 ID。未安装 Sophisticated Core 时，匹配器本身仍可打开 GUI，但不会注入 Sophisticated 的筛选逻辑。

当前实现只处理最外层条目，不递归展开嵌套 compound。列表支持滚轮滚动。

## 复合组件匹配器

- 第二个物品，在 176x166 的标准容器 GUI 中最多存放九条匹配规则。
- 存放槽是幽灵槽，和单个匹配器的预览槽一样：放入配置好的匹配器只会读取它保存的组件规则，匹配器本身不会被消耗，也不会被留在槽里。
- 光标为空时点击已占用的槽会清除该条规则；在背包中按住 Shift 点击配置好的匹配器可以把规则加入空槽。
- 除第一个已占用槽外，每个已占用槽下方都有一个三段式开关：点击上、中、下三段分别设为并且（AND）、但不（BUT）、或者（OR），悬停时会显示该连接方式的名称和说明。
- 第一个已占用槽的连接状态会被忽略，因为由它决定初始结果，所以它没有开关。
- 结果按槽位从左到右折叠：第一个已占用槽决定初始结果，之后每条规则按自己的连接状态合并（AND 保留两者，BUT 保留前者但不保留后者，OR 满足其一即可）。
- 物品提示会列出已存放的组件规则和它们的连接方式。

## 合成

- 组件匹配器：命名牌 + 金锭（无序合成）
- 复合组件匹配器：命名牌 + 钻石（无序合成）

两个匹配器物品也会出现在 Sophisticated Matcher 自己的创造模式标签页中。

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
