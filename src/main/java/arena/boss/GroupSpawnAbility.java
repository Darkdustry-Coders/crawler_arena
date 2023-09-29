package arena.boss;

import arc.math.*;
import arc.struct.Seq;
import arc.util.Time;
import arena.ai.BossAI;
import mindustry.ai.types.MissileAI;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.*;
import mindustry.type.UnitType;

import static mindustry.Vars.*;

public class GroupSpawnAbility extends Ability {
    public final Seq<Unit> units = new Seq<>();

    public final UnitType type;
    public final int amount;

    public final float x, y;
    public final float delay;

    public float timer;

    public GroupSpawnAbility(UnitType type, int amount, float x, float y) {
        this(type, amount, x, y, 900f);
    }

    public GroupSpawnAbility(UnitType type, int amount, float x, float y, float delay) {
        this.type = type;
        this.amount = amount;

        this.x = x;
        this.y = y;
        this.delay = delay;
    }

    @Override
    public void update(Unit parent) {
        timer += Time.delta * state.rules.unitBuildSpeed(parent.team);

        if (timer >= delay && Units.canCreate(parent.team, parent.type)) {
            float cx = parent.x + Angles.trnsx(parent.rotation, x, y);
            float cy = parent.y + Angles.trnsy(parent.rotation, x, y);

            for (float deg = 0; deg < 360f; deg += 360f / amount) {
                var unit = type.spawn(parent.team, cx + Mathf.cosDeg(deg) * type.hitSize, cy + Mathf.sinDeg(deg) * type.hitSize);
                unit.rotation(parent.rotation);

                if (unit.controller() instanceof MissileAI missile) missile.shooter = parent;
                else unit.controller(new BossAI());

                units.add(unit);
            }

            timer = 0f;
        }
    }

    @Override
    public void death(Unit parent) {
        units.each(Unit::kill);
    }
}