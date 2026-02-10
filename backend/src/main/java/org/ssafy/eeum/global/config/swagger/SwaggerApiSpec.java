package org.ssafy.eeum.global.config.swagger;

import org.ssafy.eeum.global.error.model.ErrorCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger API 명세를 상세 설정하기 위한 커스텀 어노테이션입니다.
 * 요약, 상세 설명, 성공 코드 및 메시지, 발생 가능 에러를 정의할 수 있습니다.
 * 
 * @summary Swagger 명세 설정 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwaggerApiSpec {
    String summary();

    String description() default "";

    int successCode() default 200;

    String successMessage() default "요청 처리에 성공하였습니다.";

    ErrorCode[] errors() default {};
}