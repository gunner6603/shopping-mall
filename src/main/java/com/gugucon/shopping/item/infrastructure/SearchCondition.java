package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.common.exception.ErrorCode;
import com.gugucon.shopping.common.exception.ShoppingException;
import com.gugucon.shopping.item.domain.SortKey;
import com.gugucon.shopping.member.domain.vo.BirthYearRange;
import com.gugucon.shopping.member.domain.vo.Gender;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class SearchCondition {

    private static final SortKey SORT_BY_RATE = SortKey.RATE;
    private static final SortKey SORT_BY_ORDER_COUNT = SortKey.ORDER_COUNT_DESC;

    private final String keyword;
    private final BirthYearRange birthYearRange;
    private final Gender gender;
    private final Pageable pageable;

    public void validateSort() {
        if (!SortKeyUtils.isValid(getSort())) {
            throw new ShoppingException(ErrorCode.INVALID_SORT);
        }
    }

    public void validateKeywordNotBlank() {
        if (keyword.isBlank()) {
            throw new ShoppingException(ErrorCode.EMPTY_INPUT);
        }
    }

    public boolean isSortedByRate() {
        return getSortKey().equals(SORT_BY_RATE);
    }

    public boolean isSortedByOrderCount() {
        return getSortKey().equals(SORT_BY_ORDER_COUNT);
    }

    public boolean hasValidFilters() {
        return birthYearRange != null && gender != null;
    }

    private Sort getSort() {
        return pageable.getSort();
    }

    private SortKey getSortKey() {
        return SortKeyUtils.map(getSort());
    }
}
