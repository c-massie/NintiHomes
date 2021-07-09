package scot.massie.mc.ninti.homes;

import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerHomesList
{
    final UUID playerId;
    final Map<String, PlayerHome> playerHomes = new HashMap<>();

    public PlayerHomesList(UUID playerId)
    {
        this.playerId = playerId;
    }

    public PlayerHome getHome(String homeName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public List<PlayerHome> getHomes()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome setHome(String homeName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome setHome(String homeName, EntityLocation location)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome requestSetHome(String homeName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome requestSetHome(String homeName, EntityLocation location)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome deleteHome(String homeName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public PlayerHome clear()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
