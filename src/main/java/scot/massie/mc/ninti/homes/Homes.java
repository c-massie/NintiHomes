package scot.massie.mc.ninti.homes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import scot.massie.lib.maths.EquationEvaluation;
import scot.massie.lib.permissions.PermissionStatus;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.currencies.Currencies;
import scot.massie.mc.ninti.core.zones.Zone;
import scot.massie.mc.ninti.core.zones.Zones;

import java.util.*;

import static scot.massie.mc.ninti.core.PluginUtils.*;

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

        final UUID playerId;
        final String homeName;

        public UUID getPlayerId()
        { return playerId; }

        public String getHomeName()
        { return homeName; }
    }

    public static class NoSuchWorldException extends Exception
    {
        public NoSuchWorldException(String worldId)
        {
            super("The world does not exist: " + worldId);
            this.worldId = worldId;
        }

        final String worldId;

        public String getWorldId()
        { return worldId; }
    }

    public static class NoPermissionToTeleportHomeException extends Exception
    {
        public NoPermissionToTeleportHomeException(PlayerEntity player, String missingPermission)
        {
            super("The player " + player.getGameProfile().getName() + " does not have the required permission: "
                  + missingPermission);

            this.missingPermission = missingPermission;
        }

        final String missingPermission;

        public String getMissingPermission()
        { return missingPermission; }
    }

    public static class CouldNotAffordToTpHomeException extends Exception
    {
        public CouldNotAffordToTpHomeException(String homeName, Map<String, Double> costs)
        {
            this.homeName = homeName;
            this.costs = Collections.unmodifiableMap(new HashMap<>(costs));
        }

        final String homeName;
        final Map<String, Double> costs;

        public String getHomeName()
        { return homeName; }

        public Map<String, Double> getCosts()
        { return costs; }
    }

    public static class HomeLocation
    {
        public HomeLocation(String worldId, int x, int y, int z, int sidewaysAngle)
        {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sidewaysAngle = sidewaysAngle;
        }

        public HomeLocation(String asString)
        {
            String[] split = asString.split(", ", 5);

            if(split.length != 5)
                throw new HomeLocationNotParsableException("Not parsable as a home location: " + asString);

            try
            {
                x = Integer.parseInt(split[0]);
                y = Integer.parseInt(split[1]);
                z = Integer.parseInt(split[2]);
                sidewaysAngle = Integer.parseInt(split[3]);
            }
            catch(NumberFormatException e)
            { throw new HomeLocationNotParsableException("Not parsable as a home location: " + asString, e); }

            worldId = split[4];
        }

        String worldId;
        int x, y, z;
        int sidewaysAngle;

        public String getWorldId()
        { return worldId; }

        public int getX()
        { return x; }

        public int getY()
        { return y; }

        public int getZ()
        { return z; }

        public int getSidewaysAngleInDegrees()
        { return sidewaysAngle; }

        @Override
        public boolean equals(Object o)
        {
            if(this == o)
                return true;

            if(o == null || getClass() != o.getClass())
                return false;

            HomeLocation other = (HomeLocation)o;

            return x == other.x
                && y == other.y
                && z == other.z
                && sidewaysAngle == other.sidewaysAngle
                && worldId.equals(other.worldId);
        }

        @Override
        public int hashCode()
        { return Objects.hash(worldId, x, y, z, sidewaysAngle); }

        @Override
        public String toString()
        { return "" + x + ", " + y + ", " + z + ", " + sidewaysAngle + ", " + worldId; }
    }

    private static class PlayerHomesRecord
    {
        public PlayerHomesRecord(UUID playerId)
        { this.playerId = playerId; }

        private final UUID playerId;
        private final Map<String, HomeLocation> playerHomes = new HashMap<>();

        public UUID getPlayerId()
        { return playerId; }

        public void setHome(String homeName, HomeLocation location)
        { playerHomes.put(homeName, location); }

        public boolean delHome(String homeName)
        { return playerHomes.remove(homeName) != null; }

        public HomeLocation getHome(String homeName)
        { return playerHomes.get(homeName); }

        public List<String> getHomeNames()
        {
            List<String> result = new ArrayList<>(playerHomes.keySet());
            result.sort(Comparator.naturalOrder());
            return result;
        }
    }

    private Homes()
    {}

    private static final Map<UUID, PlayerHomesRecord> records = new HashMap<>();

    private static PlayerHomesRecord getOrCreatePlayerRecord(UUID playerId)
    { return records.computeIfAbsent(playerId, PlayerHomesRecord::new); }

    public static List<String> getHomeNames(UUID playerId)
    {
        PlayerHomesRecord record = records.get(playerId);

        if(record == null)
            return Collections.emptyList();

        return record.getHomeNames();
    }

    public static List<UUID> getPlayersWithHomes()
    { return new ArrayList<>(records.keySet()); }

    public static boolean hasHomes(UUID playerId)
    {
        PlayerHomesRecord record = records.get(playerId);

        if(record == null)
            return false;

        return !record.playerHomes.isEmpty();
    }

    public static void setHome(UUID playerId, String homeName, HomeLocation location)
    { getOrCreatePlayerRecord(playerId).setHome(homeName, location); }

    public static boolean delHome(UUID playerId, String homeName)
    {
        PlayerHomesRecord record = records.get(playerId);

        if(record == null)
            return false;

        return record.delHome(homeName);
    }

    public static int getAllowed(UUID playerId)
    { return getAllowedUnderPermission(playerId, NintiHomes.PERMISSION_HOMES_ADD); }

    public static int getAllowedInWorld(UUID playerId, String worldId)
    {
        String worldIdWithDots = worldId.replaceAll(":", ".");
        return getAllowedUnderPermission(playerId, NintiHomes.PERMISSION_HOMES_ADD_INWORLD + "." + worldIdWithDots);
    }

    public static int getAllowedInZone(UUID playerId, String zoneName)
    { return getAllowedUnderPermission(playerId, NintiHomes.PERMISSION_HOMES_ADD_INZONE + "." + zoneName); }

    private static int getAllowedUnderPermission(UUID playerId, String permission)
    {
        PermissionStatus pstatus = Permissions.getPlayerPermissionStatus(playerId, permission);

        if(!pstatus.hasPermission())
            return 0;

        if(pstatus.getPermissionArg() == null)
            return Integer.MAX_VALUE;

        int amountAllowed;

        try
        { amountAllowed = Integer.parseInt(pstatus.getPermissionArg()); }
        catch(NumberFormatException e)
        {
            System.err.println("Could not read number of homes allowed as number (" + pstatus.getPermissionArg() + "), "
                               + "allowing infinite homes.");
            return Integer.MAX_VALUE;
        }

        return amountAllowed;
    }

    private static Map<String, Double> getCurrencyCostsToTp(ServerPlayerEntity player,
                                                            HomeLocation destination,
                                                            PermissionStatus permStatus,
                                                            double distance)
    {
        String permArg = permStatus.getPermissionArg();

        if(permArg == null)
            return Collections.emptyMap();

        Map<String, Double> result = new HashMap<>();
        String[] argLines = permArg.split("\\r?\\n|\\r");
        double transworldDifferentiator = !getWorldId(player.getServerWorld()).equals(destination.getWorldId()) ? 1 : 0;

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

    private static Map<String, Double> getCurrencyCostsToTp(ServerPlayerEntity player, HomeLocation destination)
            throws NoPermissionToTeleportHomeException
    {
        String toWorldPermission = NintiHomes.PERMISSION_HOMES_TP_TOWORLD + "."
                                   + (destination.worldId.replaceAll(":", "."));

        PermissionStatus toWorldPermissionStatus = Permissions.getPlayerPermissionStatus(player, toWorldPermission);

        if(!toWorldPermissionStatus.hasPermission())
            throw new NoPermissionToTeleportHomeException(player, toWorldPermission);

        double xdist = destination.getX() - player.getPosX();
        double ydist = destination.getY() - player.getPosY();
        double zdist = destination.getZ() - player.getPosZ();
        double distance = Math.sqrt((xdist * xdist) + (ydist * ydist) + (zdist * zdist));

        Map<String, Double> result = getCurrencyCostsToTp(player, destination, toWorldPermissionStatus, distance);
        Map<String, Double> zonesResult = new HashMap<>();

        for(Zone zone : Zones.getZonesEntityIsIn(player))
        {
            String toZonePermission = NintiHomes.PERMISSION_HOMES_TP_TOZONE + "." + zone.getName();
            PermissionStatus toZonePermissionStatus = Permissions.getPlayerPermissionStatus(player, toZonePermission);

            if(!toZonePermissionStatus.hasPermission())
                throw new NoPermissionToTeleportHomeException(player, toZonePermission);

            for(Map.Entry<String, Double> entry
                    : getCurrencyCostsToTp(player, destination, toZonePermissionStatus, distance).entrySet())
            { zonesResult.compute(entry.getKey(), (s, x) -> (x == null) ? (entry.getValue()) : (x + entry.getValue())); }
        }

        for(Map.Entry<String, Double> entry : zonesResult.entrySet())
            result.compute(entry.getKey(), (s, x) -> (x == null || x < entry.getValue()) ? (entry.getValue()) : (x));

        return result;
    }

    public static Map<String, Double> getCostToTp(ServerPlayerEntity player, String homeName)
            throws NoSuchHomeException, NoSuchWorldException, NoPermissionToTeleportHomeException
    {
        HomeLocation destination;

        synchronized(records)
        {
            PlayerHomesRecord playerRecord = records.get(player.getUniqueID());

            if(playerRecord == null)
                throw new NoSuchHomeException(player, homeName);

            destination = playerRecord.getHome(homeName);
        }

        if(destination == null)
            throw new NoSuchHomeException(player, homeName);

        if(getWorldById(destination.getWorldId()) == null)
            throw new NoSuchWorldException(destination.getWorldId());

        return getCurrencyCostsToTp(player, destination);
    }

    public static void tpPlayerHome(ServerPlayerEntity player, String homeName)
            throws NoSuchHomeException,
                   NoSuchWorldException,
                   NoPermissionToTeleportHomeException,
                   Currencies.UnrecognisedCurrencyException,
                   CouldNotAffordToTpHomeException
    {
        HomeLocation destination;

        synchronized(records)
        {
            PlayerHomesRecord playerRecord = records.get(player.getUniqueID());

            if(playerRecord == null)
                throw new NoSuchHomeException(player, homeName);

            destination = playerRecord.getHome(homeName);
        }

        if(destination == null)
            throw new NoSuchHomeException(player, homeName);

        ServerWorld destinationWorld = getWorldById(destination.getWorldId());

        if(destinationWorld == null)
            throw new NoSuchWorldException(destination.getWorldId());

        Map<String, Double> costs = getCurrencyCostsToTp(player, destination);
        boolean couldAfford = Currencies.chargePlayer(player, costs);

        if(!couldAfford)
            throw new CouldNotAffordToTpHomeException(homeName, costs);

        player.teleport(destinationWorld,
                        destination.getX(),
                        destination.getY(),
                        destination.getZ(),
                        destination.getSidewaysAngleInDegrees(), // Yaw, 0-360
                        0);                                      // Pitch, -90 to 90. -90 is straight up. 0 is level.
    }
}
