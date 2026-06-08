package site.leawsic.bettercsearch;

import cn.breezeth.ordertocook.item.OrderItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import site.leawsic.bettercsearch.util.OrderSearchHelper;

public class BetterCSearchClient implements ClientModInitializer {
    private boolean wasSneaking = false;

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasSneaking = false;
                return;
            }

            boolean isSneaking = client.player.isSneaking();

            // Rising-edge 检测：玩家刚按下潜行键时触发
            if (isSneaking && !wasSneaking) {
                ItemStack mainHand = client.player.getMainHandStack();
                if (!mainHand.isEmpty() && mainHand.getItem() instanceof OrderItem) {
                    OrderSearchHelper.searchForOrder(client);
                }
            }

            wasSneaking = isSneaking;
        });
    }
}
