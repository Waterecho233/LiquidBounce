/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket;
import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets;
import static net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT;

public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", ModuleCategory.COMBAT);
    }

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            return newValue > minDelay ? newValue : minDelay;
        }
    };
    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            return newValue < maxDelay ? newValue : maxDelay;
        }

        @Override
        public boolean isSupported() {
            return !maxDelayValue.isMinimal();
        }
    };

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true) {
        @Override
        public boolean isSupported() {
            return !invOpenValue.get();
        }
    };
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);

    private long delay;

    private boolean locked = false;

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || mc.thePlayer == null || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;

        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36).filter(i -> {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            return itemStack != null && itemStack.getItem() instanceof ItemArmor && (i < 9 || System.currentTimeMillis() - ((IMixinItemStack) (Object) itemStack).getItemDelay() >= itemDelayValue.get());
        }).mapToObj(i -> new ArmorPiece(mc.thePlayer.inventory.getStackInSlot(i), i)).collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream().max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for (int i = 0; i < 4; i++) {
            final ArmorPiece armorPiece = bestArmor[i];

            if (armorPiece == null) continue;

            int armorSlot = 3 - i;

            final ArmorPiece oldArmor = new ArmorPiece(mc.thePlayer.inventory.armorItemInSlot(armorSlot), -1);

            if (ItemUtils.isStackEmpty(oldArmor.getItemStack()) || !(oldArmor.getItemStack().getItem() instanceof ItemArmor) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!ItemUtils.isStackEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true)) {
                    locked = true;
                    return;
                }

                if (ItemUtils.isStackEmpty(mc.thePlayer.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false)) {
                    locked = true;
                    return;
                }
            }
        }

        locked = false;
    }

    public boolean isLocked() {
        return getState() && locked;
    }

    /**
     * Shift+Left-clicks the specified item
     *
     * @param item        Slot of the item to click
     * @param isArmorSlot
     * @return True if it is unable to move the item
     */
    private boolean move(int item, boolean isArmorSlot) {
        if (!isArmorSlot && item < 9 && hotbarValue.get() && !(mc.currentScreen instanceof GuiInventory)) {
            sendPackets(
                new C09PacketHeldItemChange(item),
                new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(item).getStack()),
                new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem)
            );

            delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());

            return true;
        } else if (!(noMoveValue.get() && MovementUtils.INSTANCE.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory) sendPacket(new C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT));

            boolean full = isArmorSlot;

            if (full) {
                for (ItemStack iItemStack : mc.thePlayer.inventory.mainInventory) {
                    if (ItemUtils.isStackEmpty(iItemStack)) {
                        full = false;
                        break;
                    }
                }
            }

            if (full) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 1, 4, mc.thePlayer);
            } else {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, (isArmorSlot ? item : (item < 9 ? item + 36 : item)), 0, 1, mc.thePlayer);
            }

            delay = TimeUtils.INSTANCE.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                sendPacket(new C0DPacketCloseWindow());

            return true;
        }

        return false;
    }

}
