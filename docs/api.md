# API Reference

The backend exposes report creation, query, live update, and export endpoints.

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/reports` | Create a report and enqueue async generation |
| GET | `/api/reports` | List reports |
| GET | `/api/reports/page` | List reports with pagination metadata |
| GET | `/api/reports/{id}` | Get one report |
| GET | `/api/reports/{id}/events` | Open an SSE stream for local report updates |
| GET | `/api/reports/{id}/download?format=txt\|pdf\|csv` | Download report content |

## Example Create Request

```json
{
  "customerId": "10000001",
  "customerName": "Zhang San",
  "nationalId": "110101199003077758",
  "managerName": "Manager Li",
  "managerId": "200001",
  "serviceCode": "FTTH_500M",
  "currentPlan": "Integrated Plan 199",
  "additionalServices": ["Cloud Disk", "IPTV"],
  "spendingLast6": [199, 199, 210, 185, 199, 220],
  "complaintHistory": ["2024-12 Slow broadband speed", "2025-01 Slow customer service response"],
  "networkQuality": "Download speed occasionally drops below 50% of subscribed bandwidth",
  "overrideReason": "Confirmed by operator after manual review"
}
```

## Example Success Response

```json
{
  "id": 12,
  "status": "pending",
  "message": "Report accepted. It will be generated in the background."
}
```

## Unified Error Handling

The backend uses a unified exception system built around `BaseAppException`, typed subclasses, and `@RestControllerAdvice` for consistent JSON error responses.

Error response format:

```json
{
  "type": "VALIDATION_ERROR",
  "code": "INVALID_CUSTOMER_ID",
  "message": "customerId must be exactly 8 digits.",
  "detail": {},
  "httpStatus": 400
}
```

The frontend API layer parses this structure into a typed `ApiError`, so UI code can branch on `type` and `code` instead of raw message text.

## Example Warning Response

```json
{
  "type": "WARNING",
  "code": "OVERRIDE_REASON_REQUIRED",
  "message": "Override reason is required for warning cases.",
  "detail": {
    "warnings": ["Same nationalId exists under a different customerId."]
  },
  "httpStatus": 409
}
```

## Validation

Validation checks include:

- required fields
- type and null checks for array values
- format checks for `customerId`, `managerId`, and `nationalId`
- dictionary checks for `serviceCode`

## Duplicate Detection

Duplicate-detection rules include:

- same `customer_id` + different `national_id`: `BLOCK`
- same `manager_id` + different `manager_name`: `BLOCK`
- same customer + same service + same day: `BLOCK`
- same customer + same service + different day: `WARNING`
- same `national_id` + different `customer_id`: `WARNING`

If a warning is triggered and `overrideReason` is missing, the backend rejects the request.

## Export Formats

Completed reports can be downloaded as:

- TXT
- PDF
- CSV
