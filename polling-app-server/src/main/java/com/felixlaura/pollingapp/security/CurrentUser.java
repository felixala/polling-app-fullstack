package com.felixlaura.pollingapp.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;


/**
 * Spring security provides an annotation called @AuthenticationPrincipal to access the currently authenticated
 * user in the controllers.
 * CurrentUser annotation is a wrapper around @AuthenticationPrincipal annotation.
 */

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
