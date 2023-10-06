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
import com.gugucon.shopping.order.repository.OrderRepository;
import com.gugucon.shopping.rate.domain.entity.Rate;
import com.gugucon.shopping.rate.domain.event.RateCreateEvent;
import com.gugucon.shopping.rate.repository.RateRepository;
import com.gugucon.shopping.stat.domain.entity.RateStat;
import com.gugucon.shopping.stat.repository.RateStatRepository;
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
@Import({RateStatService.class, JpaConfig.class})
class RateStatServiceTest {

    @Autowired
    private RateStatService rateStatService;

    @Autowired
    private RateStatRepository rateStatRepository;

    @Autowired
    private RateRepository rateRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    @DisplayName("별점 생성 이벤트를 처리해서 별점 통계 테이블을 업데이트한다.")
    void handle() {
        // given
        final Member member = memberRepository.save(createMemberWithoutId("test@gmail.com", LocalDate.of(2000, 1, 1), Gender.MALE));
        final Product product = productRepository.save(createProduct("test_product_A", 10000L));
        final CartItem cartItem = cartItemRepository.save(createCartItemWithoutId(member.getId(), product));
        final Order order = orderRepository.save(Order.from(member.getId(), List.of(cartItem)));
        final Rate rate = rateRepository.save(createRate(order.getOrderItems().get(0), (short) 5));
        final RateStat rateStat = rateStatRepository.save(createRateStatWithoutId(member, product.getId()));
        final RateCreateEvent rateCreateEvent = RateCreateEvent.from(rate, member.getId());

        // when
        rateStatService.handle(rateCreateEvent);

        // then
        final RateStat updatedRateStat = rateStatRepository.findById(rateStat.getId())
                .orElseThrow(IllegalArgumentException::new);
        assertThat(updatedRateStat.getCount()).isEqualTo(1L);
        assertThat(updatedRateStat.getTotalScore()).isEqualTo(Long.valueOf(rate.getScore()));
    }
}
