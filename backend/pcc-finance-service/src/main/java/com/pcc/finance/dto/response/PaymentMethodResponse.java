package com.pcc.finance.dto.response;

import com.pcc.finance.entity.PaymentMethod;

public record PaymentMethodResponse(Long id, String code, String name, String description, boolean active) {
    public static PaymentMethodResponse of(PaymentMethod m) {
        return new PaymentMethodResponse(m.getId(), m.getCode(), m.getName(), m.getDescription(), m.isActive());
    }
}
