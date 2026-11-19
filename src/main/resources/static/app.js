(() => {
  'use strict';

  // The access token lives only in this variable. It is never written to the page text,
  // the address bar or any browser storage, and it is gone when the tab closes or on log out.
  let accessToken = null;
  let signedIn = null;      // { name, roles: ['ML_LEAD'], expiresIn, expiresAt }
  let currentModel = null;  // { id, key, name, status }

  const DEV_PASSWORDS = { scientist: 'scientist-dev', lead: 'lead-dev' };
  const STATUS_TEXT = {
    0: 'No response', 200: 'OK', 201: 'Created', 400: 'Bad Request', 401: 'Unauthorized',
    403: 'Forbidden', 404: 'Not Found', 409: 'Conflict', 415: 'Unsupported Media Type',
    500: 'Server Error'
  };

  const $ = (id) => document.getElementById(id);

  // Server text is always put in with textContent, never as HTML.
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

    const result = { call: method + ' ' + path, status: response.status, data, correlationId };
    if (response.status === 401 && useToken) {
      // The token we hold was refused (expired or bad). Drop it and go back to the form.
      endSession('The server answered 401: the token was rejected or has expired. Log in again.');
    }
    return result;
  }

  // ---------------------------------------------------------------- result panel

  function resultHead(r) {
    const head = h('div', 'res-head');
    head.append(h('span', 'res-call', r.call));
    const ok = r.status >= 200 && r.status < 300;
    const label = r.status ? ('HTTP ' + r.status + ' ' + (STATUS_TEXT[r.status] || '')).trim() : 'No response';
    head.append(h('span', 'http ' + (ok ? 'ok' : 'err'), label));
    return head;
  }

  function renderResult(r, notes) {
    const box = $('result');
    box.replaceChildren(resultHead(r));

    const kv = h('dl', 'kv');
    const add = (key, value, cls) => {
      kv.append(h('dt', '', key));
      kv.append(h('dd', cls || '', String(value)));
    };
    const d = r.data;

    if (r.networkError) {
      add('error', 'could not reach the server');
    } else if (d && d.code) {
      // 400, 401, 403, 404, 409: the one error shape of this API.
      add('code', d.code);
      add('message', d.message);
      add('correlationId', d.correlationId);
    } else if (d && d.status) {
      add('status', d.status, 'status-token');
      add('businessKey', d.businessKey);
      add('name', d.name);
      add('id', d.id);
    } else if (d && d.username) {
      add('username', d.username);
      add('authorities', (d.authorities || []).join(', '));
    }
    if (kv.childNodes.length > 0) box.append(kv);

    const sent = h('p', 'muted', 'Sent X-Correlation-Id: ' + r.correlationId);
    box.append(sent);

    (notes || []).forEach((text) => box.append(h('p', 'note', text)));
  }

  function renderPlain(text) {
    $('result').replaceChildren(h('p', 'muted', text));
  }

  // ---------------------------------------------------------------- session

  function roleNames(authorities) {
    return (authorities || [])
      .filter((a) => a.startsWith('ROLE_'))
      .map((a) => a.slice('ROLE_'.length));
  }

  function renderSession(note) {
    const isIn = accessToken !== null && signedIn !== null;
    $('login-form').hidden = isIn;
    $('who').hidden = !isIn;

    if (isIn) {
      $('who-name').textContent = signedIn.name;
      const pill = $('who-role');
      const roles = signedIn.roles.join(', ') || 'no role';
      pill.textContent = roles;
      pill.className = 'pill ' + (signedIn.roles.includes('ML_LEAD') ? 'senior'
        : signedIn.roles.includes('DATA_SCIENTIST') ? 'junior' : '');
      const minutes = Math.round(signedIn.expiresIn / 60);
      $('who-exp').textContent = 'Token lifetime: ' + minutes + ' minutes (' + signedIn.expiresIn
        + ' s), valid until ' + signedIn.expiresAt.toLocaleTimeString() + '.';
    }
    $('session-note').textContent = note || '';
  }

  function endSession(note) {
    accessToken = null;
    signedIn = null;
    renderSession(note);
  }

  async function onLogin(event) {
    event.preventDefault();
    const username = $('username').value;
    const password = $('password').value;
    $('password').value = '';

    const r = await call('POST', '/api/v1/auth/login', { username, password }, { auth: false });

    if (r.status !== 200 || !r.data || !r.data.accessToken) {
      // Wrong password, blank field, or a failure: no token is kept and the form stays.
      accessToken = null;
      signedIn = null;
      renderResult(r);
      renderSession('');
      return;
    }

    accessToken = r.data.accessToken;
    const expiresIn = Number(r.data.expiresIn);
    const tokenType = r.data.tokenType;

    // The role shown on the page comes from the server, from the token it just verified.
    const me = await call('GET', '/api/me');
    if (me.status !== 200 || !me.data) {
      accessToken = null;
      renderResult(me);
      renderSession('Could not read who you are from /api/me. Not signed in.');
      return;
    }

    signedIn = {
      name: me.data.username,
      roles: roleNames(me.data.authorities),
      expiresIn,
      expiresAt: new Date(Date.now() + expiresIn * 1000)
    };

    // Show the login outcome without the token itself.
    const shown = {
      call: r.call, status: r.status, correlationId: r.correlationId,
      data: null
    };
    renderResult(shown, [
      'Token received (' + tokenType + ', expiresIn ' + expiresIn + ' = ' + Math.round(expiresIn / 60)
        + ' minutes). It is kept in memory and is not shown.',
      'GET /api/me → HTTP ' + me.status + ': ' + signedIn.name + ' is ' + (signedIn.roles.join(', ') || 'no role') + '.'
    ]);
    renderSession('');
  }

  function onLogout() {
    endSession('Logged out. The token variable is cleared. The next API call from this page has no token.');
    renderPlain('Logged out. Run an API call below: it will be 401.');
  }

  // ---------------------------------------------------------------- models

  function setCurrent(model) {
    currentModel = { id: model.id, key: model.businessKey, name: model.name, status: model.status };
    $('model-id').value = model.id;
    $('current').textContent = 'Current model: ' + model.businessKey + ' · status ' + model.status;
  }

  function modelPath(suffix) {
    const id = $('model-id').value.trim();
    if (!id) return null;
    return '/api/v1/models/' + encodeURIComponent(id) + (suffix || '');
  }

  function needModel() {
    renderPlain('Register a model first (section 2), then evaluate it.');
  }

  async function onCreate(event) {
    event.preventDefault();
    const key = $('key').value;
    const name = $('name').value;
    const r = await call('POST', '/api/v1/models', { businessKey: key, name });
    renderResult(r);
    if (r.status === 201 && r.data) {
      setCurrent(r.data);
      $('key').value = newBusinessKey();
    }
  }

  async function onRead() {
    const path = modelPath('');
    if (!path) return needModel();
    const r = await call('GET', path);
    renderResult(r);
    if (r.status === 200 && r.data) setCurrent(r.data);
  }

  async function onEvaluate() {
    const path = modelPath('/evaluate');
    if (!path) return needModel();
    const r = await call('POST', path);
    renderResult(r);
    if (r.status === 200 && r.data) setCurrent(r.data);
  }

  async function onPromote() {
    const path = modelPath('/promote');
    if (!path) return needModel();

    const id = $('model-id').value.trim();
    const before = currentModel && currentModel.id === id ? currentModel.status : null;
    const accuracy = $('accuracy').valueAsNumber;

    // Only the accuracy is sent. Who is asking is decided by the token, not by this body.
    const r = await call('POST', path, { accuracy: Number.isNaN(accuracy) ? null : accuracy });

    if (r.status === 200 && r.data) {
      setCurrent(r.data);
      renderResult(r, [
        'Promoted' + (before ? ': status ' + before + ' → ' + r.data.status : ': status is now ' + r.data.status) + '.'
      ]);
      return;
    }

    // Refused. Read the row again to show whether anything changed.
    const notes = [];
    const again = await call('GET', '/api/v1/models/' + encodeURIComponent(id));
    if (again.status === 200 && again.data) {
      const after = again.data.status;
      notes.push(before && before === after
        ? 'The row did not change: status is still ' + after + '.'
        : 'Checked with GET: status is ' + after + '.');
      setCurrent(again.data);
    }
    renderResult(r, notes);
  }

  // ---------------------------------------------------------------- wiring

  function fillDev(event) {
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
        buttons.forEach((b) => { b.disabled = false; });
      }
    };
  }

  $('login-form').addEventListener('submit', guarded(onLogin));
  $('logout-btn').addEventListener('click', onLogout);
  $('create-form').addEventListener('submit', guarded(onCreate));
  $('read-btn').addEventListener('click', guarded(onRead));
  $('evaluate-btn').addEventListener('click', guarded(onEvaluate));
  $('promote-btn').addEventListener('click', guarded(onPromote));
  document.querySelectorAll('[data-fill]').forEach((b) => b.addEventListener('click', fillDev));

  $('key').value = newBusinessKey();
  renderSession('');
})();
