package ch.ejpd.lgs.searchindex.client.adapter.aspect;

import ch.ejpd.lgs.searchindex.client.adapter.rest.Headers;
import ch.ejpd.lgs.searchindex.client.util.SenderUtil;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class SenderIdValidationAspect {

    private final SenderUtil senderUtil;

    public SenderIdValidationAspect(SenderUtil senderUtil) {
        this.senderUtil = senderUtil;
    }

    @Before(value = "execution(* ch.ejpd.lgs.searchindex.client.adapter.rest.SyncController.*(..))")
    public void validateRequestId(JoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String senderId = request.getHeader(Headers.X_LGS_SENDER_ID);

        if (Strings.isNotBlank(senderId)) {
            senderUtil.validate(senderId);
        }
    }
}
