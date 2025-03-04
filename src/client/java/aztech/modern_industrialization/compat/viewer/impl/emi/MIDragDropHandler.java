/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.compat.viewer.impl.emi;

import aztech.modern_industrialization.api.ReiDraggable;
import aztech.modern_industrialization.client.screen.MIHandledScreen;
import aztech.modern_industrialization.inventory.ConfigurableInventoryPackets;
import aztech.modern_industrialization.util.Simulation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

class MIDragDropHandler implements EmiDragDropHandler<Screen> {
    @Override
    public boolean dropStack(Screen screen, EmiIngredient ingredient, int mouseX, int mouseY) {
        if (!(screen instanceof MIHandledScreen<?>gui)) {
            return false;
        }

        // Note: might provide peculiar behavior for tags. Checking for size == 1 is an option
        EmiStack stack = ingredient.getEmiStacks().get(0);

        FluidVariant fk = stack.getKey() instanceof Fluid f ? FluidVariant.of(f, stack.getNbt()) : null;
        ItemVariant ik = stack.getKey() instanceof Item i ? ItemVariant.of(i, stack.getNbt()) : null;
        @Nullable
        GuiEventListener element = gui.getChildAt(mouseX, mouseY).orElse(null);
        if (element instanceof ReiDraggable dw) {
            if (ik != null) {
                return dw.dragItem(ik, Simulation.ACT);
            }
            if (fk != null) {
                return dw.dragFluid(fk, Simulation.ACT);
            }
        }
        AbstractContainerMenu handler = gui.getMenu();
        Slot slot = gui.getFocusedSlot();
        if (slot instanceof ReiDraggable dw) {
            int slotId = handler.slots.indexOf(slot);
            if (ik != null && dw.dragItem(ik, Simulation.ACT)) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeInt(handler.containerId);
                buf.writeVarInt(slotId);
                buf.writeBoolean(true);
                ik.toPacket(buf);
                ClientPlayNetworking.send(ConfigurableInventoryPackets.DO_SLOT_DRAGGING, buf);
                return true;
            }
            if (fk != null && dw.dragFluid(fk, Simulation.ACT)) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeInt(handler.containerId);
                buf.writeVarInt(slotId);
                buf.writeBoolean(false);
                fk.toPacket(buf);
                ClientPlayNetworking.send(ConfigurableInventoryPackets.DO_SLOT_DRAGGING, buf);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(Screen screen, EmiIngredient ingredient, PoseStack matrices, int mouseX, int mouseY, float delta) {
        if (!(screen instanceof MIHandledScreen<?>gui)) {
            return;
        }

        List<Rect2i> bounds = new ArrayList<>();
        EmiStack stack = ingredient.getEmiStacks().get(0);

        FluidVariant fk = stack.getKey() instanceof Fluid f ? FluidVariant.of(f, stack.getNbt()) : null;
        ItemVariant ik = stack.getKey() instanceof Item i ? ItemVariant.of(i, stack.getNbt()) : null;
        for (GuiEventListener element : gui.children()) {
            if (element instanceof AbstractWidget cw && element instanceof ReiDraggable dw) {
                if (ik != null && dw.dragItem(ik, Simulation.SIMULATE)) {
                    bounds.add(getWidgetBounds(cw));
                }
                if (fk != null && dw.dragFluid(fk, Simulation.SIMULATE)) {
                    bounds.add(getWidgetBounds(cw));
                }
            }
        }
        AbstractContainerMenu handler = gui.getMenu();
        for (Slot slot : handler.slots) {
            if (slot instanceof ReiDraggable dw) {
                if (ik != null && dw.dragItem(ik, Simulation.SIMULATE)) {
                    bounds.add(getSlotBounds(slot, gui));
                }
                if (fk != null && dw.dragFluid(fk, Simulation.SIMULATE)) {
                    bounds.add(getSlotBounds(slot, gui));
                }
            }
        }

        for (var b : bounds) {
            GuiComponent.fill(matrices, b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), 0x8822BB33);
        }
    }

    private static Rect2i getWidgetBounds(AbstractWidget cw) {
        return new Rect2i(cw.getX(), cw.getY(), cw.getWidth(), cw.getHeight());
    }

    private static Rect2i getSlotBounds(Slot slot, MIHandledScreen<?> screen) {
        return new Rect2i(slot.x + screen.getX(), slot.y + screen.getY(), 16, 16);
    }
}
