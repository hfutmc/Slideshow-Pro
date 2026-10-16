package org.teacon.slides.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * Command argument flavour of the regular Fabric build.
 * The projector image command keeps the mod's own unsigned-message argument type, so a URL
 * reaches the command handler exactly as it was typed, without Brigadier unquoting it.
 */
public final class CommandCompat {
   private CommandCompat() {
   }

   public static void registerArgumentType() {
      UnsignedMessageArgumentType.register();
   }

   public static ArgumentType<String> urlArgument() {
      return UnsignedMessageArgumentType.message();
   }

   public static String url(CommandContext<CommandSourceStack> context) {
      return UnsignedMessageArgumentType.getMessage(context, "url_string");
   }
}
