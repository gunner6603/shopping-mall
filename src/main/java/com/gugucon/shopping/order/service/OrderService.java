package com.gugucon.shopping.order.service;

import com.gugucon.shopping.auth.dto.MemberPrincipal;
import com.gugucon.shopping.common.dto.response.PagedResponse;
import com.gugucon.shopping.common.exception.ErrorCode;
import com.gugucon.shopping.common.exception.ShoppingException;
import com.gugucon.shopping.item.domain.entity.CartItem;
import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.repository.CartItemRepository;
import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.domain.event.OrderCompleteEvent;
import com.gugucon.shopping.order.dto.request.OrderPayRequest;
import com.gugucon.shopping.order.dto.response.OrderDetailResponse;
import com.gugucon.shopping.order.dto.response.OrderHistoryResponse;
import com.gugucon.shopping.order.dto.response.OrderPayResponse;
import com.gugucon.shopping.order.dto.response.OrderResponse;
import com.gugucon.shopping.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse order(final Long memberId) {
        final List<CartItem> cartItems = cartItemRepository.findAllByMemberIdWithProduct(memberId);
        Order.validateTotalPrice(cartItems);
        final Order order = Order.from(memberId, cartItems);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public void complete(final Order order, final MemberPrincipal principal) {
        cartItemRepository.deleteAllByMemberId(principal.getId());
        order.completePay();
        eventPublisher.publishEvent(OrderCompleteEvent.from(order));
    }

    public OrderDetailResponse getOrderDetail(final Long orderId, final Long memberId) {
        final Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ShoppingException(ErrorCode.INVALID_ORDER));

        return OrderDetailResponse.from(order);
    }

    public PagedResponse<OrderHistoryResponse> getOrderHistory(final Pageable pageable, final Long memberId) {
        final Page<Order> orders = orderRepository.findAllByMemberIdAndStatus(memberId,
                Order.OrderStatus.COMPLETED,
                pageable);

        return convertToPage(orders);
    }

    public Order findOrderBy(final Long orderId, final Long memberId) {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ShoppingException(ErrorCode.INVALID_ORDER));
    }

    @Transactional
    public Order findOrderByExclusively(final Long orderId, final Long memberId) {
        return orderRepository.findByIdAndMemberIdExclusively(orderId, memberId)
                .orElseThrow(() -> new ShoppingException(ErrorCode.INVALID_ORDER));
    }

    private PagedResponse<OrderHistoryResponse> convertToPage(final Page<Order> orders) {
        final List<OrderHistoryResponse> contents = orders.map(OrderHistoryResponse::from).toList();
        return new PagedResponse<>(contents, orders.getTotalPages(), orders.getNumber(), orders.getSize());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelPayingOrder(final Order order) {
        order.getOrderItems()
                .forEach(orderItem -> productRepository.increaseStockByIdAndValue(orderItem.getProductId(),
                        orderItem.getQuantity().getValue()));
        order.cancel();
    }

    @Transactional
    public void cancelCreatedOrder(final Order order) {
        order.cancel();
    }

    @Transactional
    public OrderPayResponse requestPay(final OrderPayRequest orderPayRequest, final Long memberId) {
        final Order order = orderRepository.findByIdAndMemberIdExclusively(orderPayRequest.getOrderId(), memberId)
                .orElseThrow(() -> new ShoppingException(ErrorCode.INVALID_ORDER));

        order.startPay(orderPayRequest.getPayType());
        decreaseStock(order);
        return OrderPayResponse.from(order);
    }

    private void decreaseStock(final Order order) {
        order.getOrderItems().forEach(orderItem -> {
            final Product product = productRepository.findByIdWithExclusiveLock(orderItem.getProductId())
                    .orElseThrow(() -> new ShoppingException(ErrorCode.UNKNOWN_ERROR));
            product.validateStockIsNotLessThan(orderItem.getQuantity());
            product.decreaseStockBy(orderItem.getQuantity());
        });
    }
}
