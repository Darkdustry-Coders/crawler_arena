package crawler;

import arc.func.Cons;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.world.Tile;

import java.util.Locale;

import static mindustry.Vars.*;
import static crawler.CrawlerLogic.*;
import static crawler.Bundle.*;
import static crawler.CrawlerVars.*;

public class PlayerData {

    public static ObjectMap<String, PlayerData> datas = new ObjectMap<>();

    public Player player;
    public Locale locale;

    public int money;
    public UnitType type;

    public static Seq<PlayerData> datas() {
        return datas.values().toSeq();
    }

    public static void each(Cons<PlayerData> cons) {
        datas.each((uuid, data) -> cons.get(data));
    }

    public PlayerData(Player player) {
        this.handlePlayerJoin(player);
        this.type = UnitTypes.dagger;
        this.update();
    }

    public void handlePlayerJoin(Player player) {
        this.player = player;
        this.locale = findLocale(player);
    }

    public void update() {
        money += waveMoney() * 100;

        if (player.dead()) {
            Tile tile = world.tile(world.width() / 2 + Mathf.random(-3, 3), world.height() / 2 + Mathf.random(-3, 3));
            if (!type.flying && tile.solid()) tile.removeNet();

            applyUnit(type.spawn(tile.worldx(), tile.worldy()));
        }

        if (player.unit().health < player.unit().maxHealth) {
            Call.effect(Fx.greenCloud, player.unit().x, player.unit().y, 0f, Pal.heal);
            bundled(player, "events.heal", Pal.heal);
            
            player.unit().heal();
        }
    }

    public void applyUnit(Unit unit) {
        player.unit(unit);
        Special special = ultra.get(type = unit.type);
        
        if (special == null) return;
        unit.maxHealth = special.health();
        unit.heal(unit.maxHealth);
        unit.armor = special.armor();

        if (special.unit() != null) unit.abilities.add(new UnitSpawnAbility(special.unit(), special.cooldown(), 0f, -8f));
        else unit.abilities.each(ability -> {
            if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = special.cooldown();
        });

        unit.apply(StatusEffects.boss);
        unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
        unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
    }
}
