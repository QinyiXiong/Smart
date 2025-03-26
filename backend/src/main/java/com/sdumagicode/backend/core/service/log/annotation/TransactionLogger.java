package com.sdumagicode.backend.core.service.log.annotation;

import com.sdumagicode.backend.enumerate.TransactionEnum;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author ronger
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionLogger {

    TransactionEnum transactionType();

}
