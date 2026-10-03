package net.hulan.ivr;

import mtr.CreativeModeTabs;
import mtr.Registry;
import mtr.RegistryObject;
import mtr.item.ItemBlockEnchanted;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.DeferredRegisterHolder;
import mtr.mappings.ForgeUtilities;
import mtr.mappings.RegistryUtilities;
import net.hulan.ivr.item.ItemNPCSpawnEgg;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.hulan.ivr.platform.NPCPlatform;
import net.hulan.ivr.entity.IVRNPC;
import net.hulan.ivr.entity.NPC;
import net.hulan.ivr.entity.NPCs;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(IVR.MOD_ID)
public class IVRForgeMain {

    private static final DeferredRegisterHolder<Item> ITEMS = new DeferredRegisterHolder<>(IVR.MOD_ID, ForgeUtilities.registryGetItem());
    private static final DeferredRegisterHolder<Block> BLOCKS = new DeferredRegisterHolder<>(IVR.MOD_ID, ForgeUtilities.registryGetBlock());
    private static final DeferredRegisterHolder<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new DeferredRegisterHolder<>(IVR.MOD_ID, ForgeUtilities.registryGetBlockEntityType());
    private static final DeferredRegisterHolder<EntityType<?>> ENTITY_TYPES = new DeferredRegisterHolder<>(IVR.MOD_ID, ForgeUtilities.registryGetEntityType());

    static {
        IVR.init(
                IVRForgeMain::registerItem,
                IVRForgeMain::registerBlock,
                IVRForgeMain::registerBlock,
                IVRForgeMain::registerBlockEntityType,
                IVRForgeMain::registerEnchantedBlock);
        NPCPlatform.getInstance().registerEntities(IVRForgeMain::registerEntity);
    }

    public IVRForgeMain() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeUtilities.registerModEventBus(IVR.MOD_ID, eventBus);
        ITEMS.register();
        BLOCKS.register();
        BLOCK_ENTITY_TYPES.register();
        ENTITY_TYPES.register();
        eventBus.register(IVRForgeEventBus.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            eventBus.register(NPCPlatform.getInstance().getClass());
        }
    }

    private static void registerItem(String path, RegistryObject<Item> item) {
        ITEMS.register(path, () -> {
            final Item itemObject = item.get();
            if (itemObject instanceof ItemWithCreativeTabBase) {
                Registry.registerCreativeModeTab(((ItemWithCreativeTabBase) itemObject).creativeModeTab.resourceLocation, itemObject);
            } else if (itemObject instanceof ItemWithCreativeTabBase.ItemPlaceOnWater) {
                Registry.registerCreativeModeTab(((ItemWithCreativeTabBase.ItemPlaceOnWater) itemObject).creativeModeTab.resourceLocation, itemObject);
            } else if (itemObject instanceof ItemNPCSpawnEgg npcSpawnEgg) {
                Registry.registerCreativeModeTab(npcSpawnEgg.creativeModeTab.resourceLocation, itemObject);
            }
            return itemObject;
        });
    }

    private static void registerBlock(String path, RegistryObject<Block> block) {
        BLOCKS.register(path, block::get);
    }

    private static void registerBlock(String path, RegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTabWrapper) {
        registerBlock(path, block);
        ITEMS.register(path, () -> {
            final BlockItem blockItem = new BlockItem(block.get(), RegistryUtilities.createItemProperties(creativeModeTabWrapper::get));
            Registry.registerCreativeModeTab(creativeModeTabWrapper.resourceLocation, blockItem);
            return blockItem;
        });
    }

    private static void registerEnchantedBlock(String path, RegistryObject<Block> block, CreativeModeTabs.Wrapper creativeModeTab) {
        registerBlock(path, block);
        ITEMS.register(path, () -> {
            final ItemBlockEnchanted itemBlockEnchanted = new ItemBlockEnchanted(block.get(), RegistryUtilities.createItemProperties(creativeModeTab::get));
            Registry.registerCreativeModeTab(creativeModeTab.resourceLocation, itemBlockEnchanted);
            return itemBlockEnchanted;
        });
    }

    private static void registerBlockEntityType(String path, RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>> blockEntityType) {
        BLOCK_ENTITY_TYPES.register(path, blockEntityType::get);
    }

    private static void registerEntity(String path, RegistryObject<? extends EntityType<? extends IVRNPC>> entity) {
        ENTITY_TYPES.register(path, entity::get);
    }

    public static class IVRForgeEventBus {

        @SubscribeEvent
        public static void onEntityAttributeCreation(final EntityAttributeCreationEvent event) {
            event.put(NPCs.TICKETS.get(), NPC.createAttributes().build());
            event.put(NPCs.FARE_ADJUSTMENTS.get(), NPC.createAttributes().build());
        }

        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            event.enqueueWork(IVRClient::init);
        }
    }
}
