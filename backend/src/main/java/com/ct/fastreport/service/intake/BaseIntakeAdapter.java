package com.ct.fastreport.service.intake;

import com.ct.fastreport.model.InternalReportRequest;

public abstract class BaseIntakeAdapter<TRawInput, TParsedPayload> {

    public abstract String sourceSystem();

    public abstract TParsedPayload parse(TRawInput rawInput);

    public abstract InternalReportRequest transform(TParsedPayload parsedPayload);

    public abstract void validate(InternalReportRequest request);

    public final InternalReportRequest intake(TRawInput rawInput) {
        TParsedPayload parsedPayload = parse(rawInput);
        InternalReportRequest request = transform(parsedPayload);
        validate(request);
        return request;
    }
}
