package de.uni_leipzig.mds.util;

import static java.util.Objects.requireNonNull;

public final class PreconditionChecks {

    private PreconditionChecks() {
    }

    public static String neitherNullNorBlank(String check, String msg) {
        requireNonNull(check, msg);
        if (check.isBlank()) {
            throw new IllegalArgumentException(msg);
        }
        return check;
    }
}
