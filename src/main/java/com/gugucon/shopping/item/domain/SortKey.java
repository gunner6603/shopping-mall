package com.gugucon.shopping.item.domain;

import lombok.Getter;

import static com.gugucon.shopping.item.domain.SortKey.Direction.ASC;
import static com.gugucon.shopping.item.domain.SortKey.Direction.DESC;

@Getter
public enum SortKey {

    ORDER_COUNT_DESC(DESC, "orderCount"),
    ID_DESC(DESC, "id"),
    PRICE_DESC(DESC, "price"),
    PRICE_ASC(ASC, "price"),
    RATE(DESC, "rate");

    private final Direction direction;
    private final String key;

    SortKey(final Direction direction, final String key) {
        this.direction = direction;
        this.key = key;
    }

    public String getDirectionName() {
        return direction.name();
    }

    public enum Direction {ASC, DESC}
}
