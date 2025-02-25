package com.gugucon.shopping.order.scheduler;

import com.gugucon.shopping.order.domain.entity.LastScanTime;
import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.domain.entity.Order.OrderStatus;
import com.gugucon.shopping.order.repository.LastScanTimeRepository;
import com.gugucon.shopping.order.repository.OrderRepository;
import com.gugucon.shopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.gugucon.shopping.order.domain.entity.Order.OrderStatus.CREATED;
import static com.gugucon.shopping.order.domain.entity.Order.OrderStatus.PAYING;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelService {

    public static final List<OrderStatus> INCOMPLETE_STATUSES = List.of(CREATED, PAYING);
    private static final Duration CANCEL_INTERVAL = Duration.ofMinutes(30);
    private static final LocalDateTime DEFAULT_SCAN_START_TIME = LocalDateTime.of(2023, 1, 1, 0, 0);

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final LastScanTimeRepository lastScanTimeRepository;

    @Transactional
    public void cancelIncompleteOrders() {
        log.info("cancelling started.");
        final LastScanTime lastScanTime = getLastScanTime();
        final LocalDateTime scanStartTime = lastScanTime.hasNullValue() ? DEFAULT_SCAN_START_TIME : lastScanTime.getTimeValue();
        final LocalDateTime scanEndTime = LocalDateTime.now().minus(CANCEL_INTERVAL);

        final List<Order> incompleteOrders = orderRepository.findAllByStatusInAndCreatedAtBetweenWithOrderItems(
                INCOMPLETE_STATUSES,
                scanStartTime,
                scanEndTime);
        log.info("number of incomplete orders={}.", incompleteOrders.size());

        boolean allSucceeded = cancelOrders(incompleteOrders);

        if (allSucceeded) {
            lastScanTime.update(scanEndTime);
            log.info("no exception thrown while cancelling order");
        }

        log.info("cancelling ended.");
    }

    private boolean cancelOrders(final List<Order> incompleteOrders) {
        boolean allSucceeded = true;
        for (Order incompleteOrder : incompleteOrders) {
            final boolean succeeded = cancelOrder(incompleteOrder);
            if (!succeeded) {
                allSucceeded = false;
            }
        }
        return allSucceeded;
    }

    private boolean cancelOrder(final Order incompleteOrder) {
        if (incompleteOrder.isCreated()) {
            orderService.cancelCreatedOrder(incompleteOrder);
            return true;
        }

        try {
            orderService.cancelPayingOrder(incompleteOrder);
            return true;
        } catch (Exception e) {
            log.warn("exception thrown while cancelling order. order id={}.", incompleteOrder.getId());
            return false;
        }
    }

    private LastScanTime getLastScanTime() {
        return lastScanTimeRepository.findAll().stream()
                .findAny()
                .orElseGet(() -> lastScanTimeRepository.save(LastScanTime.builder().build()));
    }
}
