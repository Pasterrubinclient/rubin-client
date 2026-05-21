package ru.rubin.module.impl.movement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import ru.rubin.event.EventInit;
import ru.rubin.event.impl.EventPacket;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.event.player.ScreenCloseEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.ui.gui.GuiClient;
import ru.rubin.util.other.TimerUtil;
import ru.rubin.util.player.MoveUtil;
import ru.rubin.util.player.MovementManager;

@IModule(
   name = "Inv Move",
   description = "",
   category = Category.Movement,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class InvMove extends Module {
   public static ModeSetting byppas = new ModeSetting("Режим", "GrimLightning", "GrimLightning", "Default");
   private final List<Packet<?>> packetQueue = new ArrayList<>();
   private final Set<Packet<?>> sendingPackets = new HashSet<>();
   private final TimerUtil timer = new TimerUtil();
   public boolean stop = false;
   private boolean pendingClose = false;
   private long closeTime = 0L;

   public InvMove() {
      this.addSettings(new Setting[]{byppas});
   }

   @EventInit
   public void onUpdate(ClientTickEvent e) {
      if (this.stop) {
         mc.options.jumpKey.setPressed(false);
         MovementManager.getInstance().lockMovement("GuiMove");
      } else {
         MovementManager.getInstance().unlockMovement("GuiMove");
      }

      if (this.pendingClose && System.currentTimeMillis() >= this.closeTime) {
         this.pendingClose = false;

         for (Packet<?> p : this.packetQueue) {
            if (mc.getNetworkHandler() != null) {
               mc.getNetworkHandler().sendPacket(p);
            }
         }

         this.packetQueue.clear();
         if (mc.player != null) {
            mc.execute(() -> {
               if (mc.player != null) {
                  mc.player.closeScreen();
               }
            });
         }
      }

      if (mc.player != null) {
         if (byppas.is("Default")) {
            this.handleDefaultMode();
         } else if (byppas.is("GrimLightning")) {
            this.handleBypassMode();
         }
      }
   }

   @EventInit
   public void onPacket(EventPacket e) {
      if (byppas.is("GrimLightning") && e.isSend() && e.getPacket() instanceof ClickSlotC2SPacket p && mc.currentScreen instanceof InventoryScreen) {
         if (this.sendingPackets.contains(p)) {
            this.sendingPackets.remove(p);
            return;
         }

         if (MoveUtil.isMoving()) {
            this.packetQueue.add(p);
            e.cancel();
         }
      }
   }

   @EventInit
   public void onClose(ScreenCloseEvent e) {
      if (byppas.is("GrimLightning") && mc.currentScreen instanceof InventoryScreen) {
         if (!MoveUtil.isMoving()) {
            return;
         }

         e.cancel();
         this.stop = false;
         this.timer.reset();
         this.closeTime = System.currentTimeMillis() + 150L;
         this.pendingClose = true;
      }
   }

   private void handleBypassMode() {
      KeyBinding[] movementKeys = new KeyBinding[]{
         mc.options.forwardKey,
         mc.options.backKey,
         mc.options.leftKey,
         mc.options.rightKey,
         mc.options.jumpKey,
         mc.options.sprintKey
      };
      if (!this.timer.isReached(150L)) {
         this.stop = true;
      } else {
         if (!(mc.currentScreen instanceof InventoryScreen)) {
            this.stop = false;
         }

         if (mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof GuiClient) {
            this.updateKeyBindingState(movementKeys);
         }
      }
   }

   private void handleDefaultMode() {
      if (!(mc.currentScreen instanceof InventoryScreen)) {
         this.stop = false;
      }

      KeyBinding[] movementKeys = new KeyBinding[]{
         mc.options.forwardKey,
         mc.options.backKey,
         mc.options.leftKey,
         mc.options.rightKey,
         mc.options.jumpKey,
         mc.options.sprintKey
      };
      if (mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof GuiClient) {
         this.updateKeyBindingState(movementKeys);
      }
   }

   private void updateKeyBindingState(KeyBinding[] keyBindings) {
      long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();

      for (KeyBinding keyBinding : keyBindings) {
         int keyCode = keyBinding.getDefaultKey().getCode();
         boolean isKeyPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), keyCode);
         keyBinding.setPressed(isKeyPressed);
      }
   }

   @Override
   public void onDisable() {
      this.stop = false;
      this.pendingClose = false;
      MovementManager.getInstance().unlockMovement("GuiMove");
      this.packetQueue.clear();
      super.onDisable();
   }
}
