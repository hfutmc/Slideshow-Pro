package org.teacon.slides.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.teacon.slides.Slideshow;
import org.teacon.slides.item.FlipperItem;
import org.teacon.slides.projector.ImageSourceScreen;

// Holds the client-only behaviour of shared classes, so they never reference client types themselves
public class ClientUtilities {
//#if MC >= 12105
   //$$ private static boolean attackKeyDown = false;

   //$$ public static void clientTick() {
      //$$ Minecraft mc = Minecraft.getInstance();
      //$$ if (mc.player != null) {
         //$$ int slot = mc.player.getInventory().getSelectedSlot();
         //$$ ItemStack stack = mc.player.getInventory().getItem(slot);
         //$$ if (stack.is(Slideshow.FLIPPER_ITEM) && attackKeyDown != mc.options.keyAttack.isDown()) {
            //$$ attackKeyDown = !attackKeyDown;
            //$$ if (attackKeyDown) {
               //$$ FlipperItem.sendServerFlipBack(slot);
            //$$ }
         //$$ }
      //$$ }
   //$$ }
//#else
   private static boolean attackKeyDown = false;

   public static void onInventoryTick(int slot) {
      if (attackKeyDown != Minecraft.getInstance().options.keyAttack.isDown()) {
         attackKeyDown = !attackKeyDown;
         if (attackKeyDown) {
            FlipperItem.sendServerFlipBack(slot);
         }
      }
   }
//#endif

   public static void clientSetScreen(InteractionHand hand, ItemStack itemStack) {
//#if MC >= 26_02_00
      //$$ Minecraft.getInstance().setScreenAndShow(new ImageSourceScreen(hand, itemStack));
//#else
      Minecraft.getInstance().setScreen(new ImageSourceScreen(hand, itemStack));
//#endif
   }
}
