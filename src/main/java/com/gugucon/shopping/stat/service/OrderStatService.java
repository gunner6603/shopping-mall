package com.gugucon.shopping.stat.service;

import com.gugucon.shopping.common.exception.ErrorCode;
import com.gugucon.shopping.common.exception.ShoppingException;
import com.gugucon.shopping.member.domain.entity.Member;
import com.gugucon.shopping.member.domain.vo.BirthYearRange;
import com.gugucon.shopping.member.repository.MemberRepository;
import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.domain.entity.OrderItem;
import com.gugucon.shopping.order.domain.event.OrderCompleteEvent;
import com.gugucon.shopping.order.repository.OrderRepository;
import com.gugucon.shopping.stat.repository.OrderStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderStatService {

    private final OrderStatRepository orderStatRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    @Async("threadPoolTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void handle(final OrderCompleteEvent orderCompleteEvent) {
        final Order order = orderRepository.findByIdWithOrderItems(orderCompleteEvent.getOrderId())
                .orElseThrow(() -> new ShoppingException(ErrorCode.UNKNOWN_ERROR));
        final Member member = memberRepository.findById(order.getMemberId())
                .orElseThrow((() -> new ShoppingException(ErrorCode.UNKNOWN_ERROR)));
        order.getOrderItems()
                .forEach(orderItem -> updateOrderStatBy(orderItem, member));
    }

    private void updateOrderStatBy(final OrderItem orderItem, final Member member) {
        orderStatRepository.updateOrderStatByCount(orderItem.getQuantity().getValue(),
                orderItem.getProductId(),
                BirthYearRange.from(member.getBirthDate()),
                member.getGender());
    }
}
