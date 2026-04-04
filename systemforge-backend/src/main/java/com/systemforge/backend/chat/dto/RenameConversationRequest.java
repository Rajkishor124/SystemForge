package com.systemforge.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenameConversationRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;
}
