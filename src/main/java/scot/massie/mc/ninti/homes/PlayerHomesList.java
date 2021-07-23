package scot.massie.mc.ninti.homes;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.permissions.PermissionStatus;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.exceptions.PlayerMissingPermissionException;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;
import scot.massie.mc.ninti.core.zones.Zone;
import scot.massie.mc.ninti.core.zones.Zones;
import scot.massie.mc.ninti.homes.Exceptions.ServerHomeCapReachedException;
import scot.massie.mc.ninti.homes.Exceptions.WorldHomeCapReachedException;
import scot.massie.mc.ninti.homes.Exceptions.ZoneHomeCapReachedException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerHomesList
{
    final UUID playerId;
    final Map<String, PlayerHome> playerHomes = new HashMap<>();

    public PlayerHomesList(UUID playerId)
    {
        this.playerId = playerId;
    }

    public boolean isEmpty()
    {
        synchronized(playerHomes)
        { return playerHomes.isEmpty(); }
    }

    public UUID getPlayerId()
    { return playerId; }

    public String getPlayerName()
    {
        String username = UsernameCache.getLastKnownUsername(playerId);

        if(username == null)
            username = playerId.toString();

        return username;
    }

    public PlayerHome getHome(String homeName)
    {
        synchronized(playerHomes)
        { return playerHomes.get(homeName); }
    }

    public List<PlayerHome> getHomes()
    {
        synchronized(playerHomes)
        { return new ArrayList<>(playerHomes.values()); }
    }

    public List<String> getHomeNames()
    {
        List<String> result;

        synchronized(playerHomes)
        { result = new ArrayList<>(playerHomes.keySet()); }

        result.sort(Comparator.naturalOrder());
        return result;
    }

    public Map<String, PlayerHome> getHomesAsMap()
    {
        synchronized(playerHomes)
        { return new HashMap<>(playerHomes); }
    }

    public int countHomesInWorld(String worldId)
    {
        int count = 0;

        synchronized(playerHomes)
        {
            for(PlayerHome home : playerHomes.values())
                if(home.location.getWorldId().equals(worldId))
                    count++;
        }

        return count;
    }

    public int countHomesInZone(Zone zone)
    {
        int count = 0;

        synchronized(playerHomes)
        {
            for(PlayerHome home : playerHomes.values())
                if(zone.contains(home.location))
                    count++;
        }

        return count;
    }

    public PlayerHome setHome(String homeName)
    {
        ServerPlayerEntity player = PluginUtils.getOnlinePlayer(playerId);

        if(player == null)
            return null;

        return setHome(homeName, new EntityLocation(player));
    }

    public PlayerHome setHome(String homeName, EntityLocation location)
    {
        PlayerHome newHome = new PlayerHome(playerId, homeName, location);

        synchronized(playerHomes)
        { playerHomes.put(homeName, newHome); }

        return newHome;
    }

    public PlayerHome requestSetHome(String homeName)
            throws PlayerMissingPermissionException,
                   ServerHomeCapReachedException,
                   WorldHomeCapReachedException,
                   ZoneHomeCapReachedException
    {
        ServerPlayerEntity player = PluginUtils.getOnlinePlayer(playerId);

        if(player == null)
            return null;

        return requestSetHome(homeName, new EntityLocation(player));
    }

    public PlayerHome requestSetHome(String homeName, EntityLocation location)
            throws PlayerMissingPermissionException,
                   ServerHomeCapReachedException,
                   WorldHomeCapReachedException,
                   ZoneHomeCapReachedException
    {
        String inWorldPermission = NintiHomes.PERMISSION_HOMES_ADD_INWORLD + "." + location.getWorldId();
        PermissionStatus wpstatus = Permissions.getPlayerPermissionStatus(playerId, inWorldPermission);

        if(!wpstatus.hasPermission())
            throw new PlayerMissingPermissionException(playerId, inWorldPermission);

        String wparg = wpstatus.getPermissionArg();
        String sparg = Permissions.getPlayerPermissionArg(playerId, NintiHomes.PERMISSION_HOMES_ADD);

        synchronized(playerHomes)
        {
            PlayerHome existingHome = playerHomes.get(homeName);

            if(sparg != null)
            {
                try
                {
                    int numberOfHomesAllowedOnServer = Integer.parseUnsignedInt(sparg);
                    int numberOfHomesAlreadyOnServer = playerHomes.size();

                    if(existingHome != null)
                        numberOfHomesAlreadyOnServer--;

                    if(numberOfHomesAlreadyOnServer >= numberOfHomesAllowedOnServer)
                        throw new ServerHomeCapReachedException(playerId, numberOfHomesAllowedOnServer);
                }
                catch(NumberFormatException e)
                { /* Don't check against the number of homes allowed. */ }
            }

            if(wparg != null)
            {
                try
                {
                    int numberOfHomesAllowedInWorld = Integer.parseUnsignedInt(wparg);
                    int numberOfHomesAlreadyInWorld = countHomesInWorld(location.getWorldId());

                    if((existingHome != null) && (existingHome.location.getWorldId().equals(location.getWorldId())))
                        numberOfHomesAlreadyInWorld--;

                    if(numberOfHomesAlreadyInWorld >= numberOfHomesAllowedInWorld)
                        throw new WorldHomeCapReachedException(playerId,
                                                               numberOfHomesAllowedInWorld,
                                                               location.getWorldId());
                }
                catch(NumberFormatException e)
                { /* Don't check against the number of homes allowed. */ }
            }

            for(Zone z : Zones.getZonesAt(location))
            {
                String inZonePermission = NintiHomes.PERMISSION_HOMES_ADD_INZONE + "." + z.getName();
                PermissionStatus zpstatus = Permissions.getPlayerPermissionStatus(playerId, inZonePermission);

                if(!zpstatus.hasPermission())
                    throw new PlayerMissingPermissionException(playerId, inZonePermission);

                String zparg = zpstatus.getPermissionArg();

                if(zparg != null)
                {
                    try
                    {
                        int numberOfHomesAllowedInZone = Integer.parseUnsignedInt(zparg);
                        int numberOfHomesAlreadyInZone = countHomesInZone(z);

                        if(existingHome != null && z.contains(existingHome.location))
                            numberOfHomesAlreadyInZone--;

                        if(numberOfHomesAlreadyInZone >= numberOfHomesAllowedInZone)
                            throw new ZoneHomeCapReachedException(playerId, numberOfHomesAllowedInZone, z.getName());
                    }
                    catch(NumberFormatException e)
                    { /* Don't check against the number of homes allowed */ }
                }
            }

            return setHome(homeName, location);
        }
    }

    public PlayerHome deleteHome(String homeName)
    {
        synchronized(playerHomes)
        { return playerHomes.remove(homeName); }
    }

    public void clear()
    {
        synchronized(playerHomes)
        { playerHomes.clear(); }
    }
}
