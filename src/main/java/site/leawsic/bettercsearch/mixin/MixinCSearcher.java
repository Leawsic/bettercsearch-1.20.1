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

import java.util.ArrayList;

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
                boolean isTravelersBackpack = inter.getBlockState().getBlock() instanceof TravelersBackpackBlock;
                if (inter.getBlockState().getBlock() instanceof BlockWithEntity || isTravelersBackpack) {
                    if (!(handler instanceof AbstractRecipeScreenHandler)) {
                        ArrayList<ItemStack> list = new ArrayList<>();
                        int fullCount = 0;
                        int slotCount = 0;
                        for (Slot slot : handler.slots) {
                            if (!(slot.inventory instanceof PlayerInventory)) {
                                slotCount++;
                                if (slot.hasStack()) {
                                    ItemStack stack = slot.getStack();
                                    list.add(stack);
                                    if (stack.getCount() >= stack.getMaxCount()) {
                                        fullCount++;
                                    }
                                }
                            }
                        }
                        ((CSearcher) (Object) this).updateCachedContainer(
                                inter.getWorld(),
                                inter.getPos(),
                                ((CSearcher) (Object) this).getCache().current(),
                                list,
                                fullCount >= slotCount
                        );
                    }
                }
            }
        }
    }
}