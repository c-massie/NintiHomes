package scot.massie.mc.ninti.homes.Exceptions;

import java.util.UUID;

import static scot.massie.mc.ninti.homes.NintiHomesStaticUtilMethods.*;

public class HomeCapReachedException extends Exception
{
    protected HomeCapReachedException(UUID playerId, int homesAllowed, String msg)
    {
        super(msg);
        this.playerId = playerId;
        this.homesAllowed = homesAllowed;
    }

    public HomeCapReachedException(UUID playerId, int homesAllowed)
    {
        this(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                     + "they're allowed. (" + homesAllowed + ")");
    }

    protected final UUID playerId;
    protected final int homesAllowed;

    public UUID getPlayerId()
    { return playerId; }

    public int getHomesAllowed()
    { return homesAllowed; }
}
