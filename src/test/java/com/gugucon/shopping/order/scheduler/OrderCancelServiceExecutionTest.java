package com.gugucon.shopping.order.scheduler;

import com.gugucon.shopping.common.domain.entity.BaseTimeEntity;
import com.gugucon.shopping.common.domain.vo.Quantity;
import com.gugucon.shopping.integration.config.DatabaseTruncationTestExecutionListener;
import com.gugucon.shopping.item.domain.entity.CartItem;
import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.member.domain.entity.Member;
import com.gugucon.shopping.member.domain.vo.Gender;
import com.gugucon.shopping.member.repository.MemberRepository;
import com.gugucon.shopping.order.domain.PayType;
import com.gugucon.shopping.order.domain.entity.LastScanTime;
import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.repository.LastScanTimeRepository;
import com.gugucon.shopping.order.repository.OrderItemRepository;
import com.gugucon.shopping.order.repository.OrderRepository;
import com.gugucon.shopping.utils.DomainUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.gugucon.shopping.order.domain.entity.Order.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableJpaAuditing(setDates = false)
@AutoConfigureTestDatabase
@TestExecutionListeners(value = DatabaseTruncationTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ActiveProfiles({"scheduling-test", "test"})
class OrderCancelServiceExecutionTest {

    private static final LocalDateTime DEFAULT_CREATED_AT = LocalDateTime.of(2023, 1, 1, 0, 0);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderCancelService orderCancelService;

    @Autowired
    private LastScanTimeRepository lastScanTimeRepository;

    @Test
    @DisplayName("30분 이상 전에, 그리고 마지막 스캔 시각 후에 마지막으로 변경된 결제중 주문이면 취소하고 재고를 복구한다.")
    void cancelIncompleteOrders_cancelPayingOrderLastModifiedMoreThan30MinutesAndAfterLastScanTime() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(45);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Quantity orderQuantity = order.getOrderItems().get(0).getQuantity();
        order.startPay(PayType.TOSS);
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소된_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_늘어난_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소된_주문.getStatus()).isEqualTo(CANCELED);
        assertThat(재고가_늘어난_사과.getStock()).isEqualTo(beforeStock.increaseBy(orderQuantity));
    }

    @Test
    @DisplayName("마지막으로 변경된 지 30분이 되지 않은 결제중 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelPayingOrderLastModifiedLessThan30MinutesAgo() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(15);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        order.startPay(PayType.TOSS);
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(PAYING);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("마지막 스캔 시점 전에 마지막으로 변경된 결제중 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelPayingOrderLastModifiedBeforeLastScanTime() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusHours(2);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        order.startPay(PayType.TOSS);
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(PAYING);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("처음 실행되는 경우 30분 이상 전에 마지막으로 변경된 결제중 주문이면 취소하고 재고를 복구한다.")
    void cancelIncompleteOrders_cancelPayingOrderLastModifiedMoreThan30MinutesAgo_initialExecution() {
        // given
        lastScanTimeRepository.save(LastScanTime.builder().build());

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusHours(2);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Quantity orderQuantity = order.getOrderItems().get(0).getQuantity();
        order.startPay(PayType.TOSS);
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소된_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_늘어난_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소된_주문.getStatus()).isEqualTo(CANCELED);
        assertThat(재고가_늘어난_사과.getStock()).isEqualTo(beforeStock.increaseBy(orderQuantity));
    }

    @Test
    @DisplayName("처음 실행되는 경우 마지막으로 변경된 지 30분이 되지 않은 결제중 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelPayingOrderLastModifiedLessThan30MinutesAgo_initialExecution() {
        // given
        lastScanTimeRepository.save(LastScanTime.builder().build());

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(15);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        order.startPay(PayType.TOSS);
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(PAYING);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("30분 이상 전에, 그리고 마지막 스캔 시각 후에 마지막으로 변경된 생성 상태 주문이면 취소하고 재고를 복구하지 않는다.")
    void cancelIncompleteOrders_cancelCreatedOrderLastModifiedMoreThan30MinutesAndAfterLastScanTime() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(45);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소된_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소된_주문.getStatus()).isEqualTo(CANCELED);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("마지막으로 변경된 지 30분이 되지 않은 생성 상태 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelCreatedOrderLastModifiedLessThan30MinutesAgo() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(15);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(CREATED);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("마지막 스캔 시점 전에 마지막으로 변경된 생성 상태 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelCreatedOrderLastModifiedBeforeLastScanTime() {
        // given
        final LocalDateTime lastScanTimeValue = LocalDateTime.now().minusHours(1);
        final LastScanTime lastScanTime = LastScanTime.builder()
                .timeValue(lastScanTimeValue)
                .build();
        lastScanTimeRepository.save(lastScanTime);

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusHours(2);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(CREATED);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("처음 실행되는 경우 30분 이상 전에 마지막으로 변경된 생성 상태 주문이면 취소하고 재고를 복구하지 않는다.")
    void cancelIncompleteOrders_cancelCreatedOrderLastModifiedMoreThan30MinutesAgo_initialExecution() {
        // given
        lastScanTimeRepository.save(LastScanTime.builder().build());

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusHours(2);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Quantity orderQuantity = order.getOrderItems().get(0).getQuantity();
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소된_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소된_주문.getStatus()).isEqualTo(CANCELED);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    @Test
    @DisplayName("처음 실행되는 경우 마지막으로 변경된 지 30분이 되지 않은 생성 상태 주문이면 취소되지 않는다.")
    void cancelIncompleteOrders_doNotCancelCreatedOrderLastModifiedLessThan30MinutesAgo_initialExecution() {
        // given
        lastScanTimeRepository.save(LastScanTime.builder().build());

        final Product 사과 = insertProduct("사과", 2000);
        final Quantity beforeStock = 사과.getStock();

        final Member member = DomainUtils.createMemberWithoutId("test@email.com", LocalDate.now(), Gender.FEMALE);
        updateCreatedAtAndLastModifiedAt(member, DEFAULT_CREATED_AT);

        final Member persistMember = memberRepository.save(member);

        final List<CartItem> cartItems = Stream.of(사과).map(
                product -> DomainUtils.createCartItemWithoutId(persistMember.getId(), product)).toList();

        final LocalDateTime orderCreatedAt = LocalDateTime.now().minusMinutes(15);
        final Order order = Order.from(persistMember.getId(), cartItems);
        updateCreatedAtAndLastModifiedAt(order, orderCreatedAt);

        order.getOrderItems().forEach(orderItem -> {
            updateCreatedAtAndLastModifiedAt(orderItem, orderCreatedAt);
        });
        final Order persistOrder = orderRepository.save(order);

        // when
        orderCancelService.cancelIncompleteOrders();

        // then
        final Order 취소되지_않은_주문 = orderRepository.findById(persistOrder.getId()).get();
        final Product 재고가_그대로인_사과 = productRepository.findById(사과.getId()).get();
        assertThat(취소되지_않은_주문.getStatus()).isEqualTo(CREATED);
        assertThat(재고가_그대로인_사과.getStock()).isEqualTo(beforeStock);
    }

    private Product insertProduct(final String productName, final long price) {
        final Product product = DomainUtils.createProductWithoutId(productName, price, 10);
        updateCreatedAtAndLastModifiedAt(product, DEFAULT_CREATED_AT);
        return productRepository.save(product);
    }

    private void updateCreatedAtAndLastModifiedAt(final BaseTimeEntity baseTimeEntity, LocalDateTime time) {
        ReflectionTestUtils.setField(baseTimeEntity, "createdAt", time);
        ReflectionTestUtils.setField(baseTimeEntity, "lastModifiedAt", time);
    }
}
