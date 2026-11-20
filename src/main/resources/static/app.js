(() => {
  'use strict';

  // The access token lives only in this variable. It is never written to the page text,
  // the address bar or any browser storage, and it is gone when the tab closes or on log out.
  let accessToken = null;
  let session = null;            // { name, roles, expiresIn }
  const models = [];             // models registered in this tab, newest first
  const activity = [];           // latest responses, newest first
  const MAX_ACTIVITY = 8;

  const DEV_PASSWORDS = { scientist: 'scientist-dev', lead: 'lead-dev' };
  const STATUS_TEXT = {
    0: 'No response', 200: 'OK', 201: 'Created', 400: 'Bad Request', 401: 'Unauthorized',
    403: 'Forbidden', 404: 'Not Found', 409: 'Conflict', 415: 'Unsupported Media Type',
    500: 'Server Error'
  };
  const NO_MODEL_ID = '00000000-0000-0000-0000-000000000000';

  const $ = (id) => document.getElementById(id);

  // Text from the server is always inserted with textContent, never as HTML.
  function h(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  function newCorrelationId() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
      return window.crypto.randomUUID();
    }
    return 'page-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
  }

  function newBusinessKey() {
    return 'ML-' + Date.now().toString(36).toUpperCase();
  }

  // ---------------------------------------------------------------- HTTP

  async function call(method, path, body, options) {
    const useToken = !(options && options.auth === false) && accessToken !== null;
    const correlationId = newCorrelationId();
    const headers = { 'Accept': 'application/json', 'X-Correlation-Id': correlationId };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (useToken) headers['Authorization'] = 'Bearer ' + accessToken;

    let response;
    try {
      response = await fetch(path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        cache: 'no-store',
        credentials: 'omit'
      });
    } catch (e) {
      return { call: method + ' ' + path, status: 0, data: null, correlationId, networkError: true };
    }

    let data = null;
    try { data = await response.json(); } catch (e) { /* empty or not JSON */ }

    const result = { call: method + ' ' + path, status: response.status, data, correlationId, usedToken: useToken };
    if (response.status === 401 && useToken) {
      // The token we hold was refused (expired or bad). Drop it and go back to the sign-in screen.
      endSession();
      showAlert(result, 'Your session has ended. Sign in again.');
    }
    return result;
  }

  // ---------------------------------------------------------------- views

  function showView(name) {
    $('view-home').hidden = name !== 'home';
    $('view-workspace').hidden = name !== 'workspace';
    window.scrollTo(0, 0);
  }

  function badge(status) {
    return h('span', 'badge ' + String(status).toLowerCase(), status);
  }

  function httpChip(r) {
    const ok = r.status >= 200 && r.status < 300;
    const label = r.status ? ('HTTP ' + r.status + ' ' + (STATUS_TEXT[r.status] || '')).trim() : 'No response';
    return h('span', 'http ' + (ok ? 'ok' : 'err'), label);
  }

  function isError(r) {
    return r.networkError || (r.data && r.data.code);
  }

  // ---------------------------------------------------------------- sign-in alert

  function showAlert(r, heading) {
    const box = $('login-alert');
    box.replaceChildren();
    box.className = 'alert ' + (isError(r) ? 'err' : 'ok');

    if (heading) box.append(h('strong', '', heading));
    if (r.networkError) {
      box.append(h('span', '', 'Could not reach the server.'));
    } else if (r.data && r.data.code) {
      box.append(h('strong', '', r.status + ' · ' + r.data.code));
      box.append(h('span', '', r.data.message));
      box.append(h('span', 'mono', 'correlationId ' + r.data.correlationId));
    } else if (r.data && r.data.username) {
      box.append(h('span', '', 'Signed in as ' + r.data.username + '.'));
    }
    box.hidden = false;
  }

  function clearAlert() {
    const box = $('login-alert');
    box.replaceChildren();
    box.hidden = true;
  }

  // ---------------------------------------------------------------- activity

  function addActivity(r, note) {
    activity.unshift({ r, note, at: new Date() });
    activity.length = Math.min(activity.length, MAX_ACTIVITY);
    renderActivity();
  }

  function renderActivity() {
    const list = $('activity-list');
    list.replaceChildren();
    $('activity-empty').hidden = activity.length > 0;

    activity.forEach(({ r, note, at }) => {
      const li = h('li', 'act');
      const top = h('div', 'act-top');
      top.append(h('span', 'act-call', r.call), httpChip(r), h('span', 'act-time', at.toLocaleTimeString()));
      li.append(top);

      const body = h('div', 'act-body');
      if (r.networkError) {
        body.append(h('span', '', 'Could not reach the server.'));
      } else if (r.data && r.data.code) {
        body.append(h('strong', '', r.data.code), h('span', '', r.data.message),
          h('span', 'mono', 'correlationId ' + r.data.correlationId));
      } else if (r.data && r.data.status) {
        body.append(badge(r.data.status), h('span', '', r.data.name + ' · ' + r.data.businessKey));
      } else if (r.data && r.data.username) {
        body.append(h('span', '', r.data.username + ' · ' + (r.data.authorities || []).join(', ')));
      }
      if (body.childNodes.length > 0) li.append(body);
      if (note) li.append(h('p', 'act-note', note));
      list.append(li);
    });
  }

  // ---------------------------------------------------------------- session

  function roleNames(authorities) {
    return (authorities || []).filter((a) => a.startsWith('ROLE_')).map((a) => a.slice('ROLE_'.length));
  }

  function renderSession() {
    if (!session) return;
    const senior = session.roles.includes('ML_LEAD');
    $('who-name').textContent = session.name;
    const pill = $('who-role');
    pill.textContent = session.roles.join(', ') || 'no role';
    pill.className = 'pill ' + (senior ? 'senior' : session.roles.includes('DATA_SCIENTIST') ? 'junior' : '');
    $('who-avatar').src = senior ? 'img/lead.svg' : 'img/scientist.svg';
    $('who-exp').textContent = 'Session ' + Math.round(session.expiresIn / 60) + ' min (' + session.expiresIn + ' s)';
    $('ws-hint').textContent = senior
      ? 'You can register, evaluate and promote models.'
      : 'You can register and evaluate models. Promoting needs the ML Lead role.';
  }

  function endSession() {
    accessToken = null;
    session = null;
    showView('home');
  }

  async function onLogin(event) {
    event.preventDefault();
    const username = $('username').value;
    const password = $('password').value;
    $('password').value = '';
    clearAlert();

    const login = await call('POST', '/api/v1/auth/login', { username, password }, { auth: false });
    if (login.status !== 200 || !login.data || !login.data.accessToken) {
      // Wrong password, blank field or a failure: no token is kept and the form stays.
      accessToken = null;
      session = null;
      showAlert(login);
      return;
    }

    accessToken = login.data.accessToken;
    const expiresIn = Number(login.data.expiresIn);

    // The role shown comes from the server, from the token it just verified.
    const me = await call('GET', '/api/me');
    if (me.status !== 200 || !me.data) {
      accessToken = null;
      showAlert(me, 'Could not read your profile.');
      return;
    }

    session = { name: me.data.username, roles: roleNames(me.data.authorities), expiresIn };
    addActivity({ call: login.call, status: login.status, data: null, correlationId: login.correlationId },
      'Signed in. Session lasts ' + Math.round(expiresIn / 60) + ' minutes.');
    addActivity(me);
    renderSession();
    renderModels();
    showView('workspace');
  }

  async function probeWithoutToken() {
    // The same kind of call the workspace makes, sent with no token on purpose.
    const path = models.length > 0 ? '/api/v1/models/' + encodeURIComponent(models[0].id) : '/api/me';
    const r = await call('GET', path, undefined, { auth: false });
    showAlert(r, 'Request without a token');
  }

  async function onLogout() {
    endSession();
    $('password').value = '';
    await probeWithoutToken();   // the next call from the page shows the 401
  }

  // ---------------------------------------------------------------- models

  function upsert(model) {
    const entry = { id: model.id, key: model.businessKey, name: model.name, status: model.status };
    const i = models.findIndex((m) => m.id === entry.id);
    if (i >= 0) models[i] = Object.assign(models[i], entry);
    else models.unshift(entry);
    renderModels();
  }

  function renderModels() {
    const body = $('models-body');
    body.replaceChildren();
    $('models-empty').hidden = models.length > 0;
    $('models-table').hidden = models.length === 0;

    models.forEach((m) => {
      const tr = h('tr');

      const name = h('td');
      name.append(h('span', 'm-name', m.name), h('span', 'm-key', m.key));

      const status = h('td');
      status.append(badge(m.status));

      const actions = h('td');
      const box = h('div', 'actions');

      const evaluate = h('button', 'btn ghost small', 'Evaluate');
      evaluate.type = 'button';
      evaluate.addEventListener('click', guarded(() => evaluateModel(m.id)));

      const accuracy = h('input');
      accuracy.type = 'number';
      accuracy.min = '0';
      accuracy.max = '1';
      accuracy.step = '0.01';
      accuracy.value = '0.91';
      accuracy.setAttribute('aria-label', 'Accuracy for ' + m.name);

      const promote = h('button', 'btn lock small', 'Promote');
      promote.type = 'button';
      promote.addEventListener('click', guarded(() => promoteModel(m.id, accuracy.valueAsNumber)));

      const refresh = h('button', 'btn ghost small', 'Refresh');
      refresh.type = 'button';
      refresh.addEventListener('click', guarded(() => readModel(m.id)));

      const promoteBox = h('span', 'promote-box');
      promoteBox.append(accuracy, promote);
      box.append(evaluate, promoteBox, refresh);
      actions.append(box);

      tr.append(name, status, actions);
      body.append(tr);
    });
  }

  const modelPath = (id, suffix) => '/api/v1/models/' + encodeURIComponent(id) + (suffix || '');

  async function onCreate(event) {
    event.preventDefault();
    const r = await call('POST', '/api/v1/models', {
      businessKey: $('key').value,
      name: $('name').value
    });
    addActivity(r);
    if (r.status === 201 && r.data) {
      upsert(r.data);
      $('key').value = newBusinessKey();
    }
  }

  async function readModel(id) {
    const r = await call('GET', modelPath(id));
    addActivity(r);
    if (r.status === 200 && r.data) upsert(r.data);
  }

  async function evaluateModel(id) {
    const r = await call('POST', modelPath(id, '/evaluate'));
    addActivity(r);
    if (r.status === 200 && r.data) upsert(r.data);
  }

  async function promoteModel(id, accuracy) {
    const known = models.find((m) => m.id === id);
    const before = known ? known.status : null;

    // Only the accuracy is sent. Who is asking is decided by the token, not by this body.
    const r = await call('POST', modelPath(id, '/promote'), { accuracy: Number.isNaN(accuracy) ? null : accuracy });

    if (r.status === 200 && r.data) {
      upsert(r.data);
      addActivity(r, (before ? before + ' → ' : 'Now ') + r.data.status);
      return;
    }

    // Refused: read the row again so the page can say whether anything changed.
    let note = null;
    const again = await call('GET', modelPath(id));
    if (again.status === 200 && again.data) {
      note = before && before === again.data.status
        ? 'Nothing changed: status is still ' + again.data.status + '.'
        : 'Status is ' + again.data.status + '.';
      upsert(again.data);
    }
    addActivity(r, note);
  }

  // ---------------------------------------------------------------- wiring

  function fillDemo(event) {
    const who = event.currentTarget.getAttribute('data-fill');
    $('username').value = who;
    $('password').value = DEV_PASSWORDS[who] || '';
    $('password').focus();
  }

  function guarded(fn) {
    return async (event) => {
      const buttons = document.querySelectorAll('button');
      buttons.forEach((b) => { b.disabled = true; });
      try {
        await fn(event);
      } finally {
        document.querySelectorAll('button').forEach((b) => { b.disabled = false; });
      }
    };
  }

  $('login-form').addEventListener('submit', guarded(onLogin));
  $('logout-btn').addEventListener('click', guarded(onLogout));
  $('probe-btn').addEventListener('click', guarded(probeWithoutToken));
  $('create-form').addEventListener('submit', guarded(onCreate));
  document.querySelectorAll('[data-fill]').forEach((b) => b.addEventListener('click', fillDemo));

  $('key').value = newBusinessKey();
  renderModels();
  renderActivity();
  showView('home');
})();
