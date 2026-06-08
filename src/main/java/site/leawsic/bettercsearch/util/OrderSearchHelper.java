package site.leawsic.bettercsearch.util;

import cn.breezeth.ordertocook.block.BoardBlock;
import cn.breezeth.ordertocook.item.OrderItem;
import fr.loxoz.csearcher.CSearcher;
import fr.loxoz.csearcher.compat.CText;
import fr.loxoz.csearcher.core.CacheEntry;
import fr.loxoz.csearcher.core.CachedStack;
import fr.loxoz.csearcher.core.ContainedStack;
import fr.loxoz.csearcher.core.Container;
import fr.loxoz.csearcher.core.VisualsManager;
import fr.loxoz.csearcher.util.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理 OrderToCook 订单物品的搜索逻辑。
 * 当玩家手持 OrderItem 并下蹲时，解析订单需要的食物物品，
 * 利用 CSearcher 的缓存搜索附近容器并高亮结果。
 */
public class OrderSearchHelper {

    /**
     * 根据玩家手持的 OrderItem 搜索附近容器中匹配的订单物品。
     * <p>
     * 流程：
     * 1. 从 OrderItem NBT 读取 FoodList（物品ID → 需要数量）
     * 2. 对每种物品在 CSearcher 缓存中搜索附近容器
     * 3. 收集所有匹配的容器坐标（去重）
     * 4. 用白色方框高亮所有匹配容器，视角转向第一件物品所在容器
     *
     * @param client Minecraft 客户端实例
     */
    public static void searchForOrder(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof OrderItem)) {
            return;
        }

        // 读取 OrderItem 的 NBT FoodList
        NbtCompound tag = mainHand.getNbt();
        if (tag == null || !tag.contains("FoodList", NbtCompound.COMPOUND_TYPE)) {
            Utils.postMessage(CText.translatable("text.csearcher.no_near_matches"));
            return;
        }

        NbtCompound foodList = tag.getCompound("FoodList");
        if (foodList.isEmpty()) {
            Utils.postMessage(CText.translatable("text.csearcher.no_near_matches"));
            return;
        }

        // 获取 CSearcher 实例和缓存
        CSearcher csearcher = CSearcher.inst();
        CacheEntry cache = csearcher.getCache().current();
        if (cache == null) {
            // 触发 CSearcher 的缓存缺失提示
            if (csearcher.getCache().isCurrentBlacklisted()) {
                Utils.postMessage(CText.translatable("text.csearcher.blacklisted_entry"));
            } else {
                Utils.postMessage(CText.translatable("text.csearcher.no_entry"));
            }
            return;
        }

        // 搜索每种食物物品，按容器汇总匹配的物品
        LinkedHashMap<Container, List<CachedStack>> containerMatches = new LinkedHashMap<>();

        for (String key : foodList.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;

            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) continue;

            int needed = foodList.getInt(key);
            if (needed <= 0) continue;

            // 创建搜索用的 CachedStack（仅物品类型 + 数量，不含显示名等 NBT）
            CachedStack searchStack = CachedStack.of(new ItemStack(item, Math.min(needed, item.getMaxCount())));

            // 先用 CSearcher 的严格模式搜索（匹配显示名、附魔等）
            List<ContainedStack> results = csearcher.searchNearStack(client, searchStack, cache, false);
            // 若严格模式未找到，用宽松模式再搜一次（忽略改名等 NBT 差异，仅比物品类型）
            if (results == null || results.isEmpty()) {
                results = searchNearStackNonStrict(csearcher, client, searchStack, cache);
            }
            if (results == null || results.isEmpty()) continue;

            for (ContainedStack cs : results) {
                Container container = cs.container();
                if (container == null) continue;

                // 跳过菜单立牌（BoardBlock），这些不是储存食物的容器
                if (container.getBlock() instanceof BoardBlock) continue;

                // 按容器汇总：该容器匹配的食物物品
                containerMatches.computeIfAbsent(container, k -> new ArrayList<>()).add(searchStack);
            }
        }

        // 无匹配结果
        if (containerMatches.isEmpty()) {
            Utils.postMessage(CText.translatable("text.csearcher.no_near_matches"));
            return;
        }

        // --- 视觉反馈 ---

        VisualsManager visuals = csearcher.getVisualsManager();

        // 取第一个容器（保存插入顺序）
        Map.Entry<Container, List<CachedStack>> firstEntry = containerMatches.entrySet().iterator().next();
        Container firstContainer = firstEntry.getKey();
        List<CachedStack> firstStacks = firstEntry.getValue();

        // 1. 聚焦第一个容器中所有匹配的订单物品
        for (CachedStack stack : firstStacks) {
            visuals.focusStack(stack);
        }

        // 2. 对第一个容器：白色方框 + 视角转向 + 粒子线
        Vec3d targetVec = Vec3d.ofCenter(firstContainer.getPos());
        visuals.blinkBlock(firstContainer.getPos());
        if (CSearcher.getConfig().showDirectionLine) {
            visuals.drawSparksLine(client.player.getCameraPosVec(0), targetVec, client.world);
        }
        if (CSearcher.getConfig().lookAtTarget) {
            Utils.lookAt(client.player, targetVec);
        }

        // 3. 高亮其余所有匹配容器（白色方框闪烁）
        for (Container container : containerMatches.keySet()) {
            if (container == firstContainer) continue;
            visuals.blinkBlock(container.getPos());
        }
    }

    /**
     * 以非严格模式（只比物品类型，忽略显示名、附魔等 NBT）在缓存中搜索匹配的容器。
     * 作为 searchNearStack 的 fallback，用于处理物品被改名后搜不到的问题。
     */
    private static List<ContainedStack> searchNearStackNonStrict(CSearcher csearcher, MinecraftClient client, CachedStack searchStack, CacheEntry cache) {
        if (client.player == null || client.world == null) return null;
        if (cache == null) return null;

        List<ContainedStack> results = new ArrayList<>();
        int maxDistBlocks = CSearcher.getConfig().searchNearDistance.getBlocks();

        for (Container container : cache.containers.values()) {
            if (!container.isIn(client.world)) continue;
            // 距离过滤
            if (maxDistBlocks > 0 && container.distanceTo(client.player.getPos()) > maxDistBlocks) continue;

            for (CachedStack cached : container.getFlattenContent()) {
                // 非严格匹配：仅比物品类型，忽略改名、附魔等 NBT 差异
                if (cached.isSameAs(searchStack, false)) {
                    results.add(new ContainedStack(container, cached));
                }
            }
        }

        return results;
    }
}
