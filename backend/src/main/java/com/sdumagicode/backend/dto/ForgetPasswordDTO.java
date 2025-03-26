package com.sdumagicode.backend.dto;

import lombok.Data;

/**
 * @author ronger
 */
@Data
public class ForgetPasswordDTO {
    private String code;

    private String password;
}
