package dev.ebullient.convert.io;

public enum Msg {
    ALLDONE(Character.toString(0x1F389)), // 🎉
    BREW(Character.toString(0x1F37A)), // 🍺
    CLASSES(Character.toString(0x1F913)), // 🤓
    DEBUG(Character.toString(0x1F527), "faint"), // 🔧
    DECK(Character.toString(0x1F0CF)), // 🃏
    DEITY(Character.toString(0x1F47C)), // 👼
    ERR(Character.toString(0x1F6D1) + "  ERR|"), // 🛑
    FEATURE(Character.toString(0x2B50)), // ⭐️
    FEATURETYPE(Character.toString(0x1F31F)), // 🌟
    FILTER(Character.toString(0x1F50D)), // 🔍
    FOLDER(Character.toString(0x1F4C1)), // 📁
    ITEM(Character.toString(0x1F9F8)), // 🧸
    MULTIPLE(Character.toString(0x1F4DA)), // 📚
    NOT_SET(Character.toString(0x1FAE5) + " "), // 🫥
    OK(Character.toString(0x2705) + "   OK|"), // ✅
    INFO(Character.toString(0x1F537) + " INFO|"), // 🔷
    PROGRESS(Character.toString(0x23F3)), // ⏳
    RACES(Character.toString(0x1F4D5)), // 📕
    REPRINT(Character.toString(0x1F4F0)), // 📰
    SOMEDAY(Character.toString(0x1F6A7)), // 🚧
    SOURCE(Character.toString(0x1F4D8)), // 📘
    SPELL(Character.toString(0x1F4AB)), // 💫
    TARGET(Character.toString(0x1F3AF)), // 🎯
    UNKNOWN(Character.toString(0x1F47B)), // 👻
    UNRESOLVED(Character.toString(0x1FAE3)), // 🫣
    VERBOSE(Character.toString(0x1F537) + "     |"), // 🔷
    WRITING(Character.toString(0x1F5A8) + " "), // 🖨️
    WARN(Character.toString(0x1F538) + " WARN|"),
    NOOP("");

    final String prefix;
    final String colorPrefix;

    private Msg(String prefix) {
        this.prefix = prefix + " ";
        this.colorPrefix = null;
    }

    private Msg(String prefix, String color) {
        this.prefix = prefix + " ";
        this.colorPrefix = "@|%s %s".formatted(color, prefix);
    }

    public String color(String message) {
        if (colorPrefix != null) {
            return colorPrefix + message + "|@";
        }
        return wrap(message);
    }

    public String wrap(String message) {
        return this == NOOP
                ? message
                : prefix + message;
    }
}
