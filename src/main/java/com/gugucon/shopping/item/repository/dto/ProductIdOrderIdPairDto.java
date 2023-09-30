package com.gugucon.shopping.item.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProductIdOrderIdPairDto {
    private Long productId;
    private Long orderId;
}
