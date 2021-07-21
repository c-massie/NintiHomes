package scot.massie.mc.ninti.homes.Exceptions;

import scot.massie.mc.ninti.homes.Homes;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.*;

public class WorldHomeCapReachedException extends HomeCapReachedException
{
    public WorldHomeCapReachedException(UUID playerId, int homesAllowed, String worldId)
    {
        super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                      + "they're allowed (" + homesAllowed + ") in the world " + worldId);

        this.worldId = worldId;
    }

    protected final String worldId;

    public String getWorldId()
    { return worldId; }
}