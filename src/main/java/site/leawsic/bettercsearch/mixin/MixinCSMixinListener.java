package site.leawsic.bettercsearch.mixin;

import com.tiviacz.travelersbackpack.blocks.TravelersBackpackBlock;
import fr.loxoz.csearcher.CSearcher;
import fr.loxoz.csearcher.core.CSMixinListener;
import fr.loxoz.csearcher.core.InteractionHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.client.MinecraftClient;
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

@Mixin(CSMixinListener.class)
public class MixinCSMixinListener {

    @Shadow(remap = false) @Final private CSearcher a;

    @Inject(method = "onBlockInteract", at = @At("HEAD"), remap = false)
    private void onBlockInteract(ClientPlayerEntity player, BlockHitResult hit, ActionResult result, CallbackInfo ci) {
        Block block = player.getWorld().getBlockState(hit.getBlockPos()).getBlock();
        boolean isValidContainer = (result == ActionResult.SUCCESS && block instanceof BlockWithEntity) || block instanceof TravelersBackpackBlock;
        if (isValidContainer) {
            this.a.lastInteract = new InteractionHolder(player.getWorld(), hit.getBlockPos());

            MinecraftClient.getInstance().execute(() -> {
                ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
                if (handler != null) {
                    this.a.handleContainerScreenUpdate(handler, this.a.lastInteract, false);
                }
            });
        }
    }
}