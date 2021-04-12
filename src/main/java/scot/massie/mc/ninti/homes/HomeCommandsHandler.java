package scot.massie.mc.ninti.homes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.currencies.Currencies;
import scot.massie.mc.ninti.core.exceptions.MissingPermissionException;
import scot.massie.mc.ninti.core.exceptions.NoSuchWorldException;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.command.Commands.*;
import static scot.massie.mc.ninti.core.PluginUtils.*;

public class HomeCommandsHandler
{
    /*

    home
    home [home name]
    delhome
    delhome [home name]
    sethome
    sethome [home name]
    gettphomecost
    gettphomecost [home name]
    listhomes

    homes list
    homes list [username]
    homes create [username]
    homes delete [username] [homename]
    homes delete [username]
    homes tpme [username] [homename]
    homes tp [username] [homename]
    homes tptoother [username to tp] [username with home] [homename]

     */

    private static final String noSuggestionsSuggestion = "(no suggestions)";

    private static final SuggestionProvider<CommandSource> playersOnlineSuggestionProvider
            = (context, builder) ->
    {
        for(String s : getServer().getPlayerList().getOnlinePlayerNames())
            builder.suggest(s);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> playersWithHomesSuggestionProvider
            = (context, builder) ->
    {
        for(UUID id : Homes.getPlayersWithHomes())
        {
            String username = UsernameCache.getLastKnownUsername(id);

            if(username != null)
                builder.suggest(username);
        }

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> homesPlayerHasSuggestionProvider
            = (context, builder) ->
    {
        if(!(context.getSource().getEntity() instanceof PlayerEntity))
        {
            builder.suggest(noSuggestionsSuggestion);
            return builder.buildFuture();
        }

        Map<String, EntityLocation> homes = Homes.getHomes(context.getSource().getEntity().getUniqueID());

        if(homes.isEmpty())
            builder.suggest(noSuggestionsSuggestion);
        else
            for(String homeName : homes.keySet())
                builder.suggest(homeName);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> homesAnotherPlayerHasSuggestionProvider
            = (context, builder) ->
    {
        String playerName = StringArgumentType.getString(context, "username");
        UUID playerId = getLastKnownUUIDOfPlayer(playerName);

        if(playerId == null)
        {
            builder.suggest(noSuggestionsSuggestion);
            return builder.buildFuture();
        }

        Map<String, EntityLocation> homes = Homes.getHomes(context.getSource().getEntity().getUniqueID());

        if(homes.isEmpty())
            builder.suggest(noSuggestionsSuggestion);
        else
            for(String homeName : homes.keySet())
                builder.suggest(homeName);

        return builder.buildFuture();
    };

    private static final Predicate<CommandSource> srcIsPlayer = src -> src.getEntity() instanceof ServerPlayerEntity;

    private static boolean hasPerm(CommandSource src, String... perms)
    { return Permissions.commandSourceHasPermission(src, perms); }

    private static boolean hasAnyPermUnder(CommandSource src, String... perms)
    { return Permissions.commandSourceHasPermission(src, perms); }

    private static boolean hasAnyHomes(CommandSource src)
    {
        if(!(src.getEntity() instanceof PlayerEntity))
            return false;

        return Homes.hasHomes(src.getEntity().getUniqueID());
    }

    public static final LiteralArgumentBuilder<CommandSource> homeCommand
            = literal("home")
                    .requires(src -> hasAnyPermUnder(src, NintiHomes.PERMISSION_HOMES_TP))
                    .requires(srcIsPlayer)
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdHome_specified))
                    .executes(HomeCommandsHandler::cmdHome_default);

    public static final LiteralArgumentBuilder<CommandSource> sethomeCommand
            = literal("sethome")
                    .requires(src -> hasAnyPermUnder(src, NintiHomes.PERMISSION_HOMES_ADD))
                    .requires(srcIsPlayer)
                    .then(argument("home name", StringArgumentType.word())
                            .executes(HomeCommandsHandler::cmdSethome_specified))
                    .executes(HomeCommandsHandler::cmdSethome_default);

    public static final LiteralArgumentBuilder<CommandSource> delhomeCommand
            = literal("delhome")
                    .requires(src -> hasAnyHomes(src))
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdDelhome_specified))
                    .executes(HomeCommandsHandler::cmdDelhome_default);

    public static final LiteralArgumentBuilder<CommandSource> gettphomecostCommand
            = literal("gettphomecost")
                    .requires(src -> hasAnyPermUnder(src, NintiHomes.PERMISSION_HOMES_TP))
                    .requires(srcIsPlayer)
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdGettphomecost_specified))
                    .executes(HomeCommandsHandler::cmdGettphomecost_default);

    public static final LiteralArgumentBuilder<CommandSource> listhomesCommand
            = literal("listhomes")
                      .requires(srcIsPlayer)
                      .executes(HomeCommandsHandler::cmdListhomes);

    public static final LiteralArgumentBuilder<CommandSource> homesCommand
            = literal("homes")
                    .requires(src -> hasAnyPermUnder(src, NintiHomes.PERMISSION_HOMES_ADMIN))
                    .then(literal("list")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_READ))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .executes(HomeCommandsHandler::cmdHomes_list_user))
                            .executes(HomeCommandsHandler::cmdHomes_list_all))
                    .then(literal("create")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_ADD))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .executes(HomeCommandsHandler::cmdHomes_create_here)))
                    .then(literal("delete")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_REMOVE))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_delete_one))
                                    .executes(HomeCommandsHandler::cmdHomes_delete_all)))
                    .then(literal("tpme")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_TP_ME))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_tpme))))
                    .then(literal("tp")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_TP_THEM))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_tp))))
                    .then(literal("tptoother")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_TP_OTHER))
                            .then(argument("username to tp", StringArgumentType.word())
                                    .suggests(playersOnlineSuggestionProvider)
                                    .then(argument("username", StringArgumentType.word())
                                            .suggests(playersWithHomesSuggestionProvider)
                                            .then(argument("home name", StringArgumentType.word())
                                                    .suggests(homesAnotherPlayerHasSuggestionProvider)
                                                    .executes(HomeCommandsHandler::cmdHomes_tptoother)))));

    private static int cmdHome_default(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        String homeName = "";

        try
        { Homes.requestTpPlayerToHome(player, homeName); }
        catch(Homes.NoSuchHomeException e)
        { sendMessage(cmdContext, "Could not find a default home."); }
        catch(NoSuchWorldException e)
        { sendMessage(cmdContext, "Could not find the world: " + e.getWorldId()); }
        catch(MissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to teleport to that home."); }
        catch(Currencies.UnrecognisedCurrencyException e)
        { sendMessage(cmdContext, "Unrecognised currency in costs: " + e.getCurrencyName()); }
        catch(Homes.CouldNotAffordToTpHomeException e)
        {
            String msg = "You cannot afford to teleport to that home. Required: ";

            for(Map.Entry<String, Double> i : e.getCosts().entrySet().stream().sorted().collect(Collectors.toList()))
                msg += "\n - " + i.getKey() + ": " + i.getValue();

            sendMessage(cmdContext, msg);
        }

        return 1;
    }

    private static int cmdHome_specified(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        String homeName = StringArgumentType.getString(cmdContext, "home name");

        try
        { Homes.requestTpPlayerToHome(player, homeName); }
        catch(Homes.NoSuchHomeException e)
        { sendMessage(cmdContext, "Could not find a home by the name: " + e.getHomeName()); }
        catch(NoSuchWorldException e)
        { sendMessage(cmdContext, "Could not find the world: " + e.getWorldId()); }
        catch(MissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to teleport to that home."); }
        catch(Currencies.UnrecognisedCurrencyException e)
        { sendMessage(cmdContext, "Unrecognised currency in costs: " + e.getCurrencyName()); }
        catch(Homes.CouldNotAffordToTpHomeException e)
        {
            String msg = "You cannot afford to teleport to that home. Required: ";

            for(Map.Entry<String, Double> i : e.getCosts().entrySet().stream().sorted().collect(Collectors.toList()))
                msg += "\n - " + i.getKey() + ": " + i.getValue();

            sendMessage(cmdContext, msg);
        }

        return 1;
    }

    private static int cmdDelhome_default(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        String homeName = "";

        assert player != null;
        if(Homes.deleteHome(player.getUniqueID(), homeName) != null)
            sendMessage(cmdContext, "Home deleted!");
        else
            sendMessage(cmdContext, "Did not have a default home to delete.");

        return 1;
    }

    private static int cmdDelhome_specified(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        String homeName = StringArgumentType.getString(cmdContext, "home name");

        if(Homes.deleteHome(player.getUniqueID(), homeName) != null)
            sendMessage(cmdContext, "Home \"" + homeName + "\" deleted!");
        else
            sendMessage(cmdContext, "Did not have a home by the name " + homeName +" to delete.");

        return 1;
    }

    private static int cmdSethome(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;

        try
        { Homes.requestSetHome(player, homeName); }
        catch(MissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to set a home there."); }
        catch(Homes.WorldHomeCapReachedException e)
        { sendMessage(cmdContext, "You cannot set any more homes in the world " + e.getWorldId()); }
        catch(Homes.ZoneHomeCapReachedException e)
        { sendMessage(cmdContext, "You cannot set any more homes in the zone " + e.getZoneName()); }

        return 1;
    }

    private static int cmdSethome_default(CommandContext<CommandSource> cmdContext)
    { return cmdSethome(cmdContext, ""); }

    private static int cmdSethome_specified(CommandContext<CommandSource> cmdContext)
    { return cmdSethome(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdGettphomecost_default(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;

        try
        {
            Map<String, Double> costToTp = Homes.getCostToTp(player, "");
            String message = "Cost to teleport to home:";

            for(Map.Entry<String, Double> entry : costToTp.entrySet()
                                                          .stream()
                                                          .sorted(Map.Entry.comparingByKey())
                                                          .collect(Collectors.toList()))
            { message += "\n - " + entry.getKey() + ": " + entry.getValue(); }

            sendMessage(cmdContext, message);
        }
        catch(Homes.NoSuchHomeException e)
        { sendMessage(cmdContext, "Did not have a default home to get the cost to teleport to."); }
        catch(NoSuchWorldException e)
        { sendMessage(cmdContext, "The default home's world does not exist: " + e.getWorldId()); }
        catch(MissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to teleport to that home."); }

        return 1;
    }

    private static int cmdGettphomecost_specified(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        String homeName = StringArgumentType.getString(cmdContext, "home name");

        try
        {
            Map<String, Double> costToTp = Homes.getCostToTp(player, "");
            String message = "Cost to teleport to the home " + homeName + ":";

            for(Map.Entry<String, Double> entry : costToTp.entrySet()
                                                          .stream()
                                                          .sorted(Map.Entry.comparingByKey())
                                                          .collect(Collectors.toList()))
            { message += "\n - " + entry.getKey() + ": " + entry.getValue(); }

            sendMessage(cmdContext, message);
        }
        catch(Homes.NoSuchHomeException e)
        { sendMessage(cmdContext, "Did not have a home by the name " + homeName + " to get the cost to teleport to."); }
        catch(NoSuchWorldException e)
        { sendMessage(cmdContext, "That home's (" + homeName + ") world does not exist: " + e.getWorldId()); }
        catch(MissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to teleport to the home " + homeName); }

        return 1;
    }

    private static int cmdListhomes(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_delete_one(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_delete_all(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_create_here(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_tpme(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_tp(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_tptoother(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_list_all(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHomes_list_user(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
