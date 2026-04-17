package site.leawsic.bettercsearch.mixin;

import com.tiviacz.travelersbackpack.blocks.TravelersBackpackBlock;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackBlockEntityMenu;
import com.tiviacz.travelersbackpack.inventory.menu.BackpackSettingsMenu;
import fr.loxoz.csearcher.CSearcher;
import fr.loxoz.csearcher.core.InteractionHolder;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.leawsic.bettercsearch.BetterCSearch;
import site.leawsic.bettercsearch.util.TravelersBackpackHelper;

import java.util.ArrayList;
import java.util.List;

@Mixin(CSearcher.class)
public class MixinCSearcher {

    @Inject(method = "isHandlerIgnored", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsHandlerIgnored(ScreenHandler handler, CallbackInfoReturnable<Boolean> cir) {
        String className = handler.getClass().getName();
        if (className.equals(BackpackBlockEntityMenu.class.getName()) || className.equals(BackpackSettingsMenu.class.getName())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isValidContainer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsValidContainer(net.minecraft.block.Block block, CallbackInfoReturnable<Boolean> cir) {
        if (block instanceof TravelersBackpackBlock) {
            cir.setReturnValue(true);
        }
    }

    /**
     * @reason Allow Traveler's Backpack blocks to be recognized
     * @author Leawsic
     */
    @Overwrite(remap = false)
    public void handleContainerScreenUpdate(ScreenHandler handler, InteractionHolder inter, boolean wasOpen) {
        if (inter != null) {
            if (wasOpen || System.currentTimeMillis() - inter.getTime() <= CSearcher.INTERACTION_MAX_TIME) {
                boolean isTravelersBackpackBlock = inter.getBlockState().getBlock() instanceof TravelersBackpackBlock;
                if (inter.getBlockState().getBlock() instanceof BlockWithEntity || isTravelersBackpackBlock) {
                    if (!(handler instanceof AbstractRecipeScreenHandler)) {
                        ArrayList<ItemStack> list = new ArrayList<>();
                        int fullCount = 0;
                        int slotCount = 0;
                        for (Slot slot : handler.slots) {
                            if (!(slot.inventory instanceof PlayerInventory)) {
                                slotCount++;
                                if (slot.hasStack()) {
                                    ItemStack stack = slot.getStack();
                                    // 检查是否为旅行者背包物品
                                    if (TravelersBackpackHelper.isTravelersBackpack(stack)) {
                                        // 展开背包主储存中的物品
                                        BetterCSearch.LOGGER.error("Trying to expand backpack");
                                        List<ItemStack> innerStacks = TravelersBackpackHelper.getMainStorageItems(stack);
                                        if (!innerStacks.isEmpty()) {
                                            BetterCSearch.LOGGER.info("Expanded backpack with {} inner items", innerStacks.size());
                                            list.addAll(innerStacks);
                                        }
                                    } else {
                                        list.add(stack);
                                        // 普通物品的满槽位计数
                                        if (stack.getCount() >= stack.getMaxCount()) {
                                            fullCount++;
                                        }
                                    }
                                }
                            }
                        }
                        ((CSearcher)(Object)this).updateCachedContainer(
                                inter.getWorld(),
                                inter.getPos(),
                                ((CSearcher)(Object)this).getCache().current(),
                                list,
                                fullCount >= slotCount
                        );
                    }
                }
            }
        }
    }
}