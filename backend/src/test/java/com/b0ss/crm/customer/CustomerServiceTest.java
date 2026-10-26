package com.b0ss.crm.customer;

import com.b0ss.crm.common.NotFoundException;
import com.b0ss.crm.common.PageResponse;
import com.b0ss.crm.customer.dto.CustomerRequest;
import com.b0ss.crm.customer.dto.CustomerResponse;
import com.b0ss.crm.user.Role;
import com.b0ss.crm.user.User;
import com.b0ss.crm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void listWithBlankQueryUsesFindAll() {
        when(customerRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer(1L, "Acme"))));

        PageResponse<CustomerResponse> result = customerService.list("   ", 0, 10, "name,asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Acme");
        verify(customerRepository).findAll(any(Pageable.class));
        verify(customerRepository, never()).search(anyString(), any());
    }

    @Test
    void listWithQueryUsesLowercasePattern() {
        when(customerRepository.search(eq("%acme%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer(1L, "Acme"))));

        customerService.list("ACME", 0, 10, "name,asc");

        verify(customerRepository).search(eq("%acme%"), any(Pageable.class));
    }

    @Test
    void listClampsPageAndSize() {
        when(customerRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Customer>(List.of(), invocation.getArgument(0), 0));

        customerService.list(null, -5, 500, "name,asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listWithUnknownSortFallsBackToCreatedAtDescending() {
        when(customerRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Customer>(List.of(), invocation.getArgument(0), 0));

        customerService.list(null, 0, 10, "password,asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getMissingCustomerThrowsNotFound() {
        when(customerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.get(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("9");
    }

    @Test
    void createTrimsValuesAndSetsCreator() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user("admin")));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 5L);
            return saved;
        });

        CustomerResponse response = customerService.create(
                new CustomerRequest("  Acme Ltd  ", "  +852 9000 0000 ", "  ", "  Acme Group  ", null),
                "admin");

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Acme Ltd");
        assertThat(response.phone()).isEqualTo("+852 9000 0000");
        assertThat(response.email()).isNull();
        assertThat(response.company()).isEqualTo("Acme Group");
        assertThat(response.createdBy()).isEqualTo("admin");
    }

    @Test
    void updateMissingCustomerThrowsNotFound() {
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(7L, new CustomerRequest("X", null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteMissingCustomerThrowsNotFound() {
        when(customerRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(3L))
                .isInstanceOf(NotFoundException.class);
    }

    private Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setName(name);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName("Admin User");
        user.setPassword("hash");
        user.setRole(Role.ADMIN);
        return user;
    }
}
