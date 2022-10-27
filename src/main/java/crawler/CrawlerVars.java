package crawler;

import arc.struct.OrderedMap;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class CrawlerVars {

    public static final float AIRange = 80000f;

    /** Wave when it's over */
    public static final int bossWave = 25;
    /** Interval between waves in seconds */
    public static final int waveDelay = 10, firstWaveDelay = 15;

    /** The maximum number of units that players can spawn */
    public static final int unitCap = 72;
    /** The maximum number of units of the same type that can be spawned at the start of a wave */
    public static final int maxUnits = 1000;
    /** The higher this number, the slower unit's stats grow */
    public static final float statDiv = 128f;

    /** Minimum required wave to spawn reinforcement */
    public static final int helpMinWave = 8;
    /** Reinforcements appear every second wave */
    public static final int helpSpacing = 2;
    /** Extra time needed to get support */
    public static final int helpExtraTime = 50;

    public static final float moneyExpBase = 2.2f;
    public static final float moneyRamp = 1f / 1.5f;
    public static final float extraMoneyRamp = 1f / 4000f;
    public static final float moneyMultiplier = 4.5f;

    public static final float crawlersExpBase = 2.2f;
    public static final float crawlersRamp = 1f / 1.5f;
    public static final float extraCrawlersRamp = 1f / 150f;
    public static final float crawlersMultiplier = 1f / 10f;

    public static final OrderedMap<Block, Integer> aidBlocks = new OrderedMap<>();

    public static final OrderedMap<UnitType, Integer> enemyCuts = new OrderedMap<>();
    public static final OrderedMap<UnitType, Integer> costs = new OrderedMap<>();
    public static final OrderedMap<UnitType, Special> ultra = new OrderedMap<>();

    public static void load() {
        aidBlocks.putAll(
                Blocks.liquidSource, 4,
                Blocks.powerSource, 4,
                Blocks.itemSource, 6,
                Blocks.heatSource, 2,

                Blocks.thoriumWallLarge, 8,
                Blocks.surgeWallLarge, 4,
                Blocks.berylliumWallLarge, 8,
                Blocks.tungstenWallLarge, 6,

                Blocks.mendProjector, 3,
                Blocks.forceProjector, 2,
                Blocks.regenProjector, 1,

                Blocks.shieldedWall, 6,
                Blocks.shieldProjector, 2,
                Blocks.largeShieldProjector, 1,

                Blocks.repairPoint, 4,
                Blocks.repairTurret, 2,

                Blocks.overdriveProjector, 1,

                Blocks.arc, 6,
                Blocks.lancer, 4,
                Blocks.ripple, 2,
                Blocks.cyclone, 1,
                Blocks.swarmer, 2,
                Blocks.spectre, 1,
                Blocks.foreshadow, 1,

                Blocks.breach, 4,
                Blocks.diffuse, 3,
                Blocks.sublimate, 2,
                Blocks.titan, 1,
                Blocks.afflict, 1,
                Blocks.lustre, 1,
                Blocks.scathe, 1
        );

        enemyCuts.putAll(
                UnitTypes.atrax, 10,
                UnitTypes.spiroct, 50,
                UnitTypes.arkyid, 1000,
                UnitTypes.toxopid, 20000
        );

        costs.putAll(
                UnitTypes.dagger, 25,
                UnitTypes.flare, 75,
                UnitTypes.nova, 100,
                UnitTypes.mace, 200,
                UnitTypes.atrax, 250,

                UnitTypes.horizon, 250,
                UnitTypes.pulsar, 300,
                UnitTypes.retusa, 400,
                UnitTypes.fortress, 500,
                UnitTypes.minke, 750,

                UnitTypes.oxynoe, 850,
                UnitTypes.risso, 1000,
                UnitTypes.spiroct, 1500,
                UnitTypes.quasar, 2000,
                UnitTypes.stell, 2000,
                UnitTypes.merui, 2200,
                UnitTypes.mega, 2500,
                UnitTypes.elude, 2500,

                UnitTypes.zenith, 2500,
                UnitTypes.locus, 3800,
                UnitTypes.avert, 4000,
                UnitTypes.cleroi, 4800,
                UnitTypes.cyerce, 5000,
                UnitTypes.bryde, 5000,
                UnitTypes.precept, 6400,
                UnitTypes.anthicus, 7000,
                UnitTypes.obviate, 7250,
                UnitTypes.crawler, 7500,
                UnitTypes.scepter, 10000,
                UnitTypes.vela, 15000,
                UnitTypes.vanquish, 17500,

                UnitTypes.antumbra, 18000,
                UnitTypes.quell, 23500,
                UnitTypes.quad, 25000,
                UnitTypes.arkyid, 25000,
                UnitTypes.tecta, 25000,
                UnitTypes.aegires, 30000,

                UnitTypes.sei, 75000,
                UnitTypes.poly, 100000,
                UnitTypes.eclipse, 175000,
                UnitTypes.disrupt, 225000,
                UnitTypes.conquer, 245000,
                UnitTypes.corvus, 250000,
                UnitTypes.reign, 250000,

                UnitTypes.oct, 250000,
                UnitTypes.collaris, 300000,
                UnitTypes.toxopid, 325000,
                UnitTypes.navanax, 350000,
                UnitTypes.omura, 1500000,
                UnitTypes.mono, 3750000
        );

        ultra.putAll(
                UnitTypes.crawler, new Special(400f, 10f, 60f, UnitTypes.crawler),
                UnitTypes.mono, new Special(100000f, 20f, 900f, UnitTypes.navanax),
                UnitTypes.poly, new Special(500f, 0f, 60f, UnitTypes.poly),
                UnitTypes.omura, new Special(100000f, 20f, 30f, null)
        );
    }

    public record Special(float health, float armor, float cooldown, UnitType unit) {}
}