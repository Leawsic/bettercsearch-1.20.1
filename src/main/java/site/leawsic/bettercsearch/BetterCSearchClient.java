package site.leawsic.bettercsearch;

import cn.breezeth.ordertocook.item.OrderItem;
import cn.breezeth.ordertocook.item.TakeoutBagItem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import site.leawsic.bettercsearch.util.OrderSearchHelper;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class BetterCSearchClient implements ClientModInitializer {
    private boolean wasSneaking = false;
    private BlockPos testBeamPos = null;

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

        // 测试指令：/beamtest <x> <z>  设置测试光束位置（不持有外卖包时也可看到）
        registerBeamCommand();

        // 光束渲染：外卖配送指引 + 测试光束
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            BlockPos beamTarget = getBlockPos(client);

            if (beamTarget == null) return;

            int x = beamTarget.getX();
            int z = beamTarget.getZ();
            Vec3d cameraPos = context.camera().getPos();
            int worldHeight = client.world.getHeight() + 64;
            Box beamBox = new Box(
                x + 0.25 - cameraPos.getX(), -64 - cameraPos.getY(), z + 0.25 - cameraPos.getZ(),
                x + 0.75 - cameraPos.getX(), worldHeight - cameraPos.getY(), z + 0.75 - cameraPos.getZ()
            );

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

    private BlockPos getBlockPos(MinecraftClient client) {
        BlockPos beamTarget = null;

        // 优先检查外卖包
        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isEmpty() && stack.getItem() instanceof TakeoutBagItem) {
            NbtCompound tag = stack.getNbt();
            if (tag != null && tag.contains("delivery_pos", NbtCompound.COMPOUND_TYPE)) {
                NbtCompound posTag = tag.getCompound("delivery_pos");
                beamTarget = new BlockPos(posTag.getInt("x"), 0, posTag.getInt("z"));
            }
        }

        // 其次检查测试坐标
        if (beamTarget == null) {
            beamTarget = testBeamPos;
        }
        return beamTarget;
    }

    private void registerBeamCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("beamtest")
            .then(argument("x", IntegerArgumentType.integer())
            .then(argument("z", IntegerArgumentType.integer())
            .executes(ctx -> {
                int x = IntegerArgumentType.getInteger(ctx, "x");
                int z = IntegerArgumentType.getInteger(ctx, "z");
                testBeamPos = new BlockPos(x, 0, z);
                ctx.getSource().sendFeedback(Text.literal("Beam set to (" + x + ", " + z + ")"));
                return 1;
            })))
            .then(literal("clear")
            .executes(ctx -> {
                testBeamPos = null;
                ctx.getSource().sendFeedback(Text.literal("Beam cleared"));
                return 1;
            }))
        ));
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
