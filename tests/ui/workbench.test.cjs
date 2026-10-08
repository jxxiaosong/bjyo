const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../bjyo-admin/src/main/resources/static/bjyo/js/bjyo-workbench.js'), 'utf8');
function fixture(inFrame = true, hasTabs = true) {
  const handlers = {}, calls = [];
  const window = { frameElement: inFrame ? {} : null };
  if (hasTabs) window.createMenuItem = (...args) => calls.push(args);
  const document = { addEventListener: (name, fn) => handlers[name] = fn };
  vm.runInNewContext(source, { window, document });
  const link = { href: 'http://localhost/bjyo/system/user', getAttribute: key => ({ href: '/bjyo/system/user', 'data-bjyo-tab': '用户管理' })[key] };
  function click(extra = {}, matching = true) {
    const event = { target: { closest: () => matching ? link : null }, button: 0, defaultPrevented: false,
      preventDefault() { this.defaultPrevented = true; }, ...extra };
    if (handlers.click) handlers.click(event);
    return event;
  }
  return { calls, click };
}
test('工作台入口在 iframe 中复用已有页签，保留应用上下文路径', () => {
  const f = fixture();
  assert.equal(f.click().defaultPrevented, true);
  assert.deepEqual(f.calls, [['/bjyo/system/user', '用户管理']]);
});
test('直接访问工作台时保留普通链接导航', () => {
  const f = fixture(false);
  assert.equal(f.click().defaultPrevented, false);
  assert.equal(f.calls.length, 0);
});
test('缺少页签接口时仍可正常导航', () => {
  const f = fixture(true, false);
  assert.equal(f.click().defaultPrevented, false);
});
test('修饰键和非左键保持浏览器默认行为', () => {
  const f = fixture();
  for (const extra of [{ ctrlKey: true }, { metaKey: true }, { shiftKey: true }, { altKey: true }, { button: 1 }, { defaultPrevented: true }]) f.click(extra);
  assert.equal(f.calls.length, 0);
});
test('点击其他元素不触发工作台导航', () => {
  const f = fixture();
  assert.equal(f.click({}, false).defaultPrevented, false);
  assert.equal(f.calls.length, 0);
});
