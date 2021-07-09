package scot.massie.mc.ninti.homes.Exceptions;

import scot.massie.mc.ninti.homes.Homes;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.*;

public class ZoneHomeCapReachedException extends Homes.HomeCapReachedException
{
    protected ZoneHomeCapReachedException(UUID playerId, int homesAllowed, String zoneName)
    {
        super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                      + "they're allowed (" + homesAllowed + ") in the zone " + zoneName);

        this.zoneName = zoneName;
    }

    protected final String zoneName;

    public String getZoneName()
    { return zoneName; }
}