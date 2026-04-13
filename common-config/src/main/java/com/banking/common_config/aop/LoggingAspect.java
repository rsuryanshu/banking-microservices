package com.banking.common_config.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object globalLogExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long timeTaken = System.currentTimeMillis() - start;

        log.info("[{}::{}] executed in {} ms", className, methodName, timeTaken);

        return result;
    }

    @Around("@annotation(com.banking.common_config.aop.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long timeTaken = System.currentTimeMillis() - start;

        log.info("[{}::{}] executed in {} ms", className, methodName, timeTaken);

        return result;
    }
}
