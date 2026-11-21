package com.pcc.finance.service;

import com.pcc.finance.dto.request.PaymentMethodRequest;
import com.pcc.finance.dto.response.PaymentMethodResponse;
import com.pcc.finance.entity.PaymentMethod;
import com.pcc.finance.exception.DuplicateResourceException;
import com.pcc.finance.exception.ResourceNotFoundException;
import com.pcc.finance.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository methodRepository;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> list() {
        return methodRepository.findAll().stream().map(PaymentMethodResponse::of).toList();
    }

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest req) {
        if (methodRepository.findByCode(req.code().toUpperCase()).isPresent()) {
            throw new DuplicateResourceException("Payment method exists: " + req.code());
        }
        PaymentMethod m = new PaymentMethod();
        m.setCode(req.code().toUpperCase());
        m.setName(req.name());
        m.setDescription(req.description());
        return PaymentMethodResponse.of(methodRepository.save(m));
    }

    @Transactional
    public PaymentMethodResponse toggle(Long id) {
        PaymentMethod m = methodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
        m.setActive(!m.isActive());
        return PaymentMethodResponse.of(methodRepository.save(m));
    }
}
