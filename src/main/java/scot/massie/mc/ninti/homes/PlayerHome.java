package scot.massie.mc.ninti.homes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.collections.maps.DoubleMap;
import scot.massie.lib.collections.maps.MapUtils;
import scot.massie.lib.maths.Equation;
import scot.massie.lib.permissions.PermissionStatus;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.currencies.Currencies;
import scot.massie.mc.ninti.core.exceptions.NoSuchWorldException;
import scot.massie.mc.ninti.core.exceptions.PlayerMissingPermissionException;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;
import scot.massie.mc.ninti.core.zones.Zone;
import scot.massie.mc.ninti.core.zones.Zones;
import scot.massie.mc.ninti.homes.Exceptions.CouldNotAffordToTpHomeException;

import static scot.massie.lib.utils.StringUtils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHome
{
    final static Equation.Builder tpCostEqBuilder
            = new Equation.Builder()
                      .withComparativeOperators()
                      .withVariable("distance", 0)
                      .withVariable("isAcrossWorlds", 0);

    final UUID playerId;
    final String homeName;
    final EntityLocation location;

    public PlayerHome(UUID playerId, String homeName, EntityLocation location)
    {
        this.playerId = playerId;
        this.homeName = homeName;
        this.location = location;
    }

    public String getName()
    { return homeName; }

    public EntityLocation getLocation()
    { return location; }

    public ServerPlayerEntity getPlayer()
    { return PluginUtils.getServer().getPlayerList().getPlayerByUUID(playerId); }

    public String getPlayerName()
    { return UsernameCache.getLastKnownUsername(playerId); }

    public void assertPlayerHasPermissionToTpHere() throws PlayerMissingPermissionException
    {
        PlayerEntity onlineOwner = getPlayer();

        if(onlineOwner != null)
        {
            EntityLocation playerLocation = new EntityLocation(onlineOwner);
            String fromWorldPermission = NintiHomes.PERMISSION_HOMES_TP_FROMWORLD + "." + playerLocation.getWorldId();
            Permissions.assertPlayerHasPermission(playerId, fromWorldPermission);
        }
    }

    public boolean playerHasPermissionToTpHere()
    {
        PlayerEntity onlineOwner = getPlayer();

        if(onlineOwner != null)
        {
            EntityLocation playerLocation = new EntityLocation(onlineOwner);
            String fromWorldPermission = NintiHomes.PERMISSION_HOMES_TP_FROMWORLD + "." + playerLocation.getWorldId();

            if(!Permissions.playerHasPermission(playerId, fromWorldPermission))
                return false;

            for(Zone z : Zones.getZonesAt(playerLocation))
            {
                String fromZonePermission = NintiHomes.PERMISSION_HOMES_TP_FROMZONE + "." + z.getName();

                if(!Permissions.playerHasPermission(playerId, fromZonePermission))
                    return false;
            }
        }

        String toWorldPermission = NintiHomes.PERMISSION_HOMES_TP_TOWORLD + "." + location.getWorldId();

        if(!Permissions.playerHasPermission(playerId, toWorldPermission))
            return false;

        for(Zone z : Zones.getZonesAt(location))
        {
            String toZonePermission = NintiHomes.PERMISSION_HOMES_TP_TOZONE + "." + z.getName();

            if(!Permissions.playerHasPermission(playerId, toZonePermission))
                return false;
        }

        return true;
    }

    public Map<String, Double> requestCostsToTpHere() throws PlayerMissingPermissionException
    { return getCostsToTpHere(true); }

    public Map<String, Double> getCostsToTpHere()
    {
        try
        { return getCostsToTpHere(false); }
        catch(PlayerMissingPermissionException e)
        { throw new RuntimeException("This should never occur.", e); }
    }

    private Map<String, Double> getCostsToTpHere(boolean checkPlayerHasPermission)
            throws PlayerMissingPermissionException
    {
        PlayerEntity onlineOwner = getPlayer();
        Collection<Map<String, Double>> resultParts = new ArrayList<>();

        if(onlineOwner != null)
        {
            EntityLocation playerLocation = new EntityLocation(onlineOwner);
            String fromWorldPermission = NintiHomes.PERMISSION_HOMES_TP_FROMWORLD + "." + playerLocation.getWorldId();
            resultParts.add(getTpCostsFromPerm(Permissions.getPlayerPermissionStatus(playerId, fromWorldPermission),
                                               checkPlayerHasPermission));

            for(Zone z : Zones.getZonesAt(playerLocation))
            {
                String fromZonePermission = NintiHomes.PERMISSION_HOMES_TP_FROMZONE + "." + z.getName();
                resultParts.add(getTpCostsFromPerm(Permissions.getPlayerPermissionStatus(playerId, fromZonePermission),
                                                   checkPlayerHasPermission));
            }
        }

        String toWorldPermission = NintiHomes.PERMISSION_HOMES_TP_TOWORLD + "." + location.getWorldId();
        resultParts.add(getTpCostsFromPerm(Permissions.getPlayerPermissionStatus(playerId, toWorldPermission),
                                           checkPlayerHasPermission));

        for(Zone z : Zones.getZonesAt(location))
        {
            String toZonePermission = NintiHomes.PERMISSION_HOMES_TP_TOZONE + "." + z.getName();
            resultParts.add(getTpCostsFromPerm(Permissions.getPlayerPermissionStatus(playerId, toZonePermission),
                                               checkPlayerHasPermission));
        }

        return MapUtils.sumMatchingDoubleValues(resultParts);
    }

    public Map<String, Double> requestTpCostsFromPerm(PermissionStatus permStatus)
            throws PlayerMissingPermissionException
    { return getTpCostsFromPerm(permStatus, true); }

    public Map<String, Double> getTpCostsFromPerm(PermissionStatus permStatus)
    {
        try
        { return getTpCostsFromPerm(permStatus, false); }
        catch(PlayerMissingPermissionException e)
        { throw new RuntimeException("This should never occur.", e); }
    }

    private Map<String, Double> getTpCostsFromPerm(PermissionStatus permStatus, boolean checkPlayerHasPermission)
            throws PlayerMissingPermissionException
    {
        if(checkPlayerHasPermission && !permStatus.hasPermission())
            throw new PlayerMissingPermissionException(playerId, permStatus.getPermission());

        if(permStatus.getPermissionArg() == null)
            return new HashMap<>();

        Map<String, String> tpCostEquationsAsStrings = splitColonSeparatedValuePairs(permStatus.getPermissionArg());
        DoubleMap<String> tpCosts = new DoubleMap<>();

        PlayerEntity onlineOwner = getPlayer();
        EntityLocation onlineOwnerLocation = onlineOwner == null ? null : new EntityLocation(onlineOwner);
        double playerDistance = onlineOwner == null ? 0 : onlineOwnerLocation.getDistanceTo(location);
        // 1 == true, 0 == false
        double tpIsAcrossWorlds
                = ((onlineOwner == null) || onlineOwnerLocation.getWorldId().equals(location.getWorldId())) ? 0 : 1;

        for(Map.Entry<String, String> e : tpCostEquationsAsStrings.entrySet())
        {
            if(e.getValue() == null)
            {
                System.err.println("Currency cost improperly formatted: " + e.getKey()
                                   + "\n    Should be in the form of, without square brackets: "
                                   + "[currency code]: [equation]");
                continue;
            }

            Equation eq;
            try
            { eq = tpCostEqBuilder.build(e.getValue()); }
            catch(Equation.Builder.EquationParseException ex)
            {
                System.err.println("Error parsing currency cost for " + e.getKey() + ": " + e.getValue() + ":\n\n"
                                   + ex.getMessage().replaceAll("(?m)^", "    "));

                continue;
            }

            eq.setVariable("distance", playerDistance);
            eq.setVariable("isAcrossWorlds", tpIsAcrossWorlds);

            tpCosts.add(e.getKey(), eq.evaluate());
        }

        return tpCosts;
    }

    public void chargePlayerToTpHere()
            throws Currencies.UnrecognisedCurrencyException, CouldNotAffordToTpHomeException
    {
        Map<String, Double> tpCosts = getCostsToTpHere();
        boolean couldAfford = Currencies.chargePlayer(playerId, tpCosts);

        if(!couldAfford)
            throw new CouldNotAffordToTpHomeException(playerId, homeName, tpCosts);
    }

    public void tpHere() throws NoSuchWorldException
    {
        ServerPlayerEntity player = getPlayer();

        if(player == null)
        {
            String playerName = UsernameCache.getLastKnownUsername(playerId);

            if(playerName == null)
                playerName = playerId.toString();

            System.err.println("Attempted to teleport offline player: " + playerName);
            return;
        }

        location.tpPlayerToHere(player);
    }

    public void tpHere(ServerPlayerEntity player) throws NoSuchWorldException
    { location.tpPlayerToHere(player); }

    public void requestTpHere()
            throws NoSuchWorldException,
                   PlayerMissingPermissionException,
                   Currencies.UnrecognisedCurrencyException,
                   CouldNotAffordToTpHomeException
    {
        location.assertWorldExists();
        assertPlayerHasPermissionToTpHere();
        chargePlayerToTpHere();
        tpHere();
    }
}
