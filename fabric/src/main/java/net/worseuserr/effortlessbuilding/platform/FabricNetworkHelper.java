package net.worseuserr.effortlessbuilding.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.worseuserr.effortlessbuilding.network.BreakBuildModePacket;
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
import net.worseuserr.effortlessbuilding.platform.services.INetworkHelper;

public class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(PlaceBuildModePacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToServer(BreakBuildModePacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToServer(UndoPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToServer(RedoPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToServer(UpdateModifiersC2SPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncModifiersS2CPacket packet) {
        ServerPlayNetworking.send(player, packet);
    }

    @Override
    public void sendToServer(UpdateServerConfigC2SPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncServerConfigS2CPacket packet) {
        ServerPlayNetworking.send(player, packet);
    }

    @Override
    public void sendToServer(QueryAE2CountC2SPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToServer(BuildModeHintC2SPacket packet) {
        ClientPlayNetworking.send(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncAE2CountS2CPacket packet) {
        ServerPlayNetworking.send(player, packet);
    }

}
