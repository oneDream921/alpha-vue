package io.github.onedream921.alphavue.modules.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    /**
     * 业务模块名称
     */
    String module();

    /**
     * 操作名称
     */
    String operation();

    /**
     * 操作业务类型
     */
    BusinessType type() default BusinessType.OTHER;

    /**
     * 是否记录请求参数，默认关闭以减少敏感信息风险
     */
    boolean saveRequest() default false;

    /**
     * 是否记录响应内容，默认关闭以减少敏感信息风险
     */
    boolean saveResponse() default false;

    /**
     * 记录请求参数时需要排除的敏感参数名
     */
    String[] excludeParamNames() default {"password", "token", "captcha", "secret", "key"};
}
