package com.gugucon.shopping.order.domain.event;

import com.gugucon.shopping.order.domain.entity.Order;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class OrderCompleteEvent {

    private Long orderId;

    public static OrderCompleteEvent from(final Order order) {
        return new OrderCompleteEvent(order.getId());
    }
}
