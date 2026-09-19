package net.worseuserr.effortlessbuilding.platform.services;

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

public interface INetworkHelper {

    void sendToServer(PlaceBuildModePacket packet);

    void sendToServer(BreakBuildModePacket packet);

    void sendToServer(UndoPacket packet);

    void sendToServer(RedoPacket packet);

    void sendToServer(UpdateModifiersC2SPacket packet);

    void sendToClient(ServerPlayer player, SyncModifiersS2CPacket packet);

    void sendToServer(UpdateServerConfigC2SPacket packet);

    void sendToClient(ServerPlayer player, SyncServerConfigS2CPacket packet);

    void sendToServer(QueryAE2CountC2SPacket packet);

    void sendToServer(BuildModeHintC2SPacket packet);

    void sendToClient(ServerPlayer player, SyncAE2CountS2CPacket packet);

}
