# BetterCSearch

A Fabric mod that extends [ContainerSearcher](https://modrinth.com/mod/container-searcher) (by Loxoz) to support containers from other mods. / 一个 Fabric 模组，扩展 ContainerSearcher 以支持其他模组的容器。

Currently adds compatibility for Traveler's Backpack (by Tiviacz), OrderToCook (by breezeth), and other mods using the `BlockEntityProvider` interface. / 目前已适配 Traveler's Backpack（旅行者背包）、OrderToCook（下单了），以及任何使用 `BlockEntityProvider` 接口的模组容器。

- - -

## Features / 功能

### Universal Non-BlockWithEntity Container Support / 通用非 BlockWithEntity 容器支持

CSearcher normally only recognizes `BlockWithEntity` blocks as valid containers. This mod extends recognition to any block implementing `BlockEntityProvider`, making it compatible with blocks from mods like Cluttered (fridges, cabinets, etc.) and Traveler's Backpack without needing per-mod configuration.

CSearcher 默认只将 `BlockWithEntity` 方块视为有效容器。本模组将识别范围扩展到所有实现了 `BlockEntityProvider` 接口的方块，使得 Cluttered（冰箱、柜子等）、旅行者背包等模组的容器无需逐个配置即可被搜索。

### Traveler's Backpack Support / 旅行者背包支持

- Makes CSearcher recognize Traveler's Backpack blocks as valid containers / 让 CSearcher 识别旅行者背包方块为有效容器
- Expands backpack inner inventory items into search results, allowing you to find items stored inside backpacks through CSearcher's search / 展开背包内部物品到搜索结果中，使其可被 CSearcher 搜索到
- Supports both placed backpack blocks and backpack items within other containers / 支持已放置的背包方块和容器内的背包物品

### OrderToCook Order Searching / 订单物品搜索

When holding an OrderItem (order slip) and sneaking, triggers a search for the food items listed on the order:

当手持订单（OrderItem）并下蹲时，自动搜索订单上列出的食物物品：

- Parses the order's FoodList NBT data to determine required items / 解析订单 NBT 中的 FoodList 数据
- Items already satisfied (enough quantity in inventory) are skipped automatically / 背包中已满足数量的物品自动跳过，不再搜索
- Searches all cached containers by item type (ignores display names, enchantments -- renamed items are found correctly) / 按物品类型搜索缓存容器，无视改名、附魔等 NBT 差异
- Results are sorted by distance -- the nearest container is targeted first / 结果按距离排序，优先定位最近的容器
- All matching containers are highlighted with blinking white outlines / 所有匹配容器用白色方框闪烁高亮
- The nearest container is focused: the player's view turns toward it, a particle trail is drawn, and all matching items are highlighted in the inventory slot overlay / 最近容器的视角自动转向、粒子线指引、物品栏高亮
- If multiple items from the same order are in one container, all matching slots are highlighted / 同一个容器中有多种订单物品时，全部高亮
- BoardBlocks (menu boards) are excluded from search results to avoid false positives / 菜单立牌（BoardBlock）不会参与搜索
- Tooltip display: items you already have enough of in your inventory are shown in green / 工具提示中，背包已满足数量的物品显示为绿色

### Delivery Waypoint Beam / 外卖配送指引光束

When holding a TakeoutBagItem with delivery coordinates, a golden beam is rendered at the delivery location:

当手持含配送坐标的外卖袋（TakeoutBagItem）时，在配送位置渲染一束金色光束：

- Extends from below the world to above the world height -- visible from any distance / 从地底延伸到高空，任意距离可见
- Not occluded by blocks or entities (depth test disabled) / 不会被方块或实体遮挡（深度测试关闭）
- Drawn using raw Tessellator rendering for maximum visibility / 使用 Tessellator 直接渲染，穿透力最强
- A client-side command `/beamtest <x> <z>` is available for testing the beam without a real delivery item / 提供客户端指令 `/beamtest <x> <z>` 可独立测试光束效果

- - -

## Dependencies / 依赖

- **Minecraft**: 1.20.1
- **Fabric Loader**: >=0.17.3
- **Fabric API**: *
- **[ContainerSearcher](https://modrinth.com/mod/container-searcher)**: 0.2.1 (included in `libs/`)
- **Traveler's Backpack**: 9.1.41 (included in `libs/`)
- **OrderToCook**: 1.3.2 (included in `libs/`)

## Building / 构建

```bash
./gradlew build
```

The output JAR will be in `build/libs/`. / 输出 JAR 在 `build/libs/` 目录下。

## Client Commands / 客户端指令

| Command | Permission | Description / 说明 |
|---------|-----------|-------------|
| `/beamtest <x> <z>` | None (client) / 无需权限 | Render a test beam at world coordinates / 在世界坐标处渲染测试光束 |
| `/beamtest clear` | None (client) / 无需权限 | Clear the test beam / 清除测试光束 |

- - -

## Project Structure / 项目结构

```
src/main/java/site/leawsic/bettercsearch/
  BetterCSearch.java          -- Mod initializer / 模组主入口
  BetterCSearchClient.java    -- Client: tick events, tooltip callback, beam rendering / 客户端：Tick事件、工具提示、光束渲染
  mixin/
    MixinCSearcher.java       -- Core mixin: extends CSearcher's container detection / 扩展 CSearcher 容器检测
    MixinCSMixinListener.java -- Captures block interactions for non-BlockWithEntity blocks / 捕获非标准方块的交互事件
  util/
    TravelersBackpackHelper.java -- Reads backpack inner inventory from NBT / 从 NBT 读取背包内部物品
    OrderSearchHelper.java       -- OrderItem search logic and container matching / 订单搜索与容器匹配
```

## Mixin Details / Mixin 详情

**MixinCSearcher** (target / 目标: `CSearcher`):
- `isHandlerIgnored` -- Whitelists Backpack screen handlers / 白名单放行背包屏幕处理器
- `isValidContainer` -- Accepts both `BlockWithEntity` and `BlockEntityProvider` blocks as valid containers / 接受 BlockWithEntity 和 BlockEntityProvider 方块为有效容器
- `handleContainerScreenUpdate` -- Expands backpack items and accepts `BlockEntityProvider` blocks / 展开背包物品并接受 BlockEntityProvider 方块

**MixinCSMixinListener** (target / 目标: `CSMixinListener`):
- `onBlockInteract` -- Captures interactions with `BlockWithEntity`, Traveler's Backpack, and `BlockEntityProvider` blocks / 捕获与 BlockWithEntity、旅行者背包、BlockEntityProvider 方块的交互

- - -

## License / 许可证

CC0-1.0
