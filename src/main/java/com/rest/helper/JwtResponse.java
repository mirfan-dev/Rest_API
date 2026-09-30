package com.rest.helper;

import com.rest.dtos.CustomerDtos;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {

    private String accessToken;

    private String refreshToken;

    private CustomerDtos user;
}
