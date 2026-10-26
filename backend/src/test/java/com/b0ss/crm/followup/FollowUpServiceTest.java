package com.b0ss.crm.followup;

import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.customer.Customer;
import com.b0ss.crm.customer.CustomerRepository;
import com.b0ss.crm.followup.dto.FollowUpRequest;
import com.b0ss.crm.followup.dto.FollowUpResponse;
import com.b0ss.crm.user.Role;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock
    private FollowUpRepository followUpRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowUpService followUpService;

    @Test
    void listThrowsWhenCustomerIsMissing() {
        when(customerRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.list(42L, 0, 20))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listReturnsMappedPage() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L)));
        FollowUp followUp = followUp(9L, "Called back");
        when(followUpRepository.findByCustomerId(org.mockito.ArgumentMatchers.eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(followUp)));

        var page = followUpService.list(1L, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).content()).isEqualTo("Called back");
        assertThat(page.content().get(0).customerId()).isEqualTo(1L);
    }

    @Test
    void createSavesFollowUpWithAuthor() {
        Customer customer = customer(3L);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(userRepository.findByUsername("sales")).thenReturn(Optional.of(user("sales")));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(invocation -> {
            FollowUp saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        FollowUpResponse response = followUpService.create(3L, new FollowUpRequest("  Demo done  "), "sales");

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.content()).isEqualTo("Demo done");
        assertThat(response.createdBy()).isEqualTo("sales");
        verify(followUpRepository).save(any(FollowUp.class));
    }

    @Test
    void createThrowsWhenCustomerIsMissing() {
        when(customerRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followUpService.create(8L, new FollowUpRequest("x"), "sales"))
                .isInstanceOf(NotFoundException.class);
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setName("Acme");
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private FollowUp followUp(Long id, String content) {
        FollowUp followUp = new FollowUp();
        followUp.setCustomer(customer(1L));
        followUp.setContent(content);
        ReflectionTestUtils.setField(followUp, "id", id);
        return followUp;
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName("Sam Lee");
        user.setPassword("hash");
        user.setRole(Role.SALES);
        return user;
    }
}
