/**
 * LT-01: 通常負荷テスト（Normal Load）
 * 目的: 日常利用（最大3名同時アクセス）で NFR を満たすことを確認
 *
 * 合格基準:
 *   GET  /api/schedules  P95 < 1000ms
 *   POST/PUT/DELETE      P95 <  500ms
 *   エラー率              < 1%
 *
 * 実行:
 *   k6 run docs/tests/load/lt01_normal.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://54.162.107.130:8082';
const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  vus: 3,
  duration: '3m',
  thresholds: {
    'http_req_duration{name:get_schedules}':  ['p(95)<1000'],
    'http_req_duration{name:post_schedule}':  ['p(95)<500'],
    'http_req_duration{name:put_schedule}':   ['p(95)<500'],
    'http_req_duration{name:delete_schedule}':['p(95)<500'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const rand    = Math.random();
  const today   = new Date().toISOString().slice(0, 10);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

  if (rand < 0.70) {
    // 閲覧 70%
    const r = http.get(
      `${BASE}/api/schedules?from=${today}&to=${tomorrow}`,
      { tags: { name: 'get_schedules' } }
    );
    check(r, { 'status 200': (r) => r.status === 200 });

  } else if (rand < 0.90) {
    // 登録 20%（登録後に purge して後片付け）
    const r = http.post(
      `${BASE}/api/schedules`,
      JSON.stringify({ memberId: 1, date: today, content: '負荷テスト' }),
      { headers: HEADERS, tags: { name: 'post_schedule' } }
    );
    check(r, { 'status 201': (r) => r.status === 201 });
    if (r.status === 201) {
      const id = r.json('id');
      http.post(`${BASE}/api/schedules/${id}/purge`, null, { headers: HEADERS });
    }

  } else {
    // メンバー一覧取得 10%（画面起動時相当）
    const r = http.get(`${BASE}/api/members`, { tags: { name: 'get_members' } });
    check(r, { 'status 200': (r) => r.status === 200 });
  }

  sleep(1);
}
