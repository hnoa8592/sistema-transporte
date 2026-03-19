package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.CustomerRequest;
import com.transporte.operacion.dto.CustomerResponse;
import com.transporte.operacion.entity.Customer;
import com.transporte.operacion.mapper.CustomerMapper;
import com.transporte.operacion.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public PageResponse<CustomerResponse> findAll(Pageable pageable) {
        return PageResponse.of(customerRepository.findAllByActiveTrue(pageable).map(customerMapper::toResponse));
    }

    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(findCustomerById(id));
    }

    public CustomerResponse findByDocument(String documentNumber) {
        return customerMapper.toResponse(
                customerRepository.findByDocumentNumber(documentNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer with document " + documentNumber + " not found"))
        );
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Customer", description = "Registro de nuevo cliente")
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (request.documentNumber() != null && customerRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new BusinessException("Customer with document '" + request.documentNumber() + "' already exists");
        }
        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Customer", description = "Actualización de datos del cliente")
    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findCustomerById(id);
        customerMapper.updateFromRequest(request, customer);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Customer", description = "Baja de cliente del sistema")
    @Transactional
    public void delete(UUID id) {
        Customer customer = findCustomerById(id);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    private Customer findCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
