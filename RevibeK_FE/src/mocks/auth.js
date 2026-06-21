export const mockUser = {
  id: "user-uuid",
  nickname: "리바이브",
  email: "demo@revibek.app",
  joinedAt: "2026-01-12T00:00:00Z",
  preference: {
    favoriteEra: "2세대",
    favoriteGenre: "발라드",
    favoriteMood: "위로",
  },
}

export function buildMockAuth(email) {
  return {
    accessToken: `mock-access-${Date.now()}`,
    refreshToken: `mock-refresh-${Date.now()}`,
    user: { ...mockUser, email: email || mockUser.email },
  }
}
