package com.crescentine.trajanstanks.client;

import com.crescentine.trajanstanks.common.network.packet.ShellEntitySpawnPacket;
import com.crescentine.trajanstanks.common.registry.ModEntityTypes;
import com.crescentine.trajanstanks.client.model.TankEntityModel;
import com.crescentine.trajanstanks.client.renderer.TankEntityRenderer;
import com.crescentine.trajanstanks.common.TankMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class TankModClient implements ClientModInitializer {
    public static final KeyBinding STARTMOVING = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tankmod.startmoving", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_M, // The keycode of the key
            "category.tankmod.trajanstanks" // The translation key of the keybinding's category.
    ));

    public static final KeyBinding SHOOT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.tankmod.shoot", // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_RIGHT_ALT, // The keycode of the key
            "category.tankmod.trajanstanks" // The translation key of the keybinding's category.
    ));



    public void receiveEntityPacket() {
        ClientSidePacketRegistry.INSTANCE.register(PacketID, (ctx, byteBuf) -> {
            EntityType<?> et = Registry.ENTITY_TYPE.get(byteBuf.readVarInt());
            UUID uuid = byteBuf.readUuid();
            int entityId = byteBuf.readVarInt();
            Vec3d pos = ShellEntitySpawnPacket.PacketBufUtil.readVec3d(byteBuf);
            float pitch = ShellEntitySpawnPacket.PacketBufUtil.readAngle(byteBuf);
            float yaw = ShellEntitySpawnPacket.PacketBufUtil.readAngle(byteBuf);
            ctx.getTaskQueue().execute(() -> {
                if (MinecraftClient.getInstance().world == null)
                    throw new IllegalStateException("Tried to spawn entity in a null world!");
                Entity e = et.create(MinecraftClient.getInstance().world);
                if (e == null)
                    throw new IllegalStateException("Failed to create instance of entity \"" + Registry.ENTITY_TYPE.getId(et) + "\"!");
                e.updateTrackedPosition(pos);
                e.setPos(pos.x, pos.y, pos.z);
                e.setPitch(pitch);
                e.setYaw(yaw);
                e.setId(entityId);
                e.setUuid(uuid);
                MinecraftClient.getInstance().world.addEntity(entityId, e);
            });
        });
    }
    public static final Identifier PacketID = new Identifier(TankMod.MOD_ID, "spawn_packet");

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.SHELL_ENTITY_TYPE, FlyingItemEntityRenderer::new);
        receiveEntityPacket();
        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.TANK_ENTITY_TYPE, ctx ->
                new TankEntityRenderer(ctx, new TankEntityModel()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SHOOT.wasPressed()) {
                ClientPlayNetworking.send(new Identifier("shoot"), PacketByteBufs.empty());
            }
        });

    }
}