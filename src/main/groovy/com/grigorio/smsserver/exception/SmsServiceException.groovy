package com.grigorio.smsserver.exception

class SmsServiceException extends Exception {
    class Reason {
        static def nullTelnetSvc = 'No running telnet service found'
        static def telnetNoPriv = 'Not in priveleged mode'
    }

    public SmsServiceException(String msg) {
        super(msg)
    }
}
