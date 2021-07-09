package scot.massie.mc.ninti.homes;

import scot.massie.lib.maths.Equation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Homes
{
    static final Equation.Builder teleportCostEquationBuilder =
            new Equation.Builder()
                    .withComparativeOperators()
                    .withVariable("distance", 0)
                    .withVariable("isAcrossWorlds", 0);

    static final Map<UUID, PlayerHomesList> homesLists = new HashMap<>();

    private Homes()
    {}

    public static void save()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public static void load()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
