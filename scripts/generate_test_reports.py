#!/usr/bin/env python3
import argparse
import json
import math
import random
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import List, Sequence


SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent

SERVICE_CODES = ["FTTH_200M", "FTTH_300M", "FTTH_500M", "FTTH_1000M"]
PLANS_BY_SERVICE = {
    "FTTH_200M": ["Integrated Plan 99", "Smart Home 109"],
    "FTTH_300M": ["Integrated Plan 129", "Family Plus 139"],
    "FTTH_500M": ["Integrated Plan 199", "Premium Bundle 219"],
    "FTTH_1000M": ["Integrated Plan 299", "Premium Bundle 329"],
}
MANAGERS = [
    ("300001", "Manager Li"),
    ("300002", "Manager Wang"),
    ("300003", "Manager Zhang"),
    ("300004", "Manager Chen"),
    ("300005", "Manager Liu"),
    ("300006", "Manager Zhao"),
]
ADDITIONAL_SERVICES = [
    "Cloud Disk",
    "IPTV",
    "Smart Wi-Fi",
    "Security Camera",
    "Family Data Share",
    "Streaming Bundle",
]
CUSTOMER_FIRST_NAMES = [
    "Zhang",
    "Li",
    "Wang",
    "Liu",
    "Chen",
    "Yang",
    "Zhao",
    "Huang",
    "Wu",
    "Xu",
]
CUSTOMER_LAST_NAMES = [
    "San",
    "Si",
    "Wu",
    "Lei",
    "Ming",
    "Yun",
    "Nan",
    "Qiang",
    "Jie",
    "Tao",
]
COMPLAINT_TEMPLATES = [
    "Slow broadband speed during evening peak hours",
    "Intermittent IPTV buffering on weekends",
    "Wi-Fi coverage drops in the bedroom area",
    "Customer service response felt too slow",
    "Unexpected billing increase compared with last month",
    "Packet loss noticed during video calls",
]
NETWORK_QUALITY_TEMPLATES = [
    "Download speed occasionally drops below 70% of subscribed bandwidth",
    "Upload performance is stable but evening latency rises noticeably",
    "Home Wi-Fi quality is mixed, with weak signal in one room",
    "Overall service is stable with rare short disconnects",
    "Latency is acceptable, but customer noticed occasional jitter during streaming",
]


@dataclass
class SeedRecord:
    report_id: int
    status: str
    created_at: datetime
    updated_at: datetime


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate realistic FastReport test data via API, then backfill time/status distribution."
    )
    parser.add_argument("--api-base-url", default="http://localhost:8080", help="Backend base URL")
    parser.add_argument("--count", type=int, required=True, help="Number of reports to create")
    parser.add_argument("--rate", type=float, default=10.0, help="Requests per second when posting to the API")
    parser.add_argument("--pending-ratio", type=float, default=0.05, help="Final pending ratio")
    parser.add_argument("--processing-ratio", type=float, default=0.05, help="Final processing ratio")
    parser.add_argument("--failed-ratio", type=float, default=0.10, help="Final failed ratio")
    parser.add_argument("--history-ratio", type=float, default=0.70, help="Share of records shifted into historical time windows")
    parser.add_argument("--history-span", default="7d", help="Oldest allowed history span, e.g. 6h, 3d, 14d")
    parser.add_argument("--recent-span", default="20m", help="Recent window for non-historical records")
    parser.add_argument("--settle-timeout", type=int, default=120, help="Seconds to wait for async processing before backfill")
    parser.add_argument("--poll-interval", type=float, default=2.0, help="Polling interval while waiting for terminal states")
    parser.add_argument("--seed", type=int, default=None, help="Optional deterministic random seed")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    validate_args(args)
    rng = random.Random(args.seed)

    print("Seeding strategy: hybrid API generation + database backfill")
    print(f"- API base URL: {args.api_base_url}")
    print(f"- Count: {args.count}")
    print(f"- Rate: {args.rate:.2f} req/s")

    next_customer_id = fetch_next_customer_id()
    report_ids = submit_reports(args, rng, next_customer_id)
    print(f"Accepted {len(report_ids)} reports through POST /api/reports")

    wait_for_terminal_states(report_ids, args)
    seeded_records = build_seed_records(report_ids, args, rng)
    apply_backfill(seeded_records)
    print_distribution_summary(seeded_records)
    print("Done.")
    return 0


def validate_args(args: argparse.Namespace) -> None:
    if args.count <= 0:
        raise SystemExit("--count must be greater than 0")
    if args.rate <= 0:
        raise SystemExit("--rate must be greater than 0")
    for name in ("pending_ratio", "processing_ratio", "failed_ratio", "history_ratio"):
        value = getattr(args, name)
        if value < 0 or value > 1:
            raise SystemExit(f"--{name.replace('_', '-')} must be between 0 and 1")
    if args.pending_ratio + args.processing_ratio + args.failed_ratio >= 1:
        raise SystemExit("pending + processing + failed ratios must sum to less than 1")


def fetch_next_customer_id() -> int:
    sql = "SELECT COALESCE(MAX(customer_id::bigint), 10000000) + 1 FROM customers;"
    value = run_psql_scalar(sql)
    next_id = int(value)
    if next_id > 99_999_999:
        raise SystemExit("customer_id range is exhausted")
    return next_id


def submit_reports(args: argparse.Namespace, rng: random.Random, first_customer_id: int) -> List[int]:
    report_ids: List[int] = []
    interval = 1.0 / args.rate
    for index in range(args.count):
        payload = build_payload(first_customer_id + index, index, rng)
        started = time.monotonic()
        response = post_json(f"{args.api_base_url.rstrip('/')}/api/reports", payload)
        report_ids.append(int(response["id"]))

        if (index + 1) % 50 == 0 or index == args.count - 1:
            print(f"  submitted {index + 1}/{args.count}")

        elapsed = time.monotonic() - started
        sleep_for = interval - elapsed
        if sleep_for > 0 and index < args.count - 1:
            time.sleep(sleep_for)
    return report_ids


def build_payload(customer_id_number: int, index: int, rng: random.Random) -> dict:
    customer_id = f"{customer_id_number:08d}"
    national_id = build_national_id(customer_id_number, index)
    service_code = rng.choice(SERVICE_CODES)
    current_plan = rng.choice(PLANS_BY_SERVICE[service_code])
    manager_id, manager_name = rng.choice(MANAGERS)
    customer_name = f"{rng.choice(CUSTOMER_FIRST_NAMES)} {rng.choice(CUSTOMER_LAST_NAMES)}"

    baseline = {
        "FTTH_200M": 99,
        "FTTH_300M": 129,
        "FTTH_500M": 199,
        "FTTH_1000M": 299,
    }[service_code]
    spending = [round(max(49, baseline + rng.randint(-25, 35) + rng.random()), 2) for _ in range(6)]

    complaint_count = weighted_choice(rng, [(0, 0.55), (1, 0.25), (2, 0.15), (3, 0.05)])
    complaints = [rng.choice(COMPLAINT_TEMPLATES) for _ in range(complaint_count)]

    additional_count = weighted_choice(rng, [(0, 0.25), (1, 0.40), (2, 0.25), (3, 0.10)])
    additional_services = rng.sample(ADDITIONAL_SERVICES, k=additional_count)

    return {
        "customerId": customer_id,
        "customerName": customer_name,
        "nationalId": national_id,
        "managerName": manager_name,
        "managerId": manager_id,
        "serviceCode": service_code,
        "currentPlan": current_plan,
        "additionalServices": additional_services,
        "spendingLast6": spending,
        "complaintHistory": complaints,
        "networkQuality": rng.choice(NETWORK_QUALITY_TEMPLATES),
        "overrideReason": None,
    }


def build_national_id(customer_id_number: int, index: int) -> str:
    birth_year = 1978 + (index % 20)
    birth_month = (index % 12) + 1
    birth_day = (index % 27) + 1
    sequence = customer_id_number % 1_000
    return f"110101{birth_year:04d}{birth_month:02d}{birth_day:02d}{sequence:03d}8"


def weighted_choice(rng: random.Random, weighted_values: Sequence[tuple[int, float]]) -> int:
    roll = rng.random()
    cumulative = 0.0
    for value, weight in weighted_values:
        cumulative += weight
        if roll <= cumulative:
            return value
    return weighted_values[-1][0]


def post_json(url: str, payload: dict) -> dict:
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"POST {url} failed with {exc.code}: {detail}") from exc
    except urllib.error.URLError as exc:
        raise SystemExit(f"Cannot reach {url}: {exc}") from exc


def wait_for_terminal_states(report_ids: Sequence[int], args: argparse.Namespace) -> None:
    deadline = time.monotonic() + args.settle_timeout
    total = len(report_ids)
    while True:
        terminal_count = fetch_terminal_count(report_ids)
        if terminal_count >= total:
            print(f"Async pipeline settled: {terminal_count}/{total} terminal")
            return
        if time.monotonic() >= deadline:
            print(
                f"Continuing after timeout: {terminal_count}/{total} reached completed/failed. "
                "Backfill will still run."
            )
            return
        print(f"Waiting for async processing: {terminal_count}/{total} terminal")
        time.sleep(args.poll_interval)


def fetch_terminal_count(report_ids: Sequence[int]) -> int:
    ids = ",".join(str(report_id) for report_id in report_ids)
    sql = (
        "SELECT COUNT(*) FROM reports "
        f"WHERE id = ANY(ARRAY[{ids}]::bigint[]) AND status IN ('completed', 'failed');"
    )
    return int(run_psql_scalar(sql))


def build_seed_records(report_ids: Sequence[int], args: argparse.Namespace, rng: random.Random) -> List[SeedRecord]:
    statuses = assign_statuses(len(report_ids), args, rng)
    now = datetime.now(timezone.utc)
    history_span = parse_span(args.history_span)
    recent_span = parse_span(args.recent_span)
    records: List[SeedRecord] = []
    shuffled_ids = list(report_ids)
    rng.shuffle(shuffled_ids)

    for report_id, status in zip(shuffled_ids, statuses):
        created_at = now - sample_age(rng, history_span, recent_span, args.history_ratio)
        updated_at = derive_updated_at(created_at, now, status, rng)
        records.append(SeedRecord(report_id=report_id, status=status, created_at=created_at, updated_at=updated_at))

    return records


def assign_statuses(count: int, args: argparse.Namespace, rng: random.Random) -> List[str]:
    pending_count = round(count * args.pending_ratio)
    processing_count = round(count * args.processing_ratio)
    failed_count = round(count * args.failed_ratio)
    completed_count = count - pending_count - processing_count - failed_count

    statuses = (
        ["pending"] * pending_count
        + ["processing"] * processing_count
        + ["failed"] * failed_count
        + ["completed"] * completed_count
    )
    rng.shuffle(statuses)
    return statuses


def parse_span(value: str) -> timedelta:
    value = value.strip().lower()
    if value.endswith("m"):
        return timedelta(minutes=float(value[:-1]))
    if value.endswith("h"):
        return timedelta(hours=float(value[:-1]))
    if value.endswith("d"):
        return timedelta(days=float(value[:-1]))
    raise SystemExit(f"Unsupported span '{value}'. Use formats like 30m, 6h, 7d.")


def sample_age(
    rng: random.Random,
    history_span: timedelta,
    recent_span: timedelta,
    history_ratio: float,
) -> timedelta:
    if rng.random() > history_ratio:
        return timedelta(seconds=rng.uniform(0, recent_span.total_seconds()))

    max_seconds = history_span.total_seconds()
    bucket = rng.random()
    if bucket < 0.45:
        lower, upper = 15 * 60, min(max_seconds, 6 * 3600)
    elif bucket < 0.80:
        lower, upper = 6 * 3600, min(max_seconds, 48 * 3600)
    else:
        lower, upper = 48 * 3600, max_seconds

    lower = min(lower, max_seconds)
    upper = max(lower, upper)
    return timedelta(seconds=rng.uniform(lower, upper))


def derive_updated_at(created_at: datetime, now: datetime, status: str, rng: random.Random) -> datetime:
    age_seconds = max(1.0, (now - created_at).total_seconds())
    if status == "pending":
        delta = min(age_seconds * 0.15, rng.uniform(30, 300))
    elif status == "processing":
        delta = min(age_seconds * 0.60, rng.uniform(120, 1800))
    else:
        delta = min(age_seconds * 0.95, rng.uniform(180, max(181.0, age_seconds)))
    updated_at = created_at + timedelta(seconds=delta)
    return min(updated_at, now - timedelta(seconds=1))


def apply_backfill(records: Sequence[SeedRecord]) -> None:
    values = []
    for record in records:
        values.append(
            "({id}, '{status}', TIMESTAMP '{created_at}', TIMESTAMP '{updated_at}')".format(
                id=record.report_id,
                status=record.status,
                created_at=record.created_at.strftime("%Y-%m-%d %H:%M:%S"),
                updated_at=record.updated_at.strftime("%Y-%m-%d %H:%M:%S"),
            )
        )

    sql = f"""
BEGIN;
WITH seed(id, status, created_at, updated_at) AS (
    VALUES
    {",\n    ".join(values)}
)
UPDATE reports AS r
SET
    status = seed.status,
    created_at = seed.created_at,
    updated_at = seed.updated_at,
    report_content = CASE
        WHEN seed.status = 'completed' THEN COALESCE(
            NULLIF(r.report_content, ''),
            '# Report Summary\n## Executive Summary\n- Seeded completed report for realistic UI and monitoring tests.\n## Customer Profile\n- Generated by the test data seeding script.\n## Service Assessment\n- Data was backfilled after API creation.\n## Risk Signals\n- Use for non-production validation only.\n## Recommended Actions\n1. Review the account.\n## Follow-Up Plan\n- Owner: Seed Script\n- Timeline: Within 3 business days\n- Success Measure: Record renders correctly in the application.'
        )
        ELSE NULL
    END
FROM seed
WHERE r.id = seed.id;
COMMIT;
"""
    run_psql(sql)


def print_distribution_summary(records: Sequence[SeedRecord]) -> None:
    counts: dict[str, int] = {}
    oldest = min(record.created_at for record in records)
    newest = max(record.created_at for record in records)
    for record in records:
        counts[record.status] = counts.get(record.status, 0) + 1

    print("Final seeded distribution:")
    for status in ("pending", "processing", "completed", "failed"):
        print(f"- {status}: {counts.get(status, 0)}")
    print(f"- oldest created_at (UTC): {oldest.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"- newest created_at (UTC): {newest.strftime('%Y-%m-%d %H:%M:%S')}")


def run_psql_scalar(sql: str) -> str:
    command = [
        "docker",
        "compose",
        "exec",
        "-T",
        "db",
        "psql",
        "-U",
        "fastreport",
        "-d",
        "fastreport",
        "-v",
        "ON_ERROR_STOP=1",
        "-At",
        "-c",
        sql,
    ]
    result = subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"psql query failed:\n{result.stderr.strip()}")
    return result.stdout.strip()


def run_psql(sql: str) -> None:
    command = [
        "docker",
        "compose",
        "exec",
        "-T",
        "db",
        "psql",
        "-U",
        "fastreport",
        "-d",
        "fastreport",
        "-v",
        "ON_ERROR_STOP=1",
    ]
    result = subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        input=sql,
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"psql update failed:\n{result.stderr.strip()}")


if __name__ == "__main__":
    sys.exit(main())
