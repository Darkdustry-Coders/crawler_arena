package crawler;

import arc.struct.OrderedMap;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;

public class CrawlerVarsNew {

    public static int bossWave = 25;
    public static int waveDelay = 10;

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

    public static OrderedMap<UnitType, Integer> enemy;
    public static OrderedMap<UnitType, Integer> costs;

    public static void load() {
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
    }
}
