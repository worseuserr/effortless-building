package net.worseuserr.effortlessbuilding.platform;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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

public class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(PlaceBuildModePacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToServer(BreakBuildModePacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToServer(UndoPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToServer(RedoPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToServer(UpdateModifiersC2SPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncModifiersS2CPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public void sendToServer(UpdateServerConfigC2SPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncServerConfigS2CPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Override
    public void sendToServer(QueryAE2CountC2SPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToServer(BuildModeHintC2SPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    @Override
    public void sendToClient(ServerPlayer player, SyncAE2CountS2CPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

}
