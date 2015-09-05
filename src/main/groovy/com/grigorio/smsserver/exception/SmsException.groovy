package com.grigorio.smsserver.exception

class SmsException extends Exception {
    public class Reason {
        static def argNull = 'Argument can\'t be null'
        static def argMultiPart = 'Argument is from a multi-part SMS'
        static def zeroLength = 'Array is empty'
        static def notAllPdus = 'Not enough PDUs'
        static def mixedPdus = 'PDUs are from different messages'
    }

    public SmsException(String msg) {
        super(msg)
    }
}
