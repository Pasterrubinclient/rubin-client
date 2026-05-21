package ru.rubin;

import java.io.File;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.rubin.cfg.ConfigManager;
import ru.rubin.commands.CommandBootstrap;
import ru.rubin.config.GuiManager;
import ru.rubin.config.friend.FriendManager;
import ru.rubin.event.EventManager;
import ru.rubin.event.RenderHandler;
import ru.rubin.event.render.RenderEvent;
import ru.rubin.module.api.Manager;
import ru.rubin.module.bind.BindingManager;
import ru.rubin.module.impl.combat.auraProcess.rotationProcess.ComponentManager;
import ru.rubin.rpc.RPC;
import ru.rubin.sound.SoundMixFilter;
import ru.rubin.ui.draggable.DraggableManager;
import ru.rubin.ui.gui.GuiClient;
import ru.rubin.ui.gui.GuiScreen;
import ru.rubin.util.render.animation.AnimationSystem;
import ru.rubin.util.render.backends.gl.GlBackend;
import ru.rubin.util.render.backends.gl.GlState;
import ru.rubin.util.render.core.Renderer2D;
import ru.rubin.util.render.text.FontObject;
import ru.rubin.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class Rubin implements ClientModInitializer {
   public static Rubin get;
   public Manager manager;
   public final String name = "NightDLC";
   public final String version = "v1";
   public final String title = "1.21.4";
   public final File preRoot = new File("C:\\", "NightDLC\\");
   public final File root = new File(this.preRoot, "NightDLC");
   public final String rootRes = "rubin";
   public static SoundMixFilter rtx;
   public GuiManager guiManager;
   public ComponentManager componentManager;
   public ConfigManager configManager;
   public FriendManager friendManager;
   public GuiClient guiClient;
   private final RPC rpc = new RPC();
   private static GlBackend backend;
   private static Renderer2D renderer;
   private static FontObject uiFont;
   private static volatile boolean initialized = false;
   private static volatile boolean modInitialized = false;

   public static Renderer2D getRenderer() {
      ensureRendererInitialized();
      return renderer;
   }

   public static boolean isModInitialized() {
      return modInitialized;
   }

   public void onInitializeClient() {
      System.out.println("[Rubin] onInitializeClient() START");
      get = this;
      this.manager = new Manager();
      this.friendManager = new FriendManager();
      this.configManager = new ConfigManager();
      this.guiManager = new GuiManager();
      this.componentManager = new ComponentManager();
      this.componentManager.init();
      this.guiManager.init();
      GuiScreen.selectedTheme = this.guiManager.getCurrentTheme();
      GuiScreen.preSelectedTheme = this.guiManager.getCurrentTheme();
      GuiScreen.selectedCategories = this.guiManager.getCurrentCategory();
      rtx = SoundMixFilter.makeDistorterMixer();
      rtx.init();
      this.rpc.startRpc();
      CommandBootstrap.initialize();
      BindingManager.getInstance().initialize();
      if (this.configManager != null) {
         this.configManager.load();
         if (this.configManager.findConfig("default") != null) {
            this.configManager.loadConfig("default");
         }
      }

      this.guiClient = new GuiClient();
      RenderHandler.register();
      EventManager.register(this);
      modInitialized = true;
      System.out.println("[Rubin] onInitializeClient() COMPLETE - modInitialized=" + modInitialized);
   }

   public static void ensureRendererInitialized() {
      if (!initialized) {
         onInit();
      }
   }

   private static synchronized void onInit() {
      if (!initialized) {
         backend = new GlBackend();
         renderer = new Renderer2D(backend);
         FontRegistry.initialize(backend, renderer);
         uiFont = FontRegistry.INTER_MEDIUM;
         initialized = true;
      }
   }

   public static void onRender() {
      if (modInitialized) {
         GlState.Snapshot snapshot = GlState.push();

         try {
            if (!initialized) {
               onInit();
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getWindow() == null) {
               return;
            }

            int width = client.getWindow().getFramebufferWidth();
            int height = client.getWindow().getFramebufferHeight();
            if (width <= 0 || height <= 0) {
               return;
            }

            AnimationSystem.getInstance().tick();
            DraggableManager draggableManager = DraggableManager.getInstance();
            draggableManager.beginFrame(client, renderer, width, height);
            boolean rendererBegun = false;

            try {
               renderer.begin(width, height);
               rendererBegun = true;

               try {
                  EventManager.call(new RenderEvent(client, renderer, uiFont, width, height));
               } finally {
                  if (rendererBegun) {
                     renderer.end();
                  }
               }
            } finally {
               draggableManager.endFrame();
            }
         } finally {
            GlState.pop(snapshot);
         }
      }
   }

   @Generated
   public Manager getManager() {
      return this.manager;
   }

   @Generated
   public String getName() {
      return "NightDLC";
   }

   @Generated
   public String getVersion() {
      return "v1";
   }

   @Generated
   public String getTitle() {
      return "1.21.4";
   }

   @Generated
   public File getPreRoot() {
      return this.preRoot;
   }

   @Generated
   public File getRoot() {
      return this.root;
   }

   @Generated
   public String getRootRes() {
      return "rubin";
   }

   @Generated
   public GuiManager getGuiManager() {
      return this.guiManager;
   }

   @Generated
   public ComponentManager getComponentManager() {
      return this.componentManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public GuiClient getGuiClient() {
      return this.guiClient;
   }

   @Generated
   public RPC getRpc() {
      return this.rpc;
   }

   @Generated
   public void setManager(Manager manager) {
      this.manager = manager;
   }

   @Generated
   public void setGuiManager(GuiManager guiManager) {
      this.guiManager = guiManager;
   }

   @Generated
   public void setComponentManager(ComponentManager componentManager) {
      this.componentManager = componentManager;
   }

   @Generated
   public void setConfigManager(ConfigManager configManager) {
      this.configManager = configManager;
   }

   @Generated
   public void setFriendManager(FriendManager friendManager) {
      this.friendManager = friendManager;
   }

   @Generated
   public void setGuiClient(GuiClient guiClient) {
      this.guiClient = guiClient;
   }
}
