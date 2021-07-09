package scot.massie.mc.ninti.homes;

import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.util.Map;
import java.util.UUID;

public class PlayerHome
{
    final UUID playerId;
    final String homeName;
    final EntityLocation location;

    public PlayerHome(UUID playerId, String homeName, EntityLocation location)
    {
        this.playerId = playerId;
        this.homeName = homeName;
        this.location = location;
    }

    public EntityLocation getLocation()
    {
        return location;
    }

    public boolean playerHasPermissionToTpHere()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public Map<String, Double> getCostsToTpHere()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void tpPlayerHere()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
