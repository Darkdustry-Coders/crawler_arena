package arena;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.content.*;
import mindustry.entities.Units;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.gen.*;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import useful.*;

import static arena.CrawlerVars.*;
import static mindustry.Vars.*;

public class PlayerData {
    public static final ExtendedMap<String, PlayerData> datas = new ExtendedMap<>();

    public Player player;
    public UnitType type;

    public int money;

    public PlayerData(Player player) {
        this.player = player;
        this.reset();
    }

    public void join(Player player) {
        this.player = player;
        this.respawn();
    }

    public void reset() {
        this.money = 0;
        this.type = UnitTypes.dagger;

        this.respawn();
    }

    public void afterWave() {
        if (!player.con.isConnected()) return;
        money += Mathf.ceil(Mathf.pow(moneyExpBase, 3 + state.wave * moneyRamp));

        if (player.dead())
            respawn();

        if (player.unit().health < player.unit().maxHealth) {
            player.unit().heal();
            Bundle.send(player, "events.heal");
        }
    }

    public void respawn() {
        var closest = Units.closest(player.team(), player.x, player.y, unit -> !unit.isPlayer() && unit.type == type && !unit.dead);
        if (closest != null) {
            controlUnit(closest);
            return;
        }

        var tiles = new Seq<Tile>();
        var center = world.tile(world.width() / 2, world.height() / 2);

        center.circle(8, (x, y) -> {
            if (type.flying || world.isAccessible(x, y))
                tiles.add(world.tile(x, y));
        });

        if (tiles.isEmpty())
            center.removeNet();

        controlUnit(applyUnit(type.spawn(tiles.any() ? tiles.random() : center)));
    }

    public void controlUnit(Unit unit) {
        player.unit(unit);

        Call.setPosition(player.con, unit.x, unit.y);
        Call.setCameraPosition(player.con, unit.x, unit.y);
    }

    public Unit applyUnit(Unit unit) {
        var ability = abilities.get(type = unit.type);
        if (ability == null) return unit;

        unit.health = unit.maxHealth = ability.health();
        unit.armor = ability.armor();

        var abilities = Seq.with(unit.abilities);
        abilities.removeAll(UnitSpawnAbility.class::isInstance);

        abilities.add(new UnitSpawnAbility(Structs.random(ability.types()), ability.cooldown(), 0f, -8f));
        unit.abilities(abilities.toArray());

        unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
        unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        unit.apply(StatusEffects.boss);

        return unit;
    }
}
