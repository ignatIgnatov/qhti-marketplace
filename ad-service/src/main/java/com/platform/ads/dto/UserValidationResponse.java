package com.platform.ads.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserValidationResponse {
    private boolean exists;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
}
