package arena;

import arc.struct.OrderedMap;
import mindustry.content.*;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class CrawlerVars {

    /** Wave when it's over. */
    public static final int bossWave = 25;

    /** Interval between waves in ticks. */
    public static final int firstWaveDelay = 15, waveDelay = 10, additionalDelay = 1;

    /** The maximum number of units that players can spawn. */
    public static final int unitCap = 48;

    /** The maximum number of units of the same type that can be spawned in the wave. */
    public static final int maxUnits = 144;

    /** The higher this number, the slower unit's stats grow. */
    public static final float statDiv = 96f;

    /** Minimum required wave to spawn reinforcement. */
    public static final int helpMinWave = 8;

    /** Reinforcements appear every second wave. */
    public static final int helpSpacing = 2;

    /** Extra time needed to get support. */
    public static final int helpExtraTime = 5;

    public static final float moneyExpBase = 2.2f;
    public static final float moneyRamp = 0.67f;

    public static OrderedMap<UnitType, Integer> shifts;
    public static OrderedMap<Block, Integer> reinforcement;

    public static OrderedMap<UnitType, UnitAbility> abilities;

    public static void load() {
        shifts = OrderedMap.of(
                UnitTypes.crawler, 0,
                UnitTypes.atrax, 5,
                UnitTypes.spiroct, 10,
                UnitTypes.arkyid, 15,
                UnitTypes.toxopid, 20
        );

        reinforcement = OrderedMap.of(
                Blocks.itemSource, 6,
                Blocks.liquidSource, 4,
                Blocks.powerSource, 4,
                Blocks.heatSource, 4,

                Blocks.thoriumWallLarge, 8,
                Blocks.surgeWallLarge, 6,
                Blocks.tungstenWallLarge, 8,
                Blocks.shieldedWall, 6,

                Blocks.mendProjector, 3,
                Blocks.overdriveProjector, 1,
                Blocks.forceProjector, 2,
                Blocks.regenProjector, 1,
                Blocks.repairPoint, 4,
                Blocks.repairTurret, 2,

                Blocks.hail, 4,
                Blocks.arc, 4,
                Blocks.wave, 2,
                Blocks.lancer, 2,
                Blocks.swarmer, 2,
                Blocks.salvo, 2,
                Blocks.fuse, 1,
                Blocks.ripple, 1,
                Blocks.cyclone, 1,
                Blocks.foreshadow, 1,
                Blocks.spectre, 1,
                Blocks.meltdown, 1,
                Blocks.segment, 2,
                Blocks.tsunami, 1,

                Blocks.breach, 2,
                Blocks.diffuse, 2,
                Blocks.sublimate, 2,
                Blocks.titan, 1,
                Blocks.afflict, 1,
                Blocks.lustre, 1,
                Blocks.scathe, 1,
                Blocks.smite, 1,
                Blocks.malign, 1
        );

        abilities = OrderedMap.of(
                UnitTypes.crawler, new UnitAbility(400f, 40f, 60f, UnitTypes.crawler),
                UnitTypes.mono, new UnitAbility(100000f, 1000f, 900f, UnitTypes.omura, UnitTypes.navanax),
                UnitTypes.poly, new UnitAbility(500f, 50f, 60f, UnitTypes.poly),
                UnitTypes.omura, new UnitAbility(100000f, 20f, 30f, UnitTypes.flare)
        );
    }

    public record UnitAbility(float health, float armor, float cooldown, UnitType... types) {}
}