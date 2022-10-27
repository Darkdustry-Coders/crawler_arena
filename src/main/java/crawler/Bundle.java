package crawler;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.*;

import java.text.MessageFormat;
import java.util.*;

import static crawler.PlayerData.datas;
import static mindustry.Vars.mods;

public class Bundle {

    public static final Locale defaultLocale = new Locale("en");
    public static final Seq<Locale> supportedLocales = new Seq<>();

    private static final ObjectMap<Locale, ResourceBundle> bundles = new ObjectMap<>();

    public static void load() {
        var files = mods.getMod("crawler-arena")
                .root.child("bundles").seq()
                .filter(fi -> fi.extEquals("properties"));

        files.each(fi -> {
            var codes = fi.nameWithoutExtension().split("_");

            if (codes.length == 2) { // bundle_ru.properties
                supportedLocales.add(new Locale(codes[1]));
            } else if (codes.length == 3) { // bundle_uk_UA.properties
                supportedLocales.add(new Locale(codes[1], codes[2]));
            }
        });

        supportedLocales.each(locale -> bundles.put(locale, ResourceBundle.getBundle("bundles.bundle", locale)));
    }

    public static String get(String key, String defaultValue, Locale locale) {
        try {
            var bundle = bundles.get(locale, bundles.get(defaultLocale));
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }

    public static String get(String key, Locale locale) {
        return get(key, key, locale);
    }

    public static String format(String key, Locale locale, Object... values) {
        String pattern = get(key, locale);
        if (values.length == 0) return pattern;

        return MessageFormat.format(pattern, values);
    }

    public static Locale findLocale(Player player) {
        var locale = supportedLocales.find(l -> player.locale.equals(l.toString()) || player.locale.startsWith(l.toString()));
        return locale != null ? locale : defaultLocale;
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(format(key, findLocale(player), values));
    }

    public static void sendToChat(String key, Object... values) {
        datas.each(data -> bundled(data.player, key, values));
    }

    public static void announce(String key, Object... values) {
        datas.each(data -> Call.announce(data.player.con, format(key, data.locale, values)));
    }
}