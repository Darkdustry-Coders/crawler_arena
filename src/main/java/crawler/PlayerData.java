package crawler;

import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.content.*;
import mindustry.entities.Units;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;

import java.util.Locale;

import static arc.Core.app;
import static arc.struct.Seq.with;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;
import static mindustry.Vars.*;

public class PlayerData {

    public static Seq<PlayerData> datas = new Seq<>();

    public Player player;
    public Locale locale;

    public int money = 0;
    public UnitType type = UnitTypes.dagger;

    public static PlayerData getData(String uuid) {
        return datas.find(data -> data.player.uuid().equals(uuid));
    }

    public PlayerData(Player player) {
        handlePlayerJoin(player);
    }

    public void handlePlayerJoin(Player player) {
        this.player = player;
        this.locale = findLocale(player);

        app.post(this::respawn);
    }

    public void reset() {
        money = 0;
        type = UnitTypes.dagger;

        app.post(this::respawn);
    }

    public void afterWave() {
        if (!player.con.isConnected()) return;

        money += Mathf.pow(moneyExpBase, 1f + state.wave * moneyRamp + Mathf.pow(state.wave, 2) * extraMoneyRamp) * moneyMultiplier;

        if (player.dead()) {
            respawn();
            return;
        }

        if (player.unit().health < player.unit().maxHealth) {
            player.unit().heal();
            Call.effect(player.con, Fx.greenCloud, player.unit().x, player.unit().y, 0f, Pal.heal);

            bundled(player, "events.heal");
        }
    }

    public void respawn() {
        var unit = Units.closest(player.team(), world.unitWidth() / 2f, world.unitHeight() / 2f, u -> u.type == type && !u.isPlayer());
        if (unit != null) {
            player.unit(unit);
            return;
        }

        var tile = world.tile(world.width() / 2 + Mathf.random(-3, 3), world.height() / 2 + Mathf.random(-3, 3));
        if (!type.flying && tile.solid()) tile.removeNet();

        player.unit(applyUnit(type.spawn(tile.worldx(), tile.worldy())));
    }

    public Unit applyUnit(Unit unit) {
        var special = ultra.get(type = unit.type);

        if (special == null) return unit;

        unit.health = unit.maxHealth = special.health();
        unit.armor = special.armor();

        var abilities = with(unit.abilities);

        if (special.unit() != null) abilities.add(new UnitSpawnAbility(special.unit(), special.cooldown(), 0f, -8f));
        else abilities.each(ability -> {
            if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = special.cooldown();
        });

        unit.abilities(abilities.toArray());

        unit.apply(StatusEffects.boss);
        unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
        unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);

        return unit;
    }
}