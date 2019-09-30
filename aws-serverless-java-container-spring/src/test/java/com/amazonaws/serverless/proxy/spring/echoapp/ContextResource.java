package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.ValidatedUserModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;


@RestController
@EnableWebMvc
@RequestMapping("/context")
public class ContextResource implements ServletContextAware {
    public static final String COOKIE_DOMAIN = "mydomain.com";
    public static final String COOKIE_NAME = "CustomCookie";
    public static final String COOKIE_VALUE = "CookieValue";
    public static final String EXCEPTION_REASON = "There was a conflict";
    private ServletContext context;

    @RequestMapping(path = "/echo", method= RequestMethod.GET)
    public ResponseEntity<String> getContext() {
        return new ResponseEntity<String>(this.context.getServerInfo(), HttpStatus.OK);
    }

    @RequestMapping(path = "/user", method=RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ValidatedUserModel> createUser(@Valid @RequestBody ValidatedUserModel newUser, BindingResult results) {

        if (results.hasErrors()) {
            return new ResponseEntity<ValidatedUserModel>(newUser, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<ValidatedUserModel>(newUser, HttpStatus.OK);
    }

    @RequestMapping(path = "/cookie", method=RequestMethod.GET)
    public SingleValueModel setCookie(ServletRequest request, ServletResponse response) {
        setCookie(request, response, COOKIE_NAME, COOKIE_VALUE, true, false, true, null, false);
        return new SingleValueModel();
    }

    @RequestMapping(path = "/exception", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SingleValueModel handleException() {
        throw new SpringConflictException();
    }

    public static void setCookie(ServletRequest request, ServletResponse response, String name, String value,
                                 boolean set, boolean global, boolean bSecureCookie, Integer maxAge, boolean httpOnly) {
        Cookie ck = new Cookie(name, value);

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (httpOnly) {
            ck.setHttpOnly(true);
        }

        if (set) {
            if (maxAge != null) {
                ck.setMaxAge(maxAge.intValue());
            } else {
                ck.setMaxAge(-1);
            }
        } else {
            ck.setMaxAge(0);
        }
        ck.setPath("/");

        // for local and fngn envs., we should not set cookie as a secure cookie
        if (bSecureCookie) {
            ck.setSecure(true);
        }

        ck.setDomain(COOKIE_DOMAIN);


        ((HttpServletResponse) response).addCookie(ck);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        context = servletContext;
    }

    @ControllerAdvice
    class ExceptionHandlerAdvice {
        @ExceptionHandler({ SpringConflictException.class })
        @ResponseBody
        ResponseEntity<SingleValueModel> handle(Exception exception) {
            SingleValueModel body = new SingleValueModel();
            body.setValue(resolveAnnotatedExceptionReason(exception));
            HttpStatus responseStatus = resolveAnnotatedResponseStatus(exception);
            return new ResponseEntity<>(body, responseStatus);
        }

        HttpStatus resolveAnnotatedResponseStatus(Exception exception) {
            ResponseStatus annotation = findMergedAnnotation(exception.getClass(), ResponseStatus.class);
            if (annotation != null) {
                return annotation.value();
            }
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String resolveAnnotatedExceptionReason(Exception exception) {
            ResponseStatus annotation = findMergedAnnotation(exception.getClass(), ResponseStatus.class);
            if (annotation != null && !"".equals(annotation.reason())) {
                return annotation.reason();
            }
            return exception.getLocalizedMessage();
        }
    }

    @ResponseStatus(value=HttpStatus.CONFLICT, reason= EXCEPTION_REASON)
    public class SpringConflictException extends RuntimeException {

    }
}
