package site.leawsic.bettercsearch.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, net.minecraft.server.command.CommandManager.RegistrationEnvironment environment) {
        // register test commands
        dispatcher.register(literal("bettercsearch")
            .then(literal("test_order")
                .requires(src -> src.hasPermissionLevel(2))
                .then(argument("item", StringArgumentType.word())
                .then(argument("count", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    String itemId = StringArgumentType.getString(ctx, "item");
                    int count = IntegerArgumentType.getInteger(ctx, "count");
                    return giveTestOrder(ctx.getSource(), itemId, count);
                }))))
            .then(literal("test_delivery")
                .requires(src -> src.hasPermissionLevel(2))
                .then(argument("x", IntegerArgumentType.integer())
                .then(argument("z", IntegerArgumentType.integer())
                .executes(ctx -> {
                    int x = IntegerArgumentType.getInteger(ctx, "x");
                    int z = IntegerArgumentType.getInteger(ctx, "z");
                    return giveTestDelivery(ctx.getSource(), x, z);
                }))))
        );
    }

    private static int giveTestOrder(ServerCommandSource src, String itemId, int count) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            src.sendError(Text.literal("Invalid item id: " + itemId));
            return 0;
        }
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR) {
            src.sendError(Text.literal("Unknown item: " + itemId));
            return 0;
        }

        ItemStack order = new ItemStack(Registries.ITEM.get(new Identifier("ordertocook", "order_item")));
        NbtCompound tag = order.getOrCreateNbt();
        NbtCompound foodList = new NbtCompound();
        foodList.putInt(itemId, count);
        tag.put("FoodList", foodList);
        tag.putInt("Prestige", 5);

        if (src.getPlayer() != null) {
            src.getPlayer().giveItemStack(order);
            src.sendFeedback(() -> Text.literal("Created test order: " + count + "x " + itemId), false);
        }
        return 1;
    }

    private static int giveTestDelivery(ServerCommandSource src, int x, int z) {
        ItemStack bag = new ItemStack(Registries.ITEM.get(new Identifier("ordertocook", "takeout_bag_item")));
        NbtCompound tag = bag.getOrCreateNbt();
        tag.putBoolean("Delivery", true);

        NbtCompound posTag = new NbtCompound();
        posTag.putInt("x", x);
        posTag.putInt("z", z);
        tag.put("delivery_pos", posTag);

        NbtCompound foodList = new NbtCompound();
        foodList.putInt("minecraft:cooked_beef", 3);
        tag.put("FoodList", foodList);
        tag.putLong("ExpiryTime", System.currentTimeMillis() + 600000);
        tag.putString("CustomerName", "TestCustomer");

        if (src.getPlayer() != null) {
            src.getPlayer().giveItemStack(bag);
            src.sendFeedback(() -> Text.literal("Created test delivery at (" + x + ", " + z + ")"), false);
        }
        return 1;
    }
}
