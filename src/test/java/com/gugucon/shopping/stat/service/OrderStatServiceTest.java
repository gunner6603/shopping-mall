package com.gugucon.shopping.stat.service;

import com.gugucon.shopping.common.config.JpaConfig;
import com.gugucon.shopping.item.domain.entity.CartItem;
import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.repository.CartItemRepository;
import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.member.domain.entity.Member;
import com.gugucon.shopping.member.domain.vo.Gender;
import com.gugucon.shopping.member.repository.MemberRepository;
import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.domain.event.OrderCompleteEvent;
import com.gugucon.shopping.order.repository.OrderRepository;
import com.gugucon.shopping.stat.domain.entity.OrderStat;
import com.gugucon.shopping.stat.repository.OrderStatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.gugucon.shopping.utils.DomainUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({OrderStatService.class, JpaConfig.class})
class OrderStatServiceTest {

    @Autowired
    private OrderStatService orderStatService;

    @Autowired
    private OrderStatRepository orderStatRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("주문 완료 이벤트를 처리해서 주문 통계 테이블을 업데이트한다.")
    void handle() {
        // given
        final Member member = memberRepository.save(createMemberWithoutId("test@gmail.com", LocalDate.of(2000, 1, 1), Gender.MALE));
        final Product productA = productRepository.save(createProduct("test_product_A", 10000L));
        final Product productB = productRepository.save(createProduct("test_product_B", 20000L));
        final CartItem cartItemA = cartItemRepository.save(createCartItemWithoutId(member.getId(), productA));
        final CartItem cartItemB = cartItemRepository.save(createCartItemWithoutId(member.getId(), productB));
        final Order order = orderRepository.save(Order.from(member.getId(), List.of(cartItemA, cartItemB)));
        final OrderStat orderStatA = orderStatRepository.save(createOrderStatWithoutId(member, productA.getId()));
        final OrderStat orderStatB = orderStatRepository.save(createOrderStatWithoutId(member, productB.getId()));
        final OrderCompleteEvent orderCompleteEvent = OrderCompleteEvent.from(order);

        // when
        orderStatService.handle(orderCompleteEvent);

        // then
        final OrderStat updatedOrderStatA = orderStatRepository.findById(orderStatA.getId())
                .orElseThrow(IllegalArgumentException::new);
        final OrderStat updatedOrderStatB = orderStatRepository.findById(orderStatB.getId())
                .orElseThrow(IllegalArgumentException::new);
        assertThat(updatedOrderStatA.getCount()).isEqualTo(Long.valueOf(cartItemA.getQuantity().getValue()));
        assertThat(updatedOrderStatB.getCount()).isEqualTo(Long.valueOf(cartItemB.getQuantity().getValue()));
    }
}
