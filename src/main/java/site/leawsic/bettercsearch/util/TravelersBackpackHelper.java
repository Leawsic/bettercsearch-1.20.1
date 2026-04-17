package site.leawsic.bettercsearch.util;

import com.tiviacz.travelersbackpack.items.TravelersBackpackItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import site.leawsic.bettercsearch.BetterCSearch;

import java.util.ArrayList;
import java.util.List;

public class TravelersBackpackHelper {

    public static boolean isTravelersBackpack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TravelersBackpackItem;
    }

    public static List<ItemStack> getMainStorageItems(ItemStack stack) {
        List<ItemStack> result = new ArrayList<>();
        if (!isTravelersBackpack(stack)) return result;

        NbtCompound tag = stack.getNbt();
        if (tag == null) return result;

        //todo 暂时还不支持（像查找一般item一样）容器内高亮目标背包
        if (tag.contains("Inventory", NbtCompound.COMPOUND_TYPE)) {
            NbtCompound invTag = tag.getCompound("Inventory");
            if (invTag.contains("Items", NbtCompound.LIST_TYPE)) {
                NbtList itemsList = invTag.getList("Items", NbtCompound.COMPOUND_TYPE);
                for (int i = 0; i < itemsList.size(); i++) {
                    NbtCompound itemTag = itemsList.getCompound(i);
                    // 检查 Count 是否大于 0
                    if (itemTag.getByte("Count") > 0) {
                        ItemStack inner = ItemStack.fromNbt(itemTag);
                        if (!inner.isEmpty()) {
                            result.add(inner);
                            BetterCSearch.LOGGER.error(inner.getName().getString());
                        }
                    }
                }
            }
        }
        return result;
    }
}