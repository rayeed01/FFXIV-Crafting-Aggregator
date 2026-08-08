/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Absolute API origin. Leave unset in dev so requests go through the Vite proxy. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
