package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import static com.cappleapple.animatedinventory.validation.ClientValidation.require;

/** Delays the opening content packet until empty native slots have actually rendered. */
final class InitialContentsValidation {
    private static int phase, delay = 30, round;
    private static ContainerScreen screen;
    private static long transactions;

    static void tick(Minecraft mc) {
        if (delay-- > 0) return;
        delay = 5;
        var runtime = ClientRuntime.INSTANCE;
        switch (phase++) {
            case 0 -> {
                ClientConfig.OPEN.set(ClientConfig.ScreenEffect.NONE);
                ClientConfig.CLOSE.set(ClientConfig.ScreenEffect.NONE);
                ClientConfig.APPEAR.set(ClientConfig.ItemEffect.POP);
                mc.player.getInventory().clearContent();
                var menu = ChestMenu.threeRows(81, mc.player.getInventory());
                screen = new ContainerScreen(menu, mc.player.getInventory(), Component.literal("Initial contents regression"));
                mc.player.containerMenu = menu;
                mc.setScreen(screen);
            }
            case 1 -> {
                transactions = runtime.transactions;
                contents(mc, 1, 16);
                runtime.afterInteraction();
                require(runtime.transactions == transactions, "opening contents do not create item transactions, round " + round);
                require(runtime.animations.active().isEmpty(), "opening contents do not pop in, round " + round);
                require(runtime.snapshot().items().get("slot:0").stack().getCount() == 16, "opening snapshot contains synchronized items");
                require(runtime.normalStack(screen, "slot:0", screen.getMenu().getSlot(0).getItem()).getCount() == 16,
                        "initial stack remains fully visible in native rendering");
                ClientValidation.captureSophisticated(screen, "initial-contents-" + round);
            }
            case 2 -> {
                // An unrelated menu's first content sync must not consume this screen's next change.
                screen.getMenu().getSlot(1).set(new ItemStack(Items.EMERALD, 3));
                var other = ChestMenu.threeRows(82, mc.player.getInventory());
                other.initializeContents(1, NonNullList.withSize(other.slots.size(), ItemStack.EMPTY), ItemStack.EMPTY);
                runtime.afterInteraction();
                require(runtime.transactions > transactions, "unrelated initial contents do not rebase the active menu");
                runtime.invalidate(screen, false);
                screen.resize(mc, screen.width, screen.height);
            }
            case 3 -> {
                transactions = runtime.transactions;
                contents(mc, 2, 24);
                runtime.afterInteraction();
                require(runtime.transactions > transactions, "later full content sync still animates after resize");
                require(runtime.animations.active().stream().anyMatch(a -> a.transition.type() == TransitionType.APPEAR),
                        "later new items retain appearance effects");
                runtime.invalidate(screen, false);
            }
            case 4 -> {
                var slot = screen.getMenu().getSlot(0);
                runtime.beforeInteraction(screen, slot, net.minecraft.world.inventory.ClickType.PICKUP);
                screen.getMenu().clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                runtime.afterInteraction();
                require(runtime.animations.active().stream().anyMatch(a -> "cursor".equals(a.transition.destinationId())),
                        "pickup still animates after initial synchronization");
                mc.player.closeContainer();
                if (++round < 2) phase = 0;
                else ClientValidation.finish(mc, null);
            }
        }
    }

    private static void contents(Minecraft mc, int state, int count) {
        var menu = screen.getMenu();
        var items = NonNullList.withSize(menu.slots.size(), ItemStack.EMPTY);
        items.set(0, new ItemStack(Items.DIAMOND, count));
        if (state > 1) items.set(2, new ItemStack(Items.GOLD_INGOT, 8));
        mc.getConnection().handleContainerContent(new ClientboundContainerSetContentPacket(menu.containerId, state, items, ItemStack.EMPTY));
    }
}
