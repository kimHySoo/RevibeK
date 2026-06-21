import { defineStore } from "pinia"

let idSeq = 0

export const useUiStore = defineStore("ui", {
  state: () => ({
    toasts: [],
  }),
  actions: {
    notify(message, type = "info", timeout = 3000) {
      const id = ++idSeq
      this.toasts.push({ id, message, type })
      setTimeout(() => this.dismiss(id), timeout)
    },
    toast(message, type = "info", timeout = 3000) {
      this.notify(message, type, timeout)
    },
    success(msg) {
      this.notify(msg, "success")
    },
    error(msg) {
      this.notify(msg, "error")
    },
    dismiss(id) {
      this.toasts = this.toasts.filter((t) => t.id !== id)
    },
  },
})
