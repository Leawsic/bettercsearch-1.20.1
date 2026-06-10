package site.leawsic.bettercsearch.mixin;

import cn.breezeth.ordertocook.item.OrderItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(OrderItem.class)
public class MixinOrderItem {

    @Inject(method = "appendTooltip", at = @At("TAIL"), remap = false)
    private void onTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        // 仅在客户端处理（需要访问玩家背包）
        if (world == null || !world.isClient) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        NbtCompound tag = stack.getNbt();
        if (tag == null || !tag.contains("FoodList", NbtCompound.COMPOUND_TYPE)) return;
        NbtCompound foodList = tag.getCompound("FoodList");
        if (foodList.isEmpty()) return;

        // 遍历订单上的每种食物，检查背包中是否已满足数量
        for (String key : foodList.getKeys()) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) continue;

            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) continue;

            int required = foodList.getInt(key);
            if (required <= 0) continue;

            int count = countInInventory(mc.player.getInventory(), item);
            if (count >= required) {
                // 找到对应的 tooltip 条目并改为绿色
                recolorItemInTooltip(tooltip, item, Formatting.GREEN);
            }
        }
    }

    @Unique
    private static int countInInventory(PlayerInventory inv, Item item) {
        int count = 0;
        for (ItemStack stack : inv.main) {
            if (stack.getItem() == item) count += stack.getCount();
        }
        for (ItemStack stack : inv.armor) {
            if (stack.getItem() == item) count += stack.getCount();
        }
        if (inv.offHand.get(0).getItem() == item) {
            count += inv.offHand.get(0).getCount();
        }
        return count;
    }

    @Unique
    private static void recolorItemInTooltip(List<Text> tooltip, Item item, Formatting color) {
        String targetName = item.getName().getString();
        for (int i = 0; i < tooltip.size(); i++) {
            Text entry = tooltip.get(i);
            if (entry.getString().contains(targetName)) {
                MutableText wrapped = Text.literal("").formatted(color).append(entry);
                tooltip.set(i, wrapped);
                return;
            }
        }
    }
}
