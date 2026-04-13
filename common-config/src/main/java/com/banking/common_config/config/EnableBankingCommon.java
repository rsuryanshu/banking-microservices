package com.banking.common_config.config;

import org.springframework.context.annotation.ComponentScan;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan(basePackages = "com.banking.common_config")
public @interface EnableBankingCommon {
}
