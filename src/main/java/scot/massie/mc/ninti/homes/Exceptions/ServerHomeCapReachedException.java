package scot.massie.mc.ninti.homes.Exceptions;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.getHowToReferToPlayer;

public final class ServerHomeCapReachedException extends HomeCapReachedException
{
    public ServerHomeCapReachedException(UUID playerId, int homesAllowed)
    {
        super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                      + "they're allowed (" + homesAllowed + ") on this server.");
    }
}
