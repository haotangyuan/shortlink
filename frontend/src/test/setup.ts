import "@testing-library/jest-dom/vitest";

const memoryStorage = new Map<string, string>();

Object.defineProperty(globalThis, "localStorage", {
  configurable: true,
  value: {
    getItem: (key: string) => memoryStorage.get(key) ?? null,
    setItem: (key: string, value: string) => memoryStorage.set(key, value),
    removeItem: (key: string) => memoryStorage.delete(key),
    clear: () => memoryStorage.clear(),
    key: (index: number) => Array.from(memoryStorage.keys())[index] ?? null,
    get length() {
      return memoryStorage.size;
    },
  },
});
