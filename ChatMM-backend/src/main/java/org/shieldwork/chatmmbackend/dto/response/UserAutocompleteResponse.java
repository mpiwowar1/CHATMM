package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAutocompleteResponse {
    private String email;
    private String name;
}