package com.sdumagicode.backend.core.exception;

import com.sdumagicode.backend.enumerate.TransactionCode;

/**
 * @author ronger
 */
public class TransactionException extends BusinessException {

    private int code;

    private String message;

    public TransactionException(TransactionCode transactionCode) {
        super(transactionCode.getMessage());
        this.code = transactionCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
