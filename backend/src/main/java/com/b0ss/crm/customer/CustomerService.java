package com.b0ss.crm.customer;

import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.common.PageResponse;
import com.b0ss.crm.customer.dto.CustomerRequest;
import com.b0ss.crm.customer.dto.CustomerResponse;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CustomerService {

    private static final Set<String> SORTABLE = Set.of("name", "company", "email", "createdAt", "updatedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String q, int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<Customer> result;
        if (q == null || q.isBlank()) {
            result = customerRepository.findAll(pageable);
        } else {
            result = customerRepository.search("%" + q.trim().toLowerCase() + "%", pageable);
        }
        return PageResponse.from(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request, String username) {
        Customer customer = new Customer();
        apply(customer, request);
        customer.setCreatedBy(findUser(username));
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        apply(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = find(id);
        customerRepository.delete(customer);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + id));
    }

    private User findUser(String username) {
        return username == null ? null : userRepository.findByUsername(username).orElse(null);
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setPhone(trimToNull(request.phone()));
        customer.setEmail(trimToNull(request.email()));
        customer.setCompany(trimToNull(request.company()));
        customer.setNote(trimToNull(request.note()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String field = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String candidate = parts[0].trim();
            if (SORTABLE.contains(candidate)) {
                field = candidate;
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                    direction = Sort.Direction.ASC;
                }
            }
        }
        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getCompany(),
                customer.getNote(),
                customer.getCreatedBy() == null ? null : customer.getCreatedBy().getUsername(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
