package org.teacon.slides.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.util.Utilities;

public class ProjectorImageInfoS2CPayload implements CustomPacketPayload {
   public static final Type<ProjectorImageInfoS2CPayload> ID = new Type(Slideshow.PACKET_TAG_UPDATE);
   private final BlockPos mPos;
   private final boolean bl0;
   private final String str0;
   private final boolean bl1;
   private final String str1;

   public ProjectorImageInfoS2CPayload(ProjectorBlockEntity entity) {
      this.mPos = entity.getBlockPos();
      this.bl0 = entity.mCFromID;
      this.str0 = entity.mCLocation;
      this.bl1 = entity.mCNextFromID;
      this.str1 = entity.mCNextLocation;
   }

   public ProjectorImageInfoS2CPayload(RegistryFriendlyByteBuf buffer) {
      this.mPos = buffer.readBlockPos();
      this.bl0 = buffer.readBoolean();
      this.str0 = buffer.readUtf();
      this.bl1 = buffer.readBoolean();
      this.str1 = buffer.readUtf();
   }

   public static void writeBuffer(ProjectorImageInfoS2CPayload payload, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(payload.mPos);
      buffer.writeBoolean(payload.bl0);
      buffer.writeUtf(payload.str0);
      buffer.writeBoolean(payload.bl1);
      buffer.writeUtf(payload.str1);
   }

   // Applied by the client only; kept here because it needs no client-only types
   public void applyTo(Level level) {
      if (level == null) {
         return;
      } else if (level.getBlockEntity(this.mPos) instanceof ProjectorBlockEntity entity1) {
         entity1.mCFromID = this.bl0;
         entity1.mCLocation = this.str0;
         entity1.mCNextFromID = this.bl1;
         entity1.mCNextLocation = this.str1;
         return;
      }

      Slideshow.LOGGER.debug(Utilities.MARKER, "Received illegal packet for image info in {}.", this.mPos);
   }

   public @NotNull Type<ProjectorImageInfoS2CPayload> type() {
      return ID;
   }
}
