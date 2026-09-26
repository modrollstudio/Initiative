package studio.modroll.initiative.config;

/** Something the loader had to work around to make a config file usable, and why. */
record ConfigIssue(Kind kind, String key, String message) {

    enum Kind {
        MISSING,
        INVALID,
        UNKNOWN,
        NEWER_FORMAT
    }

    static ConfigIssue missing(String key) {
        return new ConfigIssue(Kind.MISSING, key, "'" + key + "' is missing, using the built-in default");
    }

    static ConfigIssue invalid(String key, String reason) {
        return new ConfigIssue(
                Kind.INVALID, key, "'" + key + "' is invalid (" + reason + "), using the built-in default");
    }

    static ConfigIssue unknown(String key) {
        return new ConfigIssue(Kind.UNKNOWN, key, "'" + key + "' is not a setting this version has, ignoring it");
    }

    static ConfigIssue newerFormat(int declared, int supported) {
        return new ConfigIssue(
                Kind.NEWER_FORMAT,
                ConfigSource.FORMAT_VERSION,
                "written by a newer Initiative (format_version " + declared + ", this build reads " + supported
                        + "), settings this build does not have are ignored");
    }
}
