import http from "k6/http";
import { check } from "k6";

// Real load test against ingestion-service's POST /api/v1/events, run directly against the
// service (bypassing api-gateway's rate limiter, which would otherwise dominate the numbers)
// to measure the ingest -> Kafka publish path itself. See root README "Performance" section
// for the exact command and the real output this produced.
export const options = {
  scenarios: {
    ingest: {
      executor: "constant-vus",
      vus: 50,
      duration: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8082";
const TENANT_ID = "11111111-1111-1111-1111-111111111111";
const USER_ID = "22222222-2222-2222-2222-222222222222";

export default function () {
  const payload = JSON.stringify({
    source: `web-${__VU}`,
    type: "CPU_USAGE",
    metricName: "cpu.usage.percent",
    value: Math.random() * 100,
  });

  const res = http.post(`${BASE_URL}/api/v1/events`, payload, {
    headers: {
      "Content-Type": "application/json",
      "X-Tenant-Id": TENANT_ID,
      "X-User-Id": USER_ID,
      "X-User-Roles": "MEMBER",
    },
  });

  check(res, {
    "status is 202": (r) => r.status === 202,
  });
}
