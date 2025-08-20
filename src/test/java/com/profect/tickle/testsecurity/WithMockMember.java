package com.profect.tickle.testsecurity;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {
    long id() default 1L;
    String email() default "user@example.com";
    String[] roles() default { "MEMBER" };
}
