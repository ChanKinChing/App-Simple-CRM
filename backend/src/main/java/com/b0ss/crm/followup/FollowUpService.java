package com.b0ss.crm.followup;

import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.common.PageResponse;
import com.b0ss.crm.customer.Customer;
import com.b0ss.crm.customer.CustomerRepository;
import com.b0ss.crm.followup.dto.FollowUpRequest;
import com.b0ss.crm.followup.dto.FollowUpResponse;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public FollowUpService(FollowUpRepository followUpRepository,
                           CustomerRepository customerRepository,
                           UserRepository userRepository) {
        this.followUpRepository = followUpRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUpResponse> list(Long customerId, int page, int size) {
        requireCustomer(customerId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<FollowUp> result = followUpRepository.findByCustomerId(customerId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, this::toResponse);
    }

    @Transactional
    public FollowUpResponse create(Long customerId, FollowUpRequest request, String username) {
        Customer customer = requireCustomer(customerId);

        FollowUp followUp = new FollowUp();
        followUp.setCustomer(customer);
        followUp.setContent(request.content().trim());
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            followUp.setCreatedBy(user);
        }
        return toResponse(followUpRepository.save(followUp));
    }

    private Customer requireCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    private FollowUpResponse toResponse(FollowUp followUp) {
        return new FollowUpResponse(
                followUp.getId(),
                followUp.getCustomer().getId(),
                followUp.getContent(),
                followUp.getCreatedBy() == null ? null : followUp.getCreatedBy().getUsername(),
                followUp.getCreatedAt());
    }
}
