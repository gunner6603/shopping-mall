package com.gugucon.shopping.order.repository;

import com.gugucon.shopping.order.domain.entity.Order;
import com.gugucon.shopping.order.domain.entity.Order.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByMemberIdAndStatus(final Long memberId, final OrderStatus status, final Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "WHERE o.id = :id AND o.memberId = :memberId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndMemberIdExclusively(final Long id, final Long memberId);

    Optional<Order> findByIdAndMemberId(final Long id, final Long memberId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems " +
            "WHERE o.status IN :statuses AND (o.createdAt BETWEEN :start AND :end)")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Order> findAllByStatusInAndCreatedAtBetweenWithOrderItems(
            @Param("statuses") final List<OrderStatus> statuses,
            @Param("start") final LocalDateTime start,
            @Param("end") final LocalDateTime end);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderItems " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithOrderItems(Long id);
}
