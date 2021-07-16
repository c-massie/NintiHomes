package scot.massie.mc.ninti.homes;

import net.minecraft.entity.player.ServerPlayerEntity;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;
import scot.massie.mc.ninti.core.zones.Zone;
import scot.massie.mc.ninti.core.zones.Zones;

import java.util.ArrayList;
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

    public Map<String, PlayerHome> getHomesAsMap()
    {
        synchronized(playerHomes)
        { return new HashMap<>(playerHomes); }
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
    {
        ServerPlayerEntity player = PluginUtils.getOnlinePlayer(playerId);

        if(player == null)
            return null;

        return requestSetHome(homeName, new EntityLocation(player));
    }

    public PlayerHome requestSetHome(String homeName, EntityLocation location)
    {
        final String noPermissionMessage = "You do not have permission to set a home there.";
        ServerPlayerEntity player = PluginUtils.getOnlinePlayer(playerId);
        String inWorldPermission = NintiHomes.PERMISSION_HOMES_ADD_INWORLD + "." + location.getWorldId();

        if(!Permissions.playerHasPermission(playerId, inWorldPermission))
        {
            if(player != null)
                PluginUtils.sendMessage(player, noPermissionMessage);

            return null;
        }

        for(Zone z : Zones.getZonesAt(location))
        {
            String inZonePermission = NintiHomes.PERMISSION_HOMES_ADD_INZONE + "." + z.getName();

            if(!Permissions.playerHasPermission(playerId, inZonePermission))
            {
                if(player != null)
                    PluginUtils.sendMessage(player, noPermissionMessage);

                return null;
            }
        }

        return setHome(homeName, location);
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
