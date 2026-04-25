/**
 * LT-02: ストレス負荷テスト（Stress Load）
 * 目的: 全メンバー（5名）同時操作時のピーク応答劣化を確認
 *
 * 合格基準:
 *   GET /api/schedules  P95 < 1000ms
 *   GET /api/schedules  P99 < 2000ms
 *   エラー率              < 5%
 *
 * 実行:
 *   k6 run docs/tests/load/lt02_stress.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://54.162.107.130:8082';
const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  stages: [
    { duration: '30s', target: 5 },  // ランプアップ（0→5VU）
    { duration: '90s', target: 5 },  // 維持
    { duration: '30s', target: 0 },  // ランプダウン
  ],
  thresholds: {
    'http_req_duration{name:get_schedules}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:post_schedule}': ['p(95)<1000'],
    'http_req_failed': ['rate<0.05'],
  },
};

export default function () {
  const rand    = Math.random();
  const today   = new Date().toISOString().slice(0, 10);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

  if (rand < 0.75) {
    // 閲覧 75%
    const r = http.get(
      `${BASE}/api/schedules?from=${today}&to=${tomorrow}`,
      { tags: { name: 'get_schedules' } }
    );
    check(r, { 'status 200': (r) => r.status === 200 });

  } else if (rand < 0.95) {
    // 登録 20%（5名が同時に予定を入れるシナリオ）
    const memberId = Math.floor(Math.random() * 5) + 1; // 1〜5
    const r = http.post(
      `${BASE}/api/schedules`,
      JSON.stringify({ memberId, date: today, content: 'ストレステスト' }),
      { headers: HEADERS, tags: { name: 'post_schedule' } }
    );
    check(r, { 'status 201': (r) => r.status === 201 });
    if (r.status === 201) {
      const id = r.json('id');
      http.post(`${BASE}/api/schedules/${id}/purge`, null, { headers: HEADERS });
    }

  } else {
    // メンバー一覧 5%
    const r = http.get(`${BASE}/api/members`, { tags: { name: 'get_members' } });
    check(r, { 'status 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}
