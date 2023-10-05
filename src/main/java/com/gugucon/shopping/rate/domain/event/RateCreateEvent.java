package com.gugucon.shopping.rate.domain.event;

import com.gugucon.shopping.rate.domain.entity.Rate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RateCreateEvent {

    private Long rateId;
    private Long memberId;

    public static RateCreateEvent from(final Rate rate, final Long memberId) {
        return new RateCreateEvent(rate.getId(), memberId);
    }
}
