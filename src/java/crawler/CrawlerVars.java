package crawler;

import arc.struct.OrderedMap;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class CrawlerVars {

    public static int bossWave = 25;
    public static int waveDelay = 10;

    public static int unitCap = 96;
    public static int statDiv = 100;

    /** Minimum required wave to spawn reinforcement */
    public static int helpMinWave = 8;
    /** Reinforcements appear every second wave */
    public static int helpSpacing = 2;
    /** Extra time needed to get support */
    public static int helpExtraTime = 50;

    public static float moneyBase = 2.2f;
    public static float moneyRamp = 1f / 1.5f;
    public static float extraMoneyRamp = 1f / 4000f;

    public static float enemiesBase = 2.2f;
    public static float enemiesRamp = 1f / 1.5f;
    public static float extraEnemiesRamp = 1f / 150f;

    public static int reinforcementMinWave = 8;
    public static int reinforcementSpacing = 2;
    public static int reinforcementFactor = 3;
    public static int reinforcementScaling = 2;
    public static int reinforcementMax = 60 * reinforcementFactor;
    public static float rareAidChance = 1f / 20f;

    public static OrderedMap<Block, Integer> aidBlockAmounts = new OrderedMap<>();
    public static OrderedMap<Block, Integer> rareAidBlockAmounts = new OrderedMap<>();

    public static OrderedMap<UnitType, Integer> enemy;
    public static OrderedMap<UnitType, Integer> costs;
    public static OrderedMap<UnitType, Special> ultra;

    public static void load() {
        aidBlockAmounts.putAll(
                Blocks.liquidSource,       4,
                Blocks.powerSource,        4,
                Blocks.itemSource,         6,
                Blocks.constructor,        1,

                Blocks.thoriumWallLarge,   8,
                Blocks.surgeWallLarge,     4,

                Blocks.mendProjector,      3,
                Blocks.forceProjector,     2,
                Blocks.repairPoint,        4,
                Blocks.repairTurret,       2,

                Blocks.overdriveProjector, 1,

                Blocks.arc,                6,
                Blocks.lancer,             4,
                Blocks.ripple,             2,
                Blocks.cyclone,            1,
                Blocks.swarmer,            2,
                Blocks.tsunami,            1,
                Blocks.spectre,            1,
                Blocks.foreshadow,         1
        );

        rareAidBlockAmounts.putAll(
                Blocks.largeConstructor, 1,
                Blocks.groundFactory,    1,
                Blocks.airFactory,       1,
                Blocks.navalFactory,     1,
                Blocks.overdriveDome,    4,
                Blocks.boulder,          100
        );

        enemy = OrderedMap.of(
                UnitTypes.crawler, 3,
                UnitTypes.atrax, 10,
                UnitTypes.spiroct, 50,
                UnitTypes.arkyid, 1000,
                UnitTypes.toxopid, 20000
        );

        costs = OrderedMap.of(
                UnitTypes.dagger,   25,
                UnitTypes.flare,    75,
                UnitTypes.nova,     100,
                UnitTypes.mace,     200,
                UnitTypes.atrax,    250,

                UnitTypes.horizon,  250,
                UnitTypes.pulsar,   300,
                UnitTypes.retusa,   400,
                UnitTypes.risso,    500,
                UnitTypes.minke,    750,

                UnitTypes.oxynoe,   850,
                UnitTypes.fortress, 1500,
                UnitTypes.spiroct,  1500,
                UnitTypes.quasar,   2000,
                UnitTypes.mega,     2500,

                UnitTypes.zenith,   2500,
                UnitTypes.cyerce,   5000,
                UnitTypes.bryde,    5000,
                UnitTypes.crawler,  7500,
                UnitTypes.vela,     15000,

                UnitTypes.antumbra, 18000,
                UnitTypes.scepter,  20000,
                UnitTypes.quad,     25000,
                UnitTypes.arkyid,   25000,
                UnitTypes.aegires,  30000,

                UnitTypes.sei,      75000,
                UnitTypes.poly,     100000,
                UnitTypes.eclipse,  175000,
                UnitTypes.corvus,   250000,
                UnitTypes.reign,    250000,

                UnitTypes.oct,      250000,
                UnitTypes.toxopid,  325000,
                UnitTypes.navanax,  350000,
                UnitTypes.omura,    1500000,
                UnitTypes.mono,     3750000
        );

        ultra = OrderedMap.of(
                UnitTypes.crawler, new Special(400f,    10f,  60f,  UnitTypes.crawler),
                UnitTypes.mono,    new Special(100000f, 20f,  300f, UnitTypes.navanax),
                UnitTypes.poly,    new Special(500f,    100f, 60f,  UnitTypes.poly),
                UnitTypes.omura,   new Special(100000f, 20f,  30f,  null)
        );
    }

    public static record Special(float health, float armor, float cooldown, UnitType unit) {}
}
