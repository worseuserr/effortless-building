package net.worseuserr.effortlessbuilding;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.worseuserr.effortlessbuilding.menu.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.worseuserr.effortlessbuilding.modifier.ModifierServerStorage;
import net.worseuserr.effortlessbuilding.network.BreakBuildModePacket;
import net.worseuserr.effortlessbuilding.network.PacketHandler;
import net.worseuserr.effortlessbuilding.network.PlaceBuildModePacket;
import net.worseuserr.effortlessbuilding.network.UndoPacket;
import net.worseuserr.effortlessbuilding.network.RedoPacket;
import net.worseuserr.effortlessbuilding.network.UpdateModifiersC2SPacket;
import net.worseuserr.effortlessbuilding.network.SyncModifiersS2CPacket;
import net.worseuserr.effortlessbuilding.network.UpdateServerConfigC2SPacket;
import net.worseuserr.effortlessbuilding.network.SyncServerConfigS2CPacket;
import net.worseuserr.effortlessbuilding.network.QueryAE2CountC2SPacket;
import net.worseuserr.effortlessbuilding.network.SyncAE2CountS2CPacket;
import net.worseuserr.effortlessbuilding.network.BuildModeHintC2SPacket;
import net.worseuserr.effortlessbuilding.config.ServerConfig;
import net.worseuserr.effortlessbuilding.config.ServerConfigStorage;
import net.worseuserr.effortlessbuilding.config.WelcomeMessageStorage;
import net.worseuserr.effortlessbuilding.config.BuildModeHintStorage;
import net.worseuserr.effortlessbuilding.utilities.PlacedBlockTracker;
import net.worseuserr.effortlessbuilding.utilities.UndoManager;
import net.worseuserr.effortlessbuilding.item.RandomizerToolItem;

public class EffortlessBuilding implements ModInitializer {

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");

        Item randomizerTool = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "randomizer_tool"),
                new RandomizerToolItem(new Item.Properties().stacksTo(1)));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(entries -> entries.accept(randomizerTool));
        Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "randomizer"), ModMenus.RANDOMIZER);

        // Register C2S packets
        PayloadTypeRegistry.playC2S().register(PlaceBuildModePacket.TYPE, PlaceBuildModePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(BreakBuildModePacket.TYPE, BreakBuildModePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UndoPacket.TYPE, UndoPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RedoPacket.TYPE, RedoPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateModifiersC2SPacket.TYPE, UpdateModifiersC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateServerConfigC2SPacket.TYPE, UpdateServerConfigC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(QueryAE2CountC2SPacket.TYPE, QueryAE2CountC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(BuildModeHintC2SPacket.TYPE, BuildModeHintC2SPacket.STREAM_CODEC);

        // Register S2C packets
        PayloadTypeRegistry.playS2C().register(SyncModifiersS2CPacket.TYPE, SyncModifiersS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncServerConfigS2CPacket.TYPE, SyncServerConfigS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncAE2CountS2CPacket.TYPE, SyncAE2CountS2CPacket.STREAM_CODEC);

        // Register server-side handlers
        ServerPlayNetworking.registerGlobalReceiver(PlaceBuildModePacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handlePlaceBuildMode(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(BreakBuildModePacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleBreakBuildMode(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(UndoPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleUndo(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(RedoPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleRedo(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(UpdateModifiersC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleUpdateModifiers(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(UpdateServerConfigC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleUpdateServerConfig(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(QueryAE2CountC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleQueryAE2Count(payload, context.player())));
        ServerPlayNetworking.registerGlobalReceiver(BuildModeHintC2SPacket.TYPE, (payload, context) ->
                context.server().execute(() -> PacketHandler.handleBuildModeHint(context.player())));

        // Load + send modifiers and config on player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ModifierServerStorage.loadPlayer(server, player.getUUID());
            PacketHandler.sendToClient(player, new SyncModifiersS2CPacket(
                    ModifierServerStorage.serializePlayer(player.getUUID())));
            PacketHandler.sendToClient(player, new SyncServerConfigS2CPacket(
                    ServerConfig.INSTANCE.toJson()));
            WelcomeMessageStorage.showIfNeeded(player);
        });

        // Save + clean up on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            ModifierServerStorage.savePlayer(server, player.getUUID());
            ModifierServerStorage.removePlayer(player.getUUID());
            UndoManager.clearPlayer(player.getUUID());
            PlacedBlockTracker.clearPlayer(player.getUUID());
        });

        // Clear all cached data when the server stops
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ModifierServerStorage.clearAll();
            ServerConfigStorage.clear();
            WelcomeMessageStorage.clear();
            BuildModeHintStorage.clear();
        });

        // Load server config on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerConfigStorage.load(server);
            WelcomeMessageStorage.load(server);
            BuildModeHintStorage.load(server);
        });

        CommonClass.init();
    }
}
