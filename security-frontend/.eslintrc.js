module.exports = {
  root: true,
  env: {
    node: true,
    'vue/setup-compiler-macros': true   // 识别 defineProps 等宏
  },
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended'
  ],
  parserOptions: {
    parser: '@babel/eslint-parser'
  },
  rules: {
    // 可以添加自定义规则
  }
}