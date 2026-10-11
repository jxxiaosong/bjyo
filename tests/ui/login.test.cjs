const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../bjyo-admin/src/main/resources/static/bjyo/login.js'), 'utf8');

function fixture(enabled = true) {
  const requests = [], messages = [];
  let opened = 0, reset = 0, proof = null;
  const control = { open: () => opened++, reset: () => { reset++; proof = null; }, getProof: () => proof };
  const values = { "input[name='username']": ' admin ', "input[name='password']": ' secret ', "input[name='validateCode']": '' };
  const $ = arg => typeof arg === 'function' ? arg() : ({
    val: () => values[arg], is: () => false, data: () => 'loading',
    validate: () => {}, click: () => {}, attr: () => {}
  });
  $.common = { trim: value => value.trim(), isEmpty: value => !value };
  $.modal = { msg: message => messages.push(message), loading: () => {}, closeLoading: () => {} };
  $.ajax = options => requests.push(options);
  const context = { $, ctx: '/app/', captchaEnabled: enabled, captchaType: 'math',
    window: { location: { search: '' }, BJYOSliderCaptcha: function () { return control; } },
    document: {}, web_status: { SUCCESS: 0 }, location: {}, layer: {} };
  vm.runInNewContext(source, context);
  return { context, requests, messages, setProof: value => proof = value,
    get opened() { return opened; }, get reset() { return reset; } };
}

test('未通过拼图时打开安全验证，不提交密码', () => {
  const f = fixture(); f.context.login();
  assert.equal(f.opened, 1); assert.equal(f.requests.length, 0);
});
test('通过后携带服务端凭证和挑战编号，保留上下文路径', () => {
  const f = fixture(); f.setProof({ uuid: 'slider_' + 'a'.repeat(32), code: 'b'.repeat(32) });
  f.context.login(); assert.equal(f.requests.length, 1);
  const request = f.requests[0]; assert.equal(request.url, '/app/login');
  assert.equal(request.data.uuid, 'slider_' + 'a'.repeat(32));
  assert.equal(request.data.validateCode, 'b'.repeat(32));
  assert.equal(request.data.username, 'admin');
});
test('登录失败和网络错误均清除一次性凭证并恢复按钮', () => {
  for (const networkError of [false, true]) {
    const f = fixture(); f.setProof({ uuid: 'slider_' + 'a'.repeat(32), code: 'b'.repeat(32) });
    f.context.login(); const request = f.requests[0];
    if (networkError) request.error(); else request.success({ code: 500, msg: '密码错误' });
    request.complete(); assert.equal(f.reset, 1);
    f.context.login(); assert.equal(f.opened, 1);
  }
});
test('禁用验证码仍可登录，连续提交只发起一次请求', () => {
  const f = fixture(false); f.context.login(); f.context.login();
  assert.equal(f.requests.length, 1); assert.equal(f.opened, 0);
});
