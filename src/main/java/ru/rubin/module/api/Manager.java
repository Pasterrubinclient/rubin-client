package ru.rubin.module.api;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.rubin.Rubin;
import ru.rubin.module.impl.combat.AimHelper;
import ru.rubin.module.impl.combat.AutoSwap;
import ru.rubin.module.impl.combat.AutoTotem;
import ru.rubin.module.impl.combat.BowHelper;
import ru.rubin.module.impl.combat.HitAura;
import ru.rubin.module.impl.combat.HitBox;
import ru.rubin.module.impl.combat.NoFriendDamage;
import ru.rubin.module.impl.misc.ItemScroller;
import ru.rubin.module.impl.movement.InvMove;
import ru.rubin.module.impl.movement.LongJump;
import ru.rubin.module.impl.movement.NoSlow;
import ru.rubin.module.impl.movement.NoWeb;
import ru.rubin.module.impl.movement.Speed;
import ru.rubin.module.impl.movement.Sprint;
import ru.rubin.module.impl.player.MiddleClick;
import ru.rubin.module.impl.player.NoDelay;
import ru.rubin.module.impl.player.NoPush;
import ru.rubin.module.impl.player.SPJoiner;
import ru.rubin.module.impl.visuals.Arrows;
import ru.rubin.module.impl.visuals.AspectRation;
import ru.rubin.module.impl.visuals.CustomWorld;
import ru.rubin.module.impl.visuals.ESP;
import ru.rubin.module.impl.visuals.Hat;
import ru.rubin.module.impl.visuals.Hud;
import ru.rubin.module.impl.visuals.ItemESP;
import ru.rubin.module.impl.visuals.JumpCircle;
import ru.rubin.module.impl.visuals.NameTags;
import ru.rubin.module.impl.visuals.NightVision;
import ru.rubin.module.impl.visuals.NoRender;
import ru.rubin.module.impl.visuals.Particles;
import ru.rubin.module.impl.visuals.RTXSounds;
import ru.rubin.module.impl.visuals.SkinManager;
import ru.rubin.module.impl.visuals.Svetych;
import ru.rubin.module.impl.visuals.SwingAnimation;
import ru.rubin.module.impl.visuals.TargetESP;
import ru.rubin.module.impl.visuals.Test;
import ru.rubin.module.impl.visuals.Trails;

@Environment(EnvType.CLIENT)
public class Manager {
   public ArrayList<Module> module = new ArrayList<>();

   public Manager() {
      this.module.add(new HitAura());
      this.module.add(new AutoSwap());
      this.module.add(new AutoTotem());
      this.module.add(new BowHelper());
      this.module.add(new HitBox());
      this.module.add(new NoFriendDamage());
      this.module.add(new AimHelper());
      this.module.add(new Sprint());
      this.module.add(new Speed());
      this.module.add(new NoWeb());
      this.module.add(new NoSlow());
      this.module.add(new InvMove());
      this.module.add(new LongJump());
      this.module.add(new Test());
      this.module.add(new ESP());
      this.module.add(new JumpCircle());
      this.module.add(new Trails());
      this.module.add(new Hud());
      this.module.add(new Arrows());
      this.module.add(new ItemESP());
      this.module.add(new Svetych());
      this.module.add(new Particles());
      this.module.add(new NoRender());
      this.module.add(new SwingAnimation());
      this.module.add(new ru.rubin.module.impl.visuals.beautifulhands.BeautifulHands());
      this.module.add(new ru.rubin.module.impl.visuals.richi.RichiDog());
      this.module.add(new ru.rubin.rpc.DiscordRPCModule());
      this.module.add(new Hat());
      this.module.add(new TargetESP());
      this.module.add(new SkinManager());
      this.module.add(new CustomWorld());
      this.module.add(new NightVision());
      this.module.add(new AspectRation());
      this.module.add(new RTXSounds());
      this.module.add(new NameTags());
      this.module.add(new NoPush());
      this.module.add(new NoDelay());
      this.module.add(new SPJoiner());
      this.module.add(new MiddleClick());
      this.module.add(new ItemScroller());
   }

   public ArrayList<Module> getModules() {
      return this.module;
   }

   public <T extends Module> T get(Class<T> clazz) {
      return this.module.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }

   public Module getModule(Class<?> class1) {
      for (Module module1 : this.module) {
         if (module1.getClass() == class1) {
            return module1;
         }
      }

      return null;
   }

   public ArrayList<Module> getType(Category category) {
      ArrayList<Module> modules = new ArrayList<>();

      for (Module module1 : this.module) {
         if (module1.category == category) {
            modules.add(module1);
         }
      }

      return modules;
   }

   public Module[] getBind(int bind) {
      return Rubin.get.manager.module.stream().filter(module -> module.bind == bind).toArray(Module[]::new);
   }
}
