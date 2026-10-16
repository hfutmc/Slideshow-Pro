package org.teacon.slides.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 12111
//$$ import net.minecraft.server.permissions.Permission.HasCommandLevel;
//$$ import net.minecraft.server.permissions.PermissionLevel;
//#endif

/**
 * Permission nodes of this mod.
 * <p>
 * The node identifiers use {@code /} as separator, which permission providers such as LuckPerms
 * expose as {@code .}: {@code slideshow:command/image} is the node {@code slideshow.command.image}.
 * Checks go through the Fabric permission API, and a provider is optional - without LuckPerms every
 * check falls back to the vanilla operator level configured in slideshow.json, so the previous
 * behaviour is preserved.
 */
public final class PermissionCompat {
   public static final ResourceLocation COMMAND_IMAGE = node("command/image");
   public static final ResourceLocation COMMAND_PROJECTOR = node("command/projector");
   public static final ResourceLocation COMMAND_IMAGEINFO = node("command/imageinfo");
   public static final ResourceLocation PROJECTOR_USE = node("projector/use");

   private PermissionCompat() {
   }

   private static ResourceLocation node(String path) {
      return ResourceLocation.fromNamespaceAndPath("slideshow", path);
   }

   public static boolean check(CommandSourceStack source, ResourceLocation node, int fallbackOpLevel) {
//#if MC >= 26_01_02
      //$$ return source.checkPermission(node, PermissionLevel.byId(fallbackOpLevel));
//#elseif MC >= 12111
      //$$ return source.permissions().hasPermission(new HasCommandLevel(PermissionLevel.byId(fallbackOpLevel)));
//#else
      return source.hasPermission(fallbackOpLevel);
//#endif
   }

   public static boolean check(ServerPlayer player, ResourceLocation node, int fallbackOpLevel) {
//#if MC >= 26_01_02
      //$$ return player.checkPermission(node, PermissionLevel.byId(fallbackOpLevel));
//#elseif MC >= 12111
      //$$ return player.permissions().hasPermission(new HasCommandLevel(PermissionLevel.byId(fallbackOpLevel)));
//#else
      return player.hasPermissions(fallbackOpLevel);
//#endif
   }
}
