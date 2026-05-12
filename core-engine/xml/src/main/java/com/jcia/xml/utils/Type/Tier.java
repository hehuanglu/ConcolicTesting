package com.jcia.xml.utils.Type;

import java.util.HashSet;
import java.util.Set;

public enum Tier {
    VIEW,
    CONFIG,
    BUSINESS,
    DAO,
    DB,
    NOTIER;

    public Set<ComponentType> getComponents(Tier tier) {
        Set<ComponentType> componentTypes = new HashSet<>();
        for (ComponentType c : ComponentType.values()) {
            if (c.isInTier(tier))
                componentTypes.add(c);
        }
        return componentTypes;
    }
}
