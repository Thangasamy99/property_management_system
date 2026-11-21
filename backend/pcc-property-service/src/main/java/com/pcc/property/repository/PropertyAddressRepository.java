package com.pcc.property.repository;

import com.pcc.property.entity.PropertyAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyAddressRepository extends JpaRepository<PropertyAddress, Long> {
}
