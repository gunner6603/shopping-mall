package com.gugucon.shopping.stat.service;

import com.gugucon.shopping.common.exception.ErrorCode;
import com.gugucon.shopping.common.exception.ShoppingException;
import com.gugucon.shopping.member.domain.entity.Member;
import com.gugucon.shopping.member.domain.vo.BirthYearRange;
import com.gugucon.shopping.member.repository.MemberRepository;
import com.gugucon.shopping.order.domain.entity.OrderItem;
import com.gugucon.shopping.rate.domain.entity.Rate;
import com.gugucon.shopping.rate.domain.event.RateCreateEvent;
import com.gugucon.shopping.rate.repository.RateRepository;
import com.gugucon.shopping.stat.repository.RateStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Transactional
@RequiredArgsConstructor
public class RateStatService {

    private final RateStatRepository rateStatRepository;
    private final RateRepository rateRepository;
    private final MemberRepository memberRepository;

    @Async("threadPoolTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void handle(final RateCreateEvent rateCreateEvent) {
        final Rate rate = rateRepository.findByIdWithOrderItem(rateCreateEvent.getRateId())
                .orElseThrow(() -> new ShoppingException(ErrorCode.UNKNOWN_ERROR));
        final OrderItem orderItem = rate.getOrderItem();
        final Member member = memberRepository.findById(rateCreateEvent.getMemberId())
                .orElseThrow(() -> new ShoppingException(ErrorCode.UNKNOWN_ERROR));

        rateStatRepository.updateRateStatByScore(rate.getScore(),
                orderItem.getProductId(),
                BirthYearRange.from(member.getBirthDate()),
                member.getGender());
    }
}
