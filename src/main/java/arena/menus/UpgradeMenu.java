package arena.menus;

import arena.*;
import mindustry.content.UnitTypes;
import mindustry.core.UI;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import useful.*;
import useful.State.StateKey;
import useful.menu.Menu;
import useful.menu.Menu.MenuView;
import useful.menu.Menu.MenuView.OptionData;

import static arena.CrawlerVars.unitCap;
import static mindustry.Vars.state;

public class UpgradeMenu {

    public static final Menu
            unitMenu = new Menu(),
            amountMenu = new Menu();

    public static final StateKey<PlayerData> DATA = new StateKey<>("data", PlayerData.class);
    public static final StateKey<UnitCost> UNIT = new StateKey<>("unit", UnitCost.class);

    public static void load() {
        unitMenu.transform(DATA, (menu, data) -> {
            menu.title("upgrade.unit.title");
            menu.content("ui.money", data.money);

            menu.options(3, UnitCost.values()).row();
            menu.option("ui.close");
        });

        amountMenu.transform(DATA, (menu, data) -> {
            menu.title("upgrade.amount.title");
            menu.content("ui.money", data.money);

            menu.options(3, UnitAmount.values()).row();

            menu.option("ui.back", Action.back());
            menu.option("ui.close");
        });
    }
    
    public static void showUpgradeMenu(Player player, PlayerData data) {
        unitMenu.show(player, DATA, data);
    }

    public enum UnitCost implements OptionData {
        dagger(UnitTypes.dagger, 25),
        flare(UnitTypes.flare, 75),
        nova(UnitTypes.nova, 100),
        mace(UnitTypes.mace, 200),
        stell(UnitTypes.stell, 200),
        merui(UnitTypes.merui, 225),
        elude(UnitTypes.elude, 250),
        atrax(UnitTypes.atrax, 250),
        horizon(UnitTypes.horizon, 250),
        pulsar(UnitTypes.pulsar, 300),
        retusa(UnitTypes.retusa, 400),
        locus(UnitTypes.locus, 400),
        avert(UnitTypes.avert, 450),
        cleroi(UnitTypes.cleroi, 500),
        fortress(UnitTypes.fortress, 500),
        minke(UnitTypes.minke, 750),
        oxynoe(UnitTypes.oxynoe, 850),
        risso(UnitTypes.risso, 1000),
        mega(UnitTypes.mega, 1500),
        spiroct(UnitTypes.spiroct, 1500),
        quasar(UnitTypes.quasar, 2000),
        precept(UnitTypes.precept, 2250),
        zenith(UnitTypes.zenith, 2500),
        anthicus(UnitTypes.anthicus, 2500),
        obviate(UnitTypes.obviate, 2750),
        cyerce(UnitTypes.cyerce, 5000),
        bryde(UnitTypes.bryde, 5000),
        crawler(UnitTypes.crawler, 7500),
        scepter(UnitTypes.scepter, 10000),
        vela(UnitTypes.vela, 15000),
        vanquish(UnitTypes.vanquish, 17500),
        antumbra(UnitTypes.antumbra, 18000),
        quell(UnitTypes.quell, 23500),
        quad(UnitTypes.quad, 25000),
        arkyid(UnitTypes.arkyid, 25000),
        tecta(UnitTypes.tecta, 25000),
        aegires(UnitTypes.aegires, 30000),
        sei(UnitTypes.sei, 75000),
        poly(UnitTypes.poly, 100000),
        eclipse(UnitTypes.eclipse, 175000),
        disrupt(UnitTypes.disrupt, 225000),
        conquer(UnitTypes.conquer, 250000),
        corvus(UnitTypes.corvus, 250000),
        reign(UnitTypes.reign, 250000),
        oct(UnitTypes.oct, 250000),
        collaris(UnitTypes.collaris, 300000),
        toxopid(UnitTypes.toxopid, 325000),
        navanax(UnitTypes.navanax, 350000),
        omura(UnitTypes.omura, 1500000),
        mono(UnitTypes.mono, 3500000);

        public final UnitType type;
        public final long cost;

        public final char icon;

        UnitCost(UnitType type, int cost) {
            this.type = type;
            this.cost = cost;

            this.icon = CrawlerLogic.icon(type);
        }

        @Override
        public void option(MenuView menu) {
            menu.option("upgrade.unit.button", Action.openWith(amountMenu, UNIT, this), type.name, icon, UI.formatAmount(cost));
        }
    }

    public enum UnitAmount implements OptionData {
        one(1),
        two(2),
        five(5),
        ten(10),
        fifteen(15),
        twenty(20);

        public final int amount;

        UnitAmount(int amount) {
            this.amount = amount;
        }

        public boolean tooManyUnits(MenuView menu) {
            return state.rules.defaultTeam.data().countType(menu.state.get(UNIT).type) > unitCap - amount;
        }

        public boolean notEnoughMoney(MenuView menu) {
            return menu.state.get(DATA).money < menu.state.get(UNIT).cost * amount;
        }

        @Override
        public void option(MenuView menu) {
            menu.option("upgrade.amount.button", view -> {
                var data = view.state.get(DATA);
                var unit = view.state.get(UNIT);

                if (tooManyUnits(view)) {
                    Bundle.announce(view.player, "upgrade.too-many-units", state.rules.defaultTeam.data().countType(unit.type));
                    return;
                }

                if (notEnoughMoney(view)) {
                    Bundle.announce(view.player, "upgrade.not-enough-money", data.money, UI.formatAmount(unit.cost * amount));
                    return;
                }

                for (int i = 0; i < amount; i++)
                    data.applyUnit(unit.type.spawn(view.player.x, view.player.y));

                data.money -= unit.cost * amount;
                Bundle.announce(view.player, "upgrade.success", amount, unit.icon, unit.type.name);
            }, tooManyUnits(menu) || notEnoughMoney(menu) ? "scarlet" : "lime", amount, menu.state.get(UNIT).icon, UI.formatAmount(menu.state.get(UNIT).cost * amount));
        }
    }
}