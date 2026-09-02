package com.northbridge.document;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing signer information for document sign requests.
 */
@Data
@NoArgsConstructor
public class SignRequest {
    private String signerName;
}
