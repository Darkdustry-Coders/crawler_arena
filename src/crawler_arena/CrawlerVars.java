package crawler_arena;

import arc.struct.ObjectFloatMap;
import arc.struct.ObjectIntMap;
import arc.struct.OrderedMap;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class CrawlerVars {

    public static int unitCap = 96;
    public static float tipChance = 1f / 30000f;

    public static int bossWave = 25;
    public static int crawlersCeiling = 10000000;
    public static int maxUnits = 2000;
    public static int keepCrawlers = 800;
    public static float crawlersExpBase = 2.2f;
    public static float crawlersRamp = 1f / 1.5f;
    public static float extraCrawlersRamp = 1f / 150f;
    public static float crawlersMultiplier = 1f / 10f;

    public static float moneyExpBase = 2.2f;
    public static float moneyRamp = 1f / 1.5f;
    public static float extraMoneyRamp = 1f / 4000f;
    public static float moneyMultiplier = 4f;

    public static float enemySpeedBoost = 0.00002f;
    public static float crawlerHealthBase = 60f;
    public static float crawlerSpeedBase = 0.43f;
    public static float statScalingNormal = 1f / 100f;
    public static float healthMultiplierBase = 1f / 5f;

    public static float firstWaveDelay = 15f;
    public static float waveDelay = 10f;
    public static float reinforcementWaveDelayBase = 50f;
    public static float reinforcementWaveDelayRamp = 3f;
    public static float reinforcementWaveDelayMax = 90f;

    public static int reinforcementMinWave = 8;
    public static int reinforcementSpacing = 2;
    public static int reinforcementFactor = 3;
    public static int reinforcementScaling = 2;
    public static int reinforcementMax = 60 * reinforcementFactor;
    public static float rareAidChance = 1f / 20f;

    public static ObjectIntMap<Block> aidBlockAmounts = new ObjectIntMap<>();
    public static ObjectIntMap<Block> rareAidBlockAmounts = new ObjectIntMap<>();
    public static OrderedMap<UnitType, Integer> unitCosts = new OrderedMap<>();

    public static float playerCrawlerHealth = 400f;
    public static float playerCrawlerArmor = 10f;
    public static float playerCrawlerCooldown = 60f;
    public static float playerMonoHealth = 100000f;
    public static float playerMonoArmor = 20f;
    public static float playerMonoCooldown = 300f;
    public static Seq<UnitType> playerMonoSpawnTypes = Seq.with(UnitTypes.navanax, UnitTypes.toxopid, UnitTypes.corvus);
    public static float playerPolyHealth = 500f;
    public static float playerPolyArmor = 100f;
    public static float playerPolyCooldown = 60f;
    public static float playerOmuraHealth = 100000f;
    public static float playerOmuraArmor = 20f;
    public static float playerOmuraCooldown = 30f;

    public static Seq<UnitType> enemyTypes = Seq.with(UnitTypes.toxopid, UnitTypes.arkyid, UnitTypes.spiroct, UnitTypes.atrax); // *MUST* be ordered from most to least powerful
    public static ObjectIntMap<UnitType> enemyThresholds = new ObjectIntMap<>();
    public static ObjectIntMap<UnitType> enemyCrawlerCuts = new ObjectIntMap<>();
    public static ObjectFloatMap<UnitType> defaultEnemySpeeds = new ObjectFloatMap<>();
    public static float crawlerHealthRamp = 1f;
    public static float crawlerSpeedRamp = 0.003f;
    public static int bossT1Cap = 2;
    public static int bossT2Cap = 5;
    public static float bossHealthMultiplier = 6f;
    public static float bossScepterDelayBase = 600f;

    static {
        enemyTypes.each(type -> defaultEnemySpeeds.put(type, type.speed));

        enemyCrawlerCuts.putAll(
                UnitTypes.atrax, 10,
                UnitTypes.spiroct, 50,
                UnitTypes.arkyid, 1000,
                UnitTypes.toxopid, 20000
        );

        enemyThresholds.putAll(
                UnitTypes.atrax, 100,
                UnitTypes.spiroct, 400,
                UnitTypes.arkyid, 1000,
                UnitTypes.toxopid, 20000
        );

        unitCosts.putAll(
                UnitTypes.dagger, 25,
                UnitTypes.flare, 75,
                UnitTypes.nova, 100,
                UnitTypes.mace, 200,
                UnitTypes.atrax, 250,

                UnitTypes.horizon, 250,
                UnitTypes.pulsar, 300,
                UnitTypes.retusa, 400,
                UnitTypes.risso, 500,
                UnitTypes.minke, 750,

                UnitTypes.oxynoe, 850,
                UnitTypes.fortress, 1500,
                UnitTypes.spiroct, 1500,
                UnitTypes.quasar, 2000,
                UnitTypes.mega, 2500,

                UnitTypes.zenith, 2500,
                UnitTypes.cyerce, 5000,
                UnitTypes.bryde, 5000,
                UnitTypes.crawler, 7500,
                UnitTypes.vela, 15000,

                UnitTypes.antumbra, 18000,
                UnitTypes.scepter, 20000,
                UnitTypes.quad, 25000,
                UnitTypes.arkyid, 25000,
                UnitTypes.aegires, 30000,

                UnitTypes.sei, 75000,
                UnitTypes.poly, 100000,
                UnitTypes.eclipse, 175000,
                UnitTypes.corvus, 250000,
                UnitTypes.reign, 250000,

                UnitTypes.oct, 250000,
                UnitTypes.toxopid, 325000,
                UnitTypes.navanax, 350000,
                UnitTypes.omura, 1500000,
                UnitTypes.mono, 3750000
        );

        aidBlockAmounts.putAll(
                Blocks.liquidSource, 4,
                Blocks.powerSource, 4,
                Blocks.itemSource, 6,
                Blocks.constructor, 1,

                Blocks.thoriumWallLarge, 8,
                Blocks.surgeWallLarge, 4,

                Blocks.mendProjector, 3,
                Blocks.forceProjector, 2,
                Blocks.repairPoint, 4,
                Blocks.repairTurret, 2,

                Blocks.overdriveProjector, 1,

                Blocks.arc, 6,
                Blocks.lancer, 4,
                Blocks.ripple, 2,
                Blocks.cyclone, 1,
                Blocks.swarmer, 2,
                Blocks.tsunami, 1,
                Blocks.spectre, 1,
                Blocks.foreshadow, 1
        );

        rareAidBlockAmounts.putAll(
                Blocks.largeConstructor, 1,
                Blocks.groundFactory, 1,
                Blocks.airFactory, 1,
                Blocks.navalFactory, 1,
                Blocks.overdriveDome, 4,
                Blocks.boulder, 100
        );
    }
}
