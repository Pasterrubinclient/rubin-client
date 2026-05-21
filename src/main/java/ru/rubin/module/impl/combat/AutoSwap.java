package ru.rubin.module.impl.combat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
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
   description = "Жозки авто свин",
   category = Category.Combat,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class AutoSwap extends Module {
   public static ModeSetting firstItemSetting = new ModeSetting("Первый предмет", "Шар", "Золотое яблоко", "Щит", "Шар", "Тотем");
   public static ModeSetting secondItemSetting = new ModeSetting("Второй предмет", "Тотем 2", "Золотое яблоко 2", "Щит 2", "Шар 2", "Тотем 2");
   public static BindSettings bind = new BindSettings("Кнопка", -1);
   public static BooleanSetting swaprender = new BooleanSetting("Показ свапа", true);
   public static BooleanSetting onlyEnchanted = new BooleanSetting("Только Чар. тотемы", false);
   private boolean swap;
   private boolean hand;
   private final StopWatchShadow swapWatch = new StopWatchShadow();
   private final StopWatchShadow swapWatchK = new StopWatchShadow();
   private boolean bypassActive;
   private boolean bypassSwapped;
   private int bypassSlot = -1;
   private String bypassItemName = "";

   public AutoSwap() {
      this.addSettings(new Setting[]{firstItemSetting, secondItemSetting, bind, swaprender, onlyEnchanted});
   }

   @EventInit
   public void update(ClientTickEvent event) {
      ScreenHandler screenHandler = mc.player.currentScreenHandler;
      if (this.bypassActive) {
         if (!this.bypassSwapped && this.bypassSlot != -1) {
            mc.interactionManager
               .clickSlot(screenHandler.syncId, this.bypassSlot < 9 ? this.bypassSlot + 36 : this.bypassSlot, 40, SlotActionType.SWAP, mc.player);
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
            if (swaprender.get() && mc.player != null) {
               Rubin.get
                  .manager
                  .get(Hud.class)
                  .showNotification("warn", "AutoSwap - СЃРІР°РїРЅСѓР» РЅР° " + this.bypassItemName, 1200L, Renderer2D.ColorUtil.getTextTwoColor(1, 1));
               Text msg = Text.literal("AutoSwap - СЃРІР°РїРЅСѓР» РЅР° ")
                  .formatted(Formatting.WHITE)
                  .append(Text.literal(this.bypassItemName).formatted(Formatting.RED));
               mc.player.sendMessage(msg, false);
            }

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
            if (firstItemSetting.get().equals("РЁР°СЂ")) {
               this.swap(Items.PLAYER_HEAD, "РЁР°СЂ", false);
            } else if (firstItemSetting.get().equals("РўРѕС‚РµРј")) {
               this.swap(Items.TOTEM_OF_UNDYING, "РўРѕС‚РµРј", onlyEnchanted.get());
            } else if (firstItemSetting.get().equals("Р—РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ")) {
               this.swap(Items.GOLDEN_APPLE, "Р—РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ", false);
            } else if (firstItemSetting.get().equals("Р©РёС‚")) {
               this.swap(Items.SHIELD, "Р©РёС‚", false);
            }

            this.hand = false;
         }

         if (this.swap) {
            if (secondItemSetting.get().equals("РЁР°СЂ 2")) {
               this.swap(Items.PLAYER_HEAD, "РЁР°СЂ", false);
            } else if (secondItemSetting.get().equals("Р—РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ 2")) {
               this.swap(Items.GOLDEN_APPLE, "Р—РѕР»РѕС‚РѕРµ СЏР±Р»РѕРєРѕ", false);
            } else if (secondItemSetting.get().equals("РўРѕС‚РµРј 2")) {
               this.swap(Items.TOTEM_OF_UNDYING, "РўРѕС‚РµРј", onlyEnchanted.get());
            } else if (secondItemSetting.get().equals("Р©РёС‚ 2")) {
               this.swap(Items.SHIELD, "Р©РёС‚", false);
            }

            this.hand = true;
         }
      }
   }

   @EventInit
   public void input(KeyInputEvent event) {
      if (mc.currentScreen == null && this.swapWatchK.hasTimeElapsed(200L)) {
         this.swap = event.key() == bind.get();
         this.swapWatchK.reset();
      }
   }

   private void swap(Item item, String itemName, boolean onlyEnchanted) {
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

   @Override
   public void onDisable() {
      super.onDisable();
      MovementManager.getInstance().unlockMovement("AutoSwap");
   }
}
