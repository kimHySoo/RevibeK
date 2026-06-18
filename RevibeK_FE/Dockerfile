# --- build stage ---
FROM node:20-alpine AS build
WORKDIR /app

RUN corepack enable && corepack prepare pnpm@10.34.1 --activate

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .

# VITE_* 변수는 빌드 시점에 코드에 인라인되므로 런타임 -e로는 못 바꿈.
# 기본값을 /api로 고정 — nginx가 /api를 backend로 reverse proxy하므로
# 도메인이 바뀌어도 이미지 재빌드가 필요 없고 CORS도 발생하지 않음.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN pnpm build

# --- run stage ---
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
