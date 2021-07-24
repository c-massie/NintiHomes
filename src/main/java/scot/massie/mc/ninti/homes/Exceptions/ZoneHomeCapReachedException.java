package scot.massie.mc.ninti.homes.Exceptions;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.*;

public final class ZoneHomeCapReachedException extends HomeCapReachedException
{
    public ZoneHomeCapReachedException(UUID playerId, int homesAllowed, String zoneName)
    {
        super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                      + "they're allowed (" + homesAllowed + ") in the zone " + zoneName);

        this.zoneName = zoneName;
    }

    private final String zoneName;

    public String getZoneName()
    { return zoneName; }
}