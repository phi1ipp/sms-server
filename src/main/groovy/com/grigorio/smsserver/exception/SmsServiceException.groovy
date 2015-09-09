package com.grigorio.smsserver.exception

class SmsServiceException extends Exception {
    class Reason {
        static def nullTelnetSvc = 'No running telnet service found'
    }

    public SmsServiceException(String msg) {
        super(msg)
    }
}
