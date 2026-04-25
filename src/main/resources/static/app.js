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
          el.dataset.id = it.id;

          const textSpan = document.createElement('span');
          textSpan.className = 'schedule-item-text';
          textSpan.textContent = it.content;
          el.appendChild(textSpan);

          const delBtn = document.createElement('button');
          delBtn.type = 'button';
          delBtn.className = 'schedule-item-delete';
          delBtn.textContent = '✕';
          delBtn.setAttribute('aria-label', '削除');
          delBtn.addEventListener('click', (ev) => {
            ev.stopPropagation();
            if (el.dataset.editing && el._inlineCancel) {
              el._inlineCancel();
            }
            inlineDelete(it);
          });
          el.appendChild(delBtn);

          // FR-23: タップのみ開く。長押し（>300ms）は無視。
          let pressStart = 0;
          el.addEventListener('pointerdown', (ev) => {
            if (ev.target === delBtn) return;
            pressStart = Date.now();
          });
          el.addEventListener('pointercancel', () => { pressStart = 0; });
          el.addEventListener('click', (ev) => {
            if (ev.target === delBtn) return;
            const dur = pressStart ? Date.now() - pressStart : 0;
            pressStart = 0;
            if (dur > 300) {
              ev.preventDefault();
              ev.stopPropagation();
              return;
            }
            startInlineEdit(el, it);
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

function startInlineEdit(el, item) {
  if (el.dataset.editing) return;
  el.dataset.editing = '1';

  const textSpan = el.querySelector('.schedule-item-text');
  const input = document.createElement('textarea');
  input.className = 'schedule-item-input';
  input.value = item.content;
  el.classList.add('schedule-item-editing');
  textSpan.hidden = true;
  el.insertBefore(input, textSpan);

  const autoResize = () => {
    input.style.height = 'auto';
    input.style.height = input.scrollHeight + 'px';
  };
  input.addEventListener('input', autoResize);
  requestAnimationFrame(autoResize);
  input.focus();
  input.select();

  let done = false;

  const cancel = () => {
    if (done) return;
    done = true;
    delete el.dataset.editing;
    delete el._inlineCancel;
    el.classList.remove('schedule-item-editing');
    input.remove();
    textSpan.hidden = false;
  };
  el._inlineCancel = cancel;

  const save = async () => {
    if (done) return;
    const newContent = input.value.trim();
    if (!newContent || newContent === item.content) { cancel(); return; }
    if (codePointLength(newContent) > 100) {
      showToast('100文字以内で入力してください');
      input.focus();
      return;
    }
    done = true;
    delete el._inlineCancel;
    try {
      await commitPendingUndo();
      await api('PUT', `/api/schedules/${item.id}`, { memberId: item.memberId, date: item.date, content: newContent });
      showToast('更新しました');
      await loadAndRender();
    } catch {
      showToast('保存に失敗しました');
      done = false;
      cancel();
    }
  };

  input.addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); save(); }
    if (e.key === 'Escape') { cancel(); }
  });
  input.addEventListener('blur', () => setTimeout(save, 150));
}

async function inlineDelete(item) {
  try {
    await commitPendingUndo();
    await api('DELETE', `/api/schedules/${item.id}`);
    await loadAndRender();
    showUndoToast(item.id);
  } catch {
    showToast('削除に失敗しました');
  }
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
document.getElementById('btn-switch-user').addEventListener('click', () => {
  state.currentUser = null;
  localStorage.removeItem(STORAGE_KEY);
  renderSelectUser();
});
document.getElementById('btn-member-settings').addEventListener('click', openMemberModal);
document.getElementById('btn-member-modal-close').addEventListener('click', closeMemberModal);
document.getElementById('btn-member-add').addEventListener('click', addMember);
document.getElementById('member-add-input').addEventListener('keydown', e => {
  if (e.key === 'Enter') addMember();
});

// ---------- S-06 メンバー管理 ----------

function openMemberModal() {
  renderMemberManageList();
  document.getElementById('member-add-input').value = '';
  hideMemberError();
  document.getElementById('member-modal').hidden = false;
  const addSection = document.getElementById('member-add-section');
  addSection.hidden = state.members.length >= 10;
}

function closeMemberModal() {
  document.getElementById('member-modal').hidden = true;
}

function showMemberError(msg) {
  const el = document.getElementById('member-modal-error');
  el.textContent = msg;
  el.hidden = false;
}

function hideMemberError() {
  document.getElementById('member-modal-error').hidden = true;
}

function renderMemberManageList() {
  const ul = document.getElementById('member-manage-list');
  ul.innerHTML = '';
  const addSection = document.getElementById('member-add-section');
  addSection.hidden = state.members.length >= 10;

  for (const m of state.members) {
    const li = document.createElement('li');
    li.className = 'member-manage-item';
    li.dataset.id = m.id;

    const nameSpan = document.createElement('span');
    nameSpan.className = 'member-manage-name';
    nameSpan.textContent = m.name;

    const renameBtn = document.createElement('button');
    renameBtn.type = 'button';
    renameBtn.className = 'btn btn-ghost btn-sm';
    renameBtn.textContent = '変更';
    renameBtn.addEventListener('click', () => startRename(li, nameSpan, m));

    const deleteBtn = document.createElement('button');
    deleteBtn.type = 'button';
    deleteBtn.className = 'btn btn-danger btn-sm';
    deleteBtn.textContent = '削除';
    deleteBtn.addEventListener('click', () => confirmDeleteMember(m));

    li.appendChild(nameSpan);
    li.appendChild(renameBtn);
    li.appendChild(deleteBtn);
    ul.appendChild(li);
  }
}

function startRename(li, nameSpan, m) {
  if (li.querySelector('.member-rename-input')) return; // 既に編集中

  const input = document.createElement('input');
  input.type = 'text';
  input.className = 'member-rename-input';
  input.value = m.name;
  input.maxLength = 20;

  nameSpan.hidden = true;
  li.insertBefore(input, nameSpan);
  input.focus();
  input.select();

  const save = async () => {
    const newName = input.value.trim();
    if (!newName || newName === m.name) {
      nameSpan.hidden = false;
      input.remove();
      return;
    }
    try {
      await api('PUT', `/api/members/${m.id}`, { name: newName });
      state.members = await api('GET', '/api/members');
      renderMemberManageList();
      renderSelectUser();
      if (!document.getElementById('screen-schedule').hidden) {
        renderGrid(
          toISODate(state.viewDate),
          toISODate(addDays(state.viewDate, 1)),
          await api('GET', `/api/schedules?from=${toISODate(state.viewDate)}&to=${toISODate(addDays(state.viewDate, 1))}`)
        );
        document.getElementById('current-user-name').textContent =
          state.members.find(x => x.id === state.currentUser?.id)?.name ?? state.currentUser?.name ?? '';
      }
      hideMemberError();
    } catch (err) {
      showMemberError(err?.fields?.name || err?.message || '変更に失敗しました');
      nameSpan.hidden = false;
      input.remove();
    }
  };

  input.addEventListener('keydown', e => {
    if (e.key === 'Enter') { e.preventDefault(); save(); }
    if (e.key === 'Escape') { nameSpan.hidden = false; input.remove(); }
  });
  input.addEventListener('blur', save);
}

async function confirmDeleteMember(m) {
  if (!confirm(`「${m.name}」を削除しますか？\n※予定が残っている場合は削除できません。`)) return;
  try {
    await api('DELETE', `/api/members/${m.id}`);
    state.members = await api('GET', '/api/members');
    renderMemberManageList();
    renderSelectUser();
    if (state.currentUser && state.currentUser.id === m.id) {
      state.currentUser = null;
      localStorage.removeItem(STORAGE_KEY);
      closeMemberModal();
      renderSelectUser();
      setScreen('select-user');
    }
    hideMemberError();
  } catch (err) {
    showMemberError(err?.message || 'このメンバーには予定が登録されています。先に予定を削除してください。');
  }
}

async function addMember() {
  const input = document.getElementById('member-add-input');
  const name = input.value.trim();
  if (!name) { showMemberError('名前を入力してください'); return; }
  try {
    await api('POST', '/api/members', { name });
    state.members = await api('GET', '/api/members');
    input.value = '';
    renderMemberManageList();
    renderSelectUser();
    hideMemberError();
  } catch (err) {
    showMemberError(err?.fields?.name || err?.message || '追加に失敗しました');
  }
}

enableFlick();
boot().catch(err => {
  console.error(err);
  document.body.innerHTML = '<p>読み込みに失敗しました</p>';
});
