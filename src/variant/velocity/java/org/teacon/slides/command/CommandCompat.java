package org.teacon.slides.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;

/**
 * Command argument flavour of the Velocity-compatible build.
 * A Velocity proxy decodes the clientbound command tree with its own fixed table of vanilla
 * argument types and fails on anything else, which is why this flavour drops the mod's own
 * argument type instead of registering it.
 */
public final class CommandCompat {
   private CommandCompat() {
   }

   public static void registerArgumentType() {
   }

   public static ArgumentType<String> urlArgument() {
      return StringArgumentType.greedyString();
   }

   public static String url(CommandContext<CommandSourceStack> context) {
      return StringArgumentType.getString(context, "url_string");
   }
}
