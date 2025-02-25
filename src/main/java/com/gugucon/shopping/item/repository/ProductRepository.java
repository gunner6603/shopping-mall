package com.gugucon.shopping.item.repository;

import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.repository.dto.ProductIdOrderIdPairDto;
import com.gugucon.shopping.member.domain.vo.BirthYearRange;
import com.gugucon.shopping.member.domain.vo.Gender;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithExclusiveLock(final Long id);

    Page<Product> findAllByNameContainingIgnoreCase(final @NotNull String name, final Pageable pageable);

    @Query("select p from Product p " +
            "left join OrderStat os on p.id = os.productId " +
            "where p.name like %:keyword% " +
            "group by p.id " +
            "order by sum(os.count) desc ")
    Page<Product> findAllByNameSortByOrderCountDesc(@Param("keyword") final String keyword, final Pageable pageable);

    @Query(value = "select p from Product p " +
            "left join RateStat rs on rs.productId = p.id " +
            "where p.name like %:keyword% " +
            "group by p.id " +
            "order by sum(rs.totalScore) / sum(rs.count) desc",
            countQuery = "select count(p.id) from Product p where p.name like %:keyword%")
    Page<Product> findAllByNameSortByRateDesc(@Param("keyword") final String keyword, final Pageable pageable);

    @Query("UPDATE Product p " +
            "SET p.stock.value = p.stock.value + :value " +
            "WHERE p.id = :id")
    @Modifying
    void increaseStockByIdAndValue(@Param("id") final Long id, @Param("value") final Integer value);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN OrderStat os ON p.id = os.productId " +
            "WHERE os.birthYearRange = :birthYearRange " +
            "AND os.gender = :gender " +
            "AND p.name LIKE %:keyword% " +
            "ORDER BY os.count DESC")
    Page<Product> findAllByNameFilterWithBirthYearRangeAndGenderSortByOrderCountDesc(
            @Param("keyword") final String keyword,
            @Param("birthYearRange") final BirthYearRange birthYearRange,
            @Param("gender") final Gender gender,
            final Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN RateStat rs ON p.id = rs.productId " +
            "WHERE rs.birthYearRange = :birthYearRange " +
            "AND rs.gender = :gender " +
            "AND p.name LIKE %:keyword% " +
            "ORDER BY rs.totalScore / rs.count DESC")
    Page<Product> findAllByNameFilterWithBirthYearRangeAndGenderSortByRateDesc(
            @Param("keyword") final String keyword,
            @Param("birthYearRange") final BirthYearRange birthYearRange,
            @Param("gender") final Gender gender,
            final Pageable pageable);

    @Query(value = "select p.* from order_items oi "
            + "inner join products p on oi.product_id = p.id "
            + "where oi.order_id in ("
            + "select inner_o.id "
            + "from order_items inner_oi "
            + "inner join orders inner_o on inner_oi.order_id = inner_o.id "
            + "where inner_oi.product_id = :productId and inner_o.status = 'COMPLETED'"
            + ") and p.id != :productId "
            + "group by p.id "
            + "order by count(oi.id) desc, p.id desc", nativeQuery = true)
    Slice<Product> findRecommendedProducts(@Param("productId") final Long productId, final Pageable pageable);

    @Query("SELECT new com.gugucon.shopping.item.repository.dto.ProductIdOrderIdPairDto(oi.productId, o.id) " +
            "FROM Order o " +
            "INNER JOIN o.orderItems oi " +
            "WHERE o.status = 'COMPLETED'")
    List<ProductIdOrderIdPairDto> findAllIdWithOrderId();

    @Query("SELECT p.id FROM Product p")
    List<Long> findAllId();
}
