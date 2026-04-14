package com.ct.fastreport.dto;

import java.util.List;

public class ReportPageResponse {
    public List<ReportResponse> items;
    public int total;
    public int limit;
    public int offset;
}
