package de.uni_leipzig.mds.processing;

import java.util.List;

/**
 * Common code constants for different AKI related medical information.
 */
public final class Codes {
    private Codes() {
    }

    /**
     * Codes that denote a creatinine value.
     */
    public static final List<String> LAB_CODES_CREATININE = List.of("LCRE_S");

    /**
     * Codes that denote a vancomycin value.
     */
    public static final List<String> LAB_CODES_VANCOMYCIN = List.of("VANC-K_S", "VANC-S_S", "VANC-T_S", "VANC-U_S");

    /**
     * Codes that denote a dialysis procedure.
     */
    public static final List<String> PROCEDURE_CODES_DIALYSIS = List.of("8-853", "8-854", "8-855", "8-857", "8-85A");
}
