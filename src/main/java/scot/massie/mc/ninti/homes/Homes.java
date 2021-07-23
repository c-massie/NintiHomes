package scot.massie.mc.ninti.homes;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import scot.massie.lib.utils.StringUtils;
import scot.massie.mc.ninti.core.NintiCore;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Homes
{
    private static final Map<UUID, PlayerHomesList> homesLists = new HashMap<>();
    private static final Path saveFileLocation = NintiCore.DATA_FOLDER.resolve("homes.csv");
    private static final Path saveFileBackupLocation = NintiCore.DATA_FOLDER.resolve("homes_backup.csv");

    private Homes()
    {}

    public static PlayerHomesList getFor(UUID playerId)
    {
        synchronized(homesLists)
        { return homesLists.computeIfAbsent(playerId, PlayerHomesList::new); }
    }

    public static PlayerHomesList getFor(PlayerEntity player)
    { return getFor(player.getUniqueID()); }

    public static PlayerHomesList getFor(GameProfile player)
    { return getFor(player.getId()); }

    public static PlayerHomesList getForIfPresent(UUID playerId)
    {
        synchronized(homesLists)
        { return homesLists.get(playerId); }
    }

    public static PlayerHomesList getForIfPresent(PlayerEntity player)
    { return getForIfPresent(player.getUniqueID()); }

    public static PlayerHomesList getForIfPresent(GameProfile player)
    { return getForIfPresent(player.getId()); }

    public static Collection<PlayerHomesList> getAllPlayerHomeLists()
    {
        synchronized(homesLists)
        { return new ArrayList<>(homesLists.values()); }
    }

    public static boolean hasAny(UUID playerId)
    {
        synchronized(homesLists)
        {
            PlayerHomesList phl = homesLists.get(playerId);
            return (phl != null) && (!phl.isEmpty());
        }
    }

    public static boolean hasAny(PlayerEntity player)
    { return hasAny(player.getUniqueID()); }

    public static boolean hasAny(GameProfile player)
    { return hasAny(player.getId()); }

    public static List<UUID> getPlayersWithHomes()
    {
        synchronized(homesLists)
        { return new ArrayList<>(homesLists.keySet()); }
    }

    public static void save()
    {
        // CSV file
        // UUID, home name, world id, x, y, z, pitch, yaw

        try
        { Files.move(saveFileLocation, saveFileBackupLocation, StandardCopyOption.REPLACE_EXISTING); }
        catch(IOException e)
        {
            e.printStackTrace();
            System.err.println("Could not save homes file - could not move homes file to backup location.");
            return;
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(saveFileLocation.toFile())))
        {
            // Header row
            writer.write(StringUtils.toCSVRow(Arrays.asList("player id", "home name",
                                                            "world id", "x", "y", "z", "pitch", "yaw"),
                                              true));
            writer.newLine();

            List<PlayerHomesList> pHomesLists;

            synchronized(homesLists)
            { pHomesLists = new ArrayList<>(homesLists.values()); }

            for(PlayerHomesList pHomesList : pHomesLists)
            {
                String playerId = pHomesList.playerId.toString();

                for(PlayerHome pHome : pHomesList.getHomes())
                {
                    List<String> row = new ArrayList<>(8);
                    row.add(playerId);
                    row.add(pHome.homeName);
                    row.add(pHome.location.getWorldId());
                    row.add(Double.toString(pHome.location.getX()));
                    row.add(Double.toString(pHome.location.getY()));
                    row.add(Double.toString(pHome.location.getZ()));
                    row.add(Double.toString(pHome.location.getPitch()));
                    row.add(Double.toString(pHome.location.getYaw()));

                    writer.write(StringUtils.toCSVRow(row, true));
                    writer.newLine();
                }
            }

            System.out.println("Homes saved.");
        }
        catch(IOException e)
        {
            e.printStackTrace();
            System.err.println("Could not save homes file, reverting to ");

            try
            { Files.move(saveFileLocation, saveFileBackupLocation, StandardCopyOption.REPLACE_EXISTING); }
            catch(IOException e2)
            {
                e2.printStackTrace();
                System.err.println("Could not restore homes file - could not move old homes file to proper location."
                                   + "\nIt should be at: " + saveFileBackupLocation.toString());
                return;
            }

            return;
        }
    }

    public static void load()
    {
        synchronized(homesLists)
        {
            homesLists.clear();

            try(BufferedReader reader = new BufferedReader(new FileReader(saveFileLocation.toFile())))
            {
                String line = reader.readLine(); // Ignore first line, this should be the header.

                while((line = reader.readLine()) != null)
                {
                    List<String> fields = StringUtils.parseCSVRow(line);

                    if(fields.size() != 8)
                        System.err.println("Malformed home record: " + line);

                    UUID playerId;
                    String homeName;
                    String worldId;
                    double x, y, z, pitch, yaw;

                    try
                    {
                        playerId = UUID.fromString(fields.get(0));
                    }
                    catch(IllegalArgumentException e)
                    {
                        System.err.println("Malformed player ID, skipping: " + fields.get(0));
                        continue;
                    }

                    // TO DO: Include validation for these.
                    homeName = fields.get(1);
                    worldId = fields.get(2);

                    //region parse x, y, z, pitch, and yaw
                    try
                    {
                        x = Double.parseDouble(fields.get(3));
                    }
                    catch(NumberFormatException e)
                    {
                        System.err.println("Invalid X value, must be a number, skipping: " + fields.get(3));
                        continue;
                    }

                    try
                    {
                        y = Double.parseDouble(fields.get(4));
                    }
                    catch(NumberFormatException e)
                    {
                        System.err.println("Invalid Y value, must be a number, skipping: " + fields.get(4));
                        continue;
                    }

                    try
                    {
                        z = Double.parseDouble(fields.get(5));
                    }
                    catch(NumberFormatException e)
                    {
                        System.err.println("Invalid Z value, must be a number, skipping: " + fields.get(5));
                        continue;
                    }

                    try
                    {
                        pitch = Double.parseDouble(fields.get(6));
                    }
                    catch(NumberFormatException e)
                    {
                        System.err.println("Invalid pitch value, must be a number, skipping: " + fields.get(6));
                        continue;
                    }

                    try
                    {
                        yaw = Double.parseDouble(fields.get(7));
                    }
                    catch(NumberFormatException e)
                    {
                        System.err.println("Invalid yaw value, must be a number, skipping: " + fields.get(7));
                        continue;
                    }
                    //endregion

                    getFor(playerId).setHome(homeName, new EntityLocation(worldId, x, y, z, pitch, yaw));
                }

                System.out.println("Homes loaded.");
            }
            catch(FileNotFoundException e)
            {
                e.printStackTrace();
                System.out.println("No existing homes file, so the homes registry is empty.");
            }
            catch(IOException e)
            {
                e.printStackTrace();
                System.err.println("Could not read the homes file, so the homes registry is empty.");
                homesLists.clear();
            }
        }
    }
}
