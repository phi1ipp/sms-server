package com.grigorio.smsserver.exception

class MailerServiceException extends Exception {
    public class Reason {
        def static noConfig = 'No configuration found'
    }

    public MailerServiceException(String msg) {
        super(msg)
    }
}
