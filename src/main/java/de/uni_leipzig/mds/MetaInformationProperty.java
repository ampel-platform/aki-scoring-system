package de.uni_leipzig.mds;

import lombok.Getter;

/**
 * Pre-defined meta information properties.
 */
@Getter
enum MetaInformationProperty {
    NAME("scoring.system.name"),
    VERSION("scoring.system.version");

    private final String name;

    MetaInformationProperty(String name) {
        this.name = name;
    }
}
