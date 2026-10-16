package org.teacon.slides;

import net.fabricmc.api.ClientModInitializer;
//#if MC >= 12111
//$$ import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
//$$ import net.minecraft.resources.Identifier;
//$$ import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
//#else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
//#endif
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
//#if MC >= 12108
//#if MC < 26_00_00
//$$ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#endif
//#endif
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.teacon.slides.config.Config;
//#if MC < 12105
import org.teacon.slides.item.FlipperItem;
//#endif
import org.teacon.slides.network.ProjectorImageInfoS2CPayload;
import org.teacon.slides.projector.ProjectorScreen;
import org.teacon.slides.renderer.ProjectorRenderer;
import org.teacon.slides.renderer.SlideState;
import org.teacon.slides.texture.WebPDecoder;
import org.teacon.slides.util.ClientUtilities;
import org.teacon.slides.util.RegistryClient;

public class SlideshowClient implements ClientModInitializer {
   public void onInitializeClient() {
      RegistryClient.registerBlockEntityRenderer(Slideshow.PROJECTOR_BLOCK_ENTITY, ctx -> new ProjectorRenderer());
//#if MC >= 12108
//#if MC < 26_00_00
      //$$ RegistryClient.registerBlockRenderType(Slideshow.PROJECTOR_BLOCK, ChunkSectionLayer.CUTOUT);
//#else
      //$$ // 26.x takes the chunk render layer from the block model's "render_type", so no registration is needed there
//#endif
//#else
      RegistryClient.registerBlockRenderType(Slideshow.PROJECTOR_BLOCK, RenderType.cutout());
//#endif
      RegistryClient.registerTickEvent(SlideState::tick);
//#if MC >= 12105
      //$$ RegistryClient.registerTickEvent(minecraftClient -> ClientUtilities.clientTick());
//#else
      FlipperItem.clientInventoryTick = ClientUtilities::onInventoryTick;
//#endif
      RegistryClient.registerClientStartedEvent(minecraftClient -> Config.refreshProperties());
      RegistryClient.registerClientStoppingEvent(SlideState::onPlayerLeft);
      RegistryClient.registerNetworkReceiver(ProjectorImageInfoS2CPayload.ID,
         (payload, context) -> payload.applyTo(context.client().level));
      MenuScreens.register(Slideshow.PROJECTOR_SCREEN_HANDLER, ProjectorScreen::new);
//#if MC >= 26_00_00
      //$$ ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("slide_show", "client_reload"), new ResourceManagerReloadListener() {
         //$$ public void onResourceManagerReload(ResourceManager resourceManager) {
            //$$ SlideState.clearCacheID();
            //$$ Config.refreshProperties();
         //$$ }
      //$$ });
//#elseif MC >= 12111
      //$$ ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(Identifier.fromNamespaceAndPath("slide_show", "client_reload"), new ResourceManagerReloadListener() {
         //$$ public void onResourceManagerReload(ResourceManager resourceManager) {
            //$$ SlideState.clearCacheID();
            //$$ Config.refreshProperties();
         //$$ }
      //$$ });
//#else
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         private final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("slide_show", "client_reload");

         public void onResourceManagerReload(ResourceManager resourceManager) {
            SlideState.clearCacheID();
            Config.refreshProperties();
         }

         public ResourceLocation getFabricId() {
            return this.id;
         }
      });
//#endif
      WebPDecoder.init();
   }
}
