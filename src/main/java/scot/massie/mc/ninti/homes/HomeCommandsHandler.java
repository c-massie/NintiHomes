package scot.massie.mc.ninti.homes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;

import static net.minecraft.command.Commands.*;

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

    private static final SuggestionProvider<CommandSource> playersOnlineSuggestionProvider
            = (context, builder) ->
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    };

    private static final SuggestionProvider<CommandSource> playersWithHomesSuggestionProvider
            = (context, builder) ->
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    };

    private static final SuggestionProvider<CommandSource> homesPlayerHasSuggestionProvider
            = (context, builder) ->
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    };

    private static final SuggestionProvider<CommandSource> homesAnotherPlayerHasSuggestionProvider
            = (context, builder) ->
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    };

    public static final LiteralArgumentBuilder<CommandSource> homeCommand
            = literal("home")
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdHome_specified))
                    .executes(HomeCommandsHandler::cmdHome_default);

    public static final LiteralArgumentBuilder<CommandSource> sethomeCommand
            = literal("sethome")
                    .then(argument("home name", StringArgumentType.word())
                            .executes(HomeCommandsHandler::cmdSethome_specified))
                    .executes(HomeCommandsHandler::cmdSethome_default);

    public static final LiteralArgumentBuilder<CommandSource> delhomeCommand
            = literal("delhome")
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdDelhome_specified))
                    .executes(HomeCommandsHandler::cmdDelhome_default);

    public static final LiteralArgumentBuilder<CommandSource> gettphomecostCommand
            = literal("gettphomecost")
                    .then(argument("home name", StringArgumentType.word())
                            .suggests(homesPlayerHasSuggestionProvider)
                            .executes(HomeCommandsHandler::cmdGettphomecost_specified))
                    .executes(HomeCommandsHandler::cmdGettphomecost_default);

    public static final LiteralArgumentBuilder<CommandSource> listhomesCommand
            = literal("listhomes")
                      .executes(HomeCommandsHandler::cmdListhomes);

    public static final LiteralArgumentBuilder<CommandSource> homesCommand
            = literal("homes")
                    .then(literal("list")
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .executes(HomeCommandsHandler::cmdHomes_list_user))
                            .executes(HomeCommandsHandler::cmdHomes_list_all))
                    .then(literal("create")
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .executes(HomeCommandsHandler::cmdHomes_create_here)))
                    .then(literal("delete")
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_delete_one))
                                    .executes(HomeCommandsHandler::cmdHomes_delete_all)))
                    .then(literal("tpme")
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_tpme))))
                    .then(literal("tp")
                            .then(argument("username", StringArgumentType.word())
                                    .suggests(playersWithHomesSuggestionProvider)
                                    .then(argument("home name", StringArgumentType.word())
                                            .suggests(homesAnotherPlayerHasSuggestionProvider)
                                            .executes(HomeCommandsHandler::cmdHomes_tp))))
                    .then(literal("tptoother")
                            .then(argument("username to tp", StringArgumentType.word())
                                    .suggests(playersOnlineSuggestionProvider)
                                    .then(argument("username", StringArgumentType.word())
                                            .suggests(playersWithHomesSuggestionProvider)
                                            .then(argument("home name", StringArgumentType.word())
                                                    .suggests(homesAnotherPlayerHasSuggestionProvider)
                                                    .executes(HomeCommandsHandler::cmdHomes_tptoother)))));

    private static int cmdHome_default(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdHome_specified(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdDelhome_default(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdDelhome_specified(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdSethome_default(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdSethome_specified(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdGettphomecost_default(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static int cmdGettphomecost_specified(CommandContext<CommandSource> cmdContext)
    {
        throw new UnsupportedOperationException("Not implemented yet");
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
