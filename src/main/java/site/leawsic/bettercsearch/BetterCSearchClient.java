package site.leawsic.bettercsearch;

import cn.breezeth.ordertocook.item.OrderItem;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import site.leawsic.bettercsearch.util.OrderSearchHelper;

import java.util.List;

public class BetterCSearchClient implements ClientModInitializer {
    private boolean wasSneaking = false;

    @Override
    public void onInitializeClient() {
        // 订单搜索：手持 OrderItem 下蹲触发
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasSneaking = false;
                return;
            }

            boolean isSneaking = client.player.isSneaking();

            if (isSneaking && !wasSneaking) {
                ItemStack mainHand = client.player.getMainHandStack();
                if (!mainHand.isEmpty() && mainHand.getItem() instanceof OrderItem) {
                    OrderSearchHelper.searchForOrder(client);
                }
            }

            wasSneaking = isSneaking;
        });

        // 订单物品提示：背包已满足的物品用绿色标记
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (!(stack.getItem() instanceof OrderItem)) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            NbtCompound tag = stack.getNbt();
            if (tag == null || !tag.contains("FoodList", NbtCompound.COMPOUND_TYPE)) return;
            NbtCompound foodList = tag.getCompound("FoodList");
            if (foodList.isEmpty()) return;

            for (String key : foodList.getKeys()) {
                Identifier id = Identifier.tryParse(key);
                if (id == null) continue;

                Item item = Registries.ITEM.get(id);
                if (item == Items.AIR) continue;

                int required = foodList.getInt(key);
                if (required <= 0) continue;

                if (countItem(client.player.getInventory(), item) >= required) {
                    recolorLine(lines, item.getName().getString());
                }
            }
        });

        // 外卖配送指引：手持 TakeoutBagItem 时渲染黄色光束指向顾客位置
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            ItemStack stack = client.player.getMainHandStack();
            if (stack.isEmpty() || !(stack.getItem() instanceof TakeoutBagItem)) return;

            NbtCompound tag = stack.getNbt();
            if (tag == null || !tag.contains("delivery_pos", NbtCompound.COMPOUND_TYPE)) return;

            NbtCompound posTag = tag.getCompound("delivery_pos");
            int x = posTag.getInt("x");
            int z = posTag.getInt("z");

            // 从地底延伸到高空的金色光束
            Box beamBox = new Box(x + 0.25, -64, z + 0.25, x + 0.75, client.world.getHeight() + 64, z + 0.75);

            // 设置渲染状态（与 CSearcher BlockBlinker 一致）
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            context.matrixStack().push();
            WorldRenderer.drawBox(
                context.matrixStack(),
                context.consumers().getBuffer(RenderLayer.getLines()),
                beamBox,
                1.0f, 0.9f, 0.0f, 0.8f
            );
            context.matrixStack().pop();

            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        });
    }

    private static int countItem(PlayerInventory inv, Item item) {
        int count = 0;
        for (ItemStack s : inv.main) { if (s.getItem() == item) count += s.getCount(); }
        for (ItemStack s : inv.armor) { if (s.getItem() == item) count += s.getCount(); }
        if (inv.offHand.get(0).getItem() == item) count += inv.offHand.get(0).getCount();
        return count;
    }

    private static void recolorLine(List<Text> lines, String target) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getString().contains(target)) {
                lines.set(i, Text.literal(lines.get(i).getString()).formatted(Formatting.GREEN));
                return;
            }
        }
    }
}
