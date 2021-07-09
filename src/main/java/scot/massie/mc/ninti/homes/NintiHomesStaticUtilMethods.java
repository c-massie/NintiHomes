package scot.massie.mc.ninti.homes;

import net.minecraftforge.common.UsernameCache;

import java.util.UUID;

public class NintiHomesStaticUtilMethods
{
    public static String getHowToReferToPlayer(UUID playerId)
    {
        String name = UsernameCache.getLastKnownUsername(playerId);

        if(name == null)
            return "The player with the ID " + playerId;

        return "The player " + name;
    }
}
