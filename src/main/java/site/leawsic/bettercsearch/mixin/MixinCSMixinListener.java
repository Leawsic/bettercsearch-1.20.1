package site.leawsic.bettercsearch.mixin;

import com.tiviacz.travelersbackpack.blocks.TravelersBackpackBlock;
import fr.loxoz.csearcher.core.CSMixinListener;
import fr.loxoz.csearcher.core.InteractionHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.leawsic.bettercsearch.BetterCSearch;

@Mixin(CSMixinListener.class)
public class MixinCSMixinListener {

    @Shadow(remap = false) @Final private fr.loxoz.csearcher.CSearcher a; // CSearcher 实例

    @Inject(method = "onBlockInteract", at = @At("HEAD"), cancellable = true, remap = false)
    private void onBlockInteract(ClientPlayerEntity player, BlockHitResult hit, ActionResult result, CallbackInfo ci) {
        // 去掉 BlockWithEntity 检查，允许任何成功或消耗的交互设置 lastInteract
        Block block=player.getWorld().getBlockState(hit.getBlockPos()).getBlock();
        if (result == ActionResult.SUCCESS && block instanceof BlockWithEntity || block instanceof TravelersBackpackBlock) {
            BetterCSearch.LOGGER.info("Setting lastInteract for block at " + hit.getBlockPos());
            this.a.lastInteract = new InteractionHolder(player.getWorld(), hit.getBlockPos());
        }
    }
}