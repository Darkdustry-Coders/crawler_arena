package arena;

import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.ai.types.CommandAI;
import mindustry.content.*;
import mindustry.entities.Units;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import useful.*;
import useful.Bundle.LocaleProvider;

import java.util.Locale;

import static arc.Core.app;
import static arena.CrawlerVars.*;
import static mindustry.Vars.*;

public class PlayerData implements LocaleProvider {

    public static ExtendedMap<String, PlayerData> datas = new ExtendedMap<>();

    public Player player;
    public Locale locale;

    public int money = 0;
    public UnitType type = UnitTypes.dagger;

    public PlayerData(Player player) {
        this.handlePlayerJoin(player);
    }

    public void handlePlayerJoin(Player player) {
        this.player = player;
        this.locale = Bundle.locale(player);
        this.type = UnitTypes.dagger;

        app.post(this::respawn);
    }

    public void reset() {
        this.money = 0;
        this.type = UnitTypes.dagger;

        app.post(this::respawn);
    }

    public void afterWave() {
        if (!player.con.isConnected()) return;
        money += Mathf.pow(moneyExpBase, 3 + state.wave * moneyRamp);

        if (player.dead())
            respawn();

        if (player.unit().health < player.unit().maxHealth) {
            player.unit().heal();
            Call.effect(player.con, Fx.greenCloud, player.unit().x, player.unit().y, 0f, Pal.heal);

            Bundle.send(player, "events.heal");
        }
    }

    public void respawn() {
        var unit = Units.closest(player.team(), player.x, player.y, u -> u.controller() instanceof CommandAI && u.type == type && !u.dead);
        if (unit != null) {
            controlUnit(unit);
            return;
        }

        var tile = world.tile(world.width() / 2 + Mathf.range(8), world.height() / 2 + Mathf.range(8));
        if (!type.flying && tile.solid()) tile.removeNet();

        controlUnit(applyUnit(type.spawn(tile.worldx(), tile.worldy())));
    }

    public void controlUnit(Unit unit) {
        player.unit(unit);
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

    @Override
    public Locale locale() {
        return locale;
    }
}