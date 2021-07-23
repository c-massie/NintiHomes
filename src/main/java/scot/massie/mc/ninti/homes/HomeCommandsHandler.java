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
import scot.massie.mc.ninti.core.exceptions.NoSuchWorldException;
import scot.massie.mc.ninti.core.exceptions.PlayerMissingPermissionException;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;
import scot.massie.mc.ninti.homes.Exceptions.CouldNotAffordToTpHomeException;
import scot.massie.mc.ninti.homes.Exceptions.ServerHomeCapReachedException;
import scot.massie.mc.ninti.homes.Exceptions.WorldHomeCapReachedException;
import scot.massie.mc.ninti.homes.Exceptions.ZoneHomeCapReachedException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /*

    TO DO:
     - Add /homes tp|tpme|tptoother variations for the default home.
     - Store the default home in a variable/constant.

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

        PlayerHomesList homeList = Homes.getForIfPresent(context.getSource().getEntity().getUniqueID());

        if(homeList == null || homeList.isEmpty())
            builder.suggest(noSuggestionsSuggestion);
        else
            for(String homeName : homeList.getHomeNames())
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

        PlayerHomesList homeList = Homes.getForIfPresent(playerId);

        if(homeList == null || homeList.isEmpty())
            builder.suggest(noSuggestionsSuggestion);
        else
            for(String homeName : homeList.getHomeNames())
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

        return Homes.hasAny(src.getEntity().getUniqueID());
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

    @SuppressWarnings("Convert2MethodRef") // ".requires(src -> hasAnyHomes(src))" makes more sense as a lambda.
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
                            .requires(srcIsPlayer)
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_ADD))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersOnlineSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .executes(HomeCommandsHandler::cmdHomes_create_here_named))
                                    .executes(HomeCommandsHandler::cmdHomes_create_here_default)))
                    .then(literal("delete")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_REMOVE))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_delete_specified))
                                    .executes(HomeCommandsHandler::cmdHomes_delete_default)))
                    .then(literal("deleteall")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_REMOVE))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .executes(HomeCommandsHandler::cmdHomes_deleteall)))
                    .then(literal("tpme")
                            .requires(srcIsPlayer)
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_TP_ME))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_tpme))))
                    .then(literal("tp")
                            .requires(src -> hasPerm(src, NintiHomes.PERMISSION_HOMES_ADMIN_TP_THEM))
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersOnlineSuggestionProvider)
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

    private static String homeToString(PlayerHome home)
    {
        StringBuilder sb = new StringBuilder(home.getName().isEmpty() ? "" : home.getName() + " at ");
        EntityLocation loc = home.getLocation();

        sb.append("(")
          .append((int)loc.getX()).append(", ")
          .append((int)loc.getY()).append(", ")
          .append((int)loc.getZ()).append(") in ")
          .append(loc.getWorldId()).append(".");

        return sb.toString();
    }

    private static int cmdHome(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        PlayerHomesList phl = Homes.getForIfPresent(player);
        PlayerHome home;

        if((phl == null) || ((home = phl.getHome(homeName)) == null))
        {
            if(homeName.isEmpty())
                sendMessage(cmdContext, "Could not find a default home.");
            else
                sendMessage(cmdContext, "Could not find a home by the name " + homeName + ".");

            return 1;
        }

        try
        { home.requestTpHere(); }
        catch(NoSuchWorldException e)
        { sendMessage(cmdContext, "Could not find the world " + e.getWorldId() + "."); }
        catch(PlayerMissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to teleport to that home."); }
        catch(Currencies.UnrecognisedCurrencyException e)
        { sendMessage(cmdContext, "Unrecognised currency in costs: " + e.getCurrencyName()); }
        catch(CouldNotAffordToTpHomeException e)
        {
            StringBuilder msgBuilder = new StringBuilder("You cannot afford to teleport to that home. Required: ");

            for(Map.Entry<String, Double> entry : e.getCosts()
                                                   .entrySet()
                                                   .stream()
                                                   .sorted(Map.Entry.comparingByKey())
                                                   .collect(Collectors.toList()))
            { msgBuilder.append("\n - ").append(entry.getKey()).append(": ").append(entry.getValue()); }

            sendMessage(cmdContext, msgBuilder.toString());
        }

        return 1;
    }

    private static int cmdHome_default(CommandContext<CommandSource> cmdContext)
    { return cmdHome(cmdContext, ""); }

    private static int cmdHome_specified(CommandContext<CommandSource> cmdContext)
    { return cmdHome(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdDelhome(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        PlayerHomesList phl = Homes.getForIfPresent(player);

        if((phl != null) && (phl.deleteHome(homeName) != null))
            sendMessage(cmdContext, homeName.isEmpty() ? "Home deleted!" : "Home \"" + homeName + "\" deleted!");
        else
            sendMessage(cmdContext, homeName.isEmpty()
                                            ? "Did not have a default home to delete."
                                            : "Did not have a home by the name \"" + homeName + "\" to delete.");

        return 1;
    }

    private static int cmdDelhome_default(CommandContext<CommandSource> cmdContext)
    { return cmdDelhome(cmdContext, ""); }

    private static int cmdDelhome_specified(CommandContext<CommandSource> cmdContext)
    { return cmdDelhome(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdSethome(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;

        try
        { Homes.getFor(player).requestSetHome(homeName); }
        catch(PlayerMissingPermissionException e)
        { sendMessage(cmdContext, "You do not have permission to set a home there."); }
        catch(ServerHomeCapReachedException e)
        { sendMessage(cmdContext, "You cannot set as many homes as that."); }
        catch(WorldHomeCapReachedException e)
        { sendMessage(cmdContext, "You cannot set as many homes as that in the world " + e.getWorldId()); }
        catch(ZoneHomeCapReachedException e)
        { sendMessage(cmdContext, "You cannot set as many homes as that in the zone " + e.getZoneName()); }

        return 1;
    }

    private static int cmdSethome_default(CommandContext<CommandSource> cmdContext)
    { return cmdSethome(cmdContext, ""); }

    private static int cmdSethome_specified(CommandContext<CommandSource> cmdContext)
    { return cmdSethome(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdGettphomecost(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        PlayerHomesList phl = Homes.getForIfPresent(player);
        PlayerHome home;

        if((phl == null) || ((home = phl.getHome(homeName)) == null))
        {
            if(homeName.isEmpty())
                sendMessage(cmdContext, "Did not have a default home to get the cost to teleport to.");
            else
                sendMessage(cmdContext, "Did not have a home by the name " + homeName + " to get the cost to teleport"
                                        + "to.");

            return 1;
        }

        try
        {
            Map<String, Double> costsToTp = home.requestCostsToTpHere();
            StringBuilder msg = new StringBuilder("Cost to teleport to home: ");

            for(Map.Entry<String, Double> entry : costsToTp.entrySet()
                                                           .stream()
                                                           .sorted(Map.Entry.comparingByKey())
                                                           .collect(Collectors.toList()))
            { msg.append("\n - ").append(entry.getKey()).append(": ").append(entry.getValue()); }

            sendMessage(cmdContext, msg.toString());
        }
        catch(PlayerMissingPermissionException e)
        {
            if(homeName.isEmpty())
                sendMessage(cmdContext, "You do not have permission to teleport to your default home.");
            else
                sendMessage(cmdContext, "You do not have permission to teleport to that home.");
        }

        return 1;
    }

    private static int cmdGettphomecost_default(CommandContext<CommandSource> cmdContext)
    { return cmdGettphomecost(cmdContext, ""); }

    private static int cmdGettphomecost_specified(CommandContext<CommandSource> cmdContext)
    { return cmdGettphomecost(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdListhomes(CommandContext<CommandSource> cmdContext)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        PlayerHomesList phl = Homes.getForIfPresent(player);

        if((phl == null) || (phl.isEmpty()))
        {
            sendMessage(cmdContext, "You do not have any homes.");
            return 1;
        }

        StringBuilder msg = new StringBuilder("Homes: ");
        List<PlayerHome> homes = new ArrayList<>(phl.getHomes());
        homes.sort(Comparator.comparing(PlayerHome::getName));

        for(PlayerHome home : homes)
            msg.append("\n - ").append(homeToString(home));

        sendMessage(cmdContext, msg.toString());
        return 1;
    }

    private static int cmdHomes_list_all(CommandContext<CommandSource> cmdContext)
    {
        StringBuilder msgBuilder = new StringBuilder("Homes:");

        for(PlayerHomesList phl : Homes.getAllPlayerHomeLists())
        {
            if(phl.isEmpty())
                continue;

            msgBuilder.append("\n - ").append(phl.getPlayerName());

            List<PlayerHome> homes = new ArrayList<>(phl.getHomes());
            homes.sort(Comparator.comparing(PlayerHome::getName));

            for(PlayerHome home : homes)
                msgBuilder.append("\n - - ").append(homeToString(home));
        }

        sendMessage(cmdContext, msgBuilder.toString());
        return 1;
    }

    private static int cmdHomes_list_user(CommandContext<CommandSource> cmdContext)
    {
        String username = StringArgumentType.getString(cmdContext, "username");
        UUID playerId = getLastKnownUUIDOfPlayer(username);

        if(playerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + username);
            return 1;
        }

        PlayerHomesList phl = Homes.getForIfPresent(playerId);

        if(phl == null || phl.isEmpty())
        {
            sendMessage(cmdContext, username + " doesn't have any homes.");
            return 1;
        }

        StringBuilder msgBuilder = new StringBuilder("Homes of ").append(username).append(":");
        ArrayList<PlayerHome> homes = new ArrayList<>(phl.getHomes());
        homes.sort(Comparator.comparing(PlayerHome::getName));

        for(PlayerHome home : homes)
            msgBuilder.append("\n - ").append(homeToString(home));

        sendMessage(cmdContext, msgBuilder.toString());
        return 1;
    }

    private static int cmdHomes_create_here(CommandContext<CommandSource> cmdContext, String homeName)
    {
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        String usernameCreatingFor = StringArgumentType.getString(cmdContext, "username");
        UUID playerIdCreatingFor = getLastKnownUUIDOfPlayer(usernameCreatingFor);

        if(playerIdCreatingFor == null)
        {
            sendMessage(cmdContext, "No known player by the name " + usernameCreatingFor);
            return 1;
        }

        Homes.getFor(playerIdCreatingFor).setHome(homeName, new EntityLocation(player));
        sendMessage(cmdContext, "Home created!");
        return 1;
    }

    private static int cmdHomes_create_here_named(CommandContext<CommandSource> cmdContext)
    { return cmdHomes_create_here(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdHomes_create_here_default(CommandContext<CommandSource> cmdContext)
    { return cmdHomes_create_here(cmdContext, ""); }

    private static int cmdHomes_delete(CommandContext<CommandSource> cmdContext, String homeName)
    {
        String username = StringArgumentType.getString(cmdContext, "username");
        UUID playerId = getLastKnownUUIDOfPlayer(username);

        if(playerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + username);
            return 1;
        }

        PlayerHomesList phl = Homes.getForIfPresent(playerId);

        if((phl != null) && (phl.deleteHome(homeName) != null))
            sendMessage(cmdContext, "Home deleted!");
        else
        {
            if(homeName.isEmpty())
                sendMessage(cmdContext, username + " had no default home.");
            else
                sendMessage(cmdContext, username + " had no home by the name " + homeName + ".");
        }

        return 1;
    }

    private static int cmdHomes_delete_default(CommandContext<CommandSource> cmdContext)
    { return cmdHomes_delete(cmdContext, ""); }

    private static int cmdHomes_delete_specified(CommandContext<CommandSource> cmdContext)
    { return cmdHomes_delete(cmdContext, StringArgumentType.getString(cmdContext, "home name")); }

    private static int cmdHomes_deleteall(CommandContext<CommandSource> cmdContext)
    {
        String username = StringArgumentType.getString(cmdContext, "username");
        UUID playerId = getLastKnownUUIDOfPlayer(username);

        if(playerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + username);
            return 1;
        }

        PlayerHomesList phl = Homes.getForIfPresent(playerId);

        if((phl != null) && (!phl.isEmpty()))
        {
            phl.clear();
            sendMessage(cmdContext, "All homes of " + username + " deleted!");
        }
        else
            sendMessage(cmdContext, username + " had no homes to delete.");

        return 1;
    }

    private static int cmdHomes_tpme(CommandContext<CommandSource> cmdContext)
    {
        String homeOwningPlayerName = StringArgumentType.getString(cmdContext, "username");
        UUID homeOwningPlayerId = getLastKnownUUIDOfPlayer(homeOwningPlayerName);

        if(homeOwningPlayerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + homeOwningPlayerName);
            return 1;
        }

        String homeName = StringArgumentType.getString(cmdContext, "home name");
        ServerPlayerEntity player = (ServerPlayerEntity)cmdContext.getSource().getEntity();
        assert player != null;
        PlayerHomesList phl = Homes.getForIfPresent(homeOwningPlayerId);
        PlayerHome home;

        if((phl != null) && ((home = phl.getHome(homeName)) != null))
            home.tpHere(player);
        else
            sendMessage(cmdContext, homeOwningPlayerName + " does not have a home by the name " + homeName);

        return 1;
    }

    private static int cmdHomes_tp(CommandContext<CommandSource> cmdContext)
    {
        String playerName = StringArgumentType.getString(cmdContext, "username");
        UUID playerId = getLastKnownUUIDOfPlayer(playerName);

        if(playerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + playerName);
            return 1;
        }

        ServerPlayerEntity player = getOnlinePlayer(playerId);

        if(player == null)
        {
            sendMessage(cmdContext, "That player is not online right now.");
            return 1;
        }

        String homeName = StringArgumentType.getString(cmdContext, "home name");
        PlayerHomesList phl = Homes.getForIfPresent(playerId);
        PlayerHome home;

        if((phl != null) && ((home = phl.getHome(homeName)) != null))
        {
            home.tpHere(player);
            sendMessage(cmdContext, "Sent " + playerName + " to their home of " + homeName + ".");
        }
        else
            sendMessage(cmdContext, playerName + " does not have a home by the name " + homeName + ".");

        return 1;
    }

    private static int cmdHomes_tptoother(CommandContext<CommandSource> cmdContext)
    {
        String beingTpedPlayerName = StringArgumentType.getString(cmdContext, "username to tp");
        UUID beingTpedPlayerId = getLastKnownUUIDOfPlayer(beingTpedPlayerName);

        if(beingTpedPlayerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + beingTpedPlayerName);
            return 1;
        }

        ServerPlayerEntity beingTpedPlayer = getOnlinePlayer(beingTpedPlayerId);

        if(beingTpedPlayer == null)
        {
            sendMessage(cmdContext, beingTpedPlayerName + " is not online right now.");
            return 1;
        }

        String homeOwningPlayerName = StringArgumentType.getString(cmdContext, "username");
        UUID homeOwningPlayerId = getLastKnownUUIDOfPlayer(homeOwningPlayerName);

        if(homeOwningPlayerId == null)
        {
            sendMessage(cmdContext, "No known player by the name " + homeOwningPlayerName);
            return 1;
        }

        String homeName = StringArgumentType.getString(cmdContext, "home name");
        PlayerHomesList phl = Homes.getForIfPresent(homeOwningPlayerId);
        PlayerHome home;

        if((phl != null) && ((home = phl.getHome(homeName)) != null))
        {
            home.tpHere(beingTpedPlayer);
            sendMessage(cmdContext, "Sent " + beingTpedPlayerName + " to " + homeOwningPlayerName + "'s home of "
                                    + homeName);
        }
        else
            sendMessage(cmdContext, homeOwningPlayerName + " does not have a home by the name of " + homeName + ".");

        return 1;
    }
}
