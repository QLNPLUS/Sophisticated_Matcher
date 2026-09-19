# 版本矩阵

已完成并通过构建的分支：

- `master`: NeoForge 1.21.1，使用 Data Components。
- `forge-1.20.1`: Forge 1.20.1，使用旧版顶层 ItemStack NBT；worktree 位于 `D:\projects\Sophisticated_Matcher-forge-1.20.1`。

用户请求的其他版本需要各自的 NeoForge/Forge 映射和 Sophisticated Core 接口验证，目前没有把未验证的源码伪装成可发布分支：

- NeoForge：26.2、26.1.2、26.1.1、26.1、1.21.11、1.21.10、1.21.8、1.21.5、1.21.4。
- Forge：1.19.2、1.19.1、1.19、1.18.2。

高版本之间的 GUI 和组件匹配逻辑可复用，但需要按各版本 Sophisticated Core 的 `FilterLogic` 签名、NeoForge 注册 API 和资源包格式分别编译确认；旧 Forge 分支还需要沿用 1.20.1 的 NBT 路径并调整菜单/渲染映射。
