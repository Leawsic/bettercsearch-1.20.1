package site.leawsic.bettercsearch.util;

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
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        // 搜索每种食物物品，收集匹配的容器
        Set<BlockPos> matchedPositions = new LinkedHashSet<>();
        Container firstContainer = null;
        CachedStack firstCachedStack = null;

        for (String key : foodList.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;

            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) continue;

            int needed = foodList.getInt(key);
            if (needed <= 0) continue;

            // 创建搜索用的 CachedStack
            ItemStack sampleStack = new ItemStack(item, Math.min(needed, item.getMaxCount()));
            CachedStack searchStack = CachedStack.of(sampleStack);

            // 在附近容器中搜索（不跳过满容器）
            List<ContainedStack> results = csearcher.searchNearStack(client, searchStack, cache, false);
            if (results == null || results.isEmpty()) continue;

            for (ContainedStack cs : results) {
                Container container = cs.container();
                if (container == null) continue;

                matchedPositions.add(container.getPos());

                // 记录第一件匹配物品的信息用于视角转向
                if (firstContainer == null) {
                    firstContainer = container;
                    firstCachedStack = searchStack;
                }
            }
        }

        // 无匹配结果
        if (matchedPositions.isEmpty()) {
            Utils.postMessage(CText.translatable("text.csearcher.no_near_matches"));
            return;
        }

        // --- 视觉反馈 ---

        VisualsManager visuals = csearcher.getVisualsManager();

        // 1. 处理第一个匹配（高亮 + 转向 + 粒子线 + 物品焦点）
        //    targetContainerAndStack 内部会调用 blinkBlock + focusStack + lookAt + drawSparksLine
        csearcher.targetContainerAndStack(client, firstCachedStack, firstContainer);

        // 2. 高亮其余所有匹配容器（白色方框闪烁）
        for (BlockPos pos : matchedPositions) {
            if (pos.equals(firstContainer.getPos())) continue;
            visuals.blinkBlock(pos);
        }
    }
}
