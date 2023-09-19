package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.common.exception.ErrorCode;
import com.gugucon.shopping.common.exception.ShoppingException;
import com.gugucon.shopping.item.domain.SortKey;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

public class SortKeyUtils {

    private SortKeyUtils() {
    }

    public static boolean isValid(final Sort sort) {
        return Arrays.stream(SortKey.values())
                .map(SortKeyUtils::convertToSort)
                .anyMatch(mappedSortKey -> mappedSortKey.equals(sort));
    }

    public static SortKey map(final Sort sort) {
        return Arrays.stream(SortKey.values())
                .filter(sortKey -> convertToSort(sortKey).equals(sort))
                .findAny()
                .orElseThrow(() -> new ShoppingException(ErrorCode.INVALID_SORT));
    }

    private static Sort convertToSort(final SortKey sortKey) {
        return Sort.by(Sort.Direction.valueOf(sortKey.getDirectionName()), sortKey.getKey());
    }
}
