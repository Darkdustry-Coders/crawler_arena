package crawler_arena;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.ai.types.FlyingAI;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.entities.bullet.SapBulletType;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import static crawler_arena.CrawlerVars.*;
import static mindustry.Vars.*;

public class CrawlerArenaMod extends Plugin {

    public static boolean firstWaveLaunched = false, isWaveGoing = false, isGameOver = false;

    public static int wave = 1;
    public static float statScaling = 1f;

    // TODO поменять на простой ObjectMap
    public static ObjectIntMap<String> money = new ObjectIntMap<>();
    public static ObjectIntMap<String> leftUnits = new ObjectIntMap<>();
    public static ObjectMap<String, UnitType> units = new ObjectMap<>();

    public static long timer = Time.millis();

    @Override
    public void init() {
        logic = new CrawlerArenaLogic();

        content.units().each(type -> {
            type.payloadCapacity = (6f * 6f) * tilePayload;
            type.defaultController = FlyingAI::new;
        });

        // TODO нахер так задавать
        UnitTypes.crawler.defaultController = ArenaAI::new;
        UnitTypes.atrax.defaultController = ArenaAI::new;
        UnitTypes.spiroct.defaultController = ArenaAI::new;
        UnitTypes.arkyid.defaultController = ArenaAI::new;
        UnitTypes.toxopid.defaultController = ArenaAI::new;

        UnitTypes.poly.defaultController = SwarmAI::new;

        UnitTypes.scepter.defaultController = ArenaAI::new;
        UnitTypes.reign.defaultController = ArenaAI::new;

        UnitTypes.risso.flying = true;
        UnitTypes.minke.flying = true;
        UnitTypes.bryde.flying = true;
        UnitTypes.sei.flying = true;
        UnitTypes.omura.flying = true;

        UnitTypes.retusa.flying = true;
        UnitTypes.oxynoe.flying = true;
        UnitTypes.cyerce.flying = true;
        UnitTypes.aegires.flying = true;
        UnitTypes.navanax.flying = true;

        // TODO изменить ИИ чтобы не требовал maxRange и убрать это
        UnitTypes.crawler.maxRange = 80000f;
        UnitTypes.atrax.maxRange = 80000f;
        UnitTypes.spiroct.maxRange = 80000f;
        UnitTypes.arkyid.maxRange = 80000f;
        UnitTypes.toxopid.maxRange = 80000f;
        UnitTypes.reign.maxRange = 80000f;

        UnitTypes.poly.maxRange = 2000f;
        UnitTypes.poly.abilities.add(new UnitSpawnAbility(UnitTypes.poly, 480f, 0f, -32f));
        UnitTypes.poly.health = 125f;
        UnitTypes.poly.speed = 1.5f;

        // TODO сохранение Rules в переменную
        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> state.rules.defaultTeam.cores().each(Building::kill));

            Core.app.post(() -> {
                state.rules.canGameOver = false;
                state.rules.waveTimer = false;
                state.rules.waves = true;
                state.rules.unitCap = unitCap;
                state.rules.modeName = "Crawler";
                Call.setRules(state.rules);
                newGame();
            });

            firstWaveLaunched = false;
            isWaveGoing = false;
            timer = Time.millis();
        });

        // TODO переделать систему сохранения юнита
        Events.on(PlayerJoin.class, event -> {
            if (!money.containsKey(event.player.uuid()) || !units.containsKey(event.player.uuid())) {
                Bundle.bundled(event.player, "events.join.welcome");
                money.put(event.player.uuid(), (int) (money.get(event.player.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                units.put(event.player.uuid(), UnitTypes.dagger);
            } else {
                Bundle.bundled(event.player, "events.join.already-played");
            }

            if (leftUnits.containsKey(event.player.uuid())) {
                Unit unit = Groups.unit.getByID(leftUnits.remove(event.player.uuid()));
                if (unit != null) {
                    if (unit.isPlayer()) {
                        unit.getPlayer().clearUnit();
                    }
                    Time.run(60f, () -> event.player.unit(unit));
                    return;
                }
            }

            respawnPlayer(event.player);
        });

        // TODO переделать систему сохранения юнита
        Events.on(PlayerLeave.class, event -> {
            if (!event.player.dead()) {
                leftUnits.put(event.player.uuid(), event.player.unit().id);
            }
        });

        // TODO упростить
        Events.run(Trigger.update, () -> {
            if (isGameOver) return;

            if (!Groups.unit.contains(u -> u.team == state.rules.defaultTeam) && firstWaveLaunched) {
                isGameOver = true;
                if (wave > bossWave) {
                    Bundle.sendToChat("events.gameover.win");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 2f);
                } else {
                    Bundle.sendToChat("events.gameover.lose");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.waveTeam)), 2f);
                }
                return;
            }

            if (!Groups.unit.contains(u -> u.team == state.rules.waveTeam) && isWaveGoing) {
                if (wave < reinforcementMinWave || wave % reinforcementSpacing != 0) {
                    Bundle.sendToChat("events.wave", waveDelay);
                    Timer.schedule(() -> logic.runWave(), waveDelay);
                } else {
                    Bundle.sendToChat("events.next-wave", Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax));
                    Timer.schedule(this::spawnReinforcements, 2.5f);
                    Timer.schedule(() -> logic.runWave(), Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax));
                }
                Groups.player.each(p -> {
                    respawnPlayer(p);
                    money.put(p.uuid(), (int) (money.get(p.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                });
                isWaveGoing = false;
            }

            if (isWaveGoing) enemyTypes.each(type -> type.speed += enemySpeedBoost * Time.delta * statScaling);

            Groups.player.each(p -> Call.setHudText(p.con, Bundle.format("ui.money", Bundle.findLocale(p), money.get(p.uuid()))));

            if (Mathf.chance(tipChance * Time.delta)) Bundle.sendToChat("events.tip.info");
            if (Mathf.chance(tipChance * Time.delta)) Bundle.sendToChat("events.tip.upgrades");
        });

        netServer.admins.addActionFilter(action -> action.type != Administration.ActionType.breakBlock && action.type != Administration.ActionType.placeBlock);

        Log.info("Crawler Arena loaded.");
    }

    public void newGame() {
        if (firstWaveLaunched) return;

        if (Groups.player.size() <= 0) {
            Timer.schedule(this::newGame, 6f);
            isGameOver = true;
            return;
        }

        wave = 1;
        state.wave = 1;
        statScaling = 1f;
        UnitTypes.crawler.speed = crawlerSpeedBase;
        UnitTypes.crawler.health = crawlerHealthBase;
        money.clear();
        units.clear();
        leftUnits.clear();
        Groups.player.each(p -> {
            money.put(p.uuid(), 0);
            units.put(p.uuid(), UnitTypes.dagger);
            Timer.schedule(() -> respawnPlayer(p), Mathf.random(6f));
        });

        Bundle.sendToChat("events.first-wave", firstWaveDelay);
        Timer.schedule(() -> {
            logic.runWave();
            firstWaveLaunched = true;
        }, firstWaveDelay);

        Timer.schedule(() -> isGameOver = false, 12f);
        isWaveGoing = false;
    }

    // TODO упростить
    public void spawnReinforcements() {
        Bundle.sendToChat("events.aid");
        Seq<Unit> megas = new Seq<>();
        ObjectMap<Block, Integer> blocks = new ObjectMap<>();
        int megasFactor = (int) Math.min(wave * reinforcementScaling * statScaling, reinforcementMax);

        for (int i = 0; i < megasFactor; i += reinforcementFactor) {
            Unit unit = UnitTypes.mega.spawn(Team.get(Mathf.random(6, 256)), 32, world.unitHeight() / 2f + Mathf.range(120));
            unit.maxHealth(Float.MAX_VALUE);
            unit.health(unit.maxHealth);
            unit.controller(new ReinforcementAI());
            megas.add(unit);
        }

        for (int i = 0; i < megas.size; i++) {
            boolean rare = Mathf.chance(rareAidChance);
            Block block = rare ? Seq.with(rareAidBlockAmounts.keys()).random() : Seq.with(aidBlockAmounts.keys()).random();

            blocks.put(block, rare ? rareAidBlockAmounts.get(block) : aidBlockAmounts.get(block));
        }

        blocks.each((block, amount) -> {
            for (int i = 0; i < amount; i++) {
                if (megas.get(megas.size - 1) instanceof Payloadc pay) {
                    pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
                }
            }
            megas.remove(megas.size - 1);
        });
    }

    // TODO отдельный метод для хила
    public void respawnPlayer(Player player) {
        if (player.dead()) {
            Tile tile = world.tile(world.width() / 2 + Mathf.random(-3, 3), world.height() / 2 + Mathf.random(-3, 3));
            UnitType type = units.get(player.uuid(), () -> UnitTypes.dagger);

            if (!type.flying && tile.solid()) {
                tile.removeNet();
            }

            Unit unit = type.spawn(tile.worldx(), tile.worldy());
            applyUnit(unit);
            player.unit(unit);
            return;
        }

        if (player.unit().health < player.unit().maxHealth) {
            Call.effect(Fx.greenCloud, player.unit().x, player.unit().y, 0f, Pal.heal);
            player.unit().heal();
            Bundle.bundled(player, "events.heal", Pal.heal);
        }
    }

    // TODO УПРОСТИТЬ БЛЯТЬ ЭТУ ХУЙНЮ
    public void applyUnit(Unit unit) {
        if (unit.type == UnitTypes.crawler) {
            unit.maxHealth = playerCrawlerHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerCrawlerArmor;
            unit.abilities.add(new UnitSpawnAbility(UnitTypes.crawler, playerCrawlerCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        } else if (unit.type == UnitTypes.mono) {
            unit.maxHealth = playerMonoHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerMonoArmor;
            unit.abilities.add(new UnitSpawnAbility(playerMonoSpawnTypes.random(), playerMonoCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        } else if (unit.type == UnitTypes.poly) {
            unit.maxHealth = playerPolyHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerPolyArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            unit.abilities.each(ability -> {
                if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = playerPolyCooldown;
            });
        } else if (unit.type == UnitTypes.omura) {
            unit.maxHealth = playerOmuraHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerOmuraArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            unit.abilities.each(ability -> {
                if (ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = playerOmuraCooldown;
            });
        }

        unit.controller(new FlyingAI());
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        //TODO упростить, чо за херня это
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade your unit.", (args, player) -> {
            if (args.length == 2 && Strings.parseInt(args[1]) < 0) {
                Bundle.bundled(player, "exceptions.invalid-amount");
                return;
            }

            UnitType type = Seq.with(unitCosts.keys()).find(u -> u.name.equalsIgnoreCase(args[0]));
            if (type == null) {
                Bundle.bundled(player, "commands.upgrade.unit-not-found");
                return;
            }

            int amount = args.length == 2 ? Strings.parseInt(args[1]) : 1;
            if (Groups.unit.count(u -> u.type == type && u.team == state.rules.defaultTeam) > unitCap - amount) {
                Bundle.bundled(player, "commands.upgrade.too-many-units");
                return;
            }

            if (money.get(player.uuid()) >= unitCosts.get(type) * amount) {
                Seq<Unit> spawned = new Seq<>();
                for (int i = 0; i < amount; i++) {
                    Unit unit = type.spawn(player.x + Mathf.range(6f), player.y + Mathf.range(6f));
                    applyUnit(unit);
                    spawned.add(unit);
                }

                money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(type) * amount);
                units.put(player.uuid(), type);
                Unit unit = spawned.random();
                player.unit(unit);
                Bundle.bundled(player, "commands.upgrade.success", amount, type.name);

            } else {
                Bundle.bundled(player, "commands.upgrade.not-enough-money", unitCosts.get(type) * amount, money.get(player.uuid()));
            }
        });

        handler.<Player>register("information", "Show info about the Crawler Arena gamemode.", (args, player) -> Bundle.bundled(player, "commands.information"));

        handler.<Player>register("upgrades", "Show units you can upgrade to.", (args, player) -> {
            StringBuilder upgrades = new StringBuilder(Bundle.format("commands.upgrades.header", Bundle.findLocale(player)));
            unitCosts.each((type, cost) -> upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(cost <= money.get(player.uuid(), 0) ? "[lime]" : "[scarlet]").append(cost).append("[lightgray])\n"));
            player.sendMessage(upgrades.toString());
        });
    }

    public void registerServerCommands(CommandHandler handler) {
        handler.register("kill", "Kill all enemies in the current wave.", args -> Groups.unit.each(u -> u.team == state.rules.waveTeam, Unitc::kill));
    }
}
