package com.gugucon.shopping.item.service;

import com.gugucon.shopping.auth.dto.MemberPrincipal;
import com.gugucon.shopping.item.repository.OrderStatRepository;
import com.gugucon.shopping.member.domain.vo.BirthYearRange;
import com.gugucon.shopping.order.domain.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderStatService {

    private final OrderStatRepository orderStatRepository;

    @Async("threadPoolTaskExecutor")
    public void updateOrderStatBy(final MemberPrincipal principal, final OrderItem orderItem) {
        orderStatRepository.updateOrderStatByCount(orderItem.getQuantity().getValue(),
                                                   orderItem.getProductId(),
                                                   BirthYearRange.from(principal.getBirthDate()),
                                                   principal.getGender());
    }
}
