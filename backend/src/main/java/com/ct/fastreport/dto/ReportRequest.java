package com.ct.fastreport.dto;

import java.util.List;

public class ReportRequest {
    public String customerId;
    public String customerName;
    public String nationalId;
    public String managerName;
    public String managerId;
    public String serviceCode;
    public String currentPlan;
    public List<String> additionalServices;
    public List<Double> spendingLast6;
    public List<String> complaintHistory;
    public String networkQuality;
}
