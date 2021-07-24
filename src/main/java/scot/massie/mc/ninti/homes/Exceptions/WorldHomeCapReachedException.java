package scot.massie.mc.ninti.homes.Exceptions;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.*;

public final class WorldHomeCapReachedException extends HomeCapReachedException
{
    public WorldHomeCapReachedException(UUID playerId, int homesAllowed, String worldId)
    {
        super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                      + "they're allowed (" + homesAllowed + ") in the world " + worldId);

        this.worldId = worldId;
    }

    private final String worldId;

    public String getWorldId()
    { return worldId; }
}