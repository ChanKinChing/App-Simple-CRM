package com.b0ss.crm.followup;

import com.b0ss.crm.common.PageResponse;
import com.b0ss.crm.followup.dto.FollowUpRequest;
import com.b0ss.crm.followup.dto.FollowUpResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/{customerId}/follow-ups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    @GetMapping
    public PageResponse<FollowUpResponse> list(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followUpService.list(customerId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse create(@PathVariable Long customerId,
                                   @Valid @RequestBody FollowUpRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return followUpService.create(customerId, request, jwt.getSubject());
    }
}
