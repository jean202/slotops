package com.jean325.slotops.customer.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByOrganization_IdAndPhone(Long organizationId, String phone);

    boolean existsByIdAndOrganization_Id(Long id, Long organizationId);
}
