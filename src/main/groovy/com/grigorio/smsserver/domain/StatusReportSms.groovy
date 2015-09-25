package com.grigorio.smsserver.domain

class StatusReportSms extends Sms {
    public StatusReportSms(String address, char status) {
        super(address, '')
        this.status = status
    }

    @Override
    String toString() {
        String str = super.toString()
        str.replaceFirst('SMS', 'StatusReportSMS')
    }
}
