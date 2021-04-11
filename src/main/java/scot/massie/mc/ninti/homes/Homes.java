package scot.massie.mc.ninti.homes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.collections.maps.MapUtils;
import scot.massie.lib.maths.EquationEvaluation;
import scot.massie.lib.permissions.PermissionStatus;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.currencies.Currencies;
import scot.massie.mc.ninti.core.exceptions.MissingPermissionException;
import scot.massie.mc.ninti.core.exceptions.NoSuchWorldException;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;
import scot.massie.mc.ninti.core.zones.Zone;
import scot.massie.mc.ninti.core.zones.Zones;

import java.util.*;
import java.util.function.BiFunction;

public final class Homes
{
    public static class HomeLocationNotParsableException extends RuntimeException
    {
        public HomeLocationNotParsableException()
        { super(); }

        public HomeLocationNotParsableException(String msg)
        { super(msg); }

        public HomeLocationNotParsableException(String msg, Throwable cause)
        { super(msg, cause); }

        public HomeLocationNotParsableException(Throwable cause)
        { super(cause); }
    }

    public static class NoSuchHomeException extends Exception
    {
        public NoSuchHomeException(PlayerEntity player, String homeName)
        {
            super("The player " + player.getGameProfile().getName() + " has no home by the name \"" + homeName + "\".");
            this.playerId = player.getUniqueID();
            this.homeName = homeName;
        }

        public NoSuchHomeException(UUID playerId, String homeName)
        {
            super("The player " + getHowToReferToPlayer(playerId) + " has no home by the name \"" + homeName + "\".");
            this.playerId = playerId;
            this.homeName = homeName;
        }

        private static String getHowToReferToPlayer(UUID playerId)
        {
            String name = UsernameCache.getLastKnownUsername(playerId);
            return name != null ? name : "with the UUID " + playerId;
        }

        final UUID playerId;
        final String homeName;

        public UUID getPlayerId()
        { return playerId; }

        public String getHomeName()
        { return homeName; }
    }

    public static class CouldNotAffordToTpHomeException extends Exception
    {
        public CouldNotAffordToTpHomeException(UUID playerId, String homeName, Map<String, Double> costs)
        {
            this.playerId = playerId;
            this.homeName = homeName;
            this.costs = Collections.unmodifiableMap(new HashMap<>(costs));
        }

        protected final UUID playerId;
        protected final String homeName;
        protected final Map<String, Double> costs;

        public UUID getPlayerId()
        { return playerId; }

        public String getHomeName()
        { return homeName; }

        public Map<String, Double> getCosts()
        { return costs; }
    }

    public static class HomeCapReachedException extends Exception
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

    public static class WorldHomeCapReachedException extends HomeCapReachedException
    {
        protected WorldHomeCapReachedException(UUID playerId, int homesAllowed, String worldId)
        {
            super(playerId, homesAllowed, getHowToReferToPlayer(playerId) + " has reached the maximum number of homes "
                                          + "they're allowed (" + homesAllowed + ") in the world " + worldId);

            this.worldId = worldId;
        }

        protected final String worldId;

        public String getWorldId()
        { return worldId; }
    }

    public static class ZoneHomeCapReachedException extends HomeCapReachedException
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

    private static class PlayerHomesRecord
    {
        public PlayerHomesRecord(UUID playerId)
        { this.playerId = playerId; }

        private final UUID playerId;
        private final Map<String, EntityLocation> playerHomes = new HashMap<>();

        public UUID getPlayerId()
        { return playerId; }

        public void setHome(String homeName, EntityLocation location)
        { playerHomes.put(homeName, location); }

        public EntityLocation deleteHome(String homeName)
        { return playerHomes.remove(homeName); }

        public boolean hasHomeWithName(String homeName)
        { return playerHomes.containsKey(homeName); }

        public EntityLocation getHome(String homeName)
        { return playerHomes.get(homeName); }

        public Map<String, EntityLocation> getHomes()
        { return new HashMap<>(playerHomes); }

        public Map<String, EntityLocation> getHomesInZone(Zone zone)
        {
            HashMap<String, EntityLocation> result = new HashMap<>();

            for(Map.Entry<String, EntityLocation> e : playerHomes.entrySet())
                if(zone.contains(e.getValue()))
                    result.put(e.getKey(), e.getValue());

            return result;
        }

        public Map<String, EntityLocation> getHomesInWorld(String worldId)
        {
            HashMap<String, EntityLocation> result = new HashMap<>();

            for(Map.Entry<String, EntityLocation> e : playerHomes.entrySet())
                if(e.getValue().getWorldId().equals(worldId))
                    result.put(e.getKey(), e.getValue());

            return result;
        }

        public List<String> getHomeNames()
        {
            List<String> result = new ArrayList<>(playerHomes.keySet());
            result.sort(Comparator.naturalOrder());
            return result;
        }

        public int countHomes()
        { return playerHomes.size(); }

        public int countHomesInZone(Zone zone)
        {
            int count = 0;

            for(Map.Entry<String, EntityLocation> e : playerHomes.entrySet())
                if(zone.contains(e.getValue()))
                    count++;

            return count;
        }

        public int countHomesInWorld(String worldId)
        {
            int count = 0;

            for(Map.Entry<String, EntityLocation> e : playerHomes.entrySet())
                if(e.getValue().getWorldId().equals(worldId))
                    count++;

            return count;
        }

        boolean isEmpty()
        { return playerHomes.isEmpty(); }
    }

    private Homes()
    {}

    private static final Map<UUID, PlayerHomesRecord> records = new HashMap<>();

    /*

    hasHomes(UUID playerId)
    getHomes(UUID playerId)

    countHomes(UUID playerId)
    countHomesInZone(UUID playerId, String zoneName)
    countHomesInWorld(UUID playerId, String worldId)

    getPlayersWithHomes()

    setHome(UUID playerId, String homeName, HomeLocation location)
    requestSetHome(UUID playerId, String homeName, HomeLocation location)
    requestSetHome(ServerPlayerEntity player, String homeName)
    deleteHome(UUID playerId, String homeName)
    clear()

    getCostToTp(ServerPlayerEntity player, String homeName)
    getCostToTp(ServerPlayerEntity player, HomeLocation destination, PermissionStatus permStatus, double distance)
    requestCostToTp(ServerPlayerEntity player, String homeName)

    tpPlayerToHome(ServerPlayerEntity player, String homeName)
    tpPlayerToHome(ServerPlayerEntity player, UUID homeOwnerId, String homeName)
    requesPlayerTpToHome(ServerPlayerEntity player, String homeName)

    save()
    load()

     */

    //region accessors
    public static boolean hasHomes(UUID playerId)
    {
        synchronized(records)
        { return records.containsKey(playerId); }
    }

    public static Map<UUID, Map<String, EntityLocation>> getHomes()
    {
        Map<UUID, Map<String, EntityLocation>> result = new HashMap<>();

        synchronized(records)
        {
            for(Map.Entry<UUID, PlayerHomesRecord> record : records.entrySet())
                result.put(record.getKey(), record.getValue().getHomes());
        }

        return result;
    }

    public static Map<String, EntityLocation> getHomes(UUID playerId)
    {
        synchronized(records)
        {
            PlayerHomesRecord record = records.get(playerId);

            if(record == null)
                return Collections.emptyMap();

            return Collections.unmodifiableMap(new HashMap<>(record.playerHomes));
        }
    }

    public static Collection<UUID> getPlayersWithHomes()
    {
        synchronized(records)
        { return new HashSet<>(records.keySet()); }
    }

    public static Map<String, Double> getCostToTp(PlayerEntity player, String homeName)
            throws NoSuchHomeException, NoSuchWorldException, MissingPermissionException
    {
        EntityLocation destination;

        synchronized(records)
        {
            PlayerHomesRecord playerRecord = records.get(player.getUniqueID());

            if(playerRecord == null)
                throw new NoSuchHomeException(player, homeName);

            destination = playerRecord.getHome(homeName);
        }

        if(destination == null)
            throw new NoSuchHomeException(player, homeName);

        if(PluginUtils.getWorldById(destination.getWorldId()) == null)
            throw new NoSuchWorldException(destination.getWorldId());

        return getCostToTp(player, destination);
    }

    private static Map<String, Double> getCostToTp(PlayerEntity player, EntityLocation destination)
            throws MissingPermissionException
    {
        EntityLocation currentLocation = new EntityLocation(player);

        String fromWorldPermission = NintiHomes.PERMISSION_HOMES_TP_FROMWORLD + "."
                                     + (currentLocation.getWorldId().replaceAll(":", "."));

        String toWorldPermission = NintiHomes.PERMISSION_HOMES_TP_TOWORLD + "."
                                   + (destination.getWorldId().replaceAll(":", "."));

        PermissionStatus fromWorldPermissionStatus = Permissions.getPlayerPermissionStatus(player, fromWorldPermission);
        PermissionStatus toWorldPermissionStatus = Permissions.getPlayerPermissionStatus(player, toWorldPermission);

        if(!fromWorldPermissionStatus.hasPermission())
            throw new MissingPermissionException(player, fromWorldPermission);

        if(!toWorldPermissionStatus.hasPermission())
            throw new MissingPermissionException(player, toWorldPermission);

        double distance = currentLocation.getDistanceTo(destination);
        Map<String, Double> costs = MapUtils.maxValues(getCostToTp(currentLocation,
                                                                   destination,
                                                                   fromWorldPermissionStatus,
                                                                   distance),
                                                       getCostToTp(currentLocation,
                                                                   destination,
                                                                   toWorldPermissionStatus,
                                                                   distance));

        for(Zone z : Zones.getZonesAt(currentLocation))
        {
            String fromZonePermission = NintiHomes.PERMISSION_HOMES_TP_FROMZONE + "." + z.getName();
            PermissionStatus fromZonePermStatus = Permissions.getPlayerPermissionStatus(player, fromZonePermission);
            Map<String, Double> zoneCosts = getCostToTp(currentLocation, destination, fromZonePermStatus, distance);

            for(Map.Entry<String, Double> e : zoneCosts.entrySet())
                costs.compute(e.getKey(), (k, v) -> (v != null && v >= e.getValue()) ? (v) : (e.getValue()));
        }

        for(Zone z : Zones.getZonesAt(destination))
        {
            String toZonePermission = NintiHomes.PERMISSION_HOMES_TP_TOZONE + "." + z.getName();
            PermissionStatus toZonePermStatus = Permissions.getPlayerPermissionStatus(player, toZonePermission);
            Map<String, Double> zoneCosts = getCostToTp(currentLocation, destination, toZonePermStatus, distance);

            for(Map.Entry<String, Double> e : zoneCosts.entrySet())
                costs.compute(e.getKey(), (k, v) -> (v != null && v >= e.getValue()) ? (v) : (e.getValue()));
        }

        return costs;
    }

    private static Map<String, Double> getCostToTp(EntityLocation playerLocation,
                                                   EntityLocation homeLocation,
                                                   PermissionStatus permStatus,
                                                   double distance)
    {
        String permArg = permStatus.getPermissionArg();

        if(permArg == null)
            return Collections.emptyMap();

        Map<String, Double> result = new HashMap<>();
        String[] argLines = permArg.split("\\r?\\n|\\r");
        double transworldDifferentiator = playerLocation.getWorldId().equals(homeLocation.getWorldId()) ? 1 : 0;

        for(String line : argLines)
        {
            String[] lineParts = line.split(":", 2);

            String currencyName = lineParts.length == 1 ? "xp" : lineParts[0].trim();
            double amount = new EquationEvaluation(lineParts[lineParts.length == 1 ? 0 : 1])
                                    .withVariable("distance", distance)
                                    .withVariable("transworlddiff", transworldDifferentiator)
                                    .evaluate();

            Double existingAmount = result.get(currencyName);

            if(existingAmount == null || amount > existingAmount)
                result.put(currencyName, amount);
        }

        return result;
    }
    //endregion

    //region mutators
    public static void setHome(UUID playerId, String homeName, EntityLocation location)
    {
        synchronized(records)
        { records.computeIfAbsent(playerId, PlayerHomesRecord::new).setHome(homeName, location); }
    }

    public static void setHome(PlayerEntity entity, String homeName)
    { setHome(entity.getUniqueID(), homeName, new EntityLocation(entity)); }

    public static void requestSetHome(UUID playerId, String homeName, EntityLocation location)
            throws MissingPermissionException, WorldHomeCapReachedException, ZoneHomeCapReachedException
    {
        String permissionForWorld = NintiHomes.PERMISSION_HOMES_ADD_INWORLD + "."
                                    + (location.getWorldId().replaceAll(":", "."));

        int homesAllowed = getHomesAllowed(playerId, permissionForWorld);
        Zone limitingZone = null;

        for(Zone z : Zones.getZonesAt(location))
        {
            String zonePerm = NintiHomes.PERMISSION_HOMES_ADD_INZONE + "." + z.getName();
            int allowedInZone = getHomesAllowed(playerId, zonePerm);

            if(allowedInZone < homesAllowed)
            {
                homesAllowed = allowedInZone;
                limitingZone = z;
            }
        }

        synchronized(records)
        {
            PlayerHomesRecord record = records.get(playerId);
            boolean creatingRecord = false;

            if(record == null)
            {
                record = new PlayerHomesRecord(playerId);
                creatingRecord = true;
            }

            if(!record.hasHomeWithName(homeName))
            {
                if(limitingZone == null) // limiting permission is for a world
                {
                    if(record.countHomesInWorld(location.getWorldId()) > homesAllowed)
                        throw new WorldHomeCapReachedException(playerId, homesAllowed, location.getWorldId());
                }
                else // limiting permission is for a zone
                {
                    if(record.countHomesInZone(limitingZone) > homesAllowed)
                        throw new ZoneHomeCapReachedException(playerId, homesAllowed, limitingZone.getName());
                }
            }

            // Only put the newly created record in the records map if something is actually added to it.
            if(creatingRecord)
                records.put(playerId, record);

            record.setHome(homeName, location);
        }
    }

    private static int getHomesAllowed(UUID playerId, String permission) throws MissingPermissionException
    {
        PermissionStatus permStatus = Permissions.getPlayerPermissionStatus(playerId, permission);

        if(!permStatus.hasPermission())
            throw new MissingPermissionException(playerId, permission);

        if(permStatus.getPermissionArg() == null)
            return Integer.MAX_VALUE;

        try
        { return Integer.parseUnsignedInt(permStatus.getPermissionArg()); }
        catch(NumberFormatException e)
        { return Integer.MAX_VALUE; }
    }

    public static void requestSetHome(PlayerEntity entity, String homeName)
            throws MissingPermissionException, WorldHomeCapReachedException, ZoneHomeCapReachedException
    { requestSetHome(entity.getUniqueID(), homeName, new EntityLocation(entity)); }

    public static EntityLocation deleteHome(UUID playerId, String homeName)
    {
        synchronized(records)
        {
            PlayerHomesRecord record = records.get(playerId);

            if(record == null)
                return null;

            EntityLocation result = record.deleteHome(homeName);

            if(record.isEmpty())
                records.remove(playerId);

            return result;
        }
    }

    public static EntityLocation deleteHome(PlayerEntity player, String homeName)
    { return deleteHome(player.getUniqueID(), homeName); }

    public static void clear()
    {
        synchronized(records)
        { records.clear(); }
    }
    //endregion

    public static void tpPlayerToHome(ServerPlayerEntity player, String homeName) throws NoSuchHomeException
    { tpPlayerToHome(player, player.getUniqueID(), homeName); }

    public static void tpPlayerToHome(ServerPlayerEntity player, UUID homeOwningPlayerId, String homeName)
            throws NoSuchHomeException
    {
        EntityLocation home;

        synchronized(records)
        {
            PlayerHomesRecord homeOwnerRecord = records.get(homeOwningPlayerId);

            if(homeOwnerRecord == null)
                throw new NoSuchHomeException(homeOwningPlayerId, homeName);

            home = homeOwnerRecord.getHome(homeName);
        }

        if(home == null)
            throw new NoSuchHomeException(homeOwningPlayerId, homeName);

        home.tpPlayerToHere(player);
    }

    public static void tpPlayerToHome(ServerPlayerEntity player, PlayerEntity homeOwningPlayer, String homeName)
            throws NoSuchHomeException
    { tpPlayerToHome(player, homeOwningPlayer.getUniqueID(), homeName); }

    public static void requestTpPlayerToHome(ServerPlayerEntity player, String homeName)
            throws NoSuchHomeException,
                   NoSuchWorldException,
                   MissingPermissionException,
                   Currencies.UnrecognisedCurrencyException,
                   CouldNotAffordToTpHomeException
    {
        synchronized(records)
        {
            Map<String, Double> costs = getCostToTp(player, homeName);

            if(!Currencies.chargePlayer(player, costs))
                throw new CouldNotAffordToTpHomeException(player.getUniqueID(), homeName, costs);

            tpPlayerToHome(player, homeName);
        }
    }
    //endregion

    //region filehandling
    public static void save()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public static void load()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
    //endregion

    private static String getHowToReferToPlayer(UUID playerId)
    {
        String name = UsernameCache.getLastKnownUsername(playerId);

        if(name == null)
            return "The player with the ID " + playerId;

        return "The player " + name;
    }
}
