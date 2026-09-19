package net.worseuserr.effortlessbuilding;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.worseuserr.effortlessbuilding.config.ClientConfig;
import net.worseuserr.effortlessbuilding.buildpipeline.BuildPipeline;
import net.worseuserr.effortlessbuilding.buildpipeline.BuildPipelineClient;
import net.worseuserr.effortlessbuilding.network.PacketHandler;
import net.worseuserr.effortlessbuilding.network.UndoPacket;
import net.worseuserr.effortlessbuilding.network.RedoPacket;
import net.worseuserr.effortlessbuilding.render.RenderHandler;
import net.worseuserr.effortlessbuilding.utilities.KeyBindings;
import net.worseuserr.effortlessbuilding.screen.ModifiersScreen;
import net.worseuserr.effortlessbuilding.screen.RadialMenu;
import net.worseuserr.effortlessbuilding.screen.RandomizerScreen;
import net.worseuserr.effortlessbuilding.screen.RandomizerTooltipComponent;
import net.worseuserr.effortlessbuilding.item.RandomizerToolItem;
import net.worseuserr.effortlessbuilding.item.RandomizerTooltipData;
import net.worseuserr.effortlessbuilding.menu.ModMenus;
import org.lwjgl.glfw.GLFW;

public class NeoForgeClientSetup {

    // Mod-bus events (RegisterKeyMappingsEvent).
    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            ClientConfig.INSTANCE.load();

            event.register(KeyBindings.openRadialMenu);
            event.register(KeyBindings.openModifiersScreen);
            event.register(KeyBindings.undo);
            event.register(KeyBindings.redo);
        }

        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.RANDOMIZER, RandomizerScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(RandomizerTooltipData.class, RandomizerTooltipComponent::new);
        }
    }

    // Game-bus events (ClientTickEvent).
    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        private static boolean prevRightDown = false;
        private static boolean prevLeftDown = false;

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (KeyBindings.openModifiersScreen.consumeClick()) {
                Minecraft.getInstance().setScreen(new ModifiersScreen());
            }
            // Undo/redo keybindings — require Ctrl held
            Minecraft mc = Minecraft.getInstance();
            while (KeyBindings.undo.consumeClick()) {
                if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                    PacketHandler.sendToServer(new UndoPacket());
                }
            }
            while (KeyBindings.redo.consumeClick()) {
                if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                    PacketHandler.sendToServer(new RedoPacket());
                }
            }

            if (mc.screen == null) {
                if (KeyBindings.isKeyDown(KeyBindings.openRadialMenu)) {
                    mc.setScreen(RadialMenu.instance);
                }

                if (mc.player != null && mc.level != null && BuildPipelineClient.shouldInterceptPlacing()) {
                    boolean rightDown = mc.options.keyUse.isDown();
                    boolean leftDown = mc.options.keyAttack.isDown();
                    boolean rightJustPressed = rightDown && !prevRightDown;
                    boolean leftJustPressed = leftDown && !prevLeftDown;

                    if (rightJustPressed) {
                        if (mc.player.isShiftKeyDown()
                                && mc.player.getMainHandItem().getItem() instanceof RandomizerToolItem) {
                            // Vanilla item use opens the server-backed randomizer menu.
                        } else if (BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.BREAKING) {
                            BuildPipelineClient.cancelCurrentSequence();
                        } else if (BuildPipeline.isBuildTriggerItem(mc.player.getMainHandItem())
                                || BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.PLACING) {
                            BuildPipelineClient.handleRightClick(mc);
                        }
                    }
                    if (leftJustPressed && BuildPipelineClient.shouldInterceptBreaking()) {
                        if (BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.PLACING) {
                            BuildPipelineClient.cancelCurrentSequence();
                        } else if (mc.player.getMainHandItem().isEmpty()
                                || BuildPipeline.isBuildTriggerItem(mc.player.getMainHandItem())
                                || BuildPipelineClient.getBuildState() != null) {
                            BuildPipelineClient.handleLeftClick(mc);
                        }
                    }
                    prevRightDown = rightDown;
                    prevLeftDown = leftDown;
                }
            } else {
                prevRightDown = false;
                prevLeftDown = false;
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            RenderHandler.onRenderGui(event.getGuiGraphics());
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
            var camPos = event.getCamera().getPosition();
            var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            RenderHandler.onRenderLevel(event.getPoseStack(), bufferSource,
                    camPos.x, camPos.y, camPos.z);
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!event.getEntity().level().isClientSide()) return;
            if (!BuildPipelineClient.shouldInterceptBreaking()) return;
            var player = event.getEntity();
            if (player.getMainHandItem().isEmpty()
                    || BuildPipeline.isBuildTriggerItem(player.getMainHandItem())
                    || BuildPipelineClient.getBuildState() != null) {
                event.setCanceled(true);
            }
        }
    }
}
