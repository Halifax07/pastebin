import { VueMonacoEditor } from '@guolao/vue-monaco-editor'

declare module 'vue' {
  export interface GlobalComponents {
    VueMonacoEditor: typeof VueMonacoEditor
  }
}
