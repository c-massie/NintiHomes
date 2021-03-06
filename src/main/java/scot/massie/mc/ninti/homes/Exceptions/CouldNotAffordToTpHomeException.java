package scot.massie.mc.ninti.homes.Exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CouldNotAffordToTpHomeException extends Exception
{
    public CouldNotAffordToTpHomeException(UUID playerId, String homeName, Map<String, Double> costs)
    {
        this.playerId = playerId;
        this.homeName = homeName;
        this.costs = Collections.unmodifiableMap(new HashMap<>(costs));
    }

    private final UUID playerId;
    private final String homeName;
    private final Map<String, Double> costs;

    public UUID getPlayerId()
    { return playerId; }

    public String getHomeName()
    { return homeName; }

    public Map<String, Double> getCosts()
    { return costs; }
}
