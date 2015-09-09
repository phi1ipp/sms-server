package com.grigorio.smsserver.exception;

public class TelnetServiceException extends Exception {
    public class Reason {
        static def invChannel = 'Invalid channel value'
        static def delFailed = 'Can\'t delete SMS'
        static def readFailed = 'Can\'t read SMS'
        static def sendFailed = 'Can\'t send SMS'
        static def noConfig = 'No configuration found'
    }

    public TelnetServiceException(String msg) {
        super(msg)
    }
}
