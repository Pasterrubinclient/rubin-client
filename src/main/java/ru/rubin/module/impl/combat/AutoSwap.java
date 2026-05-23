package ru.rubin.module.impl.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import ru.rubin.Rubin;
import ru.rubin.event.EventInit;
import ru.rubin.event.input.KeyInputEvent;
import ru.rubin.event.lifecycle.ClientTickEvent;
import ru.rubin.module.api.Category;
import ru.rubin.module.api.IModule;
import ru.rubin.module.api.Module;
import ru.rubin.module.api.setting.Setting;
import ru.rubin.module.api.setting.impl.BindSettings;
import ru.rubin.module.api.setting.impl.BooleanSetting;
import ru.rubin.module.api.setting.impl.ModeSetting;
import ru.rubin.module.impl.visuals.Hud;
import ru.rubin.util.other.StopWatchShadow;
import ru.rubin.util.player.InvUtil;
import ru.rubin.util.player.MovementManager;
import ru.rubin.util.render.core.Renderer2D;

@IModule(
   name = "Auto Swap",
   description = "Свап предметов в оффхенд",
   category = Category.Combat,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class AutoSwap extends Module {
   public static ModeSetting swapType = new ModeSetting("Тип свапа", "Двойной", "Двойной", "Тройной");
   public static ModeSetting firstItemSetting = new ModeSetting("Первый предмет", "Шар", "Золотое яблоко", "Щит", "Шар", "Тотем")
           .hidden(() -> swapType.is("Тройной"));
   public static ModeSetting secondItemSetting = new ModeSetting("Второй предмет", "Тотем 2", "Золотое яблоко 2", "Щит 2", "Шар 2", "Тотем 2")
           .hidden(() -> swapType.is("Тройной"));
   public static BindSettings bind = new BindSettings("Кнопка", -1);
   public static BooleanSetting swaprender = new BooleanSetting("Показ свапа", true);
   public static BooleanSetting onlyEnchanted = new BooleanSetting("Только Чар. тотемы", false)
           .hidden(() -> swapType.is("Тройной"));

   // Triple swap wheel items (empty by default, filled via inventory picker)
   private final ItemStack[] wheelItems = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   private boolean tripleKeyWasPressed = false;

   private boolean swap;
   private boolean hand;
   private final StopWatchShadow swapWatch = new StopWatchShadow();
   private final StopWatchShadow swapWatchK = new StopWatchShadow();
   private boolean bypassActive;
   private boolean bypassSwapped;
   private int bypassSlot = -1;
   private String bypassItemName = "";

   public AutoSwap() {
      this.addSettings(new Setting[]{swapType, firstItemSetting, secondItemSetting, bind, swaprender, onlyEnchanted});
   }

   @EventInit
   public void update(ClientTickEvent event) {
      if (mc.player == null) return;

      if (swapType.is("Тройной")) {
         updateTripleSwap();
         return;
      }

      // Double swap logic (original)
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      if (this.bypassActive) {
         if (!this.bypassSwapped && this.bypassSlot != -1) {
            mc.interactionManager
               .clickSlot(screenHandler.syncId, this.bypassSlot < 9 ? this.bypassSlot + 36 : this.bypassSlot, 40, SlotActionType.SWAP, mc.player);
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
            notifySwap(this.bypassItemName);
            this.bypassSwapped = true;
         }

         if (this.swapWatch.hasTimeElapsed(1L)) {
            MovementManager.getInstance().unlockMovement("AutoSwap");
            this.bypassActive = false;
            this.bypassSwapped = false;
            this.bypassSlot = -1;
         }
      } else {
         if (this.swap && this.hand) {
            String first = firstItemSetting.get();
            if (first.contains("Шар")) this.swapDouble(Items.PLAYER_HEAD, "Шар", false);
            else if (first.contains("Тотем")) this.swapDouble(Items.TOTEM_OF_UNDYING, "Тотем", onlyEnchanted.get());
            else if (first.contains("яблоко")) this.swapDouble(Items.GOLDEN_APPLE, "Золотое яблоко", false);
            else if (first.contains("Щит")) this.swapDouble(Items.SHIELD, "Щит", false);
            this.hand = false;
         }

         if (this.swap) {
            String second = secondItemSetting.get();
            if (second.contains("Шар")) this.swapDouble(Items.PLAYER_HEAD, "Шар", false);
            else if (second.contains("яблоко")) this.swapDouble(Items.GOLDEN_APPLE, "Золотое яблоко", false);
            else if (second.contains("Тотем")) this.swapDouble(Items.TOTEM_OF_UNDYING, "Тотем", onlyEnchanted.get());
            else if (second.contains("Щит")) this.swapDouble(Items.SHIELD, "Щит", false);
            this.hand = true;
         }
      }
   }

   private void updateTripleSwap() {
      int key = bind.get();
      if (key <= 0) return;

      long handle = mc.getWindow().getHandle();
      boolean pressed;
      if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
         pressed = GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS;
      } else {
         pressed = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
      }

      if (pressed && !tripleKeyWasPressed && mc.currentScreen == null) {
         mc.setScreen(new AutoSwapWheelScreen(this));
      }
      tripleKeyWasPressed = pressed;
   }

   public void tripleSwapItem(Item item, String name) {
      if (mc.player == null) return;
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      int slot = InvUtil.find(item);
      if (slot == -1) return;

      int adjustedSlot = slot < 9 ? slot + 36 : slot;
      mc.interactionManager.clickSlot(screenHandler.syncId, adjustedSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(screenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(screenHandler.syncId, adjustedSlot, 0, SlotActionType.PICKUP, mc.player);
      notifySwap(name);
   }

   @EventInit
   public void input(KeyInputEvent event) {
      if (swapType.is("Двойной") && mc.currentScreen == null && this.swapWatchK.hasTimeElapsed(200L)) {
         this.swap = event.key() == bind.get();
         this.swapWatchK.reset();
      }
   }

   private void swapDouble(Item item, String itemName, boolean onlyEnchanted) {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      int slot = item == Items.TOTEM_OF_UNDYING ? InvUtil.find(item, false, onlyEnchanted) : InvUtil.find(item);
      if (slot != -1) {
         MovementManager.getInstance().lockMovement("AutoSwap");
         this.bypassActive = true;
         this.bypassSwapped = false;
         this.bypassSlot = slot;
         this.bypassItemName = itemName;
         this.swapWatch.reset();
      }
      this.swap = false;
   }

   private void notifySwap(String itemName) {
      if (swaprender.get() && mc.player != null) {
         Rubin.get.manager.get(Hud.class)
            .showNotification("warn", "AutoSwap - свапнул на " + itemName, 1200L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
         Text msg = Text.literal("AutoSwap - свапнул на ")
            .formatted(Formatting.WHITE)
            .append(Text.literal(itemName).formatted(Formatting.RED));
         mc.player.sendMessage(msg, false);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      MovementManager.getInstance().unlockMovement("AutoSwap");
   }

   public ItemStack getWheelItem(int index) {
      if (index < 0 || index >= 3) return ItemStack.EMPTY;
      return wheelItems[index] == null ? ItemStack.EMPTY : wheelItems[index];
   }

   public void setWheelItem(int index, ItemStack stack) {
      if (index < 0 || index >= 3) return;
      wheelItems[index] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
   }

   public int getBindKey() {
      return bind.get();
   }
}
