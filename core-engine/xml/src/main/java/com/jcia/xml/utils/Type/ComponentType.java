package com.jcia.xml.utils.Type;

public enum ComponentType {
    /**
     * Hibernate ComponentType
     */
    DAO(Tier.DAO),
    DB(Tier.DB),

    /**
     * Java ComponentType
     */
    JAVA(Tier.BUSINESS),

    /**
     * Struts ComponentType
     */
    STRUTS_ACTION(Tier.CONFIG),
    STRUTS_CONFIG(Tier.CONFIG),
    STRUTS_INTERCEPTOR(Tier.CONFIG),
    STRUTS_RESULT_TYPE(Tier.CONFIG),
    STRUTS_VIEW(Tier.CONFIG),

    /**
     * Tiles ComponentType
     */
    TILES_CONFIG(Tier.CONFIG),
    TILES_VIEW(Tier.VIEW),

    /**
     * Default ComponentType
     */
    DEFAULT(Tier.NOTIER);

    private Tier tier;
    ComponentType(Tier tier) {
        this.tier = tier;
    }

    public boolean isInTier(Tier tier) {
        return this.tier == tier;
    }

    public Tier getTier() {
        return this.tier;
    }
}

