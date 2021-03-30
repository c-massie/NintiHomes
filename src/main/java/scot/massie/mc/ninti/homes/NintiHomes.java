package scot.massie.mc.ninti.homes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("nintihomes")
public class NintiHomes
{
    public static final String PERMISSION_HOMES_ADD             = "ninti.homes.personal.add";
    public static final String PERMISSION_HOMES_ADD_INWORLD     = "ninti.homes.personal.add.inworld";
    public static final String PERMISSION_HOMES_ADD_INZONE      = "ninti.homes.personal.add.inzone";
    public static final String PERMISSION_HOMES_TP              = "ninti.homes.personal.tp";
    public static final String PERMISSION_HOMES_TP_TOWORLD      = "ninti.homes.personal.tp.toworld";
    public static final String PERMISSION_HOMES_TP_TOZONE       = "ninti.homes.personal.tp.tozone";
    public static final String PERMISSION_HOMES_LOCATE          = "ninti.homes.personal.locate";
    public static final String PERMISSION_HOMES_ADMIN_ADD       = "ninti.homes.admin.add";
    public static final String PERMISSION_HOMES_ADMIN_REMOVE    = "ninti.homes.admin.remove";
    public static final String PERMISSION_HOMES_ADMIN_TP        = "ninti.homes.admin.tp";
    public static final String PERMISSION_HOMES_ADMIN_LOCATE    = "ninti.homes.admin.locate";

    /*

    Permission notes:

    ninti.homes.personal.add (and sub-permissions) accepts a positive integer permission argument. This argument is the
    number of homes someone with this permission may have.

    ninti.homes.personal.tp (and sub-permissions) accepts a number or equation defining the cost to teleport and a unit
    the cost is in. (e.g. experience points, experience levels, or some other unit.) When teleportation is costed, it
    should fire a cancellable event carrying the cost and the name of the unit - this will allow other mods to remove
    the cost using their own units if they recognise the unit specified. (e.g. with a currency mod, tp may be defined
    with "gp" as the unit)

    ninti.homes.locate allows players to print their location and the location of the home in terms of coördinates, with
    the distance between them.

    The admin permissions have no limits nor costs.

     */

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public NintiHomes()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event)
    {
        // do something when the server starts
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {

    }
}
