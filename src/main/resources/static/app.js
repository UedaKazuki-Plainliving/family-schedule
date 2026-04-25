// 家族スケジュール SPA

const STORAGE_KEY = 'familySchedule.currentUser';

const state = {
  members: [],
  currentUser: null,
  viewDate: null, // Date object (= today by default, shows [viewDate, viewDate+1])
  selectedWho: null,
  editingId: null,
  pendingUndo: null, // { id, timerId } — 保留中のソフト削除
};

// ---------- utilities ----------
function toISODate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
function fromISODate(s) {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}
function addDays(d, n) {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}
function dayLabel(d) {
  const wd = ['日','月','火','水','木','金','土'][d.getDay()];
  return `${d.getMonth()+1}/${d.getDate()}(${wd})`;
}
function codePointLength(s) {
  return [...s].length;
}
function setScreen(name) {
  document.getElementById('app').dataset.screen = name;
  document.getElementById('screen-select-user').hidden = (name !== 'select-user');
  document.getElementById('screen-schedule').hidden = (name !== 'schedule');
}
function showToast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(showToast._tid);
  showToast._tid = setTimeout(() => { t.hidden = true; }, 2000);
}

async function commitPendingUndo() {
  if (!state.pendingUndo) return;
  clearTimeout(state.pendingUndo.timerId);
  const id = state.pendingUndo.id;
  state.pendingUndo = null;
  try { await api('POST', `/api/schedules/${id}/purge`); } catch {}
}

function showUndoToast(id) {
  const t = document.getElementById('toast');
  t.innerHTML = '';
  const msg = document.createTextNode('削除しました ');
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = 'toast-undo-btn';
  btn.textContent = '元に戻す';
  t.appendChild(msg);
  t.appendChild(btn);
  t.hidden = false;

  const timerId = setTimeout(async () => {
    t.hidden = true;
    state.pendingUndo = null;
    try { await api('POST', `/api/schedules/${id}/purge`); } catch {}
  }, 5000);

  state.pendingUndo = { id, timerId };

  btn.addEventListener('click', async () => {
    clearTimeout(timerId);
    t.hidden = true;
    state.pendingUndo = null;
    try {
      await api('POST', `/api/schedules/${id}/restore`);
      await loadAndRender();
      showToast('元に戻しました');
    } catch {
      showToast('復元に失敗しました');
    }
  });
}
function truncate(s, n) {
  return codePointLength(s) > n ? [...s].slice(0, n).join('') + '…' : s;
}

// ---------- API ----------
async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(path, opts);
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: 'NETWORK', message: res.statusText }));
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}

// ---------- S-01 ----------
function renderSelectUser() {
  const wrap = document.getElementById('member-btns');
  wrap.innerHTML = '';
  for (const m of state.members) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'member-btn';
    btn.textContent = m.name;
    btn.dataset.memberId = m.id;
    btn.addEventListener('click', () => {
      state.currentUser = m;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(m));
      showSchedule();
    });
    wrap.appendChild(btn);
  }
  setScreen('select-user');
}

// ---------- S-02 ----------
async function showSchedule() {
  setScreen('schedule');
  document.getElementById('current-user-name').textContent = state.currentUser.name;
  if (!state.viewDate) state.viewDate = new Date();
  await loadAndRender();
}

async function loadAndRender() {
  const from = toISODate(state.viewDate);
  const to = toISODate(addDays(state.viewDate, 1));
  document.getElementById('date-heading-left').textContent = '今日 ' + dayLabel(state.viewDate);
  document.getElementById('date-heading-right').textContent = '明日 ' + dayLabel(addDays(state.viewDate, 1));
  const list = await api('GET', `/api/schedules?from=${from}&to=${to}`);
  renderGrid(from, to, list);
}

function renderGrid(from, to, list) {
  const grid = document.getElementById('schedule-grid');
  grid.innerHTML = '';
  for (const m of state.members) {
    const nameCell = document.createElement('div');
    nameCell.className = 'member-name';
    nameCell.textContent = m.name;
    grid.appendChild(nameCell);

    for (const date of [from, to]) {
      const cell = document.createElement('div');
      cell.className = 'schedule-cell';
      cell.dataset.memberId = m.id;
      cell.dataset.date = date;
      const items = list.filter(x => x.memberId === m.id && x.date === date);
      if (items.length === 0) {
        const e = document.createElement('div');
        e.className = 'schedule-none';
        e.textContent = '予定なし';
        cell.appendChild(e);
      } else {
        for (const it of items) {
          const el = document.createElement('div');
          el.className = 'schedule-item';
          el.textContent = it.content;
          el.dataset.id = it.id;
          // FR-23: タップのみ開く。長押し（>300ms）は無視。
          let pressStart = 0;
          el.addEventListener('pointerdown', () => { pressStart = Date.now(); });
          el.addEventListener('pointercancel', () => { pressStart = 0; });
          el.addEventListener('click', (ev) => {
            const dur = pressStart ? Date.now() - pressStart : 0;
            pressStart = 0;
            if (dur > 300) {
              ev.preventDefault();
              ev.stopPropagation();
              return;
            }
            openEdit(it);
          });
          el.addEventListener('contextmenu', (ev) => ev.preventDefault());
          cell.appendChild(el);
        }
      }
      grid.appendChild(cell);
    }
  }
}

// ---------- S-03 / S-04 modal ----------
function openCreate() {
  state.editingId = null;
  document.getElementById('form-title').textContent = '予定を追加';
  document.getElementById('btn-delete').hidden = true;
  state.selectedWho = state.currentUser.id;
  renderWhoBtns();
  document.getElementById('date-input').value = toISODate(state.viewDate);
  document.getElementById('content-input').value = '';
  document.getElementById('error-msg').hidden = true;
  document.getElementById('modal').hidden = false;
}

function openEdit(item) {
  state.editingId = item.id;
  document.getElementById('form-title').textContent = '予定を編集';
  document.getElementById('btn-delete').hidden = false;
  state.selectedWho = item.memberId;
  renderWhoBtns();
  document.getElementById('date-input').value = item.date;
  document.getElementById('content-input').value = item.content;
  document.getElementById('error-msg').hidden = true;
  document.getElementById('modal').hidden = false;
}

function renderWhoBtns() {
  const wrap = document.getElementById('who-btns');
  wrap.innerHTML = '';
  for (const m of state.members) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = 'who-btn' + (state.selectedWho === m.id ? ' selected' : '');
    b.textContent = m.name;
    b.dataset.memberId = m.id;
    b.addEventListener('click', () => {
      state.selectedWho = m.id;
      renderWhoBtns();
    });
    wrap.appendChild(b);
  }
}

function closeModal() {
  document.getElementById('modal').hidden = true;
  state.editingId = null;
}

function showError(msg) {
  const e = document.getElementById('error-msg');
  e.textContent = msg;
  e.hidden = false;
}

async function submitForm(ev) {
  ev.preventDefault();
  const content = document.getElementById('content-input').value;
  const date = document.getElementById('date-input').value;
  if (!content || !content.trim()) {
    showError('内容を入力してください');
    return;
  }
  if (codePointLength(content) > 100) {
    showError('内容は100文字以内で入力してください');
    return;
  }

  const save = document.getElementById('btn-save');
  const origLabel = save.textContent;
  save.disabled = true;
  save.textContent = '保存中...';
  try {
    await commitPendingUndo();
    const body = { memberId: state.selectedWho, date, content: content.trim() };
    if (state.editingId) {
      await api('PUT', `/api/schedules/${state.editingId}`, body);
      showToast('更新しました');
    } else {
      await api('POST', '/api/schedules', body);
      showToast('保存しました');
    }
    closeModal();
    await loadAndRender();
  } catch (err) {
    const msg = err?.fields?.content || err?.fields?.memberId || err?.message || '保存に失敗しました';
    showError(msg);
  } finally {
    save.disabled = false;
    save.textContent = origLabel;
  }
}

// ---------- S-05 delete confirm ----------
function askDelete() {
  const content = document.getElementById('content-input').value;
  const text = `『${truncate(content, 20)}』を削除しますか？`;
  document.getElementById('confirm-text').textContent = text;
  document.getElementById('confirm').hidden = false;
}
function closeConfirm() { document.getElementById('confirm').hidden = true; }
async function doDelete() {
  const id = state.editingId;
  closeConfirm();
  closeModal();
  try {
    await commitPendingUndo();
    await api('DELETE', `/api/schedules/${id}`);
    await loadAndRender();
    showUndoToast(id);
  } catch (err) {
    showToast('削除に失敗しました');
  }
}

// ---------- flick ----------
function enableFlick() {
  const area = document.getElementById('screen-schedule');
  let startX = null, startY = null;
  area.addEventListener('touchstart', e => {
    const t = e.touches[0];
    startX = t.clientX; startY = t.clientY;
  }, { passive: true });
  area.addEventListener('touchend', e => {
    if (startX == null) return;
    const t = e.changedTouches[0];
    const dx = t.clientX - startX;
    const dy = t.clientY - startY;
    if (Math.abs(dx) >= 30 && Math.abs(dy) <= 20) {
      if (dx < 0) state.viewDate = addDays(state.viewDate, 1);
      else state.viewDate = addDays(state.viewDate, -1);
      loadAndRender();
    }
    startX = startY = null;
  });

  // マウスでのフリック（テスト/PC 用）
  let mx = null, my = null, dragging = false;
  area.addEventListener('mousedown', e => {
    if (e.target.closest('.schedule-item') || e.target.closest('button')) return;
    mx = e.clientX; my = e.clientY; dragging = true;
  });
  area.addEventListener('mouseup', e => {
    if (!dragging) return;
    const dx = e.clientX - mx;
    const dy = e.clientY - my;
    if (Math.abs(dx) >= 30 && Math.abs(dy) <= 20) {
      if (dx < 0) state.viewDate = addDays(state.viewDate, 1);
      else state.viewDate = addDays(state.viewDate, -1);
      loadAndRender();
    }
    dragging = false;
  });
}

// ---------- boot ----------
async function boot() {
  state.members = await api('GET', '/api/members');

  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored) {
    try {
      state.currentUser = JSON.parse(stored);
      if (state.currentUser && state.currentUser.id) {
        await showSchedule();
        return;
      }
    } catch {}
  }
  renderSelectUser();
}

document.getElementById('btn-add').addEventListener('click', openCreate);
document.getElementById('btn-cancel').addEventListener('click', closeModal);
document.getElementById('btn-close').addEventListener('click', closeModal);
document.getElementById('btn-delete').addEventListener('click', askDelete);
document.getElementById('btn-no').addEventListener('click', closeConfirm);
document.getElementById('btn-yes').addEventListener('click', doDelete);
document.getElementById('btn-today').addEventListener('click', () => {
  state.viewDate = new Date();
  loadAndRender();
});
document.getElementById('schedule-form').addEventListener('submit', submitForm);

enableFlick();
boot().catch(err => {
  console.error(err);
  document.body.innerHTML = '<p>読み込みに失敗しました</p>';
});
