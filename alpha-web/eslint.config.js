import eslint from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

export default [
    { ignores: ['dist', 'coverage'] },
    eslint.configs.recommended,
    ...tseslint.configs.recommended,
    ...pluginVue.configs['flat/recommended'],
    {
        files: ['**/*.vue'],
        languageOptions: {
            parserOptions: {
                parser: tseslint.parser,
            },
        },
    },
    {
        languageOptions: {
            globals: {
                window: 'readonly',
                File: 'readonly',
                Blob: 'readonly',
                Event: 'readonly',
                HTMLInputElement: 'readonly',
                URL: 'readonly',
            },
        },
        rules: {
            'vue/max-attributes-per-line': 'off',
            'vue/singleline-html-element-content-newline': 'off',
            'vue/html-indent': 'off',
            'vue/html-closing-bracket-newline': 'off',
            'vue/multiline-html-element-content-newline': 'off',
            'vue/multi-word-component-names': 'off',
            'vue/html-self-closing': 'off',
        },
    },
]
