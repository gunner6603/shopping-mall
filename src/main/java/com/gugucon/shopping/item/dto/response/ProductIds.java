package com.gugucon.shopping.item.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ProductIds {

    List<Long> contents;

    public static ProductIds from(final List<Long> contents) {
        return new ProductIds(new ArrayList<>(contents));
    }
}
