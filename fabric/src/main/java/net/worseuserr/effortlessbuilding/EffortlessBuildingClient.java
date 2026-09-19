package net.worseuserr.effortlessbuilding;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.InteractionResult;
import net.worseuserr.effortlessbuilding.config.ClientConfig;
import net.worseuserr.effortlessbuilding.buildpipeline.BuildPipeline;
import net.worseuserr.effortlessbuilding.buildpipeline.BuildPipelineClient;
import net.worseuserr.effortlessbuilding.network.PacketHandler;
import net.worseuserr.effortlessbuilding.network.SyncModifiersS2CPacket;
import net.worseuserr.effortlessbuilding.network.SyncServerConfigS2CPacket;
import net.worseuserr.effortlessbuilding.network.SyncAE2CountS2CPacket;
import net.worseuserr.effortlessbuilding.network.UndoPacket;
import net.worseuserr.effortlessbuilding.network.RedoPacket;
import net.worseuserr.effortlessbuilding.render.RenderHandler;
import net.worseuserr.effortlessbuilding.utilities.KeyBindings;
import net.worseuserr.effortlessbuilding.screen.ModifiersScreen;
import net.worseuserr.effortlessbuilding.menu.RadialMenu;
import net.worseuserr.effortlessbuilding.screen.RandomizerScreen;
import net.worseuserr.effortlessbuilding.screen.RandomizerTooltipComponent;
import net.worseuserr.effortlessbuilding.item.RandomizerToolItem;
import net.worseuserr.effortlessbuilding.item.RandomizerTooltipData;
import net.worseuserr.effortlessbuilding.menu.ModMenus;
import org.lwjgl.glfw.GLFW;

public class EffortlessBuildingClient implements ClientModInitializer {

    private static boolean prevRightDown = false;
    private static boolean prevLeftDown = false;

    @Override
    public void onInitializeClient() {
        ClientConfig.INSTANCE.load();
        MenuScreens.register(ModMenus.RANDOMIZER, RandomizerScreen::new);
        TooltipComponentCallback.EVENT.register(data -> data instanceof RandomizerTooltipData randomizerData
                ? new RandomizerTooltipComponent(randomizerData) : null);

        KeyBindingHelper.registerKeyBinding(KeyBindings.openRadialMenu);
        KeyBindingHelper.registerKeyBinding(KeyBindings.openModifiersScreen);
        KeyBindingHelper.registerKeyBinding(KeyBindings.undo);
        KeyBindingHelper.registerKeyBinding(KeyBindings.redo);

        // Register client-side handler for S2C modifier sync packet
        ClientPlayNetworking.registerGlobalReceiver(SyncModifiersS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> PacketHandler.handleSyncModifiers(payload)));

        // Register client-side handler for S2C server config sync packet
        ClientPlayNetworking.registerGlobalReceiver(SyncServerConfigS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> PacketHandler.handleSyncServerConfig(payload)));

        // Register client-side handler for AE2 count sync
        ClientPlayNetworking.registerGlobalReceiver(SyncAE2CountS2CPacket.TYPE, (payload, context) ->
                context.client().execute(() -> PacketHandler.handleSyncAE2Count(payload)));


        HudRenderCallback.EVENT.register((graphics, tickCounter) ->
                RenderHandler.onRenderGui(graphics));

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (context.consumers() == null || context.matrixStack() == null) return;
            var camPos = context.camera().getPosition();
            RenderHandler.onRenderLevel(
                    context.matrixStack(),
                    (MultiBufferSource.BufferSource) context.consumers(),
                    camPos.x, camPos.y, camPos.z);
        });

        // Cancel vanilla block breaking when the build pipeline should intercept
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (!BuildPipelineClient.shouldInterceptBreaking()) return InteractionResult.PASS;
            if (player.getMainHandItem().isEmpty()
                    || BuildPipeline.isBuildTriggerItem(player.getMainHandItem())
                    || BuildPipelineClient.getBuildState() != null) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.openModifiersScreen.consumeClick()) {
                Minecraft.getInstance().setScreen(new ModifiersScreen());
            }
            // Undo/redo keybindings — require Ctrl held
            while (KeyBindings.undo.consumeClick()) {
                if (InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                    PacketHandler.sendToServer(new UndoPacket());
                }
            }
            while (KeyBindings.redo.consumeClick()) {
                if (InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                        || InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                    PacketHandler.sendToServer(new RedoPacket());
                }
            }

            if (client.screen == null) {
                if (KeyBindings.isKeyDown(KeyBindings.openRadialMenu)) {
                    Minecraft.getInstance().setScreen(RadialMenu.instance);
                }

                if (client.player != null && client.level != null && BuildPipelineClient.shouldInterceptPlacing()) {
                    boolean rightDown = client.options.keyUse.isDown();
                    boolean leftDown = client.options.keyAttack.isDown();
                    boolean rightJustPressed = rightDown && !prevRightDown;
                    boolean leftJustPressed = leftDown && !prevLeftDown;

                    if (rightJustPressed) {
                        if (client.player.isShiftKeyDown()
                                && client.player.getMainHandItem().getItem() instanceof RandomizerToolItem) {
                            // Vanilla item use opens the server-backed randomizer menu.
                        } else if (BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.BREAKING) {
                            BuildPipelineClient.cancelCurrentSequence();
                        } else if (BuildPipeline.isBuildTriggerItem(client.player.getMainHandItem())
                                || BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.PLACING) {
                            BuildPipelineClient.handleRightClick(Minecraft.getInstance());
                        }
                    }
                    if (leftJustPressed && BuildPipelineClient.shouldInterceptBreaking()) {
                        if (BuildPipelineClient.getBuildState() == BuildPipeline.BuildState.PLACING) {
                            BuildPipelineClient.cancelCurrentSequence();
                        } else if (client.player.getMainHandItem().isEmpty()
                                || BuildPipeline.isBuildTriggerItem(client.player.getMainHandItem())
                                || BuildPipelineClient.getBuildState() != null) {
                            BuildPipelineClient.handleLeftClick(Minecraft.getInstance());
                        }
                    }
                    prevRightDown = rightDown;
                    prevLeftDown = leftDown;
                }
            } else {
                prevRightDown = false;
                prevLeftDown = false;
            }
        });
    }
}
