package site.leawsic.bettercsearch.util;

import cn.breezeth.ordertocook.block.BoardBlock;
import cn.breezeth.ordertocook.item.OrderItem;
import fr.loxoz.csearcher.CSearcher;
import fr.loxoz.csearcher.compat.CText;
import fr.loxoz.csearcher.core.CacheEntry;
import fr.loxoz.csearcher.core.CachedStack;
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
            if (csearcher.getCache().isCurrentBlacklisted()) {
                Utils.postMessage(CText.translatable("text.csearcher.blacklisted_entry"));
            } else {
                Utils.postMessage(CText.translatable("text.csearcher.no_entry"));
            }
            return;
        }

        // 搜索每种食物物品，按容器汇总匹配的物品
        // 直接手动遍历缓存，用 Item 实例比较（无视改名、NBT 等一切差异）
        LinkedHashMap<Container, List<CachedStack>> containerMatches = new LinkedHashMap<>();

        for (String key : foodList.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;

            Item targetItem = Registries.ITEM.get(id);
            if (targetItem == Items.AIR) continue;

            int needed = foodList.getInt(key);
            if (needed <= 0) continue;

            for (Container container : cache.containers.values()) {
                if (!container.isIn(client.world)) continue;
                // 跳过菜单立牌（BoardBlock），这些不是储存食物的容器
                if (container.getBlock() instanceof BoardBlock) continue;

                for (CachedStack cached : container.getFlattenContent()) {
                    // 直接用 Item 实例比较，彻底无视改名、附魔、damage 等 NBT 差异
                    if (cached.asItem() == targetItem) {
                        containerMatches.computeIfAbsent(container, k -> new ArrayList<>()).add(cached);
                    }
                }
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

        // 1. 聚焦第一个容器中所有匹配的订单物品
        containerMatches.entrySet().iterator().forEachRemaining(containerListEntry -> containerListEntry.getValue().forEach(visuals::focusStack));

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
}
