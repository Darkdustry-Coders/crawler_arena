package arena;

import arc.struct.Seq;
import mindustry.content.*;
import mindustry.entities.Units;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import useful.Bundle;
import useful.Bundle.LocaleProvider;

import java.util.Locale;

import static arc.Core.app;
import static arc.math.Mathf.*;
import static arc.struct.Seq.with;
import static arena.CrawlerVars.*;
import static mindustry.Vars.*;

public class PlayerData implements LocaleProvider {

    public static Seq<PlayerData> datas = new Seq<>();

    public Player player;
    public Locale locale;

    public int money = 0;
    public UnitType type = UnitTypes.dagger;

    public PlayerData(Player player) {
        this.handlePlayerJoin(player);
    }

    public static PlayerData getData(Player player) {
        return datas.find(data -> data.player.uuid().equals(player.uuid()));
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

        money += pow(moneyExpBase, 3 + state.wave * moneyRamp);

        if (player.dead()) {
            respawn();
            return;
        }

        if (player.unit().health < player.unit().maxHealth) {
            player.unit().heal();
            Call.effect(player.con, Fx.greenCloud, player.unit().x, player.unit().y, 0f, Pal.heal);

            Bundle.bundled(player, "events.heal");
        }
    }

    public void respawn() {
        var unit = Units.closest(player.team(), world.unitWidth() / 2f, world.unitHeight() / 2f, u -> !u.isPlayer() && u.type == type && !u.dead);
        if (unit != null) {
            controlUnit(unit);
            return;
        }

        var tile = world.tile(world.width() / 2 + range(8), world.height() / 2 + range(8));
        if (!type.flying && tile.solid()) tile.removeNet();

        controlUnit(applyUnit(type.spawn(tile.worldx(), tile.worldy())));
    }

    public void controlUnit(Unit unit) {
        unit.controller(player);
        Call.setCameraPosition(player.con, unit.x, unit.y);
    }

    public Unit applyUnit(Unit unit) {
        var special = specialUnits.get(type = unit.type);
        if (special == null) return unit;

        unit.health = unit.maxHealth = special.health();
        unit.armor = special.armor();

        var abilities = with(unit.abilities);

        if (special.unit() != null) abilities.add(new UnitSpawnAbility(special.unit(), special.cooldown(), 0f, -8f));
        else abilities.each(ability -> {
            if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = special.cooldown();
        });

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