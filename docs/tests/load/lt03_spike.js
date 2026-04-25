/**
 * LT-03: スパイクテスト（Spike Test）
 * 目的: 急激なアクセス増加（家族が同時に起動）に対してシステムが回復できるか確認
 *
 * 合格基準:
 *   スパイク中エラー率  < 10%
 *   回復後エラー率      < 1%
 *
 * 実行:
 *   k6 run docs/tests/load/lt03_spike.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://54.162.107.130:8082';

export const options = {
  stages: [
    { duration: '30s', target: 1  },  // ベースライン（通常時）
    { duration: '10s', target: 10 },  // スパイク（急激な増加）
    { duration: '30s', target: 10 },  // スパイク維持
    { duration: '10s', target: 1  },  // 回復
    { duration: '30s', target: 1  },  // 回復後の正常動作確認
  ],
  thresholds: {
    'http_req_failed{phase:spike}':    ['rate<0.10'],
    'http_req_failed{phase:recovery}': ['rate<0.01'],
    'http_req_failed':                 ['rate<0.10'],
  },
};

export default function () {
  const today    = new Date().toISOString().slice(0, 10);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

  // 全フェーズ共通: スケジュール一覧取得（画面起動時の主要リクエスト）
  const r = http.get(
    `${BASE}/api/schedules?from=${today}&to=${tomorrow}`,
    { tags: { name: 'get_schedules' } }
  );
  check(r, {
    'status 200':          (r) => r.status === 200,
    'response time < 2s':  (r) => r.timings.duration < 2000,
  });

  // メンバー一覧も起動時に取得される
  const rm = http.get(`${BASE}/api/members`, { tags: { name: 'get_members' } });
  check(rm, { 'status 200': (r) => r.status === 200 });

  sleep(0.5);
}
